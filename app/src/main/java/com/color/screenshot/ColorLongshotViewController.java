package com.color.screenshot;

import android.view.View;
import android.view.ViewRootImpl;
import com.color.util.ColorLog;

public class ColorLongshotViewController extends ColorLongshotController {
    private static final String TAG = "LongshotDump";

    public ColorLongshotViewController(ColorLongshotViewBase view) {
        super(view, "View");
    }

    public int getOverScrollMode(int overScrollMode) {
        ViewRootImpl viewRoot = ((View) this.mView).getViewRootImpl();
        if (viewRoot == null || !viewRoot.mViewRootHooks.getLongshotViewRoot().isLongshotConnected()) {
            return overScrollMode;
        }
        ColorLog.d("LongshotDump", "getOverScrollMode : NEVER");
        return 2;
    }
}
