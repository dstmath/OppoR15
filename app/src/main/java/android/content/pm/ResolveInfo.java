package android.content.pm;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.app.OppoThemeHelper;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Printer;
import android.util.Slog;
import java.text.Collator;
import java.util.Comparator;

public class ResolveInfo implements Parcelable {
    public static final Creator<ResolveInfo> CREATOR = new Creator<ResolveInfo>() {
        public ResolveInfo createFromParcel(Parcel source) {
            return new ResolveInfo(source, null);
        }

        public ResolveInfo[] newArray(int size) {
            return new ResolveInfo[size];
        }
    };
    private static final String TAG = "ResolveInfo";
    public ActivityInfo activityInfo;
    public AuxiliaryResolveInfo auxiliaryInfo;
    public IntentFilter filter;
    public boolean handleAllWebDataURI;
    public int icon;
    public int iconResourceId;
    @Deprecated
    public boolean instantAppAvailable;
    public boolean isDefault;
    public boolean isInstantAppAvailable;
    public boolean isMultiApp;
    public int labelRes;
    public int match;
    public boolean noResourceId;
    public CharSequence nonLocalizedLabel;
    public int preferredOrder;
    public int priority;
    public ProviderInfo providerInfo;
    public String resolvePackageName;
    public ServiceInfo serviceInfo;
    public int specificIndex;
    public boolean system;
    public int targetUserId;

    public static class DisplayNameComparator implements Comparator<ResolveInfo> {
        private final Collator mCollator = Collator.getInstance();
        private PackageManager mPM;

        public DisplayNameComparator(PackageManager pm) {
            this.mPM = pm;
            this.mCollator.setStrength(0);
        }

        public final int compare(ResolveInfo a, ResolveInfo b) {
            if (a.targetUserId != -2) {
                return 1;
            }
            if (b.targetUserId != -2) {
                return -1;
            }
            CharSequence sa = a.loadLabel(this.mPM);
            if (sa == null) {
                sa = a.activityInfo.name;
            }
            CharSequence sb = b.loadLabel(this.mPM);
            if (sb == null) {
                sb = b.activityInfo.name;
            }
            return this.mCollator.compare(sa.toString(), sb.toString());
        }
    }

    /* synthetic */ ResolveInfo(Parcel source, ResolveInfo -this1) {
        this(source);
    }

    public ComponentInfo getComponentInfo() {
        if (this.activityInfo != null) {
            return this.activityInfo;
        }
        if (this.serviceInfo != null) {
            return this.serviceInfo;
        }
        if (this.providerInfo != null) {
            return this.providerInfo;
        }
        throw new IllegalStateException("Missing ComponentInfo!");
    }

    public CharSequence loadLabel(PackageManager pm) {
        if (this.nonLocalizedLabel != null) {
            return this.nonLocalizedLabel;
        }
        CharSequence label;
        if (!(this.resolvePackageName == null || this.labelRes == 0)) {
            label = pm.getText(this.resolvePackageName, this.labelRes, null);
            if (label != null) {
                return label.toString().trim();
            }
        }
        ComponentInfo ci = getComponentInfo();
        ApplicationInfo ai = ci.applicationInfo;
        if (this.labelRes != 0) {
            label = pm.getText(ci.packageName, this.labelRes, ai);
            if (label != null) {
                return label.toString().trim();
            }
        }
        CharSequence data = ci.loadLabel(pm);
        if (data != null) {
            data = data.toString().trim();
        }
        return data;
    }

    public int resolveLabelResId() {
        if (this.labelRes != 0) {
            return this.labelRes;
        }
        ComponentInfo componentInfo = getComponentInfo();
        if (componentInfo.labelRes != 0) {
            return componentInfo.labelRes;
        }
        return componentInfo.applicationInfo.labelRes;
    }

