package com.android.server.pm;

import android.content.Intent;
import android.content.pm.InstantAppInfo;
import android.content.pm.PackageParser.Package;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.ByteStringUtils;
import android.util.PackageUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class InstantAppRegistry {
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_LABEL = "label";
    private static final String ATTR_NAME = "name";
    private static final boolean DEBUG = false;
    private static final long DEFAULT_INSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_INSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final long DEFAULT_UNINSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_UNINSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final String INSTANT_APPS_FOLDER = "instant";
    private static final String INSTANT_APP_ANDROID_ID_FILE = "android_id";
    private static final String INSTANT_APP_COOKIE_FILE_PREFIX = "cookie_";
    private static final String INSTANT_APP_COOKIE_FILE_SIFFIX = ".dat";
    private static final String INSTANT_APP_ICON_FILE = "icon.png";
    private static final String INSTANT_APP_METADATA_FILE = "metadata.xml";
    private static final String LOG_TAG = "InstantAppRegistry";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final CookiePersistence mCookiePersistence = new CookiePersistence(BackgroundThread.getHandler().getLooper());
    @GuardedBy("mService.mPackages")
    private SparseArray<SparseBooleanArray> mInstalledInstantAppUids;
    @GuardedBy("mService.mPackages")
    private SparseArray<SparseArray<SparseBooleanArray>> mInstantGrants;
    private final PackageManagerService mService;
    @GuardedBy("mService.mPackages")
    private SparseArray<List<UninstalledInstantAppState>> mUninstalledInstantApps;

    private final class CookiePersistence extends Handler {
        private static final long PERSIST_COOKIE_DELAY_MILLIS = 1000;
        private final SparseArray<ArrayMap<Package, SomeArgs>> mPendingPersistCookies = new SparseArray();

        public CookiePersistence(Looper looper) {
            super(looper);
        }

        public void schedulePersistLPw(int userId, Package pkg, byte[] cookie) {
            File newCookieFile = InstantAppRegistry.computeInstantCookieFile(pkg.packageName, PackageUtils.computeSignaturesSha256Digest(pkg.mSignatures), userId);
            if (pkg.mSignatures.length > 0) {
                File oldCookieFile = InstantAppRegistry.peekInstantCookieFile(pkg.packageName, userId);
                if (!(oldCookieFile == null || (newCookieFile.equals(oldCookieFile) ^ 1) == 0)) {
                    oldCookieFile.delete();
                }
            }
            cancelPendingPersistLPw(pkg, userId);
            addPendingPersistCookieLPw(userId, pkg, cookie, newCookieFile);
            sendMessageDelayed(obtainMessage(userId, pkg), 1000);
        }

        public byte[] getPendingPersistCookieLPr(Package pkg, int userId) {
            ArrayMap<Package, SomeArgs> pendingWorkForUser = (ArrayMap) this.mPendingPersistCookies.get(userId);
            if (pendingWorkForUser != null) {
                SomeArgs state = (SomeArgs) pendingWorkForUser.get(pkg);
                if (state != null) {
                    return (byte[]) state.arg1;
                }
            }
            return null;
        }

        public void cancelPendingPersistLPw(Package pkg, int userId) {
            removeMessages(userId, pkg);
            SomeArgs state = removePendingPersistCookieLPr(pkg, userId);
            if (state != null) {
                state.recycle();
            }
        }

        private void addPendingPersistCookieLPw(int userId, Package pkg, byte[] cookie, File cookieFile) {
            ArrayMap<Package, SomeArgs> pendingWorkForUser = (ArrayMap) this.mPendingPersistCookies.get(userId);
            if (pendingWorkForUser == null) {
                pendingWorkForUser = new ArrayMap();
                this.mPendingPersistCookies.put(userId, pendingWorkForUser);
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = cookie;
            args.arg2 = cookieFile;
            pendingWorkForUser.put(pkg, args);
        }

        private SomeArgs removePendingPersistCookieLPr(Package pkg, int userId) {
            ArrayMap<Package, SomeArgs> pendingWorkForUser = (ArrayMap) this.mPendingPersistCookies.get(userId);
            SomeArgs state = null;
            if (pendingWorkForUser != null) {
                state = (SomeArgs) pendingWorkForUser.remove(pkg);
                if (pendingWorkForUser.isEmpty()) {
                    this.mPendingPersistCookies.remove(userId);
                }
            }
            return state;
        }

        public void handleMessage(Message message) {
            int userId = message.what;
            Package pkg = message.obj;
            SomeArgs state = removePendingPersistCookieLPr(pkg, userId);
            if (state != null) {
                byte[] cookie = state.arg1;
                File cookieFile = state.arg2;
                state.recycle();
                InstantAppRegistry.this.persistInstantApplicationCookie(cookie, pkg.packageName, cookieFile, userId);
            }
        }
    }

    private static final class UninstalledInstantAppState {
        final InstantAppInfo mInstantAppInfo;
        final long mTimestamp;

        public UninstalledInstantAppState(InstantAppInfo instantApp, long timestamp) {
            this.mInstantAppInfo = instantApp;
            this.mTimestamp = timestamp;
        }
    }

    public InstantAppRegistry(PackageManagerService service) {
        this.mService = service;
    }

    public byte[] getInstantAppCookieLPw(String packageName, int userId) {
        Package pkg = (Package) this.mService.mPackages.get(packageName);
        if (pkg == null) {
            return null;
        }
        byte[] pendingCookie = this.mCookiePersistence.getPendingPersistCookieLPr(pkg, userId);
        if (pendingCookie != null) {
            return pendingCookie;
        }
        File cookieFile = peekInstantCookieFile(packageName, userId);
        if (cookieFile != null && cookieFile.exists()) {
            try {
                return IoUtils.readFileAsByteArray(cookieFile.toString());
            } catch (IOException e) {
                Slog.w(LOG_TAG, "Error reading cookie file: " + cookieFile);
            }
        }
        return null;
    }

    public boolean setInstantAppCookieLPw(String packageName, byte[] cookie, int userId) {
        if (cookie != null && cookie.length > 0) {
            int maxCookieSize = this.mService.mContext.getPackageManager().getInstantAppCookieMaxBytes();
            if (cookie.length > maxCookieSize) {
                Slog.e(LOG_TAG, "Instant app cookie for package " + packageName + " size " + cookie.length + " bytes while max size is " + maxCookieSize);
                return false;
            }
        }
        Package pkg = (Package) this.mService.mPackages.get(packageName);
        if (pkg == null) {
            return false;
        }
        this.mCookiePersistence.schedulePersistLPw(userId, pkg, cookie);
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void persistInstantApplicationCookie(byte[] cookie, String packageName, File cookieFile, int userId) {
        Throwable th = null;
        synchronized (this.mService.mPackages) {
            File appDir = getInstantApplicationDir(packageName, userId);
            if (appDir.exists() || (appDir.mkdirs() ^ 1) == 0) {
                if (cookieFile.exists() && (cookieFile.delete() ^ 1) != 0) {
                    Slog.e(LOG_TAG, "Cannot delete instant app cookie file");
                }
                if (cookie == null || cookie.length <= 0) {
                }
            } else {
                Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
                return;
            }
        }
        if (th != null) {
            try {
                throw th;
            } catch (IOException e) {
                IOException e2 = e;
                FileOutputStream fileOutputStream = fos;
            }
        }
    }

    public Bitmap getInstantAppIconLPw(String packageName, int userId) {
        File iconFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ICON_FILE);
        if (iconFile.exists()) {
            return BitmapFactory.decodeFile(iconFile.toString());
        }
        return null;
    }

    public String getInstantAppAndroidIdLPw(String packageName, int userId) {
        File idFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ANDROID_ID_FILE);
        if (idFile.exists()) {
            try {
                return IoUtils.readFileAsString(idFile.getAbsolutePath());
            } catch (IOException e) {
                Slog.e(LOG_TAG, "Failed to read instant app android id file: " + idFile, e);
            }
        }
        return generateInstantAppAndroidIdLPw(packageName, userId);
    }

    private String generateInstantAppAndroidIdLPw(String packageName, int userId) {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        byte[] randomBytes = new byte[8];
        new SecureRandom().nextBytes(randomBytes);
        String id = ByteStringUtils.toHexString(randomBytes).toLowerCase(Locale.US);
        File appDir = getInstantApplicationDir(packageName, userId);
        if (appDir.exists() || (appDir.mkdirs() ^ 1) == 0) {
            File idFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ANDROID_ID_FILE);
            FileOutputStream fos = null;
            try {
                FileOutputStream fos2 = new FileOutputStream(idFile);
                try {
                    fos2.write(id.getBytes());
                    if (fos2 != null) {
                        try {
                            fos2.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        try {
                            throw th2;
                        } catch (IOException e2) {
                            e = e2;
                            fos = fos2;
                        }
                    } else {
                        return id;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    fos = fos2;
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        try {
                            throw th2;
                        } catch (IOException e3) {
                            e = e3;
                            Slog.e(LOG_TAG, "Error writing instant app android id file: " + idFile, e);
                            return id;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Throwable th52) {
                        if (th2 == null) {
                            th2 = th52;
                        } else if (th2 != th52) {
                            th2.addSuppressed(th52);
                        }
                    }
                }
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (IOException e32) {
                    e = e32;
                    Slog.e(LOG_TAG, "Error writing instant app android id file: " + idFile, e);
                    return id;
                }
            }
        }
        Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
        return id;
    }

    public List<InstantAppInfo> getInstantAppsLPr(int userId) {
        List<InstantAppInfo> installedApps = getInstalledInstantApplicationsLPr(userId);
        List<InstantAppInfo> uninstalledApps = getUninstalledInstantApplicationsLPr(userId);
        if (installedApps == null) {
            return uninstalledApps;
        }
        if (uninstalledApps != null) {
            installedApps.addAll(uninstalledApps);
        }
        return installedApps;
    }

    public void onPackageInstalledLPw(Package pkg, int[] userIds) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            for (int userId : userIds) {
                if (this.mService.mPackages.get(pkg.packageName) != null && (ps.getInstalled(userId) ^ 1) == 0) {
                    propagateInstantAppPermissionsIfNeeded(pkg.packageName, userId);
                    if (ps.getInstantApp(userId)) {
                        addInstantAppLPw(userId, ps.appId);
                    }
                    removeUninstalledInstantAppStateLPw(new -$Lambda$KFbchFEqJgs_hY1HweauKRNA_ds((byte) 1, pkg), userId);
                    File instantAppDir = getInstantApplicationDir(pkg.packageName, userId);
                    new File(instantAppDir, INSTANT_APP_METADATA_FILE).delete();
                    new File(instantAppDir, INSTANT_APP_ICON_FILE).delete();
                    File currentCookieFile = peekInstantCookieFile(pkg.packageName, userId);
                    if (currentCookieFile != null) {
                        String[] signaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(pkg.mSignatures);
                        if (!currentCookieFile.equals(computeInstantCookieFile(pkg.packageName, PackageUtils.computeSignaturesSha256Digest(signaturesSha256Digests), userId))) {
                            if (pkg.mSignatures.length <= 1 || !currentCookieFile.equals(computeInstantCookieFile(pkg.packageName, signaturesSha256Digests[0], userId))) {
                                Slog.i(LOG_TAG, "Signature for package " + pkg.packageName + " changed - dropping cookie");
                                this.mCookiePersistence.cancelPendingPersistLPw(pkg, userId);
                                currentCookieFile.delete();
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                    continue;
                }
            }
        }
    }

    public void onPackageUninstalledLPw(Package pkg, int[] userIds) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            for (int userId : userIds) {
                if (this.mService.mPackages.get(pkg.packageName) == null || !ps.getInstalled(userId)) {
                    if (ps.getInstantApp(userId)) {
                        addUninstalledInstantAppLPw(pkg, userId);
                        removeInstantAppLPw(userId, ps.appId);
                    } else {
                        deleteDir(getInstantApplicationDir(pkg.packageName, userId));
                        this.mCookiePersistence.cancelPendingPersistLPw(pkg, userId);
                        removeAppLPw(userId, ps.appId);
                    }
                }
            }
        }
    }

    public void onUserRemovedLPw(int userId) {
        if (this.mUninstalledInstantApps != null) {
            this.mUninstalledInstantApps.remove(userId);
            if (this.mUninstalledInstantApps.size() <= 0) {
                this.mUninstalledInstantApps = null;
            }
        }
        if (this.mInstalledInstantAppUids != null) {
            this.mInstalledInstantAppUids.remove(userId);
            if (this.mInstalledInstantAppUids.size() <= 0) {
                this.mInstalledInstantAppUids = null;
            }
        }
        if (this.mInstantGrants != null) {
            this.mInstantGrants.remove(userId);
            if (this.mInstantGrants.size() <= 0) {
                this.mInstantGrants = null;
            }
        }
        deleteDir(getInstantApplicationsDir(userId));
    }

    public boolean isInstantAccessGranted(int userId, int targetAppId, int instantAppId) {
        if (this.mInstantGrants == null) {
            return false;
        }
        SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
        if (targetAppList == null) {
            return false;
        }
        SparseBooleanArray instantGrantList = (SparseBooleanArray) targetAppList.get(targetAppId);
        if (instantGrantList == null) {
            return false;
        }
        return instantGrantList.get(instantAppId);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void grantInstantAccessLPw(int userId, Intent intent, int targetAppId, int instantAppId) {
        if (this.mInstalledInstantAppUids != null) {
            SparseBooleanArray instantAppList = (SparseBooleanArray) this.mInstalledInstantAppUids.get(userId);
            if (instantAppList != null && (instantAppList.get(instantAppId) ^ 1) == 0 && !instantAppList.get(targetAppId)) {
                if (intent != null && "android.intent.action.VIEW".equals(intent.getAction())) {
                    Set<String> categories = intent.getCategories();
                    if (categories != null && categories.contains("android.intent.category.BROWSABLE")) {
                        return;
                    }
                }
                if (this.mInstantGrants == null) {
                    this.mInstantGrants = new SparseArray();
                }
                SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
                if (targetAppList == null) {
                    targetAppList = new SparseArray();
                    this.mInstantGrants.put(userId, targetAppList);
                }
                SparseBooleanArray instantGrantList = (SparseBooleanArray) targetAppList.get(targetAppId);
                if (instantGrantList == null) {
                    instantGrantList = new SparseBooleanArray();
                    targetAppList.put(targetAppId, instantGrantList);
                }
                instantGrantList.put(instantAppId, true);
            }
        }
    }

    public void addInstantAppLPw(int userId, int instantAppId) {
        if (this.mInstalledInstantAppUids == null) {
            this.mInstalledInstantAppUids = new SparseArray();
        }
        SparseBooleanArray instantAppList = (SparseBooleanArray) this.mInstalledInstantAppUids.get(userId);
        if (instantAppList == null) {
            instantAppList = new SparseBooleanArray();
            this.mInstalledInstantAppUids.put(userId, instantAppList);
        }
        instantAppList.put(instantAppId, true);
    }

    private void removeInstantAppLPw(int userId, int instantAppId) {
        if (this.mInstalledInstantAppUids != null) {
            SparseBooleanArray instantAppList = (SparseBooleanArray) this.mInstalledInstantAppUids.get(userId);
            if (instantAppList != null) {
                instantAppList.delete(instantAppId);
                if (this.mInstantGrants != null) {
                    SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
                    if (targetAppList != null) {
                        for (int i = targetAppList.size() - 1; i >= 0; i--) {
                            ((SparseBooleanArray) targetAppList.valueAt(i)).delete(instantAppId);
                        }
                    }
                }
            }
        }
    }

    private void removeAppLPw(int userId, int targetAppId) {
        if (this.mInstantGrants != null) {
            SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
            if (targetAppList != null) {
                targetAppList.delete(targetAppId);
            }
        }
    }

    private void addUninstalledInstantAppLPw(Package pkg, int userId) {
        InstantAppInfo uninstalledApp = createInstantAppInfoForPackage(pkg, userId, false);
        if (uninstalledApp != null) {
            if (this.mUninstalledInstantApps == null) {
                this.mUninstalledInstantApps = new SparseArray();
            }
            List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates == null) {
                uninstalledAppStates = new ArrayList();
                this.mUninstalledInstantApps.put(userId, uninstalledAppStates);
            }
            uninstalledAppStates.add(new UninstalledInstantAppState(uninstalledApp, System.currentTimeMillis()));
            writeUninstalledInstantAppMetadata(uninstalledApp, userId);
            writeInstantApplicationIconLPw(pkg, userId);
        }
    }

    private void writeInstantApplicationIconLPw(Package pkg, int userId) {
        Exception e;
        Throwable th;
        if (getInstantApplicationDir(pkg.packageName, userId).exists()) {
            Bitmap bitmap;
            Drawable icon = pkg.applicationInfo.loadIcon(this.mService.mContext.getPackageManager());
            if (icon instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) icon).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                icon.draw(canvas);
            }
            Throwable th2 = null;
            FileOutputStream out = null;
            try {
                FileOutputStream out2 = new FileOutputStream(new File(getInstantApplicationDir(pkg.packageName, userId), INSTANT_APP_ICON_FILE));
                try {
                    bitmap.compress(CompressFormat.PNG, 100, out2);
                    if (out2 != null) {
                        try {
                            out2.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        try {
                            throw th2;
                        } catch (Exception e2) {
                            e = e2;
                            out = out2;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    out = out2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        try {
                            throw th2;
                        } catch (Exception e3) {
                            e = e3;
                            Slog.e(LOG_TAG, "Error writing instant app icon", e);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable th52) {
                        if (th2 == null) {
                            th2 = th52;
                        } else if (th2 != th52) {
                            th2.addSuppressed(th52);
                        }
                    }
                }
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (Exception e32) {
                    e = e32;
                    Slog.e(LOG_TAG, "Error writing instant app icon", e);
                }
            }
        }
    }

    boolean hasInstantApplicationMetadataLPr(String packageName, int userId) {
        if (hasUninstalledInstantAppStateLPr(packageName, userId)) {
            return true;
        }
        return hasInstantAppMetadataLPr(packageName, userId);
    }

    public void deleteInstantApplicationMetadataLPw(String packageName, int userId) {
        removeUninstalledInstantAppStateLPw(new -$Lambda$KFbchFEqJgs_hY1HweauKRNA_ds((byte) 0, packageName), userId);
        File instantAppDir = getInstantApplicationDir(packageName, userId);
        new File(instantAppDir, INSTANT_APP_METADATA_FILE).delete();
        new File(instantAppDir, INSTANT_APP_ICON_FILE).delete();
        new File(instantAppDir, INSTANT_APP_ANDROID_ID_FILE).delete();
        File cookie = peekInstantCookieFile(packageName, userId);
        if (cookie != null) {
            cookie.delete();
        }
    }

    private void removeUninstalledInstantAppStateLPw(Predicate<UninstalledInstantAppState> criteria, int userId) {
        if (this.mUninstalledInstantApps != null) {
            List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates != null) {
                for (int i = uninstalledAppStates.size() - 1; i >= 0; i--) {
                    if (criteria.test((UninstalledInstantAppState) uninstalledAppStates.get(i))) {
                        uninstalledAppStates.remove(i);
                        if (uninstalledAppStates.isEmpty()) {
                            this.mUninstalledInstantApps.remove(userId);
                            if (this.mUninstalledInstantApps.size() <= 0) {
                                this.mUninstalledInstantApps = null;
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean hasUninstalledInstantAppStateLPr(String packageName, int userId) {
        if (this.mUninstalledInstantApps == null) {
            return false;
        }
        List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
        if (uninstalledAppStates == null) {
            return false;
        }
        int appCount = uninstalledAppStates.size();
        for (int i = 0; i < appCount; i++) {
            if (packageName.equals(((UninstalledInstantAppState) uninstalledAppStates.get(i)).mInstantAppInfo.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInstantAppMetadataLPr(String packageName, int userId) {
        File instantAppDir = getInstantApplicationDir(packageName, userId);
        if (new File(instantAppDir, INSTANT_APP_METADATA_FILE).exists() || new File(instantAppDir, INSTANT_APP_ICON_FILE).exists() || new File(instantAppDir, INSTANT_APP_ANDROID_ID_FILE).exists() || peekInstantCookieFile(packageName, userId) != null) {
            return true;
        }
        return false;
    }

    void pruneInstantApps() {
        try {
            pruneInstantApps(JobStatus.NO_LATEST_RUNTIME, Global.getLong(this.mService.mContext.getContentResolver(), "installed_instant_app_max_cache_period", 15552000000L), Global.getLong(this.mService.mContext.getContentResolver(), "uninstalled_instant_app_max_cache_period", 15552000000L));
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed and uninstalled instant apps", e);
        }
    }

    boolean pruneInstalledInstantApps(long neededSpace, long maxInstalledCacheDuration) {
        try {
            return pruneInstantApps(neededSpace, maxInstalledCacheDuration, JobStatus.NO_LATEST_RUNTIME);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed instant apps", e);
            return false;
        }
    }

    boolean pruneUninstalledInstantApps(long neededSpace, long maxUninstalledCacheDuration) {
        try {
            return pruneInstantApps(neededSpace, JobStatus.NO_LATEST_RUNTIME, maxUninstalledCacheDuration);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning uninstalled instant apps", e);
            return false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean pruneInstantApps(long neededSpace, long maxInstalledCacheDuration, long maxUninstalledCacheDuration) throws IOException {
        Throwable th;
        File file = ((StorageManager) this.mService.mContext.getSystemService(StorageManager.class)).findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        if (file.getUsableSpace() >= neededSpace) {
            return true;
        }
        List<String> packagesToDelete = null;
        long now = System.currentTimeMillis();
        synchronized (this.mService.mPackages) {
            try {
                int[] allUsers = PackageManagerService.sUserManager.getUserIds();
                int packageCount = this.mService.mPackages.size();
                int i = 0;
                while (true) {
                    List<String> packagesToDelete2 = packagesToDelete;
                    if (i >= packageCount) {
                        break;
                    }
                    try {
                        Package pkg = (Package) this.mService.mPackages.valueAt(i);
                        if (now - pkg.getLatestPackageUseTimeInMills() < maxInstalledCacheDuration) {
                            packagesToDelete = packagesToDelete2;
                        } else if (pkg.mExtras instanceof PackageSetting) {
                            PackageSetting ps = pkg.mExtras;
                            boolean installedOnlyAsInstantApp = false;
                            for (int userId : allUsers) {
                                if (ps.getInstalled(userId)) {
                                    if (!ps.getInstantApp(userId)) {
                                        installedOnlyAsInstantApp = false;
                                        break;
                                    }
                                    installedOnlyAsInstantApp = true;
                                }
                            }
                            if (installedOnlyAsInstantApp) {
                                if (packagesToDelete2 == null) {
                                    packagesToDelete = new ArrayList();
                                } else {
                                    packagesToDelete = packagesToDelete2;
                                }
                                packagesToDelete.add(pkg.packageName);
                            } else {
                                packagesToDelete = packagesToDelete2;
                            }
                        } else {
                            packagesToDelete = packagesToDelete2;
                        }
                        i++;
                    } catch (Throwable th2) {
                        th = th2;
                        packagesToDelete = packagesToDelete2;
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* synthetic */ int lambda$-com_android_server_pm_InstantAppRegistry_29061(String lhs, String rhs) {
        Package lhsPkg = (Package) this.mService.mPackages.get(lhs);
        Package rhsPkg = (Package) this.mService.mPackages.get(rhs);
        if (lhsPkg == null && rhsPkg == null) {
            return 0;
        }
        if (lhsPkg == null) {
            return -1;
        }
        if (rhsPkg == null || lhsPkg.getLatestPackageUseTimeInMills() > rhsPkg.getLatestPackageUseTimeInMills()) {
            return 1;
        }
        if (lhsPkg.getLatestPackageUseTimeInMills() < rhsPkg.getLatestPackageUseTimeInMills()) {
            return -1;
        }
        if (!(lhsPkg.mExtras instanceof PackageSetting) || !(rhsPkg.mExtras instanceof PackageSetting)) {
            return 0;
        }
        return ((PackageSetting) lhsPkg.mExtras).firstInstallTime > ((PackageSetting) rhsPkg.mExtras).firstInstallTime ? 1 : -1;
    }

    static /* synthetic */ boolean lambda$-com_android_server_pm_InstantAppRegistry_31751(long maxUninstalledCacheDuration, UninstalledInstantAppState state) {
        return System.currentTimeMillis() - state.mTimestamp > maxUninstalledCacheDuration;
    }

    private List<InstantAppInfo> getInstalledInstantApplicationsLPr(int userId) {
        List<InstantAppInfo> result = null;
        int packageCount = this.mService.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            Package pkg = (Package) this.mService.mPackages.valueAt(i);
            PackageSetting ps = pkg.mExtras;
            if (ps != null && (ps.getInstantApp(userId) ^ 1) == 0) {
                InstantAppInfo info = createInstantAppInfoForPackage(pkg, userId, true);
                if (info != null) {
                    if (result == null) {
                        result = new ArrayList();
                    }
                    result.add(info);
                }
            }
        }
        return result;
    }

    private InstantAppInfo createInstantAppInfoForPackage(Package pkg, int userId, boolean addApplicationInfo) {
        PackageSetting ps = pkg.mExtras;
        if (ps == null || !ps.getInstalled(userId)) {
            return null;
        }
        String[] requestedPermissions = new String[pkg.requestedPermissions.size()];
        pkg.requestedPermissions.toArray(requestedPermissions);
        Set<String> permissions = ps.getPermissionsState().getPermissions(userId);
        String[] grantedPermissions = new String[permissions.size()];
        permissions.toArray(grantedPermissions);
        if (addApplicationInfo) {
            return new InstantAppInfo(pkg.applicationInfo, requestedPermissions, grantedPermissions);
        }
        return new InstantAppInfo(pkg.applicationInfo.packageName, pkg.applicationInfo.loadLabel(this.mService.mContext.getPackageManager()), requestedPermissions, grantedPermissions);
    }

    private List<InstantAppInfo> getUninstalledInstantApplicationsLPr(int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates = getUninstalledInstantAppStatesLPr(userId);
        if (uninstalledAppStates == null || uninstalledAppStates.isEmpty()) {
            return null;
        }
        List<InstantAppInfo> uninstalledApps = null;
        int stateCount = uninstalledAppStates.size();
        for (int i = 0; i < stateCount; i++) {
            UninstalledInstantAppState uninstalledAppState = (UninstalledInstantAppState) uninstalledAppStates.get(i);
            if (uninstalledApps == null) {
                uninstalledApps = new ArrayList();
            }
            uninstalledApps.add(uninstalledAppState.mInstantAppInfo);
        }
        return uninstalledApps;
    }

    private void propagateInstantAppPermissionsIfNeeded(String packageName, int userId) {
        InstantAppInfo appInfo = peekOrParseUninstalledInstantAppInfo(packageName, userId);
        if (appInfo != null && !ArrayUtils.isEmpty(appInfo.getGrantedPermissions())) {
            long identity = Binder.clearCallingIdentity();
            try {
                for (String grantedPermission : appInfo.getGrantedPermissions()) {
                    BasePermission bp = (BasePermission) this.mService.mSettings.mPermissions.get(grantedPermission);
                    if (bp != null && ((bp.isRuntime() || bp.isDevelopment()) && bp.isInstant())) {
                        this.mService.grantRuntimePermission(packageName, grantedPermission, userId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private InstantAppInfo peekOrParseUninstalledInstantAppInfo(String packageName, int userId) {
        UninstalledInstantAppState uninstalledAppState;
        if (this.mUninstalledInstantApps != null) {
            List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates != null) {
                int appCount = uninstalledAppStates.size();
                for (int i = 0; i < appCount; i++) {
                    uninstalledAppState = (UninstalledInstantAppState) uninstalledAppStates.get(i);
                    if (uninstalledAppState.mInstantAppInfo.getPackageName().equals(packageName)) {
                        return uninstalledAppState.mInstantAppInfo;
                    }
                }
            }
        }
        uninstalledAppState = parseMetadataFile(new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_METADATA_FILE));
        if (uninstalledAppState == null) {
            return null;
        }
        return uninstalledAppState.mInstantAppInfo;
    }

    private List<UninstalledInstantAppState> getUninstalledInstantAppStatesLPr(int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates = null;
        if (this.mUninstalledInstantApps != null) {
            uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates != null) {
                return uninstalledAppStates;
            }
        }
        File instantAppsDir = getInstantApplicationsDir(userId);
        if (instantAppsDir.exists()) {
            File[] files = instantAppsDir.listFiles();
            if (files != null) {
                for (File instantDir : files) {
                    if (instantDir.isDirectory()) {
                        UninstalledInstantAppState uninstalledAppState = parseMetadataFile(new File(instantDir, INSTANT_APP_METADATA_FILE));
                        if (uninstalledAppState != null) {
                            if (uninstalledAppStates == null) {
                                uninstalledAppStates = new ArrayList();
                            }
                            uninstalledAppStates.add(uninstalledAppState);
                        }
                    }
                }
            }
        }
        if (uninstalledAppStates != null) {
            if (this.mUninstalledInstantApps == null) {
                this.mUninstalledInstantApps = new SparseArray();
            }
            this.mUninstalledInstantApps.put(userId, uninstalledAppStates);
        }
        return uninstalledAppStates;
    }

    private static UninstalledInstantAppState parseMetadataFile(File metadataFile) {
        if (!metadataFile.exists()) {
            return null;
        }
        try {
            FileInputStream in = new AtomicFile(metadataFile).openRead();
            File instantDir = metadataFile.getParentFile();
            long timestamp = metadataFile.lastModified();
            String packageName = instantDir.getName();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(in, StandardCharsets.UTF_8.name());
                UninstalledInstantAppState uninstalledInstantAppState = new UninstalledInstantAppState(parseMetadata(parser, packageName), timestamp);
                IoUtils.closeQuietly(in);
                return uninstalledInstantAppState;
            } catch (Exception e) {
                throw new IllegalStateException("Failed parsing instant metadata file: " + metadataFile, e);
            } catch (Throwable th) {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            Slog.i(LOG_TAG, "No instant metadata file");
            return null;
        }
    }

    private static File computeInstantCookieFile(String packageName, String sha256Digest, int userId) {
        return new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_COOKIE_FILE_PREFIX + sha256Digest + INSTANT_APP_COOKIE_FILE_SIFFIX);
    }

    private static File peekInstantCookieFile(String packageName, int userId) {
        File appDir = getInstantApplicationDir(packageName, userId);
        if (!appDir.exists()) {
            return null;
        }
        File[] files = appDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!file.isDirectory() && file.getName().startsWith(INSTANT_APP_COOKIE_FILE_PREFIX) && file.getName().endsWith(INSTANT_APP_COOKIE_FILE_SIFFIX)) {
                return file;
            }
        }
        return null;
    }

    private static InstantAppInfo parseMetadata(XmlPullParser parser, String packageName) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (TAG_PACKAGE.equals(parser.getName())) {
                return parsePackage(parser, packageName);
            }
        }
        return null;
    }

    private static InstantAppInfo parsePackage(XmlPullParser parser, String packageName) throws IOException, XmlPullParserException {
        String label = parser.getAttributeValue(null, ATTR_LABEL);
        List<String> outRequestedPermissions = new ArrayList();
        List<String> outGrantedPermissions = new ArrayList();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (TAG_PERMISSIONS.equals(parser.getName())) {
                parsePermissions(parser, outRequestedPermissions, outGrantedPermissions);
            }
        }
        String[] requestedPermissions = new String[outRequestedPermissions.size()];
        outRequestedPermissions.toArray(requestedPermissions);
        String[] grantedPermissions = new String[outGrantedPermissions.size()];
        outGrantedPermissions.toArray(grantedPermissions);
        return new InstantAppInfo(packageName, label, requestedPermissions, grantedPermissions);
    }

    private static void parsePermissions(XmlPullParser parser, List<String> outRequestedPermissions, List<String> outGrantedPermissions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (TAG_PERMISSION.equals(parser.getName())) {
                String permission = XmlUtils.readStringAttribute(parser, ATTR_NAME);
                outRequestedPermissions.add(permission);
                if (XmlUtils.readBooleanAttribute(parser, ATTR_GRANTED)) {
                    outGrantedPermissions.add(permission);
                }
            }
        }
    }

    private void writeUninstalledInstantAppMetadata(InstantAppInfo instantApp, int userId) {
        File appDir = getInstantApplicationDir(instantApp.getPackageName(), userId);
        if (appDir.exists() || (appDir.mkdirs() ^ 1) == 0) {
            AtomicFile destination = new AtomicFile(new File(appDir, INSTANT_APP_METADATA_FILE));
            AutoCloseable autoCloseable = null;
            try {
                autoCloseable = destination.startWrite();
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(autoCloseable, StandardCharsets.UTF_8.name());
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startDocument(null, Boolean.valueOf(true));
                serializer.startTag(null, TAG_PACKAGE);
                serializer.attribute(null, ATTR_LABEL, instantApp.loadLabel(this.mService.mContext.getPackageManager()).toString());
                serializer.startTag(null, TAG_PERMISSIONS);
                for (String permission : instantApp.getRequestedPermissions()) {
                    serializer.startTag(null, TAG_PERMISSION);
                    serializer.attribute(null, ATTR_NAME, permission);
                    if (ArrayUtils.contains(instantApp.getGrantedPermissions(), permission)) {
                        serializer.attribute(null, ATTR_GRANTED, String.valueOf(true));
                    }
                    serializer.endTag(null, TAG_PERMISSION);
                }
                serializer.endTag(null, TAG_PERMISSIONS);
                serializer.endTag(null, TAG_PACKAGE);
                serializer.endDocument();
                destination.finishWrite(autoCloseable);
            } catch (Throwable t) {
                Slog.wtf(LOG_TAG, "Failed to write instant state, restoring backup", t);
                destination.failWrite(null);
            } finally {
                IoUtils.closeQuietly(autoCloseable);
            }
        }
    }

    private static File getInstantApplicationsDir(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), INSTANT_APPS_FOLDER);
    }

    private static File getInstantApplicationDir(String packageName, int userId) {
        return new File(getInstantApplicationsDir(userId), packageName);
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
    }
}
