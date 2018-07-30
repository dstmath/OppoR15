package com.color.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ColorLayoutParams implements Parcelable {
    public static final Creator<ColorLayoutParams> CREATOR = new Creator<ColorLayoutParams>() {
        public ColorLayoutParams createFromParcel(Parcel in) {
            return new ColorLayoutParams(in);
        }

        public ColorLayoutParams[] newArray(int size) {
            return new ColorLayoutParams[size];
        }
    };
    private boolean mHasNavigationBar = false;
    private boolean mHasStatusBar = false;
    private int mNavigationBarColor = 0;
    private boolean mUpdateNavigationBar = false;

    public ColorLayoutParams(Parcel in) {
        readFromParcel(in);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" ColorLayoutParams[");
        sb.append(" mHasStatusBar=");
        sb.append(this.mHasStatusBar);
        sb.append(" mHasNavigationBar=");
        sb.append(this.mHasNavigationBar);
        sb.append(" mUpdateNavigationBar=");
        sb.append(this.mUpdateNavigationBar);
        sb.append(" mNavigationBarColor=0x");
        sb.append(Integer.toHexString(this.mNavigationBarColor));
        sb.append(" ]");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ColorLayoutParams other = (ColorLayoutParams) obj;
        return this.mHasStatusBar == other.mHasStatusBar && this.mHasNavigationBar == other.mHasNavigationBar && this.mUpdateNavigationBar == other.mUpdateNavigationBar && this.mNavigationBarColor == other.mNavigationBarColor;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        int i;
        int i2 = 1;
        if (this.mHasStatusBar) {
            i = 1;
        } else {
            i = 0;
        }
        out.writeInt(i);
        if (this.mHasNavigationBar) {
            i = 1;
        } else {
            i = 0;
        }
        out.writeInt(i);
        if (!this.mUpdateNavigationBar) {
            i2 = 0;
        }
        out.writeInt(i2);
        out.writeInt(this.mNavigationBarColor);
    }

    public void readFromParcel(Parcel in) {
        boolean z;
        boolean z2 = true;
        if (in.readInt() == 1) {
            z = true;
        } else {
            z = false;
        }
        this.mHasStatusBar = z;
        if (in.readInt() == 1) {
            z = true;
        } else {
            z = false;
        }
        this.mHasNavigationBar = z;
        if (in.readInt() != 1) {
            z2 = false;
        }
        this.mUpdateNavigationBar = z2;
        this.mNavigationBarColor = in.readInt();
    }

    public void set(ColorLayoutParams src) {
        this.mHasStatusBar = src.mHasStatusBar;
        this.mHasNavigationBar = src.mHasNavigationBar;
        this.mUpdateNavigationBar = src.mUpdateNavigationBar;
        this.mNavigationBarColor = src.mNavigationBarColor;
    }

    public void setHasStatusBar(boolean value) {
        this.mHasStatusBar = value;
    }

    public boolean hasStatusBar() {
        return this.mHasStatusBar;
    }

    public void setHasNavigationBar(boolean value) {
        this.mHasNavigationBar = value;
    }

    public boolean hasNavigationBar() {
        return this.mHasNavigationBar;
    }

    public void setUpdateNavigationBar(boolean value) {
        this.mUpdateNavigationBar = value;
    }

    public boolean isUpdateNavigationBar() {
        return this.mUpdateNavigationBar;
    }

    public void setNavigationBarColor(int value) {
        this.mNavigationBarColor = value;
    }

    public int getNavigationBarColor() {
        return this.mNavigationBarColor;
    }
}
