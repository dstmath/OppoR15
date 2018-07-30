package com.color.screenshot;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class ColorLongshotViewInfo implements Parcelable {
    public static final Creator<ColorLongshotViewInfo> CREATOR = new Creator<ColorLongshotViewInfo>() {
        public ColorLongshotViewInfo createFromParcel(Parcel in) {
            return new ColorLongshotViewInfo(in);
        }

        public ColorLongshotViewInfo[] newArray(int size) {
            return new ColorLongshotViewInfo[size];
        }
    };

    public ColorLongshotViewInfo(Parcel in) {
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    }

    public void readFromParcel(Parcel in) {
    }
}
