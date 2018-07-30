package android.app.admin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Printer;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class DeviceAdminInfo implements Parcelable {
    public static final Creator<DeviceAdminInfo> CREATOR = new Creator<DeviceAdminInfo>() {
        public DeviceAdminInfo createFromParcel(Parcel source) {
            return new DeviceAdminInfo(source);
        }

        public DeviceAdminInfo[] newArray(int size) {
            return new DeviceAdminInfo[size];
        }
    };
    static final String TAG = "DeviceAdminInfo";
    public static final int USES_ENCRYPTED_STORAGE = 7;
    public static final int USES_POLICY_DEVICE_OWNER = -2;
    public static final int USES_POLICY_DISABLE_CAMERA = 8;
    public static final int USES_POLICY_DISABLE_KEYGUARD_FEATURES = 9;
    public static final int USES_POLICY_EXPIRE_PASSWORD = 6;
    public static final int USES_POLICY_FORCE_LOCK = 3;
    public static final int USES_POLICY_LIMIT_PASSWORD = 0;
    public static final int USES_POLICY_PROFILE_OWNER = -1;
    public static final int USES_POLICY_RESET_PASSWORD = 2;
    public static final int USES_POLICY_SETS_GLOBAL_PROXY = 5;
    public static final int USES_POLICY_WATCH_LOGIN = 1;
    public static final int USES_POLICY_WIPE_DATA = 4;
    static HashMap<String, Integer> sKnownPolicies = new HashMap();
    static ArrayList<PolicyInfo> sPoliciesDisplayOrder = new ArrayList();
    static SparseArray<PolicyInfo> sRevKnownPolicies = new SparseArray();
    final ActivityInfo mActivityInfo;
    int mUsesPolicies;
    boolean mVisible;

    public static class PolicyInfo {
        public final int description;
        public final int descriptionForSecondaryUsers;
        public final int ident;
        public final int label;
        public final int labelForSecondaryUsers;
        public final String tag;

        public PolicyInfo(int ident, String tag, int label, int description) {
            this(ident, tag, label, description, label, description);
        }

        public PolicyInfo(int ident, String tag, int label, int description, int labelForSecondaryUsers, int descriptionForSecondaryUsers) {
            this.ident = ident;
            this.tag = tag;
            this.label = label;
            this.description = description;
            this.labelForSecondaryUsers = labelForSecondaryUsers;
            this.descriptionForSecondaryUsers = descriptionForSecondaryUsers;
        }
    }

    static {
        sPoliciesDisplayOrder.add(new PolicyInfo(4, "wipe-data", 17040712, 17040701, 17040713, 17040702));
        sPoliciesDisplayOrder.add(new PolicyInfo(2, "reset-password", 17040709, 17040697));
        sPoliciesDisplayOrder.add(new PolicyInfo(0, "limit-password", 17040708, 17040696));
        sPoliciesDisplayOrder.add(new PolicyInfo(1, "watch-login", 17040711, 17040699, 17040711, 17040700));
        sPoliciesDisplayOrder.add(new PolicyInfo(3, "force-lock", 17040707, 17040695));
        sPoliciesDisplayOrder.add(new PolicyInfo(5, "set-global-proxy", 17040710, 17040698));
        sPoliciesDisplayOrder.add(new PolicyInfo(6, "expire-password", 17040706, 17040694));
        sPoliciesDisplayOrder.add(new PolicyInfo(7, "encrypted-storage", 17040705, 17040693));
        sPoliciesDisplayOrder.add(new PolicyInfo(8, "disable-camera", 17040703, 17040691));
        sPoliciesDisplayOrder.add(new PolicyInfo(9, "disable-keyguard-features", 17040704, 17040692));
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo pi = (PolicyInfo) sPoliciesDisplayOrder.get(i);
            sRevKnownPolicies.put(pi.ident, pi);
            sKnownPolicies.put(pi.tag, Integer.valueOf(pi.ident));
        }
    }

    public DeviceAdminInfo(Context context, ResolveInfo resolveInfo) throws XmlPullParserException, IOException {
        this(context, resolveInfo.activityInfo);
    }

    public DeviceAdminInfo(Context context, ActivityInfo activityInfo) throws XmlPullParserException, IOException {
        this.mActivityInfo = activityInfo;
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        try {
            parser = this.mActivityInfo.loadXmlMetaData(pm, DeviceAdminReceiver.DEVICE_ADMIN_META_DATA);
            if (parser == null) {
                throw new XmlPullParserException("No android.app.device_admin meta-data");
            }
            int type;
            Resources res = pm.getResourcesForApplication(this.mActivityInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            do {
                type = parser.next();
                if (type == 1) {
                    break;
                }
            } while (type != 2);
            if ("device-admin".equals(parser.getName())) {
                TypedArray sa = res.obtainAttributes(attrs, R.styleable.DeviceAdmin);
                this.mVisible = sa.getBoolean(0, true);
                sa.recycle();
                int outerDepth = parser.getDepth();
                while (true) {
                    type = parser.next();
                    if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        if (parser != null) {
                            parser.close();
                            return;
                        }
                        return;
                    } else if (type != 3 && type != 4 && parser.getName().equals("uses-policies")) {
                        int innerDepth = parser.getDepth();
                        while (true) {
                            type = parser.next();
                            if (type == 1 || (type == 3 && parser.getDepth() <= innerDepth)) {
                                break;
                            } else if (!(type == 3 || type == 4)) {
                                String policyName = parser.getName();
                                Integer val = (Integer) sKnownPolicies.get(policyName);
                                if (val != null) {
                                    this.mUsesPolicies |= 1 << val.intValue();
                                } else {
                                    Log.w(TAG, "Unknown tag under uses-policies of " + getComponent() + ": " + policyName);
                                }
                            }
                        }
                    }
                }
                if (parser != null) {
                    parser.close();
                    return;
                }
                return;
            }
            throw new XmlPullParserException("Meta-data does not start with device-admin tag");
        } catch (NameNotFoundException e) {
            throw new XmlPullParserException("Unable to create context for: " + this.mActivityInfo.packageName);
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    DeviceAdminInfo(Parcel source) {
        this.mActivityInfo = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(source);
        this.mUsesPolicies = source.readInt();
    }

    public String getPackageName() {
        return this.mActivityInfo.packageName;
    }

    public String getReceiverName() {
        return this.mActivityInfo.name;
    }

    public ActivityInfo getActivityInfo() {
        return this.mActivityInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mActivityInfo.loadLabel(pm);
    }

    public CharSequence loadDescription(PackageManager pm) throws NotFoundException {
        if (this.mActivityInfo.descriptionRes != 0) {
            return pm.getText(this.mActivityInfo.packageName, this.mActivityInfo.descriptionRes, this.mActivityInfo.applicationInfo);
        }
        throw new NotFoundException();
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mActivityInfo.loadIcon(pm);
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    public boolean usesPolicy(int policyIdent) {
        return (this.mUsesPolicies & (1 << policyIdent)) != 0;
    }

    public String getTagForPolicy(int policyIdent) {
        return ((PolicyInfo) sRevKnownPolicies.get(policyIdent)).tag;
    }

    public ArrayList<PolicyInfo> getUsedPolicies() {
        ArrayList<PolicyInfo> res = new ArrayList();
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo pi = (PolicyInfo) sPoliciesDisplayOrder.get(i);
            if (usesPolicy(pi.ident)) {
                res.add(pi);
            }
        }
        return res;
    }

    public void writePoliciesToXml(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
        out.attribute(null, "flags", Integer.toString(this.mUsesPolicies));
    }

    public void readPoliciesFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        this.mUsesPolicies = Integer.parseInt(parser.getAttributeValue(null, "flags"));
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + "Receiver:");
        this.mActivityInfo.dump(pw, prefix + "  ");
    }

    public String toString() {
        return "DeviceAdminInfo{" + this.mActivityInfo.name + "}";
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.mActivityInfo.writeToParcel(dest, flags);
        dest.writeInt(this.mUsesPolicies);
    }

    public int describeContents() {
        return 0;
    }
}
