package com.color.screenshot;

public abstract class ColorLongshotController {
    final String mSource;
    final ColorLongshotViewBase mView;

    public ColorLongshotController(ColorLongshotViewBase view, String source) {
        this.mView = view;
        this.mSource = source;
    }

    public boolean findInfo(ColorLongshotViewInfo info) {
        return true;
    }
}
