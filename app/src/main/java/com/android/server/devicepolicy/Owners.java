package com.android.server.devicepolicy;

import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

class Owners {
    private static final String ATTR_COMPONENT_NAME = "component";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_REMOTE_BUGREPORT_HASH = "remoteBugreportHash";
    private static final String ATTR_REMOTE_BUGREPORT_URI = "remoteBugreportUri";
    private static final String ATTR_USERID = "userId";
    private static final String ATTR_USER_RESTRICTIONS_MIGRATED = "userRestrictionsMigrated";
    private static final boolean DEBUG = false;
    private static final String DEVICE_OWNER_XML = "device_owner_2.xml";
    private static final String DEVICE_OWNER_XML_LEGACY = "device_owner.xml";
    private static final String PROFILE_OWNER_XML = "profile_owner.xml";
    private static final String TAG = "DevicePolicyManagerService";
    private static final String TAG_DEVICE_INITIALIZER = "device-initializer";
    private static final String TAG_DEVICE_OWNER = "device-owner";
    private static final String TAG_DEVICE_OWNER_CONTEXT = "device-owner-context";
    private static final String TAG_PENDING_OTA_INFO = "pending-ota-info";
    private static final String TAG_PROFILE_OWNER = "profile-owner";
    private static final String TAG_ROOT = "root";
    private static final String TAG_SYSTEM_UPDATE_POLICY = "system-update-policy";
    private OwnerInfo mDeviceOwner;
    private int mDeviceOwnerUserId = -10000;
    private final Object mLock = new Object();
    private final PackageManagerInternal mPackageManagerInternal;
    private final ArrayMap<Integer, OwnerInfo> mProfileOwners = new ArrayMap();
    private SystemUpdateInfo mSystemUpdateInfo;
    private SystemUpdatePolicy mSystemUpdatePolicy;
    private final UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;

    private static abstract class FileReadWriter {
        private final File mFile;

        abstract boolean readInner(XmlPullParser xmlPullParser, int i, String str);

        abstract boolean shouldWrite();

        abstract void writeInner(XmlSerializer xmlSerializer) throws IOException;

        protected FileReadWriter(File file) {
            this.mFile = file;
        }

        void writeToFileLocked() {
            if (shouldWrite()) {
                AtomicFile f = new AtomicFile(this.mFile);
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = f.startWrite();
                    XmlSerializer out = new FastXmlSerializer();
                    out.setOutput(fileOutputStream, StandardCharsets.UTF_8.name());
                    out.startDocument(null, Boolean.valueOf(true));
                    out.startTag(null, Owners.TAG_ROOT);
                    writeInner(out);
                    out.endTag(null, Owners.TAG_ROOT);
                    out.endDocument();
                    out.flush();
                    f.finishWrite(fileOutputStream);
                } catch (IOException e) {
                    Slog.e(Owners.TAG, "Exception when writing", e);
                    if (fileOutputStream != null) {
                        f.failWrite(fileOutputStream);
                    }
                }
                return;
            }
            if (this.mFile.exists() && !this.mFile.delete()) {
                Slog.e(Owners.TAG, "Failed to remove " + this.mFile.getPath());
            }
        }

