package android.appwidget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import com.android.internal.appwidget.IAppWidgetHost.Stub;
import com.android.internal.appwidget.IAppWidgetService;
import java.lang.ref.WeakReference;
import java.util.List;

public class AppWidgetHost {
    static final int HANDLE_PROVIDERS_CHANGED = 3;
    static final int HANDLE_PROVIDER_CHANGED = 2;
    static final int HANDLE_UPDATE = 1;
    static final int HANDLE_VIEW_DATA_CHANGED = 4;
    static IAppWidgetService sService;
    static boolean sServiceInitialized = false;
    static final Object sServiceLock = new Object();
    private final Callbacks mCallbacks;
    private String mContextOpPackageName;
    private DisplayMetrics mDisplayMetrics;
    private final Handler mHandler;
    private final int mHostId;
    private OnClickHandler mOnClickHandler;
    private final SparseArray<AppWidgetHostView> mViews;

    static class Callbacks extends Stub {
        private final WeakReference<Handler> mWeakHandler;

        public Callbacks(Handler handler) {
            this.mWeakHandler = new WeakReference(handler);
        }

        public void updateAppWidget(int appWidgetId, RemoteViews views) {
            if (isLocalBinder() && views != null) {
                views = views.clone();
            }
            Handler handler = (Handler) this.mWeakHandler.get();
            if (handler != null) {
                handler.obtainMessage(1, appWidgetId, 0, views).sendToTarget();
            }
        }

        public void providerChanged(int appWidgetId, AppWidgetProviderInfo info) {
            if (isLocalBinder() && info != null) {
                info = info.clone();
            }
            Handler handler = (Handler) this.mWeakHandler.get();
            if (handler != null) {
                handler.obtainMessage(2, appWidgetId, 0, info).sendToTarget();
            }
        }

        public void providersChanged() {
            Handler handler = (Handler) this.mWeakHandler.get();
            if (handler != null) {
                handler.obtainMessage(3).sendToTarget();
            }
        }

        public void viewDataChanged(int appWidgetId, int viewId) {
            Handler handler = (Handler) this.mWeakHandler.get();
            if (handler != null) {
                handler.obtainMessage(4, appWidgetId, viewId).sendToTarget();
            }
        }

        private static boolean isLocalBinder() {
            return Process.myPid() == Binder.getCallingPid();
        }
    }

    class UpdateHandler extends Handler {
        public UpdateHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AppWidgetHost.this.updateAppWidgetView(msg.arg1, (RemoteViews) msg.obj);
                    return;
                case 2:
                    AppWidgetHost.this.onProviderChanged(msg.arg1, (AppWidgetProviderInfo) msg.obj);
                    return;
                case 3:
                    AppWidgetHost.this.onProvidersChanged();
                    return;
                case 4:
                    AppWidgetHost.this.viewDataChanged(msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    public AppWidgetHost(Context context, int hostId) {
        this(context, hostId, null, context.getMainLooper());
    }

    public AppWidgetHost(Context context, int hostId, OnClickHandler handler, Looper looper) {
        this.mViews = new SparseArray();
        this.mContextOpPackageName = context.getOpPackageName();
        this.mHostId = hostId;
        this.mOnClickHandler = handler;
        this.mHandler = new UpdateHandler(looper);
        this.mCallbacks = new Callbacks(this.mHandler);
        this.mDisplayMetrics = context.getResources().getDisplayMetrics();
        bindService(context);
    }

    private static void bindService(Context context) {
        synchronized (sServiceLock) {
            if (sServiceInitialized) {
                return;
            }
            sServiceInitialized = true;
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)) {
                sService = IAppWidgetService.Stub.asInterface(ServiceManager.getService(Context.APPWIDGET_SERVICE));
                return;
            }
        }
    }

