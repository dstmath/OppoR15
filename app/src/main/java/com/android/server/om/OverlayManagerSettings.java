package com.android.server.om;

import android.content.om.OverlayInfo;
import android.util.ArrayMap;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.om.-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.AnonymousClass1;
import com.android.server.om.-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.AnonymousClass2;
import com.android.server.om.-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.AnonymousClass3;
import com.android.server.om.-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.AnonymousClass4;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class OverlayManagerSettings {
    private final ArrayList<SettingsItem> mItems = new ArrayList();

    static final class BadKeyException extends RuntimeException {
        BadKeyException(String packageName, int userId) {
            super("Bad key mPackageName=" + packageName + " mUserId=" + userId);
        }
    }

    private static final class Serializer {
        private static final String ATTR_BASE_CODE_PATH = "baseCodePath";
        private static final String ATTR_IS_ENABLED = "isEnabled";
        private static final String ATTR_IS_STATIC = "isStatic";
        private static final String ATTR_PACKAGE_NAME = "packageName";
        private static final String ATTR_PRIORITY = "priority";
        private static final String ATTR_STATE = "state";
        private static final String ATTR_TARGET_PACKAGE_NAME = "targetPackageName";
        private static final String ATTR_USER_ID = "userId";
        private static final String ATTR_VERSION = "version";
        private static final int CURRENT_VERSION = 3;
        private static final String TAG_ITEM = "item";
        private static final String TAG_OVERLAYS = "overlays";

        private Serializer() {
        }

        public static void restore(ArrayList<SettingsItem> table, InputStream is) throws IOException, XmlPullParserException {
            Throwable th;
            Throwable th2 = null;
            InputStreamReader reader = null;
            try {
                InputStreamReader reader2 = new InputStreamReader(is);
                try {
                    table.clear();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(reader2);
                    XmlUtils.beginDocument(parser, TAG_OVERLAYS);
                    int version = XmlUtils.readIntAttribute(parser, "version");
                    if (version != 3) {
                        upgrade(version);
                    }
                    int depth = parser.getDepth();
                    while (XmlUtils.nextElementWithin(parser, depth)) {
                        if (parser.getName().equals(TAG_ITEM)) {
                            table.add(restoreRow(parser, depth + 1));
                        }
                    }
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    reader = reader2;
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        throw th;
                    }
                    throw th2;
                }
            } catch (Throwable th6) {
                th = th6;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable th52) {
                        if (th2 == null) {
                            th2 = th52;
                        } else if (th2 != th52) {
                            th2.addSuppressed(th52);
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        }

        private static void upgrade(int oldVersion) throws XmlPullParserException {
            switch (oldVersion) {
                case 0:
                case 1:
                case 2:
                    throw new XmlPullParserException("old version " + oldVersion + "; ignoring");
                default:
                    throw new XmlPullParserException("unrecognized version " + oldVersion);
            }
        }

        private static SettingsItem restoreRow(XmlPullParser parser, int depth) throws IOException {
            return new SettingsItem(XmlUtils.readStringAttribute(parser, "packageName"), XmlUtils.readIntAttribute(parser, ATTR_USER_ID), XmlUtils.readStringAttribute(parser, ATTR_TARGET_PACKAGE_NAME), XmlUtils.readStringAttribute(parser, ATTR_BASE_CODE_PATH), XmlUtils.readIntAttribute(parser, "state"), XmlUtils.readBooleanAttribute(parser, ATTR_IS_ENABLED), XmlUtils.readBooleanAttribute(parser, ATTR_IS_STATIC), XmlUtils.readIntAttribute(parser, ATTR_PRIORITY));
        }

        public static void persist(ArrayList<SettingsItem> table, OutputStream os) throws IOException, XmlPullParserException {
            FastXmlSerializer xml = new FastXmlSerializer();
            xml.setOutput(os, "utf-8");
            xml.startDocument(null, Boolean.valueOf(true));
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startTag(null, TAG_OVERLAYS);
            XmlUtils.writeIntAttribute(xml, "version", 3);
            int N = table.size();
            for (int i = 0; i < N; i++) {
                persistRow(xml, (SettingsItem) table.get(i));
            }
            xml.endTag(null, TAG_OVERLAYS);
            xml.endDocument();
        }

        private static void persistRow(FastXmlSerializer xml, SettingsItem item) throws IOException {
            xml.startTag(null, TAG_ITEM);
            XmlUtils.writeStringAttribute(xml, "packageName", item.mPackageName);
            XmlUtils.writeIntAttribute(xml, ATTR_USER_ID, item.mUserId);
            XmlUtils.writeStringAttribute(xml, ATTR_TARGET_PACKAGE_NAME, item.mTargetPackageName);
            XmlUtils.writeStringAttribute(xml, ATTR_BASE_CODE_PATH, item.mBaseCodePath);
            XmlUtils.writeIntAttribute(xml, "state", item.mState);
            XmlUtils.writeBooleanAttribute(xml, ATTR_IS_ENABLED, item.mIsEnabled);
            XmlUtils.writeBooleanAttribute(xml, ATTR_IS_STATIC, item.mIsStatic);
            XmlUtils.writeIntAttribute(xml, ATTR_PRIORITY, item.mPriority);
            xml.endTag(null, TAG_ITEM);
        }
    }

    private static final class SettingsItem {
        private String mBaseCodePath;
        private OverlayInfo mCache;
        private boolean mIsEnabled;
        private boolean mIsStatic;
        private final String mPackageName;
        private int mPriority;
        private int mState;
        private final String mTargetPackageName;
        private final int mUserId;

        SettingsItem(String packageName, int userId, String targetPackageName, String baseCodePath, int state, boolean isEnabled, boolean isStatic, int priority) {
            this.mPackageName = packageName;
            this.mUserId = userId;
            this.mTargetPackageName = targetPackageName;
            this.mBaseCodePath = baseCodePath;
            this.mState = state;
            this.mIsEnabled = isEnabled;
            this.mCache = null;
            this.mIsStatic = isStatic;
            this.mPriority = priority;
        }

        SettingsItem(String packageName, int userId, String targetPackageName, String baseCodePath, boolean isStatic, int priority) {
            this(packageName, userId, targetPackageName, baseCodePath, -1, false, isStatic, priority);
        }

        private String getTargetPackageName() {
            return this.mTargetPackageName;
        }

        private int getUserId() {
            return this.mUserId;
        }

        private String getBaseCodePath() {
            return this.mBaseCodePath;
        }

        private boolean setBaseCodePath(String path) {
            if (this.mBaseCodePath.equals(path)) {
                return false;
            }
            this.mBaseCodePath = path;
            invalidateCache();
            return true;
        }

        private int getState() {
            return this.mState;
        }

        private boolean setState(int state) {
            if (this.mState == state) {
                return false;
            }
            this.mState = state;
            invalidateCache();
            return true;
        }

        private boolean isEnabled() {
            return this.mIsEnabled;
        }

        private boolean setEnabled(boolean enable) {
            if (this.mIsEnabled == enable) {
                return false;
            }
            this.mIsEnabled = enable;
            invalidateCache();
            return true;
        }

        private OverlayInfo getOverlayInfo() {
            if (this.mCache == null) {
                this.mCache = new OverlayInfo(this.mPackageName, this.mTargetPackageName, this.mBaseCodePath, this.mState, this.mUserId);
            }
            return this.mCache;
        }

        private void invalidateCache() {
            this.mCache = null;
        }

        private boolean isStatic() {
            return this.mIsStatic;
        }

        private int getPriority() {
            return this.mPriority;
        }
    }

    OverlayManagerSettings() {
    }

    void init(String packageName, int userId, String targetPackageName, String baseCodePath, boolean isStatic, int priority) {
        remove(packageName, userId);
        SettingsItem item = new SettingsItem(packageName, userId, targetPackageName, baseCodePath, isStatic, priority);
        if (isStatic) {
            int i = this.mItems.size() - 1;
            while (i >= 0) {
                SettingsItem parentItem = (SettingsItem) this.mItems.get(i);
                if (parentItem.mIsStatic && parentItem.mPriority <= priority) {
                    break;
                }
                i--;
            }
            int pos = i + 1;
            if (pos == this.mItems.size()) {
                this.mItems.add(item);
                return;
            } else {
                this.mItems.add(pos, item);
                return;
            }
        }
        this.mItems.add(item);
    }

    boolean remove(String packageName, int userId) {
        int idx = select(packageName, userId);
        if (idx < 0) {
            return false;
        }
        this.mItems.remove(idx);
        return true;
    }

    OverlayInfo getOverlayInfo(String packageName, int userId) throws BadKeyException {
        int idx = select(packageName, userId);
        if (idx >= 0) {
            return ((SettingsItem) this.mItems.get(idx)).getOverlayInfo();
        }
        throw new BadKeyException(packageName, userId);
    }

    boolean setBaseCodePath(String packageName, int userId, String path) throws BadKeyException {
        int idx = select(packageName, userId);
        if (idx >= 0) {
            return ((SettingsItem) this.mItems.get(idx)).setBaseCodePath(path);
        }
        throw new BadKeyException(packageName, userId);
    }

    boolean getEnabled(String packageName, int userId) throws BadKeyException {
        int idx = select(packageName, userId);
        if (idx >= 0) {
            return ((SettingsItem) this.mItems.get(idx)).isEnabled();
        }
        throw new BadKeyException(packageName, userId);
    }

    boolean setEnabled(String packageName, int userId, boolean enable) throws BadKeyException {
        int idx = select(packageName, userId);
        if (idx >= 0) {
            return ((SettingsItem) this.mItems.get(idx)).setEnabled(enable);
        }
        throw new BadKeyException(packageName, userId);
    }

    int getState(String packageName, int userId) throws BadKeyException {
        int idx = select(packageName, userId);
        if (idx >= 0) {
            return ((SettingsItem) this.mItems.get(idx)).getState();
        }
        throw new BadKeyException(packageName, userId);
    }

    boolean setState(String packageName, int userId, int state) throws BadKeyException {
        int idx = select(packageName, userId);
        if (idx >= 0) {
            return ((SettingsItem) this.mItems.get(idx)).setState(state);
        }
        throw new BadKeyException(packageName, userId);
    }

    List<OverlayInfo> getOverlaysForTarget(String targetPackageName, int userId) {
        return (List) selectWhereTarget(targetPackageName, userId).map(-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.$INST$0).collect(Collectors.toList());
    }

    ArrayMap<String, List<OverlayInfo>> getOverlaysForUser(int userId) {
        return (ArrayMap) selectWhereUser(userId).map(-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.$INST$1).collect(Collectors.groupingBy(-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk.$INST$2, AnonymousClass1.$INST$0, Collectors.toList()));
    }

    int[] getUsers() {
        return this.mItems.stream().mapToInt(AnonymousClass2.$INST$0).distinct().toArray();
    }

    boolean removeUser(int userId) {
        boolean removed = false;
        int i = 0;
        while (i < this.mItems.size()) {
            if (((SettingsItem) this.mItems.get(i)).getUserId() == userId) {
                this.mItems.remove(i);
                removed = true;
                i--;
            }
            i++;
        }
        return removed;
    }

    boolean setPriority(String packageName, String newParentPackageName, int userId) {
        if (packageName.equals(newParentPackageName)) {
            return false;
        }
        int moveIdx = select(packageName, userId);
        if (moveIdx < 0) {
            return false;
        }
        int parentIdx = select(newParentPackageName, userId);
        if (parentIdx < 0) {
            return false;
        }
        SettingsItem itemToMove = (SettingsItem) this.mItems.get(moveIdx);
        if (!itemToMove.getTargetPackageName().equals(((SettingsItem) this.mItems.get(parentIdx)).getTargetPackageName())) {
            return false;
        }
        this.mItems.remove(moveIdx);
        int newParentIdx = select(newParentPackageName, userId);
        this.mItems.add(newParentIdx, itemToMove);
        return moveIdx != newParentIdx;
    }

    boolean setLowestPriority(String packageName, int userId) {
        int idx = select(packageName, userId);
        if (idx <= 0) {
            return false;
        }
        SettingsItem item = (SettingsItem) this.mItems.get(idx);
        this.mItems.remove(item);
        this.mItems.add(0, item);
        return true;
    }

    boolean setHighestPriority(String packageName, int userId) {
        int idx = select(packageName, userId);
        if (idx < 0 || idx == this.mItems.size() - 1) {
            return false;
        }
        SettingsItem item = (SettingsItem) this.mItems.get(idx);
        this.mItems.remove(idx);
        this.mItems.add(item);
        return true;
    }

    void dump(PrintWriter p) {
        IndentingPrintWriter pw = new IndentingPrintWriter(p, "  ");
        pw.println("Settings");
        pw.increaseIndent();
        if (this.mItems.isEmpty()) {
            pw.println("<none>");
            return;
        }
        int N = this.mItems.size();
        for (int i = 0; i < N; i++) {
            SettingsItem item = (SettingsItem) this.mItems.get(i);
            pw.println(item.mPackageName + ":" + item.getUserId() + " {");
            pw.increaseIndent();
            pw.print("mPackageName.......: ");
            pw.println(item.mPackageName);
            pw.print("mUserId............: ");
            pw.println(item.getUserId());
            pw.print("mTargetPackageName.: ");
            pw.println(item.getTargetPackageName());
            pw.print("mBaseCodePath......: ");
            pw.println(item.getBaseCodePath());
            pw.print("mState.............: ");
            pw.println(OverlayInfo.stateToString(item.getState()));
            pw.print("mIsEnabled.........: ");
            pw.println(item.isEnabled());
            pw.print("mIsStatic..........: ");
            pw.println(item.isStatic());
            pw.decreaseIndent();
            pw.println("}");
        }
    }

    void restore(InputStream is) throws IOException, XmlPullParserException {
        Serializer.restore(this.mItems, is);
    }

    void persist(OutputStream os) throws IOException, XmlPullParserException {
        Serializer.persist(this.mItems, os);
    }

    private int select(String packageName, int userId) {
        int N = this.mItems.size();
        for (int i = 0; i < N; i++) {
            SettingsItem item = (SettingsItem) this.mItems.get(i);
            if (item.mUserId == userId && item.mPackageName.equals(packageName)) {
                return i;
            }
        }
        return -1;
    }

    static /* synthetic */ boolean lambda$-com_android_server_om_OverlayManagerSettings_19551(int userId, SettingsItem item) {
        return item.mUserId == userId;
    }

    private Stream<SettingsItem> selectWhereUser(int userId) {
        return this.mItems.stream().filter(new AnonymousClass4(userId));
    }

    private Stream<SettingsItem> selectWhereTarget(String targetPackageName, int userId) {
        return selectWhereUser(userId).filter(new AnonymousClass3(targetPackageName));
    }
}
