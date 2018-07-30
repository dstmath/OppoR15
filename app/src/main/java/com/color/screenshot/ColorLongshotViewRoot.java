package com.color.screenshot;

import com.color.util.ColorLog;

public class ColorLongshotViewRoot {
    private static final String TAG = "LongshotDump";
    private boolean mIsLongshotConnected = false;

    public void setLongshotConnected(boolean isConnected) {
        this.mIsLongshotConnected = isConnected;
        ColorLog.d("LongshotDump", "setLongshotConnected : ", Boolean.valueOf(this.mIsLongshotConnected));
    }

    public boolean isLongshotConnected() {
        return this.mIsLongshotConnected;
    }
}
