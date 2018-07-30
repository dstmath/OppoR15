package android.os;

import android.content.pm.UserInfo;
import android.graphics.Bitmap;

public abstract class UserManagerInternal {
    public static final int CAMERA_DISABLED_GLOBALLY = 2;
    public static final int CAMERA_DISABLED_LOCALLY = 1;
    public static final int CAMERA_NOT_DISABLED = 0;

    public interface UserRestrictionsListener {
        void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2);
    }

    public abstract void addUserRestrictionsListener(UserRestrictionsListener userRestrictionsListener);

    public abstract UserInfo createUserEvenWhenDisallowed(String str, int i);

    public abstract Bundle getBaseUserRestrictions(int i);

    public abstract int[] getUserIds();

    public abstract boolean getUserRestriction(int i, String str);

    public abstract boolean isUserRunning(int i);

    public abstract boolean isUserUnlocked(int i);

    public abstract boolean isUserUnlockingOrUnlocked(int i);

    public abstract void onEphemeralUserStop(int i);

    public abstract void removeAllUsers();

    public abstract boolean removeUserEvenWhenDisallowed(int i);

    public abstract void removeUserRestrictionsListener(UserRestrictionsListener userRestrictionsListener);

    public abstract void removeUserState(int i);

    public abstract void setBaseUserRestrictionsByDpmsForMigration(int i, Bundle bundle);

    public abstract void setDeviceManaged(boolean z);

    public abstract void setDevicePolicyUserRestrictions(int i, Bundle bundle, boolean z, int i2);

    public abstract void setForceEphemeralUsers(boolean z);

    public abstract void setUserIcon(int i, Bitmap bitmap);

    public abstract void setUserManaged(int i, boolean z);

    public abstract void setUserState(int i, int i2);
}
