package android.preference;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferenceGroupAdapter extends BaseAdapter implements OnPreferenceChangeInternalListener {
    private static final String TAG = "PreferenceGroupAdapter";
    private static LayoutParams sWrapperLayoutParams = new LayoutParams(-1, -2);
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Xiaokang.Feng@Plf.SDK, 2017-04-14 : Modify for listview", property = OppoRomType.ROM)
    private int[] DRAWABLEIDS = new int[]{201851138, 201851139, 201851140, 201851141};
    @OppoHook(level = OppoHookType.NEW_FIELD, note = "Xiaokang.Feng@Plf.SDK, 2017-04-14 : Add for listview", property = OppoRomType.ROM)
    private boolean isOppoStyle = false;
    private int mGroupPadding = 0;
    private Handler mHandler = new Handler();
    private boolean mHasReturnedViewTypeCount = false;
    private Drawable mHighlightedDrawable;
    private int mHighlightedPosition = -1;
    private volatile boolean mIsSyncing = false;
    private PreferenceGroup mPreferenceGroup;
    private ArrayList<PreferenceLayout> mPreferenceLayouts;
    private List<Preference> mPreferenceList;
    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            PreferenceGroupAdapter.this.syncMyPreferences();
        }
    };
    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();

    private static class PreferenceLayout implements Comparable<PreferenceLayout> {
        private String name;
        private int resId;
        private int widgetResId;

        /* synthetic */ PreferenceLayout(PreferenceLayout -this0) {
            this();
        }

        private PreferenceLayout() {
        }

        public int compareTo(PreferenceLayout other) {
            int compareNames = this.name.compareTo(other.name);
            if (compareNames != 0) {
                return compareNames;
            }
            if (this.resId != other.resId) {
                return this.resId - other.resId;
            }
            if (this.widgetResId == other.widgetResId) {
                return 0;
            }
            return this.widgetResId - other.widgetResId;
        }
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Xiaokang.Feng@Plf.SDK : Modify for group listview", property = OppoRomType.ROM)
    public PreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        Context context = preferenceGroup.getContext();
        if (context != null) {
            this.isOppoStyle = context.isOppoStyle();
            this.mGroupPadding = context.getResources().getDimensionPixelSize(201655564);
        }
        this.mPreferenceGroup = preferenceGroup;
        this.mPreferenceGroup.setOnPreferenceChangeInternalListener(this);
        this.mPreferenceList = new ArrayList();
        this.mPreferenceLayouts = new ArrayList();
        syncMyPreferences();
    }

    private void syncMyPreferences() {
        synchronized (this) {
            if (this.mIsSyncing) {
                return;
            }
            this.mIsSyncing = true;
            List<Preference> newPreferenceList = new ArrayList(this.mPreferenceList.size());
            flattenPreferenceGroup(newPreferenceList, this.mPreferenceGroup);
            this.mPreferenceList = newPreferenceList;
            notifyDataSetChanged();
            synchronized (this) {
                this.mIsSyncing = false;
                notifyAll();
            }
        }
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Xiaokang.Feng@Plf.SDK : Modify for group listview", property = OppoRomType.ROM)
    private void flattenPreferenceGroup(List<Preference> preferences, PreferenceGroup group) {
        group.sortPreferences();
        int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            Preference preference = group.getPreference(i);
            if (this.isOppoStyle && (group instanceof PreferenceCategory) && preference.isUseDefaultPosition()) {
                if (groupSize == 1) {
                    preference.setDefaultPositionStyle(3);
                } else if (i == 0) {
                    preference.setDefaultPositionStyle(0);
                } else if (i == groupSize - 1) {
                    preference.setDefaultPositionStyle(2);
                } else {
                    preference.setDefaultPositionStyle(1);
                }
            }
            preferences.add(preference);
            if (!this.mHasReturnedViewTypeCount && preference.isRecycleEnabled()) {
                addPreferenceClassName(preference);
            }
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceAsGroup = (PreferenceGroup) preference;
                if (preferenceAsGroup.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(preferences, preferenceAsGroup);
                }
            }
            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout in) {
        PreferenceLayout pl = in != null ? in : new PreferenceLayout();
        pl.name = preference.getClass().getName();
        pl.resId = preference.getLayoutResource();
        pl.widgetResId = preference.getWidgetLayoutResource();
        return pl;
    }

    @OppoHook(level = OppoHookType.CHANGE_ACCESS, note = "Xiaokang.Feng@Plf.SDK, 2017-10-13 : [-private +public] Modify for switch preference", property = OppoRomType.ROM)
    public void addPreferenceClassName(Preference preference) {
        PreferenceLayout pl = createPreferenceLayout(preference, null);
        int insertPos = Collections.binarySearch(this.mPreferenceLayouts, pl);
        if (insertPos < 0) {
            this.mPreferenceLayouts.add((insertPos * -1) - 1, pl);
        }
    }

    public int getCount() {
        return this.mPreferenceList.size();
    }

    public Preference getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return (Preference) this.mPreferenceList.get(position);
    }

    public long getItemId(int position) {
        if (position < 0 || position >= getCount()) {
            return Long.MIN_VALUE;
        }
        return getItem(position).getId();
    }

    public void setHighlighted(int position) {
        this.mHighlightedPosition = position;
    }

    public void setHighlightedDrawable(Drawable drawable) {
        this.mHighlightedDrawable = drawable;
    }

    @OppoHook(level = OppoHookType.CHANGE_CODE, note = "Xiaokang.Feng@Plf.SDK, 2017-04-14 : Modify for listview", property = OppoRomType.ROM)
    public View getView(int position, View convertView, ViewGroup parent) {
        Preference preference = getItem(position);
        this.mTempPreferenceLayout = createPreferenceLayout(preference, this.mTempPreferenceLayout);
        if (Collections.binarySearch(this.mPreferenceLayouts, this.mTempPreferenceLayout) < 0 || getItemViewType(position) == getHighlightItemViewType()) {
            convertView = null;
        }
        View result = preference.getView(convertView, parent);
        if (position == this.mHighlightedPosition && this.mHighlightedDrawable != null) {
            View wrapper = new FrameLayout(parent.getContext());
            wrapper.setLayoutParams(sWrapperLayoutParams);
            wrapper.setBackgroundDrawable(this.mHighlightedDrawable);
            wrapper.addView(result);
            result = wrapper;
        }
        if (this.isOppoStyle) {
            int style = preference.getPositionStyle();
            if (style >= 0 && style <= 3) {
                result.setBackgroundResource(this.DRAWABLEIDS[style]);
            }
        }
        return result;
    }

    public boolean isEnabled(int position) {
        if (position < 0 || position >= getCount()) {
            return true;
        }
        return getItem(position).isSelectable();
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public void onPreferenceChange(Preference preference) {
        notifyDataSetChanged();
    }

    public void onPreferenceHierarchyChange(Preference preference) {
        this.mHandler.removeCallbacks(this.mSyncRunnable);
        this.mHandler.post(this.mSyncRunnable);
    }

    public boolean hasStableIds() {
        return true;
    }

    private int getHighlightItemViewType() {
        return getViewTypeCount() - 1;
    }

    public int getItemViewType(int position) {
        if (position == this.mHighlightedPosition) {
            return getHighlightItemViewType();
        }
        if (!this.mHasReturnedViewTypeCount) {
            this.mHasReturnedViewTypeCount = true;
        }
        Preference preference = getItem(position);
        if (!preference.isRecycleEnabled()) {
            return -1;
        }
        this.mTempPreferenceLayout = createPreferenceLayout(preference, this.mTempPreferenceLayout);
        int viewType = Collections.binarySearch(this.mPreferenceLayouts, this.mTempPreferenceLayout);
        if (viewType < 0) {
            return -1;
        }
        return viewType;
    }

    public int getViewTypeCount() {
        if (!this.mHasReturnedViewTypeCount) {
            this.mHasReturnedViewTypeCount = true;
        }
        return Math.max(1, this.mPreferenceLayouts.size()) + 1;
    }
}