    public int resolveIconResId() {
        if (this.icon != 0) {
            return this.icon;
        }
        ComponentInfo componentInfo = getComponentInfo();
        if (componentInfo.icon != 0) {
            return componentInfo.icon;
        }
        return componentInfo.applicationInfo.icon;
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "YaoJun.Luo@Plf.SDK : Modify for rom theme", property = OppoRomType.ROM)
    public Drawable loadIcon(PackageManager pm) {
        Drawable dr = null;
        if (!(this.resolvePackageName == null || this.iconResourceId == 0)) {
            dr = loadDrawableFromTheme(pm, this.resolvePackageName, null);
            if (dr == null) {
                dr = pm.getDrawable(this.resolvePackageName, this.iconResourceId, null);
            }
        }
        ComponentInfo ci = getComponentInfo();
        if (dr == null && this.iconResourceId != 0) {
            ApplicationInfo ai = ci.applicationInfo;
            dr = loadDrawableFromTheme(pm, ci.packageName, ai);
            if (dr == null) {
                dr = pm.getDrawable(ci.packageName, this.iconResourceId, ai);
            }
        }
        if (dr != null) {
            return pm.getUserBadgedIcon(dr, new UserHandle(UserHandle.myUserId()));
        }
        return ci.loadIcon(pm);
    }

    final int getIconResourceInternal() {
        if (this.iconResourceId != 0) {
            return this.iconResourceId;
        }
        ComponentInfo ci = getComponentInfo();
        if (ci != null) {
            return ci.getIconResource();
        }
        return 0;
    }

    public final int getIconResource() {
        if (this.noResourceId) {
            return 0;
        }
        return getIconResourceInternal();
    }

    public void dump(Printer pw, String prefix) {
        dump(pw, prefix, 3);
    }

    public void dump(Printer pw, String prefix, int dumpFlags) {
        if (this.filter != null) {
            pw.println(prefix + "Filter:");
            this.filter.dump(pw, prefix + "  ");
        }
        pw.println(prefix + "priority=" + this.priority + " preferredOrder=" + this.preferredOrder + " match=0x" + Integer.toHexString(this.match) + " specificIndex=" + this.specificIndex + " isDefault=" + this.isDefault);
        if (this.resolvePackageName != null) {
            pw.println(prefix + "resolvePackageName=" + this.resolvePackageName);
        }
        if (!(this.labelRes == 0 && this.nonLocalizedLabel == null && this.icon == 0)) {
            pw.println(prefix + "labelRes=0x" + Integer.toHexString(this.labelRes) + " nonLocalizedLabel=" + this.nonLocalizedLabel + " icon=0x" + Integer.toHexString(this.icon));
        }
        if (this.activityInfo != null) {
            pw.println(prefix + "ActivityInfo:");
            this.activityInfo.dump(pw, prefix + "  ", dumpFlags);
        } else if (this.serviceInfo != null) {
            pw.println(prefix + "ServiceInfo:");
            this.serviceInfo.dump(pw, prefix + "  ", dumpFlags);
        } else if (this.providerInfo != null) {
            pw.println(prefix + "ProviderInfo:");
            this.providerInfo.dump(pw, prefix + "  ", dumpFlags);
        }
    }

    public ResolveInfo() {
        this.specificIndex = -1;
        this.isMultiApp = false;
        this.targetUserId = -2;
    }

    public ResolveInfo(ResolveInfo orig) {
        this.specificIndex = -1;
        this.isMultiApp = false;
        this.activityInfo = orig.activityInfo;
        this.serviceInfo = orig.serviceInfo;
        this.providerInfo = orig.providerInfo;
        this.filter = orig.filter;
        this.priority = orig.priority;
        this.preferredOrder = orig.preferredOrder;
        this.match = orig.match;
        this.specificIndex = orig.specificIndex;
        this.labelRes = orig.labelRes;
        this.nonLocalizedLabel = orig.nonLocalizedLabel;
        this.icon = orig.icon;
        this.resolvePackageName = orig.resolvePackageName;
        this.noResourceId = orig.noResourceId;
        this.iconResourceId = orig.iconResourceId;
        this.system = orig.system;
        this.targetUserId = orig.targetUserId;
        this.handleAllWebDataURI = orig.handleAllWebDataURI;
        this.isInstantAppAvailable = orig.isInstantAppAvailable;
        this.instantAppAvailable = this.isInstantAppAvailable;
    }

