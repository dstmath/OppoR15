package android.widget;

import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.Application;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.StrictMode;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Filter;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewStub.ViewReplaceRunnable;
import android.widget.AdapterView.OnItemClickListener;
import com.android.internal.R;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.util.Preconditions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.Executor;
import libcore.util.Objects;

public class RemoteViews implements Parcelable, Filter {
    private static final Action ACTION_NOOP = new RuntimeAction() {
        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
        }
    };
    private static final int BITMAP_REFLECTION_ACTION_TAG = 12;
    public static final Creator<RemoteViews> CREATOR = new Creator<RemoteViews>() {
        public RemoteViews createFromParcel(Parcel parcel) {
            return new RemoteViews(parcel);
        }

        public RemoteViews[] newArray(int size) {
            return new RemoteViews[size];
        }
    };
    private static final OnClickHandler DEFAULT_ON_CLICK_HANDLER = new OnClickHandler();
    static final String EXTRA_REMOTEADAPTER_APPWIDGET_ID = "remoteAdapterAppWidgetId";
    private static final int LAYOUT_PARAM_ACTION_TAG = 19;
    private static final String LOG_TAG = "RemoteViews";
    private static final int MAX_NESTED_VIEWS = 10;
    private static final int MODE_HAS_LANDSCAPE_AND_PORTRAIT = 1;
    private static final int MODE_NORMAL = 0;
    private static final int OVERRIDE_TEXT_COLORS_TAG = 20;
    private static final int REFLECTION_ACTION_TAG = 2;
    private static final int SET_DRAWABLE_PARAMETERS_TAG = 3;
    private static final int SET_EMPTY_VIEW_ACTION_TAG = 6;
    private static final int SET_ON_CLICK_FILL_IN_INTENT_TAG = 9;
    private static final int SET_ON_CLICK_PENDING_INTENT_TAG = 1;
    private static final int SET_PENDING_INTENT_TEMPLATE_TAG = 8;
    private static final int SET_REFLECTION_ACTION_WITHOUT_PARAMS_TAG = 5;
    private static final int SET_REMOTE_INPUTS_ACTION_TAG = 18;
    private static final int SET_REMOTE_VIEW_ADAPTER_INTENT_TAG = 10;
    private static final int SET_REMOTE_VIEW_ADAPTER_LIST_TAG = 15;
    private static final int TEXT_VIEW_DRAWABLE_ACTION_TAG = 11;
    private static final int TEXT_VIEW_DRAWABLE_COLOR_FILTER_ACTION_TAG = 17;
    private static final int TEXT_VIEW_SIZE_ACTION_TAG = 13;
    private static final int VIEW_GROUP_ACTION_ADD_TAG = 4;
    private static final int VIEW_GROUP_ACTION_REMOVE_TAG = 7;
    private static final int VIEW_PADDING_ACTION_TAG = 14;
    private static final ArrayMap<Method, Method> sAsyncMethods = new ArrayMap();
    private static final ThreadLocal<Object[]> sInvokeArgsTls = new ThreadLocal<Object[]>() {
        protected Object[] initialValue() {
            return new Object[1];
        }
    };
    private static final ArrayMap<Class<? extends View>, ArrayMap<MutablePair<String, Class<?>>, Method>> sMethods = new ArrayMap();
    private static final Object[] sMethodsLock = new Object[0];
    private ArrayList<Action> mActions;
    private ApplicationInfo mApplication;
    private BitmapCache mBitmapCache;
    private boolean mIsRoot;
    private boolean mIsWidgetCollectionChild;
    private RemoteViews mLandscape;
    private final int mLayoutId;
    private MemoryUsageCounter mMemoryUsageCounter;
    private final MutablePair<String, Class<?>> mPair;
    private RemoteViews mPortrait;
    private boolean mReapplyDisallowed;

    private static abstract class Action implements Parcelable {
        public static final int MERGE_APPEND = 1;
        public static final int MERGE_IGNORE = 2;
        public static final int MERGE_REPLACE = 0;
        int viewId;

        /* synthetic */ Action(Action -this0) {
            this();
        }

        public abstract void apply(View view, ViewGroup viewGroup, OnClickHandler onClickHandler) throws ActionException;

        public abstract String getActionName();

        private Action() {
        }

        public int describeContents() {
            return 0;
        }

        public void updateMemoryUsageEstimate(MemoryUsageCounter counter) {
        }

        public void setBitmapCache(BitmapCache bitmapCache) {
        }

        public int mergeBehavior() {
            return 0;
        }

        public String getUniqueKey() {
            return getActionName() + this.viewId;
        }

        public Action initActionAsync(ViewTree root, ViewGroup rootParent, OnClickHandler handler) {
            return this;
        }

        public boolean prefersAsyncApply() {
            return false;
        }

        public boolean hasSameAppInfo(ApplicationInfo parentInfo) {
            return true;
        }
    }

    private static abstract class RuntimeAction extends Action {
        /* synthetic */ RuntimeAction(RuntimeAction -this0) {
            this();
        }

        private RuntimeAction() {
            super();
        }

        public final String getActionName() {
            return "RuntimeAction";
        }

        public final void writeToParcel(Parcel dest, int flags) {
            throw new UnsupportedOperationException();
        }
    }

    public static class ActionException extends RuntimeException {
        public ActionException(Exception ex) {
            super(ex);
        }

        public ActionException(String message) {
            super(message);
        }
    }

    private class AsyncApplyTask extends AsyncTask<Void, Void, ViewTree> implements OnCancelListener {
        private Action[] mActions;
        final Context mContext;
        private Exception mError;
        final OnClickHandler mHandler;
        final OnViewAppliedListener mListener;
        final ViewGroup mParent;
        final RemoteViews mRV;
        private View mResult;
        private ViewTree mTree;

        /* synthetic */ AsyncApplyTask(RemoteViews this$0, RemoteViews rv, ViewGroup parent, Context context, OnViewAppliedListener listener, OnClickHandler handler, View result, AsyncApplyTask -this7) {
            this(rv, parent, context, listener, handler, result);
        }

        private AsyncApplyTask(RemoteViews rv, ViewGroup parent, Context context, OnViewAppliedListener listener, OnClickHandler handler, View result) {
            this.mRV = rv;
            this.mParent = parent;
            this.mContext = context;
            this.mListener = listener;
            this.mHandler = handler;
            this.mResult = result;
            RemoteViews.loadTransitionOverride(context, handler);
        }

        protected ViewTree doInBackground(Void... params) {
            try {
                if (this.mResult == null) {
                    this.mResult = RemoteViews.this.inflateView(this.mContext, this.mRV, this.mParent);
                }
                this.mTree = new ViewTree(this.mResult, null);
                if (this.mRV.mActions != null) {
                    int count = this.mRV.mActions.size();
                    this.mActions = new Action[count];
                    for (int i = 0; i < count && (isCancelled() ^ 1) != 0; i++) {
                        this.mActions[i] = ((Action) this.mRV.mActions.get(i)).initActionAsync(this.mTree, this.mParent, this.mHandler);
                    }
                } else {
                    this.mActions = null;
                }
                return this.mTree;
            } catch (Exception e) {
                this.mError = e;
                return null;
            }
        }

        protected void onPostExecute(ViewTree viewTree) {
            if (this.mError == null) {
                try {
                    if (this.mActions != null) {
                        OnClickHandler handler = this.mHandler == null ? RemoteViews.DEFAULT_ON_CLICK_HANDLER : this.mHandler;
                        for (Action a : this.mActions) {
                            a.apply(viewTree.mRoot, this.mParent, handler);
                        }
                    }
                } catch (Exception e) {
                    this.mError = e;
                }
            }
            if (this.mListener != null) {
                if (this.mError != null) {
                    this.mListener.onError(this.mError);
                } else {
                    this.mListener.onViewApplied(viewTree.mRoot);
                }
            } else if (this.mError == null) {
            } else {
                if (this.mError instanceof ActionException) {
                    throw ((ActionException) this.mError);
                }
                throw new ActionException(this.mError);
            }
        }

        public void onCancel() {
            cancel(true);
        }
    }

    private static class BitmapCache {
        ArrayList<Bitmap> mBitmaps = new ArrayList();

        public BitmapCache(Parcel source) {
            int count = source.readInt();
            for (int i = 0; i < count; i++) {
                this.mBitmaps.add((Bitmap) Bitmap.CREATOR.createFromParcel(source));
            }
        }

        public int getBitmapId(Bitmap b) {
            if (b == null) {
                return -1;
            }
            if (this.mBitmaps.contains(b)) {
                return this.mBitmaps.indexOf(b);
            }
            this.mBitmaps.add(b);
            return this.mBitmaps.size() - 1;
        }

        public Bitmap getBitmapForId(int id) {
            if (id == -1 || id >= this.mBitmaps.size()) {
                return null;
            }
            return (Bitmap) this.mBitmaps.get(id);
        }

        public void writeBitmapsToParcel(Parcel dest, int flags) {
            int count = this.mBitmaps.size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                ((Bitmap) this.mBitmaps.get(i)).writeToParcel(dest, flags);
            }
        }

        public void assimilate(BitmapCache bitmapCache) {
            ArrayList<Bitmap> bitmapsToBeAdded = bitmapCache.mBitmaps;
            int count = bitmapsToBeAdded.size();
            for (int i = 0; i < count; i++) {
                Bitmap b = (Bitmap) bitmapsToBeAdded.get(i);
                if (!this.mBitmaps.contains(b)) {
                    this.mBitmaps.add(b);
                }
            }
        }

        public void addBitmapMemory(MemoryUsageCounter memoryCounter) {
            for (int i = 0; i < this.mBitmaps.size(); i++) {
                memoryCounter.addBitmapMemory((Bitmap) this.mBitmaps.get(i));
            }
        }

        protected BitmapCache clone() {
            BitmapCache bitmapCache = new BitmapCache();
            bitmapCache.mBitmaps.addAll(this.mBitmaps);
            return bitmapCache;
        }
    }

    private class BitmapReflectionAction extends Action {
        Bitmap bitmap;
        int bitmapId;
        String methodName;

        BitmapReflectionAction(int viewId, String methodName, Bitmap bitmap) {
            super();
            this.bitmap = bitmap;
            this.viewId = viewId;
            this.methodName = methodName;
            this.bitmapId = RemoteViews.this.mBitmapCache.getBitmapId(bitmap);
        }

        BitmapReflectionAction(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.methodName = in.readString();
            this.bitmapId = in.readInt();
            this.bitmap = RemoteViews.this.mBitmapCache.getBitmapForId(this.bitmapId);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(12);
            dest.writeInt(this.viewId);
            dest.writeString(this.methodName);
            dest.writeInt(this.bitmapId);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) throws ActionException {
            new ReflectionAction(this.viewId, this.methodName, 12, this.bitmap).apply(root, rootParent, handler);
        }

        public void setBitmapCache(BitmapCache bitmapCache) {
            this.bitmapId = bitmapCache.getBitmapId(this.bitmap);
        }

        public String getActionName() {
            return "BitmapReflectionAction";
        }
    }

    private static class LayoutParamAction extends Action {
        public static final int LAYOUT_MARGIN_BOTTOM_DIMEN = 3;
        public static final int LAYOUT_MARGIN_END_DIMEN = 1;
        public static final int LAYOUT_WIDTH = 2;
        int property;
        int value;

        public LayoutParamAction(int viewId, int property, int value) {
            super();
            this.viewId = viewId;
            this.property = property;
            this.value = value;
        }

        public LayoutParamAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.property = parcel.readInt();
            this.value = parcel.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(19);
            dest.writeInt(this.viewId);
            dest.writeInt(this.property);
            dest.writeInt(this.value);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                LayoutParams layoutParams = target.getLayoutParams();
                if (layoutParams != null) {
                    switch (this.property) {
                        case 1:
                            if (layoutParams instanceof MarginLayoutParams) {
                                ((MarginLayoutParams) layoutParams).setMarginEnd(resolveDimenPixelOffset(target, this.value));
                                target.setLayoutParams(layoutParams);
                                break;
                            }
                            break;
                        case 2:
                            layoutParams.width = this.value;
                            target.setLayoutParams(layoutParams);
                            break;
                        case 3:
                            if (layoutParams instanceof MarginLayoutParams) {
                                ((MarginLayoutParams) layoutParams).bottomMargin = resolveDimenPixelOffset(target, this.value);
                                target.setLayoutParams(layoutParams);
                                break;
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property " + this.property);
                    }
                }
            }
        }

        private static int resolveDimenPixelOffset(View target, int value) {
            if (value == 0) {
                return 0;
            }
            return target.getContext().getResources().getDimensionPixelOffset(value);
        }

        public String getActionName() {
            return "LayoutParamAction" + this.property + ".";
        }
    }

    private class MemoryUsageCounter {
        int mMemoryUsage;

        /* synthetic */ MemoryUsageCounter(RemoteViews this$0, MemoryUsageCounter -this1) {
            this();
        }

        private MemoryUsageCounter() {
        }

        public void clear() {
            this.mMemoryUsage = 0;
        }

        public void increment(int numBytes) {
            this.mMemoryUsage += numBytes;
        }

        public int getMemoryUsage() {
            return this.mMemoryUsage;
        }

        public void addBitmapMemory(Bitmap b) {
            increment(b.getAllocationByteCount());
        }
    }

    static class MutablePair<F, S> {
        F first;
        S second;

        MutablePair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof MutablePair)) {
                return false;
            }
            MutablePair<?, ?> p = (MutablePair) o;
            if (Objects.equal(p.first, this.first)) {
                z = Objects.equal(p.second, this.second);
            }
            return z;
        }

        public int hashCode() {
            int i = 0;
            int hashCode = this.first == null ? 0 : this.first.hashCode();
            if (this.second != null) {
                i = this.second.hashCode();
            }
            return hashCode ^ i;
        }
    }

    public static class OnClickHandler {
        private int mEnterAnimationId;

        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            return onClickHandler(view, pendingIntent, fillInIntent, -1);
        }

        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent, int launchStackId) {
            try {
                ActivityOptions opts;
                Context context = view.getContext();
                if (this.mEnterAnimationId != 0) {
                    opts = ActivityOptions.makeCustomAnimation(context, this.mEnterAnimationId, 0);
                } else {
                    opts = ActivityOptions.makeBasic();
                }
                if (launchStackId != -1) {
                    opts.setLaunchStackId(launchStackId);
                }
                context.startIntentSender(pendingIntent.getIntentSender(), fillInIntent, 268435456, 268435456, 0, opts.toBundle());
                return true;
            } catch (SendIntentException e) {
                Log.e(RemoteViews.LOG_TAG, "Cannot send pending intent: ", e);
                return false;
            } catch (Exception e2) {
                Log.e(RemoteViews.LOG_TAG, "Cannot send pending intent due to unknown exception: ", e2);
                return false;
            }
        }

        public void setEnterAnimationId(int enterAnimationId) {
            this.mEnterAnimationId = enterAnimationId;
        }
    }

    public interface OnViewAppliedListener {
        void onError(Exception exception);

        void onViewApplied(View view);
    }

    private class OverrideTextColorsAction extends Action {
        private final int textColor;

        public OverrideTextColorsAction(int textColor) {
            super();
            this.textColor = textColor;
        }

        public OverrideTextColorsAction(Parcel parcel) {
            super();
            this.textColor = parcel.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(20);
            dest.writeInt(this.textColor);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            Stack<View> viewsToProcess = new Stack();
            viewsToProcess.add(root);
            while (!viewsToProcess.isEmpty()) {
                View v = (View) viewsToProcess.pop();
                if (v instanceof TextView) {
                    TextView textView = (TextView) v;
                    textView.setText(NotificationColorUtil.clearColorSpans(textView.getText()));
                    textView.setTextColor(this.textColor);
                }
                if (v instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) v;
                    for (int i = 0; i < viewGroup.getChildCount(); i++) {
                        viewsToProcess.push(viewGroup.getChildAt(i));
                    }
                }
            }
        }

        public String getActionName() {
            return "OverrideTextColorsAction";
        }
    }

    private final class ReflectionAction extends Action {
        static final int BITMAP = 12;
        static final int BOOLEAN = 1;
        static final int BUNDLE = 13;
        static final int BYTE = 2;
        static final int CHAR = 8;
        static final int CHAR_SEQUENCE = 10;
        static final int COLOR_STATE_LIST = 15;
        static final int DOUBLE = 7;
        static final int FLOAT = 6;
        static final int ICON = 16;
        static final int INT = 4;
        static final int INTENT = 14;
        static final int LONG = 5;
        static final int SHORT = 3;
        static final int STRING = 9;
        static final int URI = 11;
        String methodName;
        int type;
        Object value;

        ReflectionAction(int viewId, String methodName, int type, Object value) {
            super();
            this.viewId = viewId;
            this.methodName = methodName;
            this.type = type;
            this.value = value;
        }

        ReflectionAction(RemoteViews this$0, Parcel in) {
            boolean z = false;
            RemoteViews.this = this$0;
            super();
            this.viewId = in.readInt();
            this.methodName = in.readString();
            this.type = in.readInt();
            switch (this.type) {
                case 1:
                    if (in.readInt() != 0) {
                        z = true;
                    }
                    this.value = Boolean.valueOf(z);
                    return;
                case 2:
                    this.value = Byte.valueOf(in.readByte());
                    return;
                case 3:
                    this.value = Short.valueOf((short) in.readInt());
                    return;
                case 4:
                    this.value = Integer.valueOf(in.readInt());
                    return;
                case 5:
                    this.value = Long.valueOf(in.readLong());
                    return;
                case 6:
                    this.value = Float.valueOf(in.readFloat());
                    return;
                case 7:
                    this.value = Double.valueOf(in.readDouble());
                    return;
                case 8:
                    this.value = Character.valueOf((char) in.readInt());
                    return;
                case 9:
                    this.value = in.readString();
                    return;
                case 10:
                    this.value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    return;
                case 11:
                    if (in.readInt() != 0) {
                        this.value = Uri.CREATOR.createFromParcel(in);
                        return;
                    }
                    return;
                case 12:
                    if (in.readInt() != 0) {
                        this.value = Bitmap.CREATOR.createFromParcel(in);
                        return;
                    }
                    return;
                case 13:
                    this.value = in.readBundle();
                    return;
                case 14:
                    if (in.readInt() != 0) {
                        this.value = Intent.CREATOR.createFromParcel(in);
                        return;
                    }
                    return;
                case 15:
                    if (in.readInt() != 0) {
                        this.value = ColorStateList.CREATOR.createFromParcel(in);
                        return;
                    }
                    return;
                case 16:
                    if (in.readInt() != 0) {
                        this.value = Icon.CREATOR.createFromParcel(in);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public void writeToParcel(Parcel out, int flags) {
            int i = 1;
            out.writeInt(2);
            out.writeInt(this.viewId);
            out.writeString(this.methodName);
            out.writeInt(this.type);
            switch (this.type) {
                case 1:
                    out.writeInt(((Boolean) this.value).booleanValue() ? 1 : 0);
                    return;
                case 2:
                    out.writeByte(((Byte) this.value).byteValue());
                    return;
                case 3:
                    out.writeInt(((Short) this.value).shortValue());
                    return;
                case 4:
                    out.writeInt(((Integer) this.value).intValue());
                    return;
                case 5:
                    out.writeLong(((Long) this.value).longValue());
                    return;
                case 6:
                    out.writeFloat(((Float) this.value).floatValue());
                    return;
                case 7:
                    out.writeDouble(((Double) this.value).doubleValue());
                    return;
                case 8:
                    out.writeInt(((Character) this.value).charValue());
                    return;
                case 9:
                    out.writeString((String) this.value);
                    return;
                case 10:
                    TextUtils.writeToParcel((CharSequence) this.value, out, flags);
                    return;
                case 11:
                    if (this.value == null) {
                        i = 0;
                    }
                    out.writeInt(i);
                    if (this.value != null) {
                        ((Uri) this.value).writeToParcel(out, flags);
                        return;
                    }
                    return;
                case 12:
                    if (this.value == null) {
                        i = 0;
                    }
                    out.writeInt(i);
                    if (this.value != null) {
                        ((Bitmap) this.value).writeToParcel(out, flags);
                        return;
                    }
                    return;
                case 13:
                    out.writeBundle((Bundle) this.value);
                    return;
                case 14:
                    if (this.value == null) {
                        i = 0;
                    }
                    out.writeInt(i);
                    if (this.value != null) {
                        ((Intent) this.value).writeToParcel(out, flags);
                        return;
                    }
                    return;
                case 15:
                    if (this.value == null) {
                        i = 0;
                    }
                    out.writeInt(i);
                    if (this.value != null) {
                        ((ColorStateList) this.value).writeToParcel(out, flags);
                        return;
                    }
                    return;
                case 16:
                    if (this.value == null) {
                        i = 0;
                    }
                    out.writeInt(i);
                    if (this.value != null) {
                        ((Icon) this.value).writeToParcel(out, flags);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private Class<?> getParameterType() {
            switch (this.type) {
                case 1:
                    return Boolean.TYPE;
                case 2:
                    return Byte.TYPE;
                case 3:
                    return Short.TYPE;
                case 4:
                    return Integer.TYPE;
                case 5:
                    return Long.TYPE;
                case 6:
                    return Float.TYPE;
                case 7:
                    return Double.TYPE;
                case 8:
                    return Character.TYPE;
                case 9:
                    return String.class;
                case 10:
                    return CharSequence.class;
                case 11:
                    return Uri.class;
                case 12:
                    return Bitmap.class;
                case 13:
                    return Bundle.class;
                case 14:
                    return Intent.class;
                case 15:
                    return ColorStateList.class;
                case 16:
                    return Icon.class;
                default:
                    return null;
            }
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view != null) {
                Class<?> param = getParameterType();
                if (param == null) {
                    throw new ActionException("bad type: " + this.type);
                }
                try {
                    RemoteViews.this.getMethod(view, this.methodName, param).invoke(view, RemoteViews.wrapArg(this.value));
                } catch (ActionException e) {
                    throw e;
                } catch (Exception ex) {
                    throw new ActionException(ex);
                }
            }
        }

        public Action initActionAsync(ViewTree root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view == null) {
                return RemoteViews.ACTION_NOOP;
            }
            Class<?> param = getParameterType();
            if (param == null) {
                throw new ActionException("bad type: " + this.type);
            }
            try {
                Method asyncMethod = RemoteViews.this.getAsyncMethod(RemoteViews.this.getMethod(view, this.methodName, param));
                if (asyncMethod == null) {
                    return this;
                }
                Runnable endAction = (Runnable) asyncMethod.invoke(view, RemoteViews.wrapArg(this.value));
                if (endAction == null) {
                    return RemoteViews.ACTION_NOOP;
                }
                if (endAction instanceof ViewReplaceRunnable) {
                    root.createTree();
                    root.findViewTreeById(this.viewId).replaceView(((ViewReplaceRunnable) endAction).view);
                }
                return new RunnableAction(endAction);
            } catch (ActionException e) {
                throw e;
            } catch (Exception ex) {
                throw new ActionException(ex);
            }
        }

        public int mergeBehavior() {
            if (this.methodName.equals("smoothScrollBy")) {
                return 1;
            }
            return 0;
        }

        public String getActionName() {
            return "ReflectionAction" + this.methodName + this.type;
        }

        public boolean prefersAsyncApply() {
            return this.type == 11 || this.type == 16;
        }
    }

    private final class ReflectionActionWithoutParams extends Action {
        final String methodName;

        ReflectionActionWithoutParams(int viewId, String methodName) {
            super();
            this.viewId = viewId;
            this.methodName = methodName;
        }

        ReflectionActionWithoutParams(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.methodName = in.readString();
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(5);
            out.writeInt(this.viewId);
            out.writeString(this.methodName);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view != null) {
                try {
                    RemoteViews.this.getMethod(view, this.methodName, null).invoke(view, new Object[0]);
                } catch (ActionException e) {
                    throw e;
                } catch (Exception ex) {
                    throw new ActionException(ex);
                }
            }
        }

        public int mergeBehavior() {
            if (this.methodName.equals("showNext") || this.methodName.equals("showPrevious")) {
                return 2;
            }
            return 0;
        }

        public String getActionName() {
            return "ReflectionActionWithoutParams";
        }
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RemoteView {
    }

    private static class RemoteViewsContextWrapper extends ContextWrapper {
        private final Context mContextForResources;

        RemoteViewsContextWrapper(Context context, Context contextForResources) {
            super(context);
            this.mContextForResources = contextForResources;
        }

        public Resources getResources() {
            return this.mContextForResources.getResources();
        }

        public Theme getTheme() {
            return this.mContextForResources.getTheme();
        }

        public String getPackageName() {
            return this.mContextForResources.getPackageName();
        }
    }

    private static final class RunnableAction extends RuntimeAction {
        private final Runnable mRunnable;

        RunnableAction(Runnable r) {
            super();
            this.mRunnable = r;
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            this.mRunnable.run();
        }
    }

    private class SetDrawableParameters extends Action {
        int alpha;
        int colorFilter;
        Mode filterMode;
        int level;
        boolean targetBackground;

        public SetDrawableParameters(int id, boolean targetBackground, int alpha, int colorFilter, Mode mode, int level) {
            super();
            this.viewId = id;
            this.targetBackground = targetBackground;
            this.alpha = alpha;
            this.colorFilter = colorFilter;
            this.filterMode = mode;
            this.level = level;
        }

        public SetDrawableParameters(RemoteViews this$0, Parcel parcel) {
            boolean z = false;
            RemoteViews.this = this$0;
            super();
            this.viewId = parcel.readInt();
            if (parcel.readInt() != 0) {
                z = true;
            }
            this.targetBackground = z;
            this.alpha = parcel.readInt();
            this.colorFilter = parcel.readInt();
            if (parcel.readInt() != 0) {
                this.filterMode = Mode.valueOf(parcel.readString());
            } else {
                this.filterMode = null;
            }
            this.level = parcel.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(3);
            dest.writeInt(this.viewId);
            dest.writeInt(this.targetBackground ? 1 : 0);
            dest.writeInt(this.alpha);
            dest.writeInt(this.colorFilter);
            if (this.filterMode != null) {
                dest.writeInt(1);
                dest.writeString(this.filterMode.toString());
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(this.level);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                Drawable targetDrawable = null;
                if (this.targetBackground) {
                    targetDrawable = target.getBackground();
                } else if (target instanceof ImageView) {
                    targetDrawable = ((ImageView) target).getDrawable();
                }
                if (targetDrawable != null) {
                    if (this.alpha != -1) {
                        targetDrawable.mutate().setAlpha(this.alpha);
                    }
                    if (this.filterMode != null) {
                        targetDrawable.mutate().setColorFilter(this.colorFilter, this.filterMode);
                    }
                    if (this.level != -1) {
                        targetDrawable.mutate().setLevel(this.level);
                    }
                }
            }
        }

        public String getActionName() {
            return "SetDrawableParameters";
        }
    }

    private class SetEmptyView extends Action {
        int emptyViewId;
        int viewId;

        SetEmptyView(int viewId, int emptyViewId) {
            super();
            this.viewId = viewId;
            this.emptyViewId = emptyViewId;
        }

        SetEmptyView(Parcel in) {
            super();
            this.viewId = in.readInt();
            this.emptyViewId = in.readInt();
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(6);
            out.writeInt(this.viewId);
            out.writeInt(this.emptyViewId);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View view = root.findViewById(this.viewId);
            if (view instanceof AdapterView) {
                AdapterView<?> adapterView = (AdapterView) view;
                View emptyView = root.findViewById(this.emptyViewId);
                if (emptyView != null) {
                    adapterView.setEmptyView(emptyView);
                }
            }
        }

        public String getActionName() {
            return "SetEmptyView";
        }
    }

    private class SetOnClickFillInIntent extends Action {
        Intent fillInIntent;

        public SetOnClickFillInIntent(int id, Intent fillInIntent) {
            super();
            this.viewId = id;
            this.fillInIntent = fillInIntent;
        }

        public SetOnClickFillInIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.fillInIntent = (Intent) Intent.CREATOR.createFromParcel(parcel);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(9);
            dest.writeInt(this.viewId);
            this.fillInIntent.writeToParcel(dest, 0);
        }

        public void apply(View root, ViewGroup rootParent, final OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (RemoteViews.this.mIsWidgetCollectionChild) {
                    if (target == root) {
                        target.setTagInternal(R.id.fillInIntent, this.fillInIntent);
                    } else if (this.fillInIntent != null) {
                        target.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                View parent = (View) v.getParent();
                                while (parent != null && ((parent instanceof AdapterView) ^ 1) != 0 && (!(parent instanceof AppWidgetHostView) || (parent instanceof RemoteViewsFrameLayout))) {
                                    parent = (View) parent.getParent();
                                }
                                if (!(parent instanceof AdapterView)) {
                                    Log.e(RemoteViews.LOG_TAG, "Collection item doesn't have AdapterView parent");
                                } else if (parent.getTag() instanceof PendingIntent) {
                                    PendingIntent pendingIntent = (PendingIntent) parent.getTag();
                                    SetOnClickFillInIntent.this.fillInIntent.setSourceBounds(RemoteViews.getSourceBounds(v));
                                    handler.onClickHandler(v, pendingIntent, SetOnClickFillInIntent.this.fillInIntent);
                                } else {
                                    Log.e(RemoteViews.LOG_TAG, "Attempting setOnClickFillInIntent without calling setPendingIntentTemplate on parent.");
                                }
                            }
                        });
                    }
                    return;
                }
                Log.e(RemoteViews.LOG_TAG, "The method setOnClickFillInIntent is available only from RemoteViewsFactory (ie. on collection items).");
            }
        }

        public String getActionName() {
            return "SetOnClickFillInIntent";
        }
    }

    private class SetOnClickPendingIntent extends Action {
        PendingIntent pendingIntent;

        public SetOnClickPendingIntent(int id, PendingIntent pendingIntent) {
            super();
            this.viewId = id;
            this.pendingIntent = pendingIntent;
        }

        public SetOnClickPendingIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            if (parcel.readInt() != 0) {
                this.pendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
            }
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i = 1;
            dest.writeInt(1);
            dest.writeInt(this.viewId);
            if (this.pendingIntent == null) {
                i = 0;
            }
            dest.writeInt(i);
            if (this.pendingIntent != null) {
                this.pendingIntent.writeToParcel(dest, 0);
            }
        }

        public void apply(View root, ViewGroup rootParent, final OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (RemoteViews.this.mIsWidgetCollectionChild) {
                    Log.w(RemoteViews.LOG_TAG, "Cannot setOnClickPendingIntent for collection item (id: " + this.viewId + ")");
                    ApplicationInfo appInfo = root.getContext().getApplicationInfo();
                    if (appInfo != null && appInfo.targetSdkVersion >= 16) {
                        return;
                    }
                }
                OnClickListener onClickListener = null;
                if (this.pendingIntent != null) {
                    onClickListener = new OnClickListener() {
                        public void onClick(View v) {
                            Rect rect = RemoteViews.getSourceBounds(v);
                            Intent intent = new Intent();
                            intent.setSourceBounds(rect);
                            handler.onClickHandler(v, SetOnClickPendingIntent.this.pendingIntent, intent);
                        }
                    };
                }
                target.setOnClickListener(onClickListener);
            }
        }

        public String getActionName() {
            return "SetOnClickPendingIntent";
        }
    }

    private class SetPendingIntentTemplate extends Action {
        PendingIntent pendingIntentTemplate;

        public SetPendingIntentTemplate(int id, PendingIntent pendingIntentTemplate) {
            super();
            this.viewId = id;
            this.pendingIntentTemplate = pendingIntentTemplate;
        }

        public SetPendingIntentTemplate(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.pendingIntentTemplate = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(8);
            dest.writeInt(this.viewId);
            this.pendingIntentTemplate.writeToParcel(dest, 0);
        }

        public void apply(View root, ViewGroup rootParent, final OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (target instanceof AdapterView) {
                    AdapterView<?> av = (AdapterView) target;
                    av.setOnItemClickListener(new OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (view instanceof ViewGroup) {
                                ViewGroup vg = (ViewGroup) view;
                                if (parent instanceof AdapterViewAnimator) {
                                    vg = (ViewGroup) vg.getChildAt(0);
                                }
                                if (vg != null) {
                                    Intent fillInIntent = null;
                                    int childCount = vg.getChildCount();
                                    for (int i = 0; i < childCount; i++) {
                                        Intent tag = vg.getChildAt(i).getTag(R.id.fillInIntent);
                                        if (tag instanceof Intent) {
                                            fillInIntent = tag;
                                            break;
                                        }
                                    }
                                    if (fillInIntent != null) {
                                        new Intent().setSourceBounds(RemoteViews.getSourceBounds(view));
                                        handler.onClickHandler(view, SetPendingIntentTemplate.this.pendingIntentTemplate, fillInIntent);
                                    }
                                }
                            }
                        }
                    });
                    av.setTag(this.pendingIntentTemplate);
                    return;
                }
                Log.e(RemoteViews.LOG_TAG, "Cannot setPendingIntentTemplate on a view which is notan AdapterView (id: " + this.viewId + ")");
            }
        }

        public String getActionName() {
            return "SetPendingIntentTemplate";
        }
    }

    private class SetRemoteInputsAction extends Action {
        final Parcelable[] remoteInputs;

        public SetRemoteInputsAction(int viewId, RemoteInput[] remoteInputs) {
            super();
            this.viewId = viewId;
            this.remoteInputs = remoteInputs;
        }

        public SetRemoteInputsAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.remoteInputs = (Parcelable[]) parcel.createTypedArray(RemoteInput.CREATOR);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(18);
            dest.writeInt(this.viewId);
            dest.writeTypedArray(this.remoteInputs, flags);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                target.setTagInternal(R.id.remote_input_tag, this.remoteInputs);
            }
        }

        public String getActionName() {
            return "SetRemoteInputsAction";
        }
    }

    private class SetRemoteViewsAdapterIntent extends Action {
        Intent intent;
        boolean isAsync = false;

        public SetRemoteViewsAdapterIntent(int id, Intent intent) {
            super();
            this.viewId = id;
            this.intent = intent;
        }

        public SetRemoteViewsAdapterIntent(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.intent = (Intent) Intent.CREATOR.createFromParcel(parcel);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(10);
            dest.writeInt(this.viewId);
            this.intent.writeToParcel(dest, flags);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (!(rootParent instanceof AppWidgetHostView)) {
                    Log.e(RemoteViews.LOG_TAG, "SetRemoteViewsAdapterIntent action can only be used for AppWidgets (root id: " + this.viewId + ")");
                } else if ((target instanceof AbsListView) || ((target instanceof AdapterViewAnimator) ^ 1) == 0) {
                    this.intent.putExtra(RemoteViews.EXTRA_REMOTEADAPTER_APPWIDGET_ID, ((AppWidgetHostView) rootParent).getAppWidgetId());
                    if (target instanceof AbsListView) {
                        AbsListView v = (AbsListView) target;
                        v.setRemoteViewsAdapter(this.intent, this.isAsync);
                        v.setRemoteViewsOnClickHandler(handler);
                    } else if (target instanceof AdapterViewAnimator) {
                        AdapterViewAnimator v2 = (AdapterViewAnimator) target;
                        v2.setRemoteViewsAdapter(this.intent, this.isAsync);
                        v2.setRemoteViewsOnClickHandler(handler);
                    }
                } else {
                    Log.e(RemoteViews.LOG_TAG, "Cannot setRemoteViewsAdapter on a view which is not an AbsListView or AdapterViewAnimator (id: " + this.viewId + ")");
                }
            }
        }

        public Action initActionAsync(ViewTree root, ViewGroup rootParent, OnClickHandler handler) {
            SetRemoteViewsAdapterIntent copy = new SetRemoteViewsAdapterIntent(this.viewId, this.intent);
            copy.isAsync = true;
            return copy;
        }

        public String getActionName() {
            return "SetRemoteViewsAdapterIntent";
        }
    }

    private class SetRemoteViewsAdapterList extends Action {
        ArrayList<RemoteViews> list;
        int viewTypeCount;

        public SetRemoteViewsAdapterList(int id, ArrayList<RemoteViews> list, int viewTypeCount) {
            super();
            this.viewId = id;
            this.list = list;
            this.viewTypeCount = viewTypeCount;
        }

        public SetRemoteViewsAdapterList(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.viewTypeCount = parcel.readInt();
            int count = parcel.readInt();
            this.list = new ArrayList();
            for (int i = 0; i < count; i++) {
                this.list.add((RemoteViews) RemoteViews.CREATOR.createFromParcel(parcel));
            }
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(15);
            dest.writeInt(this.viewId);
            dest.writeInt(this.viewTypeCount);
            if (this.list == null || this.list.size() == 0) {
                dest.writeInt(0);
                return;
            }
            int count = this.list.size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                ((RemoteViews) this.list.get(i)).writeToParcel(dest, flags);
            }
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                if (!(rootParent instanceof AppWidgetHostView)) {
                    Log.e(RemoteViews.LOG_TAG, "SetRemoteViewsAdapterIntent action can only be used for AppWidgets (root id: " + this.viewId + ")");
                } else if ((target instanceof AbsListView) || ((target instanceof AdapterViewAnimator) ^ 1) == 0) {
                    Adapter a;
                    if (target instanceof AbsListView) {
                        AbsListView v = (AbsListView) target;
                        a = v.getAdapter();
                        if (!(a instanceof RemoteViewsListAdapter) || this.viewTypeCount > a.getViewTypeCount()) {
                            v.setAdapter(new RemoteViewsListAdapter(v.getContext(), this.list, this.viewTypeCount));
                        } else {
                            ((RemoteViewsListAdapter) a).setViewsList(this.list);
                        }
                    } else if (target instanceof AdapterViewAnimator) {
                        AdapterViewAnimator v2 = (AdapterViewAnimator) target;
                        a = v2.getAdapter();
                        if (!(a instanceof RemoteViewsListAdapter) || this.viewTypeCount > a.getViewTypeCount()) {
                            v2.setAdapter(new RemoteViewsListAdapter(v2.getContext(), this.list, this.viewTypeCount));
                        } else {
                            ((RemoteViewsListAdapter) a).setViewsList(this.list);
                        }
                    }
                } else {
                    Log.e(RemoteViews.LOG_TAG, "Cannot setRemoteViewsAdapter on a view which is not an AbsListView or AdapterViewAnimator (id: " + this.viewId + ")");
                }
            }
        }

        public String getActionName() {
            return "SetRemoteViewsAdapterList";
        }
    }

    private class TextViewDrawableAction extends Action {
        int d1;
        int d2;
        int d3;
        int d4;
        boolean drawablesLoaded = false;
        Icon i1;
        Icon i2;
        Icon i3;
        Icon i4;
        Drawable id1;
        Drawable id2;
        Drawable id3;
        Drawable id4;
        boolean isRelative = false;
        boolean useIcons = false;

        public TextViewDrawableAction(int viewId, boolean isRelative, int d1, int d2, int d3, int d4) {
            super();
            this.viewId = viewId;
            this.isRelative = isRelative;
            this.useIcons = false;
            this.d1 = d1;
            this.d2 = d2;
            this.d3 = d3;
            this.d4 = d4;
        }

        public TextViewDrawableAction(int viewId, boolean isRelative, Icon i1, Icon i2, Icon i3, Icon i4) {
            super();
            this.viewId = viewId;
            this.isRelative = isRelative;
            this.useIcons = true;
            this.i1 = i1;
            this.i2 = i2;
            this.i3 = i3;
            this.i4 = i4;
        }

        public TextViewDrawableAction(RemoteViews this$0, Parcel parcel) {
            boolean z = true;
            RemoteViews.this = this$0;
            super();
            this.viewId = parcel.readInt();
            this.isRelative = parcel.readInt() != 0;
            if (parcel.readInt() == 0) {
                z = false;
            }
            this.useIcons = z;
            if (this.useIcons) {
                if (parcel.readInt() != 0) {
                    this.i1 = (Icon) Icon.CREATOR.createFromParcel(parcel);
                }
                if (parcel.readInt() != 0) {
                    this.i2 = (Icon) Icon.CREATOR.createFromParcel(parcel);
                }
                if (parcel.readInt() != 0) {
                    this.i3 = (Icon) Icon.CREATOR.createFromParcel(parcel);
                }
                if (parcel.readInt() != 0) {
                    this.i4 = (Icon) Icon.CREATOR.createFromParcel(parcel);
                    return;
                }
                return;
            }
            this.d1 = parcel.readInt();
            this.d2 = parcel.readInt();
            this.d3 = parcel.readInt();
            this.d4 = parcel.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            dest.writeInt(11);
            dest.writeInt(this.viewId);
            dest.writeInt(this.isRelative ? 1 : 0);
            if (this.useIcons) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeInt(i);
            if (this.useIcons) {
                if (this.i1 != null) {
                    dest.writeInt(1);
                    this.i1.writeToParcel(dest, 0);
                } else {
                    dest.writeInt(0);
                }
                if (this.i2 != null) {
                    dest.writeInt(1);
                    this.i2.writeToParcel(dest, 0);
                } else {
                    dest.writeInt(0);
                }
                if (this.i3 != null) {
                    dest.writeInt(1);
                    this.i3.writeToParcel(dest, 0);
                } else {
                    dest.writeInt(0);
                }
                if (this.i4 != null) {
                    dest.writeInt(1);
                    this.i4.writeToParcel(dest, 0);
                    return;
                }
                dest.writeInt(0);
                return;
            }
            dest.writeInt(this.d1);
            dest.writeInt(this.d2);
            dest.writeInt(this.d3);
            dest.writeInt(this.d4);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target != null) {
                if (this.drawablesLoaded) {
                    if (this.isRelative) {
                        target.setCompoundDrawablesRelativeWithIntrinsicBounds(this.id1, this.id2, this.id3, this.id4);
                    } else {
                        target.setCompoundDrawablesWithIntrinsicBounds(this.id1, this.id2, this.id3, this.id4);
                    }
                } else if (this.useIcons) {
                    Context ctx = target.getContext();
                    Drawable id1 = this.i1 == null ? null : this.i1.loadDrawable(ctx);
                    Drawable id2 = this.i2 == null ? null : this.i2.loadDrawable(ctx);
                    Drawable id3 = this.i3 == null ? null : this.i3.loadDrawable(ctx);
                    Drawable id4 = this.i4 == null ? null : this.i4.loadDrawable(ctx);
                    if (this.isRelative) {
                        target.setCompoundDrawablesRelativeWithIntrinsicBounds(id1, id2, id3, id4);
                    } else {
                        target.setCompoundDrawablesWithIntrinsicBounds(id1, id2, id3, id4);
                    }
                } else if (this.isRelative) {
                    target.setCompoundDrawablesRelativeWithIntrinsicBounds(this.d1, this.d2, this.d3, this.d4);
                } else {
                    target.setCompoundDrawablesWithIntrinsicBounds(this.d1, this.d2, this.d3, this.d4);
                }
            }
        }

        public Action initActionAsync(ViewTree root, ViewGroup rootParent, OnClickHandler handler) {
            Drawable drawable = null;
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target == null) {
                return RemoteViews.ACTION_NOOP;
            }
            TextViewDrawableAction copy;
            if (this.useIcons) {
                copy = new TextViewDrawableAction(this.viewId, this.isRelative, this.i1, this.i2, this.i3, this.i4);
            } else {
                copy = new TextViewDrawableAction(this.viewId, this.isRelative, this.d1, this.d2, this.d3, this.d4);
            }
            copy.drawablesLoaded = true;
            Context ctx = target.getContext();
            if (this.useIcons) {
                copy.id1 = this.i1 == null ? null : this.i1.loadDrawable(ctx);
                copy.id2 = this.i2 == null ? null : this.i2.loadDrawable(ctx);
                copy.id3 = this.i3 == null ? null : this.i3.loadDrawable(ctx);
                if (this.i4 != null) {
                    drawable = this.i4.loadDrawable(ctx);
                }
                copy.id4 = drawable;
            } else {
                copy.id1 = this.d1 == 0 ? null : ctx.getDrawable(this.d1);
                copy.id2 = this.d2 == 0 ? null : ctx.getDrawable(this.d2);
                copy.id3 = this.d3 == 0 ? null : ctx.getDrawable(this.d3);
                if (this.d4 != 0) {
                    drawable = ctx.getDrawable(this.d4);
                }
                copy.id4 = drawable;
            }
            return copy;
        }

        public boolean prefersAsyncApply() {
            return this.useIcons;
        }

        public String getActionName() {
            return "TextViewDrawableAction";
        }
    }

    private class TextViewDrawableColorFilterAction extends Action {
        final int color;
        final int index;
        final boolean isRelative;
        final Mode mode;

        public TextViewDrawableColorFilterAction(int viewId, boolean isRelative, int index, int color, Mode mode) {
            super();
            this.viewId = viewId;
            this.isRelative = isRelative;
            this.index = index;
            this.color = color;
            this.mode = mode;
        }

        public TextViewDrawableColorFilterAction(RemoteViews this$0, Parcel parcel) {
            boolean z = false;
            RemoteViews.this = this$0;
            super();
            this.viewId = parcel.readInt();
            if (parcel.readInt() != 0) {
                z = true;
            }
            this.isRelative = z;
            this.index = parcel.readInt();
            this.color = parcel.readInt();
            this.mode = readPorterDuffMode(parcel);
        }

        private Mode readPorterDuffMode(Parcel parcel) {
            int mode = parcel.readInt();
            if (mode < 0 || mode >= Mode.values().length) {
                return Mode.CLEAR;
            }
            return Mode.values()[mode];
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(17);
            dest.writeInt(this.viewId);
            dest.writeInt(this.isRelative ? 1 : 0);
            dest.writeInt(this.index);
            dest.writeInt(this.color);
            dest.writeInt(this.mode.ordinal());
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target != null) {
                Drawable[] drawables;
                if (this.isRelative) {
                    drawables = target.getCompoundDrawablesRelative();
                } else {
                    drawables = target.getCompoundDrawables();
                }
                if (this.index < 0 || this.index >= 4) {
                    throw new IllegalStateException("index must be in range [0, 3].");
                }
                Drawable d = drawables[this.index];
                if (d != null) {
                    d.mutate();
                    d.setColorFilter(this.color, this.mode);
                }
            }
        }

        public String getActionName() {
            return "TextViewDrawableColorFilterAction";
        }
    }

    private class TextViewSizeAction extends Action {
        float size;
        int units;

        public TextViewSizeAction(int viewId, int units, float size) {
            super();
            this.viewId = viewId;
            this.units = units;
            this.size = size;
        }

        public TextViewSizeAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.units = parcel.readInt();
            this.size = parcel.readFloat();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(13);
            dest.writeInt(this.viewId);
            dest.writeInt(this.units);
            dest.writeFloat(this.size);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            TextView target = (TextView) root.findViewById(this.viewId);
            if (target != null) {
                target.setTextSize(this.units, this.size);
            }
        }

        public String getActionName() {
            return "TextViewSizeAction";
        }
    }

    private class ViewGroupActionAdd extends Action {
        private int mIndex;
        private RemoteViews mNestedViews;

        ViewGroupActionAdd(RemoteViews this$0, int viewId, RemoteViews nestedViews) {
            this(viewId, nestedViews, -1);
        }

        ViewGroupActionAdd(int viewId, RemoteViews nestedViews, int index) {
            super();
            this.viewId = viewId;
            this.mNestedViews = nestedViews;
            this.mIndex = index;
            if (nestedViews != null) {
                RemoteViews.this.configureRemoteViewsAsChild(nestedViews);
            }
        }

        ViewGroupActionAdd(Parcel parcel, BitmapCache bitmapCache, ApplicationInfo info, int depth) {
            super();
            this.viewId = parcel.readInt();
            this.mIndex = parcel.readInt();
            this.mNestedViews = new RemoteViews(parcel, bitmapCache, info, depth, null);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(4);
            dest.writeInt(this.viewId);
            dest.writeInt(this.mIndex);
            this.mNestedViews.writeToParcel(dest, flags);
        }

        public boolean hasSameAppInfo(ApplicationInfo parentInfo) {
            if (this.mNestedViews.mApplication.packageName.equals(parentInfo.packageName) && this.mNestedViews.mApplication.uid == parentInfo.uid) {
                return true;
            }
            return false;
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            Context context = root.getContext();
            ViewGroup target = (ViewGroup) root.findViewById(this.viewId);
            if (target != null) {
                target.addView(this.mNestedViews.apply(context, target, handler), this.mIndex);
            }
        }

        public Action initActionAsync(ViewTree root, ViewGroup rootParent, OnClickHandler handler) {
            root.createTree();
            ViewTree target = root.findViewTreeById(this.viewId);
            if (target == null || ((target.mRoot instanceof ViewGroup) ^ 1) != 0) {
                return RemoteViews.ACTION_NOOP;
            }
            final ViewGroup targetVg = (ViewGroup) target.mRoot;
            final AsyncApplyTask task = this.mNestedViews.getAsyncApplyTask(root.mRoot.getContext(), targetVg, null, handler);
            final ViewTree tree = task.doInBackground(new Void[0]);
            if (tree == null) {
                throw new ActionException(task.mError);
            }
            target.addChild(tree, this.mIndex);
            return new RuntimeAction() {
                public void apply(View root, ViewGroup rootParent, OnClickHandler handler) throws ActionException {
                    task.onPostExecute(tree);
                    targetVg.addView(task.mResult, ViewGroupActionAdd.this.mIndex);
                }
            };
        }

        public void updateMemoryUsageEstimate(MemoryUsageCounter counter) {
            counter.increment(this.mNestedViews.estimateMemoryUsage());
        }

        public void setBitmapCache(BitmapCache bitmapCache) {
            this.mNestedViews.setBitmapCache(bitmapCache);
        }

        public int mergeBehavior() {
            return 1;
        }

        public boolean prefersAsyncApply() {
            return this.mNestedViews.prefersAsyncApply();
        }

        public String getActionName() {
            return "ViewGroupActionAdd";
        }
    }

    private class ViewGroupActionRemove extends Action {
        private static final int REMOVE_ALL_VIEWS_ID = -2;
        private int mViewIdToKeep;

        ViewGroupActionRemove(RemoteViews this$0, int viewId) {
            this(viewId, -2);
        }

        ViewGroupActionRemove(int viewId, int viewIdToKeep) {
            super();
            this.viewId = viewId;
            this.mViewIdToKeep = viewIdToKeep;
        }

        ViewGroupActionRemove(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.mViewIdToKeep = parcel.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(7);
            dest.writeInt(this.viewId);
            dest.writeInt(this.mViewIdToKeep);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            ViewGroup target = (ViewGroup) root.findViewById(this.viewId);
            if (target != null) {
                if (this.mViewIdToKeep == -2) {
                    target.removeAllViews();
                } else {
                    removeAllViewsExceptIdToKeep(target);
                }
            }
        }

        public Action initActionAsync(ViewTree root, ViewGroup rootParent, OnClickHandler handler) {
            root.createTree();
            ViewTree target = root.findViewTreeById(this.viewId);
            if (target == null || ((target.mRoot instanceof ViewGroup) ^ 1) != 0) {
                return RemoteViews.ACTION_NOOP;
            }
            final ViewGroup targetVg = (ViewGroup) target.mRoot;
            target.mChildren = null;
            return new RuntimeAction() {
                public void apply(View root, ViewGroup rootParent, OnClickHandler handler) throws ActionException {
                    if (ViewGroupActionRemove.this.mViewIdToKeep == -2) {
                        targetVg.removeAllViews();
                    } else {
                        ViewGroupActionRemove.this.removeAllViewsExceptIdToKeep(targetVg);
                    }
                }
            };
        }

        private void removeAllViewsExceptIdToKeep(ViewGroup viewGroup) {
            for (int index = viewGroup.getChildCount() - 1; index >= 0; index--) {
                if (viewGroup.getChildAt(index).getId() != this.mViewIdToKeep) {
                    viewGroup.removeViewAt(index);
                }
            }
        }

        public String getActionName() {
            return "ViewGroupActionRemove";
        }

        public int mergeBehavior() {
            return 1;
        }
    }

    private class ViewPaddingAction extends Action {
        int bottom;
        int left;
        int right;
        int top;

        public ViewPaddingAction(int viewId, int left, int top, int right, int bottom) {
            super();
            this.viewId = viewId;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public ViewPaddingAction(Parcel parcel) {
            super();
            this.viewId = parcel.readInt();
            this.left = parcel.readInt();
            this.top = parcel.readInt();
            this.right = parcel.readInt();
            this.bottom = parcel.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(14);
            dest.writeInt(this.viewId);
            dest.writeInt(this.left);
            dest.writeInt(this.top);
            dest.writeInt(this.right);
            dest.writeInt(this.bottom);
        }

        public void apply(View root, ViewGroup rootParent, OnClickHandler handler) {
            View target = root.findViewById(this.viewId);
            if (target != null) {
                target.setPadding(this.left, this.top, this.right, this.bottom);
            }
        }

        public String getActionName() {
            return "ViewPaddingAction";
        }
    }

    private static class ViewTree {
        private static final int INSERT_AT_END_INDEX = -1;
        private ArrayList<ViewTree> mChildren;
        private View mRoot;

        /* synthetic */ ViewTree(View root, ViewTree -this1) {
            this(root);
        }

        private ViewTree(View root) {
            this.mRoot = root;
        }

        public void createTree() {
            if (this.mChildren == null) {
                this.mChildren = new ArrayList();
                if (this.mRoot instanceof ViewGroup) {
                    ViewGroup vg = this.mRoot;
                    int count = vg.getChildCount();
                    for (int i = 0; i < count; i++) {
                        addViewChild(vg.getChildAt(i));
                    }
                }
            }
        }

        public ViewTree findViewTreeById(int id) {
            if (this.mRoot.getId() == id) {
                return this;
            }
            if (this.mChildren == null) {
                return null;
            }
            for (ViewTree tree : this.mChildren) {
                ViewTree result = tree.findViewTreeById(id);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        public void replaceView(View v) {
            this.mRoot = v;
            this.mChildren = null;
            createTree();
        }

        public <T extends View> T findViewById(int id) {
            T t = null;
            if (this.mChildren == null) {
                return this.mRoot.findViewById(id);
            }
            ViewTree tree = findViewTreeById(id);
            if (tree != null) {
                t = tree.mRoot;
            }
            return t;
        }

        public void addChild(ViewTree child) {
            addChild(child, -1);
        }

        public void addChild(ViewTree child, int index) {
            if (this.mChildren == null) {
                this.mChildren = new ArrayList();
            }
            child.createTree();
            if (index == -1) {
                this.mChildren.add(child);
            } else {
                this.mChildren.add(index, child);
            }
        }

        private void addViewChild(View v) {
            if (!v.isRootNamespace()) {
                ViewTree target;
                if (v.getId() != 0) {
                    ViewTree tree = new ViewTree(v);
                    this.mChildren.add(tree);
                    target = tree;
                } else {
                    target = this;
                }
                if ((v instanceof ViewGroup) && target.mChildren == null) {
                    target.mChildren = new ArrayList();
                    ViewGroup vg = (ViewGroup) v;
                    int count = vg.getChildCount();
                    for (int i = 0; i < count; i++) {
                        target.addViewChild(vg.getChildAt(i));
                    }
                }
            }
        }
    }

    /* synthetic */ RemoteViews(Parcel parcel, BitmapCache bitmapCache, ApplicationInfo info, int depth, RemoteViews -this4) {
        this(parcel, bitmapCache, info, depth);
    }

    public void setRemoteInputs(int viewId, RemoteInput[] remoteInputs) {
        this.mActions.add(new SetRemoteInputsAction(viewId, remoteInputs));
    }

    public void reduceImageSizes(int maxWidth, int maxHeight) {
        ArrayList<Bitmap> cache = this.mBitmapCache.mBitmaps;
        for (int i = 0; i < cache.size(); i++) {
            cache.set(i, Icon.scaleDownIfNecessary((Bitmap) cache.get(i), maxWidth, maxHeight));
        }
    }

    public void overrideTextColors(int textColor) {
        addAction(new OverrideTextColorsAction(textColor));
    }

    public void setReapplyDisallowed() {
        this.mReapplyDisallowed = true;
    }

    public boolean isReapplyDisallowed() {
        return this.mReapplyDisallowed;
    }

    public void mergeRemoteViews(RemoteViews newRv) {
        if (newRv != null) {
            int i;
            Action a;
            RemoteViews copy = newRv.clone();
            HashMap<String, Action> map = new HashMap();
            if (this.mActions == null) {
                this.mActions = new ArrayList();
            }
            int count = this.mActions.size();
            for (i = 0; i < count; i++) {
                a = (Action) this.mActions.get(i);
                map.put(a.getUniqueKey(), a);
            }
            ArrayList<Action> newActions = copy.mActions;
            if (newActions != null) {
                count = newActions.size();
                for (i = 0; i < count; i++) {
                    a = (Action) newActions.get(i);
                    String key = ((Action) newActions.get(i)).getUniqueKey();
                    int mergeBehavior = ((Action) newActions.get(i)).mergeBehavior();
                    if (map.containsKey(key) && mergeBehavior == 0) {
                        this.mActions.remove(map.get(key));
                        map.remove(key);
                    }
                    if (mergeBehavior == 0 || mergeBehavior == 1) {
                        this.mActions.add(a);
                    }
                }
                this.mBitmapCache = new BitmapCache();
                setBitmapCache(this.mBitmapCache);
                recalculateMemoryUsage();
            }
        }
    }

    private static Rect getSourceBounds(View v) {
        float appScale = v.getContext().getResources().getCompatibilityInfo().applicationScale;
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        Rect rect = new Rect();
        rect.left = (int) ((((float) pos[0]) * appScale) + 0.5f);
        rect.top = (int) ((((float) pos[1]) * appScale) + 0.5f);
        rect.right = (int) ((((float) (pos[0] + v.getWidth())) * appScale) + 0.5f);
        rect.bottom = (int) ((((float) (pos[1] + v.getHeight())) * appScale) + 0.5f);
        return rect;
    }

    private Method getMethod(View view, String methodName, Class<?> paramType) {
        Method method;
        Class<? extends View> klass = view.getClass();
        synchronized (sMethodsLock) {
            ArrayMap<MutablePair<String, Class<?>>, Method> methods = (ArrayMap) sMethods.get(klass);
            if (methods == null) {
                methods = new ArrayMap();
                sMethods.put(klass, methods);
            }
            this.mPair.first = methodName;
            this.mPair.second = paramType;
            method = (Method) methods.get(this.mPair);
            if (method == null) {
                if (paramType == null) {
                    try {
                        method = klass.getMethod(methodName, new Class[0]);
                    } catch (NoSuchMethodException e) {
                        throw new ActionException("view: " + klass.getName() + " doesn't have method: " + methodName + getParameters(paramType));
                    }
                }
                method = klass.getMethod(methodName, new Class[]{paramType});
                if (method.isAnnotationPresent(RemotableViewMethod.class)) {
                    methods.put(new MutablePair(methodName, paramType), method);
                } else {
                    throw new ActionException("view: " + klass.getName() + " can't use method with RemoteViews: " + methodName + getParameters(paramType));
                }
            }
        }
        return method;
    }

    private Method getAsyncMethod(Method method) {
        synchronized (sAsyncMethods) {
            int valueIndex = sAsyncMethods.indexOfKey(method);
            if (valueIndex >= 0) {
                Method method2 = (Method) sAsyncMethods.valueAt(valueIndex);
                return method2;
            }
            RemotableViewMethod annotation = (RemotableViewMethod) method.getAnnotation(RemotableViewMethod.class);
            Method method3 = null;
            if (!annotation.asyncImpl().isEmpty()) {
                try {
                    method3 = method.getDeclaringClass().getMethod(annotation.asyncImpl(), method.getParameterTypes());
                    if (!method3.getReturnType().equals(Runnable.class)) {
                        throw new ActionException("Async implementation for " + method.getName() + " does not return a Runnable");
                    }
                } catch (NoSuchMethodException e) {
                    throw new ActionException("Async implementation declared but not defined for " + method.getName());
                }
            }
            sAsyncMethods.put(method, method3);
            return method3;
        }
    }

    private static String getParameters(Class<?> paramType) {
        if (paramType == null) {
            return "()";
        }
        return "(" + paramType + ")";
    }

    private static Object[] wrapArg(Object value) {
        Object[] args = (Object[]) sInvokeArgsTls.get();
        args[0] = value;
        return args;
    }

    private void configureRemoteViewsAsChild(RemoteViews rv) {
        this.mBitmapCache.assimilate(rv.mBitmapCache);
        rv.setBitmapCache(this.mBitmapCache);
        rv.setNotRoot();
    }

    void setNotRoot() {
        this.mIsRoot = false;
    }

    public RemoteViews(String packageName, int layoutId) {
        this(getApplicationInfo(packageName, UserHandle.myUserId()), layoutId);
    }

    public RemoteViews(String packageName, int userId, int layoutId) {
        this(getApplicationInfo(packageName, userId), layoutId);
    }

    protected RemoteViews(ApplicationInfo application, int layoutId) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mPair = new MutablePair(null, null);
        this.mApplication = application;
        this.mLayoutId = layoutId;
        this.mBitmapCache = new BitmapCache();
        this.mMemoryUsageCounter = new MemoryUsageCounter();
        recalculateMemoryUsage();
    }

    private boolean hasLandscapeAndPortraitLayouts() {
        return (this.mLandscape == null || this.mPortrait == null) ? false : true;
    }

    public RemoteViews(RemoteViews landscape, RemoteViews portrait) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mPair = new MutablePair(null, null);
        if (landscape == null || portrait == null) {
            throw new RuntimeException("Both RemoteViews must be non-null");
        } else if (landscape.mApplication.uid == portrait.mApplication.uid && (landscape.mApplication.packageName.equals(portrait.mApplication.packageName) ^ 1) == 0) {
            this.mApplication = portrait.mApplication;
            this.mLayoutId = portrait.getLayoutId();
            this.mLandscape = landscape;
            this.mPortrait = portrait;
            this.mMemoryUsageCounter = new MemoryUsageCounter();
            this.mBitmapCache = new BitmapCache();
            configureRemoteViewsAsChild(landscape);
            configureRemoteViewsAsChild(portrait);
            recalculateMemoryUsage();
        } else {
            throw new RuntimeException("Both RemoteViews must share the same package and user");
        }
    }

    public RemoteViews(Parcel parcel) {
        this(parcel, null, null, 0);
    }

    private RemoteViews(Parcel parcel, BitmapCache bitmapCache, ApplicationInfo info, int depth) {
        this.mIsRoot = true;
        this.mLandscape = null;
        this.mPortrait = null;
        this.mIsWidgetCollectionChild = false;
        this.mPair = new MutablePair(null, null);
        if (depth <= 10 || UserHandle.getAppId(Binder.getCallingUid()) == 1000) {
            depth++;
            int mode = parcel.readInt();
            if (bitmapCache == null) {
                this.mBitmapCache = new BitmapCache(parcel);
            } else {
                setBitmapCache(bitmapCache);
                setNotRoot();
            }
            if (mode == 0) {
                if (parcel.readInt() != 0) {
                    info = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(parcel);
                }
                this.mApplication = info;
                this.mLayoutId = parcel.readInt();
                this.mIsWidgetCollectionChild = parcel.readInt() == 1;
                int count = parcel.readInt();
                if (count > 0) {
                    this.mActions = new ArrayList(count);
                    for (int i = 0; i < count; i++) {
                        int tag = parcel.readInt();
                        switch (tag) {
                            case 1:
                                this.mActions.add(new SetOnClickPendingIntent(parcel));
                                break;
                            case 2:
                                this.mActions.add(new ReflectionAction(this, parcel));
                                break;
                            case 3:
                                this.mActions.add(new SetDrawableParameters(this, parcel));
                                break;
                            case 4:
                                this.mActions.add(new ViewGroupActionAdd(parcel, this.mBitmapCache, this.mApplication, depth));
                                break;
                            case 5:
                                this.mActions.add(new ReflectionActionWithoutParams(parcel));
                                break;
                            case 6:
                                this.mActions.add(new SetEmptyView(parcel));
                                break;
                            case 7:
                                this.mActions.add(new ViewGroupActionRemove(parcel));
                                break;
                            case 8:
                                this.mActions.add(new SetPendingIntentTemplate(parcel));
                                break;
                            case 9:
                                this.mActions.add(new SetOnClickFillInIntent(parcel));
                                break;
                            case 10:
                                this.mActions.add(new SetRemoteViewsAdapterIntent(parcel));
                                break;
                            case 11:
                                this.mActions.add(new TextViewDrawableAction(this, parcel));
                                break;
                            case 12:
                                this.mActions.add(new BitmapReflectionAction(parcel));
                                break;
                            case 13:
                                this.mActions.add(new TextViewSizeAction(parcel));
                                break;
                            case 14:
                                this.mActions.add(new ViewPaddingAction(parcel));
                                break;
                            case 15:
                                this.mActions.add(new SetRemoteViewsAdapterList(parcel));
                                break;
                            case 17:
                                this.mActions.add(new TextViewDrawableColorFilterAction(this, parcel));
                                break;
                            case 18:
                                this.mActions.add(new SetRemoteInputsAction(parcel));
                                break;
                            case 19:
                                this.mActions.add(new LayoutParamAction(parcel));
                                break;
                            case 20:
                                this.mActions.add(new OverrideTextColorsAction(parcel));
                                break;
                            default:
                                throw new ActionException("Tag " + tag + " not found");
                        }
                    }
                }
            } else {
                this.mLandscape = new RemoteViews(parcel, this.mBitmapCache, info, depth);
                this.mPortrait = new RemoteViews(parcel, this.mBitmapCache, this.mLandscape.mApplication, depth);
                this.mApplication = this.mPortrait.mApplication;
                this.mLayoutId = this.mPortrait.getLayoutId();
            }
            this.mReapplyDisallowed = parcel.readInt() == 0;
            this.mMemoryUsageCounter = new MemoryUsageCounter();
            recalculateMemoryUsage();
            return;
        }
        throw new IllegalArgumentException("Too many nested views.");
    }

    public RemoteViews clone() {
        RemoteViews rv;
        synchronized (this) {
            Preconditions.checkState(this.mIsRoot, "RemoteView has been attached to another RemoteView. May only clone the root of a RemoteView hierarchy.");
            Parcel p = Parcel.obtain();
            this.mIsRoot = false;
            writeToParcel(p, 2);
            p.setDataPosition(0);
            this.mIsRoot = true;
            rv = new RemoteViews(p, this.mBitmapCache.clone(), this.mApplication, 0);
            rv.mIsRoot = true;
            p.recycle();
        }
        return rv;
    }

    public String getPackage() {
        return this.mApplication != null ? this.mApplication.packageName : null;
    }

    public int getLayoutId() {
        return this.mLayoutId;
    }

    void setIsWidgetCollectionChild(boolean isWidgetCollectionChild) {
        this.mIsWidgetCollectionChild = isWidgetCollectionChild;
    }

    private void recalculateMemoryUsage() {
        this.mMemoryUsageCounter.clear();
        if (hasLandscapeAndPortraitLayouts()) {
            this.mMemoryUsageCounter.increment(this.mLandscape.estimateMemoryUsage());
            this.mMemoryUsageCounter.increment(this.mPortrait.estimateMemoryUsage());
            this.mBitmapCache.addBitmapMemory(this.mMemoryUsageCounter);
            return;
        }
        if (this.mActions != null) {
            int count = this.mActions.size();
            for (int i = 0; i < count; i++) {
                ((Action) this.mActions.get(i)).updateMemoryUsageEstimate(this.mMemoryUsageCounter);
            }
        }
        if (this.mIsRoot) {
            this.mBitmapCache.addBitmapMemory(this.mMemoryUsageCounter);
        }
    }

    private void setBitmapCache(BitmapCache bitmapCache) {
        this.mBitmapCache = bitmapCache;
        if (hasLandscapeAndPortraitLayouts()) {
            this.mLandscape.setBitmapCache(bitmapCache);
            this.mPortrait.setBitmapCache(bitmapCache);
        } else if (this.mActions != null) {
            int count = this.mActions.size();
            for (int i = 0; i < count; i++) {
                ((Action) this.mActions.get(i)).setBitmapCache(bitmapCache);
            }
        }
    }

    public int estimateMemoryUsage() {
        return this.mMemoryUsageCounter.getMemoryUsage();
    }

    private void addAction(Action a) {
        if (hasLandscapeAndPortraitLayouts()) {
            throw new RuntimeException("RemoteViews specifying separate landscape and portrait layouts cannot be modified. Instead, fully configure the landscape and portrait layouts individually before constructing the combined layout.");
        }
        if (this.mActions == null) {
            this.mActions = new ArrayList();
        }
        this.mActions.add(a);
        a.updateMemoryUsageEstimate(this.mMemoryUsageCounter);
    }

    public void addView(int viewId, RemoteViews nestedView) {
        Action viewGroupActionRemove;
        if (nestedView == null) {
            viewGroupActionRemove = new ViewGroupActionRemove(this, viewId);
        } else {
            viewGroupActionRemove = new ViewGroupActionAdd(this, viewId, nestedView);
        }
        addAction(viewGroupActionRemove);
    }

    public void addView(int viewId, RemoteViews nestedView, int index) {
        addAction(new ViewGroupActionAdd(viewId, nestedView, index));
    }

    public void removeAllViews(int viewId) {
        addAction(new ViewGroupActionRemove(this, viewId));
    }

    public void removeAllViewsExceptId(int viewId, int viewIdToKeep) {
        addAction(new ViewGroupActionRemove(viewId, viewIdToKeep));
    }

    public void showNext(int viewId) {
        addAction(new ReflectionActionWithoutParams(viewId, "showNext"));
    }

    public void showPrevious(int viewId) {
        addAction(new ReflectionActionWithoutParams(viewId, "showPrevious"));
    }

    public void setDisplayedChild(int viewId, int childIndex) {
        setInt(viewId, "setDisplayedChild", childIndex);
    }

    public void setViewVisibility(int viewId, int visibility) {
        setInt(viewId, "setVisibility", visibility);
    }

    public void setTextViewText(int viewId, CharSequence text) {
        setCharSequence(viewId, "setText", text);
    }

    public void setTextViewTextSize(int viewId, int units, float size) {
        addAction(new TextViewSizeAction(viewId, units, size));
    }

    public void setTextViewCompoundDrawables(int viewId, int left, int top, int right, int bottom) {
        addAction(new TextViewDrawableAction(viewId, false, left, top, right, bottom));
    }

    public void setTextViewCompoundDrawablesRelative(int viewId, int start, int top, int end, int bottom) {
        addAction(new TextViewDrawableAction(viewId, true, start, top, end, bottom));
    }

    public void setTextViewCompoundDrawablesRelativeColorFilter(int viewId, int index, int color, Mode mode) {
        if (index < 0 || index >= 4) {
            throw new IllegalArgumentException("index must be in range [0, 3].");
        }
        addAction(new TextViewDrawableColorFilterAction(viewId, true, index, color, mode));
    }

    public void setTextViewCompoundDrawables(int viewId, Icon left, Icon top, Icon right, Icon bottom) {
        addAction(new TextViewDrawableAction(viewId, false, left, top, right, bottom));
    }

    public void setTextViewCompoundDrawablesRelative(int viewId, Icon start, Icon top, Icon end, Icon bottom) {
        addAction(new TextViewDrawableAction(viewId, true, start, top, end, bottom));
    }

    public void setImageViewResource(int viewId, int srcId) {
        setInt(viewId, "setImageResource", srcId);
    }

    public void setImageViewUri(int viewId, Uri uri) {
        setUri(viewId, "setImageURI", uri);
    }

    public void setImageViewBitmap(int viewId, Bitmap bitmap) {
        setBitmap(viewId, "setImageBitmap", bitmap);
    }

    public void setImageViewIcon(int viewId, Icon icon) {
        setIcon(viewId, "setImageIcon", icon);
    }

    public void setEmptyView(int viewId, int emptyViewId) {
        addAction(new SetEmptyView(viewId, emptyViewId));
    }

    public void setChronometer(int viewId, long base, String format, boolean started) {
        setLong(viewId, "setBase", base);
        setString(viewId, "setFormat", format);
        setBoolean(viewId, "setStarted", started);
    }

    public void setChronometerCountDown(int viewId, boolean isCountDown) {
        setBoolean(viewId, "setCountDown", isCountDown);
    }

    public void setProgressBar(int viewId, int max, int progress, boolean indeterminate) {
        setBoolean(viewId, "setIndeterminate", indeterminate);
        if (!indeterminate) {
            setInt(viewId, "setMax", max);
            setInt(viewId, "setProgress", progress);
        }
    }

    public void setOnClickPendingIntent(int viewId, PendingIntent pendingIntent) {
        addAction(new SetOnClickPendingIntent(viewId, pendingIntent));
    }

    public void setPendingIntentTemplate(int viewId, PendingIntent pendingIntentTemplate) {
        addAction(new SetPendingIntentTemplate(viewId, pendingIntentTemplate));
    }

    public void setOnClickFillInIntent(int viewId, Intent fillInIntent) {
        addAction(new SetOnClickFillInIntent(viewId, fillInIntent));
    }

    public void setDrawableParameters(int viewId, boolean targetBackground, int alpha, int colorFilter, Mode mode, int level) {
        addAction(new SetDrawableParameters(viewId, targetBackground, alpha, colorFilter, mode, level));
    }

    public void setProgressTintList(int viewId, ColorStateList tint) {
        addAction(new ReflectionAction(viewId, "setProgressTintList", 15, tint));
    }

    public void setProgressBackgroundTintList(int viewId, ColorStateList tint) {
        addAction(new ReflectionAction(viewId, "setProgressBackgroundTintList", 15, tint));
    }

    public void setProgressIndeterminateTintList(int viewId, ColorStateList tint) {
        addAction(new ReflectionAction(viewId, "setIndeterminateTintList", 15, tint));
    }

    public void setTextColor(int viewId, int color) {
        setInt(viewId, "setTextColor", color);
    }

    public void setTextColor(int viewId, ColorStateList colors) {
        addAction(new ReflectionAction(viewId, "setTextColor", 15, colors));
    }

    @Deprecated
    public void setRemoteAdapter(int appWidgetId, int viewId, Intent intent) {
        setRemoteAdapter(viewId, intent);
    }

    public void setRemoteAdapter(int viewId, Intent intent) {
        addAction(new SetRemoteViewsAdapterIntent(viewId, intent));
    }

    public void setRemoteAdapter(int viewId, ArrayList<RemoteViews> list, int viewTypeCount) {
        addAction(new SetRemoteViewsAdapterList(viewId, list, viewTypeCount));
    }

    public void setScrollPosition(int viewId, int position) {
        setInt(viewId, "smoothScrollToPosition", position);
    }

    public void setRelativeScrollPosition(int viewId, int offset) {
        setInt(viewId, "smoothScrollByOffset", offset);
    }

    public void setViewPadding(int viewId, int left, int top, int right, int bottom) {
        addAction(new ViewPaddingAction(viewId, left, top, right, bottom));
    }

    public void setViewLayoutMarginEndDimen(int viewId, int endMarginDimen) {
        addAction(new LayoutParamAction(viewId, 1, endMarginDimen));
    }

    public void setViewLayoutMarginBottomDimen(int viewId, int bottomMarginDimen) {
        addAction(new LayoutParamAction(viewId, 3, bottomMarginDimen));
    }

    public void setViewLayoutWidth(int viewId, int layoutWidth) {
        if (layoutWidth == 0 || layoutWidth == -1 || layoutWidth == -2) {
            this.mActions.add(new LayoutParamAction(viewId, 2, layoutWidth));
            return;
        }
        throw new IllegalArgumentException("Only supports 0, WRAP_CONTENT and MATCH_PARENT");
    }

    public void setBoolean(int viewId, String methodName, boolean value) {
        addAction(new ReflectionAction(viewId, methodName, 1, Boolean.valueOf(value)));
    }

    public void setByte(int viewId, String methodName, byte value) {
        addAction(new ReflectionAction(viewId, methodName, 2, Byte.valueOf(value)));
    }

    public void setShort(int viewId, String methodName, short value) {
        addAction(new ReflectionAction(viewId, methodName, 3, Short.valueOf(value)));
    }

    public void setInt(int viewId, String methodName, int value) {
        addAction(new ReflectionAction(viewId, methodName, 4, Integer.valueOf(value)));
    }

    public void setLong(int viewId, String methodName, long value) {
        addAction(new ReflectionAction(viewId, methodName, 5, Long.valueOf(value)));
    }

    public void setFloat(int viewId, String methodName, float value) {
        addAction(new ReflectionAction(viewId, methodName, 6, Float.valueOf(value)));
    }

    public void setDouble(int viewId, String methodName, double value) {
        addAction(new ReflectionAction(viewId, methodName, 7, Double.valueOf(value)));
    }

    public void setChar(int viewId, String methodName, char value) {
        addAction(new ReflectionAction(viewId, methodName, 8, Character.valueOf(value)));
    }

    public void setString(int viewId, String methodName, String value) {
        addAction(new ReflectionAction(viewId, methodName, 9, value));
    }

    public void setCharSequence(int viewId, String methodName, CharSequence value) {
        addAction(new ReflectionAction(viewId, methodName, 10, value));
    }

    public void setUri(int viewId, String methodName, Uri value) {
        if (value != null) {
            value = value.getCanonicalUri();
            if (StrictMode.vmFileUriExposureEnabled()) {
                value.checkFileUriExposed("RemoteViews.setUri()");
            }
        }
        addAction(new ReflectionAction(viewId, methodName, 11, value));
    }

    public void setBitmap(int viewId, String methodName, Bitmap value) {
        addAction(new BitmapReflectionAction(viewId, methodName, value));
    }

    public void setBundle(int viewId, String methodName, Bundle value) {
        addAction(new ReflectionAction(viewId, methodName, 13, value));
    }

    public void setIntent(int viewId, String methodName, Intent value) {
        addAction(new ReflectionAction(viewId, methodName, 14, value));
    }

    public void setIcon(int viewId, String methodName, Icon value) {
        addAction(new ReflectionAction(viewId, methodName, 16, value));
    }

    public void setContentDescription(int viewId, CharSequence contentDescription) {
        setCharSequence(viewId, "setContentDescription", contentDescription);
    }

    public void setAccessibilityTraversalBefore(int viewId, int nextId) {
        setInt(viewId, "setAccessibilityTraversalBefore", nextId);
    }

    public void setAccessibilityTraversalAfter(int viewId, int nextId) {
        setInt(viewId, "setAccessibilityTraversalAfter", nextId);
    }

    public void setLabelFor(int viewId, int labeledId) {
        setInt(viewId, "setLabelFor", labeledId);
    }

    private RemoteViews getRemoteViewsToApply(Context context) {
        if (!hasLandscapeAndPortraitLayouts()) {
            return this;
        }
        if (context.getResources().getConfiguration().orientation == 2) {
            return this.mLandscape;
        }
        return this.mPortrait;
    }

    public View apply(Context context, ViewGroup parent) {
        return apply(context, parent, null);
    }

    public View apply(Context context, ViewGroup parent, OnClickHandler handler) {
        RemoteViews rvToApply = getRemoteViewsToApply(context);
        View result = inflateView(context, rvToApply, parent);
        loadTransitionOverride(context, handler);
        rvToApply.performApply(result, parent, handler);
        return result;
    }

    private View inflateView(Context context, RemoteViews rv, ViewGroup parent) {
        LayoutInflater inflater = ((LayoutInflater) context.getSystemService("layout_inflater")).cloneInContext(new RemoteViewsContextWrapper(context, getContextForResources(context)));
        inflater.setFilter(this);
        View v = inflater.inflate(rv.getLayoutId(), parent, false);
        v.setTagInternal(R.id.widget_frame, Integer.valueOf(rv.getLayoutId()));
        return v;
    }

    private static void loadTransitionOverride(Context context, OnClickHandler handler) {
        if (handler != null && context.getResources().getBoolean(R.bool.config_overrideRemoteViewsActivityTransition)) {
            TypedArray windowStyle = context.getTheme().obtainStyledAttributes(R.styleable.Window);
            TypedArray windowAnimationStyle = context.obtainStyledAttributes(windowStyle.getResourceId(8, 0), R.styleable.WindowAnimation);
            handler.setEnterAnimationId(windowAnimationStyle.getResourceId(26, 0));
            windowStyle.recycle();
            windowAnimationStyle.recycle();
        }
    }

    public CancellationSignal applyAsync(Context context, ViewGroup parent, Executor executor, OnViewAppliedListener listener) {
        return applyAsync(context, parent, executor, listener, null);
    }

    private CancellationSignal startTaskOnExecutor(AsyncApplyTask task, Executor executor) {
        CancellationSignal cancelSignal = new CancellationSignal();
        cancelSignal.setOnCancelListener(task);
        if (executor == null) {
            executor = AsyncTask.THREAD_POOL_EXECUTOR;
        }
        task.executeOnExecutor(executor, new Void[0]);
        return cancelSignal;
    }

    public CancellationSignal applyAsync(Context context, ViewGroup parent, Executor executor, OnViewAppliedListener listener, OnClickHandler handler) {
        return startTaskOnExecutor(getAsyncApplyTask(context, parent, listener, handler), executor);
    }

    private AsyncApplyTask getAsyncApplyTask(Context context, ViewGroup parent, OnViewAppliedListener listener, OnClickHandler handler) {
        return new AsyncApplyTask(this, getRemoteViewsToApply(context), parent, context, listener, handler, null, null);
    }

    public void reapply(Context context, View v) {
        reapply(context, v, null);
    }

    public void reapply(Context context, View v, OnClickHandler handler) {
        RemoteViews rvToApply = getRemoteViewsToApply(context);
        if (!hasLandscapeAndPortraitLayouts() || ((Integer) v.getTag(R.id.widget_frame)).intValue() == rvToApply.getLayoutId()) {
            rvToApply.performApply(v, (ViewGroup) v.getParent(), handler);
            return;
        }
        throw new RuntimeException("Attempting to re-apply RemoteViews to a view that that does not share the same root layout id.");
    }

    public CancellationSignal reapplyAsync(Context context, View v, Executor executor, OnViewAppliedListener listener) {
        return reapplyAsync(context, v, executor, listener, null);
    }

    public CancellationSignal reapplyAsync(Context context, View v, Executor executor, OnViewAppliedListener listener, OnClickHandler handler) {
        RemoteViews rvToApply = getRemoteViewsToApply(context);
        if (!hasLandscapeAndPortraitLayouts() || ((Integer) v.getTag(R.id.widget_frame)).intValue() == rvToApply.getLayoutId()) {
            return startTaskOnExecutor(new AsyncApplyTask(this, rvToApply, (ViewGroup) v.getParent(), context, listener, handler, v, null), executor);
        }
        throw new RuntimeException("Attempting to re-apply RemoteViews to a view that that does not share the same root layout id.");
    }

    private void performApply(View v, ViewGroup parent, OnClickHandler handler) {
        if (this.mActions != null) {
            if (handler == null) {
                handler = DEFAULT_ON_CLICK_HANDLER;
            }
            int count = this.mActions.size();
            for (int i = 0; i < count; i++) {
                ((Action) this.mActions.get(i)).apply(v, parent, handler);
            }
        }
    }

    public boolean prefersAsyncApply() {
        if (this.mActions != null) {
            int count = this.mActions.size();
            for (int i = 0; i < count; i++) {
                if (((Action) this.mActions.get(i)).prefersAsyncApply()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Context getContextForResources(Context context) {
        if (this.mApplication != null) {
            if (context.getUserId() == UserHandle.getUserId(this.mApplication.uid) && context.getPackageName().equals(this.mApplication.packageName)) {
                return context;
            }
            try {
                return context.createApplicationContext(this.mApplication, 4);
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Package name " + this.mApplication.packageName + " not found");
            }
        }
        return context;
    }

    public int getSequenceNumber() {
        return this.mActions == null ? 0 : this.mActions.size();
    }

    public boolean onLoadClass(Class clazz) {
        return clazz.isAnnotationPresent(RemoteView.class);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i = 1;
        if (hasLandscapeAndPortraitLayouts()) {
            dest.writeInt(1);
            if (this.mIsRoot) {
                this.mBitmapCache.writeBitmapsToParcel(dest, flags);
            }
            this.mLandscape.writeToParcel(dest, flags);
            this.mPortrait.writeToParcel(dest, flags | 2);
        } else {
            int i2;
            int count;
            dest.writeInt(0);
            if (this.mIsRoot) {
                this.mBitmapCache.writeBitmapsToParcel(dest, flags);
            }
            if (this.mIsRoot || (flags & 2) == 0) {
                dest.writeInt(1);
                this.mApplication.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(this.mLayoutId);
            if (this.mIsWidgetCollectionChild) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            dest.writeInt(i2);
            if (this.mActions != null) {
                count = this.mActions.size();
            } else {
                count = 0;
            }
            dest.writeInt(count);
            for (int i3 = 0; i3 < count; i3++) {
                Action a = (Action) this.mActions.get(i3);
                if (a.hasSameAppInfo(this.mApplication)) {
                    i2 = 2;
                } else {
                    i2 = 0;
                }
                a.writeToParcel(dest, i2);
            }
        }
        if (!this.mReapplyDisallowed) {
            i = 0;
        }
        dest.writeInt(i);
    }

    private static ApplicationInfo getApplicationInfo(String packageName, int userId) {
        if (packageName == null) {
            return null;
        }
        Application application = ActivityThread.currentApplication();
        if (application == null) {
            throw new IllegalStateException("Cannot create remote views out of an aplication.");
        }
        ApplicationInfo applicationInfo = application.getApplicationInfo();
        if (!(UserHandle.getUserId(applicationInfo.uid) == userId && (applicationInfo.packageName.equals(packageName) ^ 1) == 0)) {
            try {
                applicationInfo = application.getBaseContext().createPackageContextAsUser(packageName, 0, new UserHandle(userId)).getApplicationInfo();
            } catch (NameNotFoundException e) {
                throw new IllegalArgumentException("No such package " + packageName);
            }
        }
        return applicationInfo;
    }
}
