package android.telecom;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import java.util.Objects;

public final class PhoneAccountHandle implements Parcelable {
    public static final Creator<PhoneAccountHandle> CREATOR = new Creator<PhoneAccountHandle>() {
        public PhoneAccountHandle createFromParcel(Parcel in) {
            return new PhoneAccountHandle(in, null);
        }

        public PhoneAccountHandle[] newArray(int size) {
            return new PhoneAccountHandle[size];
        }
    };
    private final ComponentName mComponentName;
    private final String mId;
    private int mSlotId;
    private int mSubId;
    private final UserHandle mUserHandle;

    /* synthetic */ PhoneAccountHandle(Parcel in, PhoneAccountHandle -this1) {
        this(in);
    }

    public PhoneAccountHandle(ComponentName componentName, String id, int subId, int slotId) {
        this(componentName, id, Process.myUserHandle());
        initSubAndSlotId(subId, slotId);
    }

    private void initSubAndSlotId(int subId, int slotId) {
        this.mSubId = subId;
        this.mSlotId = slotId;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public int getSlotId() {
        return this.mSlotId;
    }

    public PhoneAccountHandle(ComponentName componentName, String id) {
        this(componentName, id, Process.myUserHandle());
    }

    public PhoneAccountHandle(ComponentName componentName, String id, UserHandle userHandle) {
        this.mSubId = -1;
        this.mSlotId = -1;
        checkParameters(componentName, userHandle);
        this.mComponentName = componentName;
        this.mId = id;
        this.mUserHandle = userHandle;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public String getId() {
        return this.mId;
    }

    public UserHandle getUserHandle() {
        return this.mUserHandle;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mComponentName, this.mId, this.mUserHandle});
    }

    public String toString() {
        return this.mComponentName + ", " + Log.pii(this.mId) + ", " + this.mUserHandle + ", " + this.mSubId + ",SlotId = " + this.mSlotId;
    }

    public boolean equals(Object other) {
        return (other != null && (other instanceof PhoneAccountHandle) && Objects.equals(((PhoneAccountHandle) other).getComponentName(), getComponentName()) && Objects.equals(((PhoneAccountHandle) other).getId(), getId())) ? Objects.equals(((PhoneAccountHandle) other).getUserHandle(), getUserHandle()) : false;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        this.mComponentName.writeToParcel(out, flags);
        out.writeString(this.mId);
        this.mUserHandle.writeToParcel(out, flags);
        out.writeInt(this.mSubId);
        out.writeInt(this.mSlotId);
    }

    private void checkParameters(ComponentName componentName, UserHandle userHandle) {
        if (componentName == null) {
            Log.w("PhoneAccountHandle", new Exception("PhoneAccountHandle has been created with null ComponentName!"));
        }
        if (userHandle == null) {
            Log.w("PhoneAccountHandle", new Exception("PhoneAccountHandle has been created with null UserHandle!"));
        }
    }

    private PhoneAccountHandle(Parcel in) {
        this((ComponentName) ComponentName.CREATOR.createFromParcel(in), in.readString(), (UserHandle) UserHandle.CREATOR.createFromParcel(in));
        initSubAndSlotId(in.readInt(), in.readInt());
    }
}