    public String toString() {
        ComponentInfo ci = getComponentInfo();
        StringBuilder sb = new StringBuilder(128);
        sb.append("ResolveInfo{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        ComponentName.appendShortString(sb, ci.packageName, ci.name);
        if (this.priority != 0) {
            sb.append(" p=");
            sb.append(this.priority);
        }
        if (this.preferredOrder != 0) {
            sb.append(" o=");
            sb.append(this.preferredOrder);
        }
        sb.append(" m=0x");
        sb.append(Integer.toHexString(this.match));
        if (this.targetUserId != -2) {
            sb.append(" targetUserId=");
            sb.append(this.targetUserId);
        }
        sb.append('}');
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        int i;
        int i2 = 1;
        if (this.activityInfo != null) {
            dest.writeInt(1);
            this.activityInfo.writeToParcel(dest, parcelableFlags);
        } else if (this.serviceInfo != null) {
            dest.writeInt(2);
            this.serviceInfo.writeToParcel(dest, parcelableFlags);
        } else if (this.providerInfo != null) {
            dest.writeInt(3);
            this.providerInfo.writeToParcel(dest, parcelableFlags);
        } else {
            dest.writeInt(0);
        }
        if (this.filter != null) {
            dest.writeInt(1);
            this.filter.writeToParcel(dest, parcelableFlags);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.priority);
        dest.writeInt(this.preferredOrder);
        dest.writeInt(this.match);
        dest.writeInt(this.specificIndex);
        dest.writeInt(this.labelRes);
        TextUtils.writeToParcel(this.nonLocalizedLabel, dest, parcelableFlags);
        dest.writeInt(this.icon);
        dest.writeString(this.resolvePackageName);
        dest.writeInt(this.targetUserId);
        dest.writeInt(this.system ? 1 : 0);
        if (this.noResourceId) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeInt(this.iconResourceId);
        if (this.handleAllWebDataURI) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        if (!this.isInstantAppAvailable) {
            i2 = 0;
        }
        dest.writeInt(i2);
    }

    private ResolveInfo(Parcel source) {
        boolean z;
        boolean z2 = true;
        this.specificIndex = -1;
        this.isMultiApp = false;
        this.activityInfo = null;
        this.serviceInfo = null;
        this.providerInfo = null;
        switch (source.readInt()) {
            case 1:
                this.activityInfo = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(source);
                break;
            case 2:
                this.serviceInfo = (ServiceInfo) ServiceInfo.CREATOR.createFromParcel(source);
                break;
            case 3:
                this.providerInfo = (ProviderInfo) ProviderInfo.CREATOR.createFromParcel(source);
                break;
            default:
                Slog.w(TAG, "Missing ComponentInfo!");
                break;
        }
        if (source.readInt() != 0) {
            this.filter = (IntentFilter) IntentFilter.CREATOR.createFromParcel(source);
        }
        this.priority = source.readInt();
        this.preferredOrder = source.readInt();
        this.match = source.readInt();
        this.specificIndex = source.readInt();
        this.labelRes = source.readInt();
        this.nonLocalizedLabel = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        this.icon = source.readInt();
        this.resolvePackageName = source.readString();
        this.targetUserId = source.readInt();
        this.system = source.readInt() != 0;
        if (source.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.noResourceId = z;
        this.iconResourceId = source.readInt();
        if (source.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.handleAllWebDataURI = z;
        if (source.readInt() == 0) {
            z2 = false;
        }
        this.isInstantAppAvailable = z2;
        this.instantAppAvailable = z2;
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "YaoJun.Luo@Plf.SDK : Add for rom theme", property = OppoRomType.ROM)
    private Drawable loadDrawableFromTheme(PackageManager pm, String packageName, ApplicationInfo ai) {
        return OppoThemeHelper.getDrawable(pm, packageName, this.icon, ai, this.activityInfo != null ? this.activityInfo : this.serviceInfo, OppoThemeHelper.isCustomizedIcon(this.filter));
    }
}
