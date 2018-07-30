package android.view.inputmethod;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.voice.VoiceInteractionSession;
import com.android.internal.inputmethod.IInputContentUriToken;
import com.android.internal.inputmethod.IInputContentUriToken.Stub;
import java.security.InvalidParameterException;

public final class InputContentInfo implements Parcelable {
    public static final Creator<InputContentInfo> CREATOR = new Creator<InputContentInfo>() {
        public InputContentInfo createFromParcel(Parcel source) {
            return new InputContentInfo(source, null);
        }

        public InputContentInfo[] newArray(int size) {
            return new InputContentInfo[size];
        }
    };
    private final Uri mContentUri;
    private final int mContentUriOwnerUserId;
    private final ClipDescription mDescription;
    private final Uri mLinkUri;
    private IInputContentUriToken mUriToken;

    /* synthetic */ InputContentInfo(Parcel source, InputContentInfo -this1) {
        this(source);
    }

    public InputContentInfo(Uri contentUri, ClipDescription description) {
        this(contentUri, description, null);
    }

    public InputContentInfo(Uri contentUri, ClipDescription description, Uri linkUri) {
        validateInternal(contentUri, description, linkUri, true);
        this.mContentUri = contentUri;
        this.mContentUriOwnerUserId = ContentProvider.getUserIdFromUri(this.mContentUri, UserHandle.myUserId());
        this.mDescription = description;
        this.mLinkUri = linkUri;
    }

    public boolean validate() {
        return validateInternal(this.mContentUri, this.mDescription, this.mLinkUri, false);
    }

    private static boolean validateInternal(Uri contentUri, ClipDescription description, Uri linkUri, boolean throwException) {
        if (contentUri == null) {
            if (!throwException) {
                return false;
            }
            throw new NullPointerException("contentUri");
        } else if (description != null) {
            if (VoiceInteractionSession.KEY_CONTENT.equals(contentUri.getScheme())) {
                if (linkUri != null) {
                    String scheme = linkUri.getScheme();
                    if (scheme == null || !(scheme.equalsIgnoreCase("http") || (scheme.equalsIgnoreCase("https") ^ 1) == 0)) {
                        if (!throwException) {
                            return false;
                        }
                        throw new InvalidParameterException("linkUri must have either http or https scheme");
                    }
                }
                return true;
            } else if (!throwException) {
                return false;
            } else {
                throw new InvalidParameterException("contentUri must have content scheme");
            }
        } else if (!throwException) {
            return false;
        } else {
            throw new NullPointerException("description");
        }
    }

    public Uri getContentUri() {
        if (this.mContentUriOwnerUserId != UserHandle.myUserId()) {
            return ContentProvider.maybeAddUserId(this.mContentUri, this.mContentUriOwnerUserId);
        }
        return this.mContentUri;
    }

    public ClipDescription getDescription() {
        return this.mDescription;
    }

    public Uri getLinkUri() {
        return this.mLinkUri;
    }

    void setUriToken(IInputContentUriToken token) {
        if (this.mUriToken != null) {
            throw new IllegalStateException("URI token is already set");
        }
        this.mUriToken = token;
    }

    public void requestPermission() {
        if (this.mUriToken != null) {
            try {
                this.mUriToken.take();
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public void releasePermission() {
        if (this.mUriToken != null) {
            try {
                this.mUriToken.release();
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        Uri.writeToParcel(dest, this.mContentUri);
        dest.writeInt(this.mContentUriOwnerUserId);
        this.mDescription.writeToParcel(dest, flags);
        Uri.writeToParcel(dest, this.mLinkUri);
        if (this.mUriToken != null) {
            dest.writeInt(1);
            dest.writeStrongBinder(this.mUriToken.asBinder());
            return;
        }
        dest.writeInt(0);
    }

    private InputContentInfo(Parcel source) {
        this.mContentUri = (Uri) Uri.CREATOR.createFromParcel(source);
        this.mContentUriOwnerUserId = source.readInt();
        this.mDescription = (ClipDescription) ClipDescription.CREATOR.createFromParcel(source);
        this.mLinkUri = (Uri) Uri.CREATOR.createFromParcel(source);
        if (source.readInt() == 1) {
            this.mUriToken = Stub.asInterface(source.readStrongBinder());
        } else {
            this.mUriToken = null;
        }
    }

    public int describeContents() {
        return 0;
    }
}
