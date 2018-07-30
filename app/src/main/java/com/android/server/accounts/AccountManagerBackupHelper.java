package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountManagerInternal;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class AccountManagerBackupHelper {
    private static final String ATTR_ACCOUNT_SHA_256 = "account-sha-256";
    private static final String ATTR_DIGEST = "digest";
    private static final String ATTR_PACKAGE = "package";
    private static final long PENDING_RESTORE_TIMEOUT_MILLIS = 3600000;
    private static final String TAG = "AccountManagerBackupHelper";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final AccountManagerInternal mAccountManagerInternal;
    private final AccountManagerService mAccountManagerService;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private Runnable mRestoreCancelCommand;
    @GuardedBy("mLock")
    private RestorePackageMonitor mRestorePackageMonitor;
    @GuardedBy("mLock")
    private List<PendingAppPermission> mRestorePendingAppPermissions;

    private final class CancelRestoreCommand implements Runnable {
        /* synthetic */ CancelRestoreCommand(AccountManagerBackupHelper this$0, CancelRestoreCommand -this1) {
            this();
        }

        private CancelRestoreCommand() {
        }

        public void run() {
            synchronized (AccountManagerBackupHelper.this.mLock) {
                AccountManagerBackupHelper.this.mRestorePendingAppPermissions = null;
                if (AccountManagerBackupHelper.this.mRestorePackageMonitor != null) {
                    AccountManagerBackupHelper.this.mRestorePackageMonitor.unregister();
                    AccountManagerBackupHelper.this.mRestorePackageMonitor = null;
                }
            }
        }
    }

    private final class PendingAppPermission {
        private final String accountDigest;
        private final String certDigest;
        private final String packageName;
        private final int userId;

        public PendingAppPermission(String accountDigest, String packageName, String certDigest, int userId) {
            this.accountDigest = accountDigest;
            this.packageName = packageName;
            this.certDigest = certDigest;
            this.userId = userId;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean apply(PackageManager packageManager) {
            Account account = null;
            UserAccounts accounts = AccountManagerBackupHelper.this.mAccountManagerService.getUserAccounts(this.userId);
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    for (Account[] accountsPerType : accounts.accountCache.values()) {
                        for (Account accountPerType : (Account[]) accountsPerType$iterator.next()) {
                            if (this.accountDigest.equals(PackageUtils.computeSha256Digest(accountPerType.name.getBytes()))) {
                                account = accountPerType;
                                continue;
                                break;
                            }
                        }
                    }
                }
            }
            if (account == null) {
                return false;
            }
            try {
                PackageInfo packageInfo = packageManager.getPackageInfoAsUser(this.packageName, 64, this.userId);
                String[] signaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(packageInfo.signatures);
                if (!this.certDigest.equals(PackageUtils.computeSignaturesSha256Digest(signaturesSha256Digests)) && (packageInfo.signatures.length <= 1 || (this.certDigest.equals(signaturesSha256Digests[0]) ^ 1) != 0)) {
                    return false;
                }
                int uid = packageInfo.applicationInfo.uid;
                if (!AccountManagerBackupHelper.this.mAccountManagerInternal.hasAccountAccess(account, uid)) {
                    AccountManagerBackupHelper.this.mAccountManagerService.grantAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid);
                }
                return true;
            } catch (NameNotFoundException e) {
                return false;
            }
        }
    }

    private final class RestorePackageMonitor extends PackageMonitor {
        /* synthetic */ RestorePackageMonitor(AccountManagerBackupHelper this$0, RestorePackageMonitor -this1) {
            this();
        }

        private RestorePackageMonitor() {
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onPackageAdded(String packageName, int uid) {
            synchronized (AccountManagerBackupHelper.this.mLock) {
                if (AccountManagerBackupHelper.this.mRestorePendingAppPermissions == null) {
                } else if (UserHandle.getUserId(uid) != 0) {
                } else {
                    for (int i = AccountManagerBackupHelper.this.mRestorePendingAppPermissions.size() - 1; i >= 0; i--) {
                        PendingAppPermission pendingAppPermission = (PendingAppPermission) AccountManagerBackupHelper.this.mRestorePendingAppPermissions.get(i);
                        if (pendingAppPermission.packageName.equals(packageName) && pendingAppPermission.apply(AccountManagerBackupHelper.this.mAccountManagerService.mContext.getPackageManager())) {
                            AccountManagerBackupHelper.this.mRestorePendingAppPermissions.remove(i);
                        }
                    }
                    if (AccountManagerBackupHelper.this.mRestorePendingAppPermissions.isEmpty() && AccountManagerBackupHelper.this.mRestoreCancelCommand != null) {
                        AccountManagerBackupHelper.this.mAccountManagerService.mHandler.removeCallbacks(AccountManagerBackupHelper.this.mRestoreCancelCommand);
                        AccountManagerBackupHelper.this.mRestoreCancelCommand.run();
                        AccountManagerBackupHelper.this.mRestoreCancelCommand = null;
                    }
                }
            }
        }
    }

    public AccountManagerBackupHelper(AccountManagerService accountManagerService, AccountManagerInternal accountManagerInternal) {
        this.mAccountManagerService = accountManagerService;
        this.mAccountManagerInternal = accountManagerInternal;
    }

    public byte[] backupAccountAccessPermissions(int userId) {
        UserAccounts accounts = this.mAccountManagerService.getUserAccounts(userId);
        synchronized (accounts.dbLock) {
            synchronized (accounts.cacheLock) {
                List<Pair<String, Integer>> allAccountGrants = accounts.accountsDb.findAllAccountGrants();
                if (allAccountGrants.isEmpty()) {
                    return null;
                }
                try {
                    ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                    XmlSerializer serializer = new FastXmlSerializer();
                    serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
                    serializer.startDocument(null, Boolean.valueOf(true));
                    serializer.startTag(null, TAG_PERMISSIONS);
                    PackageManager packageManager = this.mAccountManagerService.mContext.getPackageManager();
                    for (Pair<String, Integer> grant : allAccountGrants) {
                        String accountName = grant.first;
                        String[] packageNames = packageManager.getPackagesForUid(((Integer) grant.second).intValue());
                        if (packageNames != null) {
                            for (String packageName : packageNames) {
                                try {
                                    String digest = PackageUtils.computeSignaturesSha256Digest(packageManager.getPackageInfoAsUser(packageName, 64, userId).signatures);
                                    if (digest != null) {
                                        serializer.startTag(null, TAG_PERMISSION);
                                        serializer.attribute(null, ATTR_ACCOUNT_SHA_256, PackageUtils.computeSha256Digest(accountName.getBytes()));
                                        serializer.attribute(null, ATTR_PACKAGE, packageName);
                                        serializer.attribute(null, ATTR_DIGEST, digest);
                                        serializer.endTag(null, TAG_PERMISSION);
                                    }
                                } catch (NameNotFoundException e) {
                                    Slog.i(TAG, "Skipping backup of account access grant for non-existing package: " + packageName);
                                }
                            }
                        }
                    }
                    serializer.endTag(null, TAG_PERMISSIONS);
                    serializer.endDocument();
                    serializer.flush();
                    return dataStream.toByteArray();
                } catch (IOException e2) {
                    Log.e(TAG, "Error backing up account access grants", e2);
                    return null;
                }
            }
        }
    }

    public void restoreAccountAccessPermissions(byte[] data, int userId) {
        try {
            AccountManagerBackupHelper accountManagerBackupHelper;
            ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(dataStream, StandardCharsets.UTF_8.name());
            PackageManager packageManager = this.mAccountManagerService.mContext.getPackageManager();
            int permissionsOuterDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, permissionsOuterDepth)) {
                if (TAG_PERMISSIONS.equals(parser.getName())) {
                    int permissionOuterDepth = parser.getDepth();
                    while (XmlUtils.nextElementWithin(parser, permissionOuterDepth)) {
                        if (TAG_PERMISSION.equals(parser.getName())) {
                            String accountDigest = parser.getAttributeValue(null, ATTR_ACCOUNT_SHA_256);
                            if (TextUtils.isEmpty(accountDigest)) {
                                XmlUtils.skipCurrentTag(parser);
                            }
                            String packageName = parser.getAttributeValue(null, ATTR_PACKAGE);
                            if (TextUtils.isEmpty(packageName)) {
                                XmlUtils.skipCurrentTag(parser);
                            }
                            String digest = parser.getAttributeValue(null, ATTR_DIGEST);
                            if (TextUtils.isEmpty(digest)) {
                                XmlUtils.skipCurrentTag(parser);
                            }
                            PendingAppPermission pendingAppPermission = new PendingAppPermission(accountDigest, packageName, digest, userId);
                            if (pendingAppPermission.apply(packageManager)) {
                                continue;
                            } else {
                                synchronized (this.mLock) {
                                    if (this.mRestorePackageMonitor == null) {
                                        accountManagerBackupHelper = this;
                                        this.mRestorePackageMonitor = new RestorePackageMonitor();
                                        this.mRestorePackageMonitor.register(this.mAccountManagerService.mContext, this.mAccountManagerService.mHandler.getLooper(), true);
                                    }
                                    if (this.mRestorePendingAppPermissions == null) {
                                        this.mRestorePendingAppPermissions = new ArrayList();
                                    }
                                    this.mRestorePendingAppPermissions.add(pendingAppPermission);
                                }
                            }
                        }
                    }
                    continue;
                }
            }
            accountManagerBackupHelper = this;
            this.mRestoreCancelCommand = new CancelRestoreCommand();
            this.mAccountManagerService.mHandler.postDelayed(this.mRestoreCancelCommand, PENDING_RESTORE_TIMEOUT_MILLIS);
        } catch (Exception e) {
            Log.e(TAG, "Error restoring app permissions", e);
        }
    }
}