        void readFromFileLocked() {
            if (this.mFile.exists()) {
                AutoCloseable autoCloseable = null;
                try {
                    autoCloseable = new AtomicFile(this.mFile).openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(autoCloseable, StandardCharsets.UTF_8.name());
                    int depth = 0;
                    while (true) {
                        int type = parser.next();
                        if (type != 1) {
                            switch (type) {
                                case 2:
                                    depth++;
                                    String tag = parser.getName();
                                    if (depth != 1) {
                                        if (readInner(parser, depth, tag)) {
                                            break;
                                        }
                                        IoUtils.closeQuietly(autoCloseable);
                                        return;
                                    } else if (!Owners.TAG_ROOT.equals(tag)) {
                                        Slog.e(Owners.TAG, "Invalid root tag: " + tag);
                                        break;
                                    } else {
                                        continue;
                                    }
                                case 3:
                                    depth--;
                                    continue;
                                default:
                                    break;
                            }
                            IoUtils.closeQuietly(autoCloseable);
                        } else {
                            IoUtils.closeQuietly(autoCloseable);
                        }
                    }
                } catch (Exception e) {
                    Slog.e(Owners.TAG, "Error parsing owners information file", e);
                } finally {
                }
            }
        }
    }

    private class DeviceOwnerReadWriter extends FileReadWriter {
        protected DeviceOwnerReadWriter() {
            super(Owners.this.getDeviceOwnerFileWithTestOverride());
        }

        boolean shouldWrite() {
            if (Owners.this.mDeviceOwner == null && Owners.this.mSystemUpdatePolicy == null && Owners.this.mSystemUpdateInfo == null) {
                return false;
            }
            return true;
        }

        void writeInner(XmlSerializer out) throws IOException {
            if (Owners.this.mDeviceOwner != null) {
                Owners.this.mDeviceOwner.writeToXml(out, Owners.TAG_DEVICE_OWNER);
                out.startTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
                out.attribute(null, Owners.ATTR_USERID, String.valueOf(Owners.this.mDeviceOwnerUserId));
                out.endTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
            }
            if (Owners.this.mSystemUpdatePolicy != null) {
                out.startTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
                Owners.this.mSystemUpdatePolicy.saveToXml(out);
                out.endTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
            }
            if (Owners.this.mSystemUpdateInfo != null) {
                Owners.this.mSystemUpdateInfo.writeToXml(out, Owners.TAG_PENDING_OTA_INFO);
            }
        }

        boolean readInner(XmlPullParser parser, int depth, String tag) {
            if (depth > 2) {
                return true;
            }
            if (tag.equals(Owners.TAG_DEVICE_OWNER)) {
                Owners.this.mDeviceOwner = OwnerInfo.readFromXml(parser);
                Owners.this.mDeviceOwnerUserId = 0;
            } else if (tag.equals(Owners.TAG_DEVICE_OWNER_CONTEXT)) {
                String userIdString = parser.getAttributeValue(null, Owners.ATTR_USERID);
                try {
                    Owners.this.mDeviceOwnerUserId = Integer.parseInt(userIdString);
                } catch (NumberFormatException e) {
                    Slog.e(Owners.TAG, "Error parsing user-id " + userIdString);
                }
            } else if (!tag.equals(Owners.TAG_DEVICE_INITIALIZER)) {
                if (tag.equals(Owners.TAG_SYSTEM_UPDATE_POLICY)) {
                    Owners.this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(parser);
                } else if (tag.equals(Owners.TAG_PENDING_OTA_INFO)) {
                    Owners.this.mSystemUpdateInfo = SystemUpdateInfo.readFromXml(parser);
                } else {
                    Slog.e(Owners.TAG, "Unexpected tag: " + tag);
                    return false;
                }
            }
            return true;
        }
    }

    static class OwnerInfo {
        public final ComponentName admin;
        public final String name;
        public final String packageName;
        public String remoteBugreportHash;
        public String remoteBugreportUri;
        public boolean userRestrictionsMigrated;

        public OwnerInfo(String name, String packageName, boolean userRestrictionsMigrated, String remoteBugreportUri, String remoteBugreportHash) {
            this.name = name;
            this.packageName = packageName;
            this.admin = new ComponentName(packageName, "");
            this.userRestrictionsMigrated = userRestrictionsMigrated;
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
        }

        public OwnerInfo(String name, ComponentName admin, boolean userRestrictionsMigrated, String remoteBugreportUri, String remoteBugreportHash) {
            this.name = name;
            this.admin = admin;
            this.packageName = admin.getPackageName();
            this.userRestrictionsMigrated = userRestrictionsMigrated;
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
        }

        public void writeToXml(XmlSerializer out, String tag) throws IOException {
            out.startTag(null, tag);
            out.attribute(null, Owners.ATTR_PACKAGE, this.packageName);
            if (this.name != null) {
                out.attribute(null, Owners.ATTR_NAME, this.name);
            }
            if (this.admin != null) {
                out.attribute(null, Owners.ATTR_COMPONENT_NAME, this.admin.flattenToString());
            }
            out.attribute(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED, String.valueOf(this.userRestrictionsMigrated));
            if (this.remoteBugreportUri != null) {
                out.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_URI, this.remoteBugreportUri);
            }
            if (this.remoteBugreportHash != null) {
                out.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_HASH, this.remoteBugreportHash);
            }
            out.endTag(null, tag);
        }

        public static OwnerInfo readFromXml(XmlPullParser parser) {
            String packageName = parser.getAttributeValue(null, Owners.ATTR_PACKAGE);
            String name = parser.getAttributeValue(null, Owners.ATTR_NAME);
            String componentName = parser.getAttributeValue(null, Owners.ATTR_COMPONENT_NAME);
            boolean userRestrictionsMigrated = "true".equals(parser.getAttributeValue(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED));
            String remoteBugreportUri = parser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_URI);
            String remoteBugreportHash = parser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_HASH);
            if (componentName != null) {
                ComponentName admin = ComponentName.unflattenFromString(componentName);
                if (admin != null) {
                    return new OwnerInfo(name, admin, userRestrictionsMigrated, remoteBugreportUri, remoteBugreportHash);
                }
                Slog.e(Owners.TAG, "Error parsing owner file. Bad component name " + componentName);
            }
            return new OwnerInfo(name, packageName, userRestrictionsMigrated, remoteBugreportUri, remoteBugreportHash);
        }

        public void dump(String prefix, PrintWriter pw) {
            pw.println(prefix + "admin=" + this.admin);
            pw.println(prefix + "name=" + this.name);
            pw.println(prefix + "package=" + this.packageName);
        }
    }

    private class ProfileOwnerReadWriter extends FileReadWriter {
        private final int mUserId;

        ProfileOwnerReadWriter(int userId) {
            super(Owners.this.getProfileOwnerFileWithTestOverride(userId));
            this.mUserId = userId;
        }

        boolean shouldWrite() {
            return Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId)) != null;
        }

        void writeInner(XmlSerializer out) throws IOException {
            OwnerInfo profileOwner = (OwnerInfo) Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId));
            if (profileOwner != null) {
                profileOwner.writeToXml(out, Owners.TAG_PROFILE_OWNER);
            }
        }

        boolean readInner(XmlPullParser parser, int depth, String tag) {
            if (depth > 2) {
                return true;
            }
            if (tag.equals(Owners.TAG_PROFILE_OWNER)) {
                Owners.this.mProfileOwners.put(Integer.valueOf(this.mUserId), OwnerInfo.readFromXml(parser));
                return true;
            }
            Slog.e(Owners.TAG, "Unexpected tag: " + tag);
            return false;
        }
    }

    public Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal) {
        this.mUserManager = userManager;
        this.mUserManagerInternal = userManagerInternal;
        this.mPackageManagerInternal = packageManagerInternal;
    }

    void load() {
        synchronized (this.mLock) {
            File legacy = getLegacyConfigFileWithTestOverride();
            List<UserInfo> users = this.mUserManager.getUsers(true);
            if (readLegacyOwnerFileLocked(legacy)) {
                writeDeviceOwner();
                for (Integer intValue : getProfileOwnerKeys()) {
                    writeProfileOwner(intValue.intValue());
                }
                if (!legacy.delete()) {
                    Slog.e(TAG, "Failed to remove the legacy setting file");
                }
            } else {
                new DeviceOwnerReadWriter().readFromFileLocked();
                for (UserInfo ui : users) {
                    new ProfileOwnerReadWriter(ui.id).readFromFileLocked();
                }
            }
            this.mUserManagerInternal.setDeviceManaged(hasDeviceOwner());
            for (UserInfo ui2 : users) {
                this.mUserManagerInternal.setUserManaged(ui2.id, hasProfileOwner(ui2.id));
            }
            if (hasDeviceOwner() && hasProfileOwner(getDeviceOwnerUserId())) {
                Slog.w(TAG, String.format("User %d has both DO and PO, which is not supported", new Object[]{Integer.valueOf(getDeviceOwnerUserId())}));
            }
            pushToPackageManagerLocked();
        }
    }

    private void pushToPackageManagerLocked() {
        String str;
        SparseArray<String> po = new SparseArray();
        for (int i = this.mProfileOwners.size() - 1; i >= 0; i--) {
            po.put(((Integer) this.mProfileOwners.keyAt(i)).intValue(), ((OwnerInfo) this.mProfileOwners.valueAt(i)).packageName);
        }
        PackageManagerInternal packageManagerInternal = this.mPackageManagerInternal;
        int i2 = this.mDeviceOwnerUserId;
        if (this.mDeviceOwner != null) {
            str = this.mDeviceOwner.packageName;
        } else {
            str = null;
        }
        packageManagerInternal.setDeviceAndProfileOwnerPackages(i2, str, po);
    }

    String getDeviceOwnerPackageName() {
        String str = null;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                str = this.mDeviceOwner.packageName;
            }
        }
        return str;
    }

    int getDeviceOwnerUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mDeviceOwnerUserId;
        }
        return i;
    }

    Pair<Integer, ComponentName> getDeviceOwnerUserIdAndComponent() {
        synchronized (this.mLock) {
            if (this.mDeviceOwner == null) {
                return null;
            }
            Pair<Integer, ComponentName> create = Pair.create(Integer.valueOf(this.mDeviceOwnerUserId), this.mDeviceOwner.admin);
            return create;
        }
    }

    String getDeviceOwnerName() {
        String str = null;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                str = this.mDeviceOwner.name;
            }
        }
        return str;
    }

    ComponentName getDeviceOwnerComponent() {
        ComponentName componentName = null;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                componentName = this.mDeviceOwner.admin;
            }
        }
        return componentName;
    }

    String getDeviceOwnerRemoteBugreportUri() {
        String str = null;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                str = this.mDeviceOwner.remoteBugreportUri;
            }
        }
        return str;
    }

    String getDeviceOwnerRemoteBugreportHash() {
        String str = null;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                str = this.mDeviceOwner.remoteBugreportHash;
            }
        }
        return str;
    }

    void setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        if (userId < 0) {
            Slog.e(TAG, "Invalid user id for device owner user: " + userId);
            return;
        }
        synchronized (this.mLock) {
            setDeviceOwnerWithRestrictionsMigrated(admin, ownerName, userId, true);
        }
    }

    void setDeviceOwnerWithRestrictionsMigrated(ComponentName admin, String ownerName, int userId, boolean userRestrictionsMigrated) {
        synchronized (this.mLock) {
            this.mDeviceOwner = new OwnerInfo(ownerName, admin, userRestrictionsMigrated, null, null);
            this.mDeviceOwnerUserId = userId;
            this.mUserManagerInternal.setDeviceManaged(true);
            pushToPackageManagerLocked();
        }
    }

    void clearDeviceOwner() {
        synchronized (this.mLock) {
            this.mDeviceOwner = null;
            this.mDeviceOwnerUserId = -10000;
            this.mUserManagerInternal.setDeviceManaged(false);
            pushToPackageManagerLocked();
        }
    }

    void setProfileOwner(ComponentName admin, String ownerName, int userId) {
        synchronized (this.mLock) {
            this.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, admin, true, null, null));
            this.mUserManagerInternal.setUserManaged(userId, true);
            pushToPackageManagerLocked();
        }
    }

    void removeProfileOwner(int userId) {
        synchronized (this.mLock) {
            this.mProfileOwners.remove(Integer.valueOf(userId));
            this.mUserManagerInternal.setUserManaged(userId, false);
            pushToPackageManagerLocked();
        }
    }

    ComponentName getProfileOwnerComponent(int userId) {
        ComponentName componentName = null;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                componentName = profileOwner.admin;
            }
        }
        return componentName;
    }

    String getProfileOwnerName(int userId) {
        String str = null;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                str = profileOwner.name;
            }
        }
        return str;
    }

    String getProfileOwnerPackage(int userId) {
        String str = null;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                str = profileOwner.packageName;
            }
        }
        return str;
    }

    Set<Integer> getProfileOwnerKeys() {
        Set<Integer> keySet;
        synchronized (this.mLock) {
            keySet = this.mProfileOwners.keySet();
        }
        return keySet;
    }

    SystemUpdatePolicy getSystemUpdatePolicy() {
        SystemUpdatePolicy systemUpdatePolicy;
        synchronized (this.mLock) {
            systemUpdatePolicy = this.mSystemUpdatePolicy;
        }
        return systemUpdatePolicy;
    }

    void setSystemUpdatePolicy(SystemUpdatePolicy systemUpdatePolicy) {
        synchronized (this.mLock) {
            this.mSystemUpdatePolicy = systemUpdatePolicy;
        }
    }

    void clearSystemUpdatePolicy() {
        synchronized (this.mLock) {
            this.mSystemUpdatePolicy = null;
        }
    }

    boolean hasDeviceOwner() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null;
        }
        return z;
    }

    boolean isDeviceOwnerUserId(int userId) {
        boolean z = false;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null && this.mDeviceOwnerUserId == userId) {
                z = true;
            }
        }
        return z;
    }

    boolean hasProfileOwner(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = getProfileOwnerComponent(userId) != null;
        }
        return z;
    }

    boolean getDeviceOwnerUserRestrictionsNeedsMigration() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null ? this.mDeviceOwner.userRestrictionsMigrated ^ 1 : false;
        }
        return z;
    }

    boolean getProfileOwnerUserRestrictionsNeedsMigration(int userId) {
        boolean z;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            z = profileOwner != null ? profileOwner.userRestrictionsMigrated ^ 1 : false;
        }
        return z;
    }

    void setDeviceOwnerUserRestrictionsMigrated() {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.userRestrictionsMigrated = true;
            }
            writeDeviceOwner();
        }
    }

    void setDeviceOwnerRemoteBugreportUriAndHash(String remoteBugreportUri, String remoteBugreportHash) {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.remoteBugreportUri = remoteBugreportUri;
                this.mDeviceOwner.remoteBugreportHash = remoteBugreportHash;
            }
            writeDeviceOwner();
        }
    }

    void setProfileOwnerUserRestrictionsMigrated(int userId) {
        synchronized (this.mLock) {
            OwnerInfo profileOwner = (OwnerInfo) this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                profileOwner.userRestrictionsMigrated = true;
            }
            writeProfileOwner(userId);
        }
    }

    private boolean readLegacyOwnerFileLocked(java.io.File r22) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r4_4 java.lang.Object) in PHI: PHI: (r4_5 java.lang.Object) = (r4_3 java.lang.Object), (r4_4 java.lang.Object) binds: {(r4_3 java.lang.Object)=B:28:0x00b6, (r4_4 java.lang.Object)=B:29:0x00b8}
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
        r21 = this;
        r1 = r22.exists();
        if (r1 != 0) goto L_0x0008;
    L_0x0006:
        r1 = 0;
        return r1;
    L_0x0008:
        r1 = new android.util.AtomicFile;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r22;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1.<init>(r0);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r14 = r1.openRead();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r15 = android.util.Xml.newPullParser();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = java.nio.charset.StandardCharsets.UTF_8;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r1.name();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r15.setInput(r14, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x0020:
        r19 = r15.next();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = 1;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r19;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r0 == r1) goto L_0x011f;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x0029:
        r1 = 2;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r19;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r0 != r1) goto L_0x0020;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x002e:
        r18 = r15.getName();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = "device-owner";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r18;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r0.equals(r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r1 == 0) goto L_0x006b;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x003d:
        r1 = "name";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r2 = r15.getAttributeValue(r7, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = "package";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r3 = r15.getAttributeValue(r7, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = new com.android.server.devicepolicy.Owners$OwnerInfo;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r4 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r5 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r6 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1.<init>(r2, r3, r4, r5, r6);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r21;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0.mDeviceOwner = r1;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r21;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0.mDeviceOwnerUserId = r1;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        goto L_0x0020;
    L_0x005f:
        r13 = move-exception;
        r1 = "DevicePolicyManagerService";
        r7 = "Error parsing device-owner file";
        android.util.Slog.e(r1, r7, r13);
    L_0x0069:
        r1 = 1;
        return r1;
    L_0x006b:
        r1 = "device-initializer";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r18;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r0.equals(r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r1 != 0) goto L_0x0020;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x0076:
        r1 = "profile-owner";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r18;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r0.equals(r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r1 == 0) goto L_0x00ee;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x0081:
        r1 = "package";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r17 = r15.getAttributeValue(r7, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = "name";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r5 = r15.getAttributeValue(r7, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = "component";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r16 = r15.getAttributeValue(r7, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = "userId";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r15.getAttributeValue(r7, r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r20 = java.lang.Integer.parseInt(r1);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r4 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r16 == 0) goto L_0x00b6;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00a8:
        r6 = android.content.ComponentName.unflattenFromString(r16);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r6 == 0) goto L_0x00d1;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00ae:
        r4 = new com.android.server.devicepolicy.Owners$OwnerInfo;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r8 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r9 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r4.<init>(r5, r6, r7, r8, r9);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00b6:
        if (r4 != 0) goto L_0x00c4;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00b8:
        r4 = new com.android.server.devicepolicy.Owners$OwnerInfo;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r10 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r11 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r12 = 0;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r4;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r8 = r5;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r9 = r17;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7.<init>(r8, r9, r10, r11, r12);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00c4:
        r0 = r21;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r0.mProfileOwners;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = java.lang.Integer.valueOf(r20);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1.put(r7, r4);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        goto L_0x0020;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00d1:
        r1 = "DevicePolicyManagerService";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = new java.lang.StringBuilder;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7.<init>();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r8 = "Error parsing device-owner file. Bad component name ";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r7.append(r8);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r16;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r7.append(r0);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r7.toString();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        android.util.Slog.e(r1, r7);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        goto L_0x00b6;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00ee:
        r1 = "system-update-policy";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r18;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1 = r1.equals(r0);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        if (r1 == 0) goto L_0x0103;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x00f9:
        r1 = android.app.admin.SystemUpdatePolicy.restoreFromXml(r15);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r21;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0.mSystemUpdatePolicy = r1;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        goto L_0x0020;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x0103:
        r1 = new org.xmlpull.v1.XmlPullParserException;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = new java.lang.StringBuilder;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7.<init>();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r8 = "Unexpected tag in device owner file: ";	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r7.append(r8);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r0 = r18;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r7.append(r0);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r7 = r7.toString();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        r1.<init>(r7);	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        throw r1;	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
    L_0x011f:
        r14.close();	 Catch:{ XmlPullParserException -> 0x005f, XmlPullParserException -> 0x005f }
        goto L_0x0069;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.Owners.readLegacyOwnerFileLocked(java.io.File):boolean");
    }

    void writeDeviceOwner() {
        synchronized (this.mLock) {
            new DeviceOwnerReadWriter().writeToFileLocked();
        }
    }

    void writeProfileOwner(int userId) {
        synchronized (this.mLock) {
            new ProfileOwnerReadWriter(userId).writeToFileLocked();
        }
    }

    boolean saveSystemUpdateInfo(SystemUpdateInfo newInfo) {
        synchronized (this.mLock) {
            if (Objects.equals(newInfo, this.mSystemUpdateInfo)) {
                return false;
            }
            this.mSystemUpdateInfo = newInfo;
            new DeviceOwnerReadWriter().writeToFileLocked();
            return true;
        }
    }

    public SystemUpdateInfo getSystemUpdateInfo() {
        SystemUpdateInfo systemUpdateInfo;
        synchronized (this.mLock) {
            systemUpdateInfo = this.mSystemUpdateInfo;
        }
        return systemUpdateInfo;
    }

    public void dump(String prefix, PrintWriter pw) {
        boolean needBlank = false;
        if (this.mDeviceOwner != null) {
            pw.println(prefix + "Device Owner: ");
            this.mDeviceOwner.dump(prefix + "  ", pw);
            pw.println(prefix + "  User ID: " + this.mDeviceOwnerUserId);
            needBlank = true;
        }
        if (this.mSystemUpdatePolicy != null) {
            if (needBlank) {
                pw.println();
            }
            pw.println(prefix + "System Update Policy: " + this.mSystemUpdatePolicy);
            needBlank = true;
        }
        if (this.mProfileOwners != null) {
            for (Entry<Integer, OwnerInfo> entry : this.mProfileOwners.entrySet()) {
                if (needBlank) {
                    pw.println();
                }
                pw.println(prefix + "Profile Owner (User " + entry.getKey() + "): ");
                ((OwnerInfo) entry.getValue()).dump(prefix + "  ", pw);
                needBlank = true;
            }
        }
        if (this.mSystemUpdateInfo != null) {
            if (needBlank) {
                pw.println();
            }
            pw.println(prefix + "Pending System Update: " + this.mSystemUpdateInfo);
        }
    }

    File getLegacyConfigFileWithTestOverride() {
        File file = new File(Environment.getDataSystemDirectory(), DEVICE_OWNER_XML_LEGACY);
        if (file.exists() || !SystemProperties.getBoolean("persist.sys.set_device_owner", false)) {
            return file;
        }
        file = new File("/system/etc", DEVICE_OWNER_XML_LEGACY);
        SystemProperties.set("persist.sys.set_device_owner", "false");
        return file;
    }

    File getDeviceOwnerFileWithTestOverride() {
        return new File(Environment.getDataSystemDirectory(), DEVICE_OWNER_XML);
    }

    File getProfileOwnerFileWithTestOverride(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), PROFILE_OWNER_XML);
    }
}