    public void startListening() {
        if (sService != null) {
            int N;
            int[] idsToUpdate;
            int i;
            synchronized (this.mViews) {
                N = this.mViews.size();
                idsToUpdate = new int[N];
                for (i = 0; i < N; i++) {
                    idsToUpdate[i] = this.mViews.keyAt(i);
                }
            }
            try {
                List<PendingHostUpdate> updates = sService.startListening(this.mCallbacks, this.mContextOpPackageName, this.mHostId, idsToUpdate).getList();
                N = updates.size();
                for (i = 0; i < N; i++) {
                    PendingHostUpdate update = (PendingHostUpdate) updates.get(i);
                    switch (update.type) {
                        case 0:
                            updateAppWidgetView(update.appWidgetId, update.views);
                            break;
                        case 1:
                            onProviderChanged(update.appWidgetId, update.widgetInfo);
                            break;
                        case 2:
                            viewDataChanged(update.appWidgetId, update.viewId);
                            break;
                        default:
                            break;
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("system server dead?", e);
            }
        }
    }

    public void stopListening() {
        if (sService != null) {
            try {
                sService.stopListening(this.mContextOpPackageName, this.mHostId);
            } catch (RemoteException e) {
                throw new RuntimeException("system server dead?", e);
            }
        }
    }

    public int allocateAppWidgetId() {
        if (sService == null) {
            return -1;
        }
        try {
            return sService.allocateAppWidgetId(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public final void startAppWidgetConfigureActivityForResult(Activity activity, int appWidgetId, int intentFlags, int requestCode, Bundle options) {
        if (sService != null) {
            try {
                IntentSender intentSender = sService.createAppWidgetConfigIntentSender(this.mContextOpPackageName, appWidgetId, intentFlags);
                if (intentSender != null) {
                    activity.startIntentSenderForResult(intentSender, requestCode, null, 0, 0, 0, options);
                    return;
                }
                throw new ActivityNotFoundException();
            } catch (SendIntentException e) {
                throw new ActivityNotFoundException();
            } catch (RemoteException e2) {
                throw new RuntimeException("system server dead?", e2);
            }
        }
    }

    public int[] getAppWidgetIds() {
        if (sService == null) {
            return new int[0];
        }
        try {
            return sService.getAppWidgetIdsForHost(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public void deleteAppWidgetId(int appWidgetId) {
        if (sService != null) {
            synchronized (this.mViews) {
                this.mViews.remove(appWidgetId);
                try {
                    sService.deleteAppWidgetId(this.mContextOpPackageName, appWidgetId);
                } catch (RemoteException e) {
                    throw new RuntimeException("system server dead?", e);
                }
            }
        }
    }

    public void deleteHost() {
        if (sService != null) {
            try {
                sService.deleteHost(this.mContextOpPackageName, this.mHostId);
            } catch (RemoteException e) {
                throw new RuntimeException("system server dead?", e);
            }
        }
    }

    public static void deleteAllHosts() {
        if (sService != null) {
            try {
                sService.deleteAllHosts();
            } catch (RemoteException e) {
                throw new RuntimeException("system server dead?", e);
            }
        }
    }

    public final AppWidgetHostView createView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        if (sService == null) {
            return null;
        }
        AppWidgetHostView view = onCreateView(context, appWidgetId, appWidget);
        view.setOnClickHandler(this.mOnClickHandler);
        view.setAppWidget(appWidgetId, appWidget);
        synchronized (this.mViews) {
            this.mViews.put(appWidgetId, view);
        }
        try {
            view.updateAppWidget(sService.getAppWidgetViews(this.mContextOpPackageName, appWidgetId));
            return view;
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        return new AppWidgetHostView(context, this.mOnClickHandler);
    }

    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
        AppWidgetHostView v;
        appWidget.updateDimensions(this.mDisplayMetrics);
        synchronized (this.mViews) {
            v = (AppWidgetHostView) this.mViews.get(appWidgetId);
        }
        if (v != null) {
            v.resetAppWidget(appWidget);
        }
    }

    protected void onProvidersChanged() {
    }

    void updateAppWidgetView(int appWidgetId, RemoteViews views) {
        AppWidgetHostView v;
        synchronized (this.mViews) {
            v = (AppWidgetHostView) this.mViews.get(appWidgetId);
        }
        if (v != null) {
            v.updateAppWidget(views);
        }
    }

    void viewDataChanged(int appWidgetId, int viewId) {
        AppWidgetHostView v;
        synchronized (this.mViews) {
            v = (AppWidgetHostView) this.mViews.get(appWidgetId);
        }
        if (v != null) {
            v.viewDataChanged(viewId);
        }
    }

    protected void clearViews() {
        synchronized (this.mViews) {
            this.mViews.clear();
        }
    }
}
