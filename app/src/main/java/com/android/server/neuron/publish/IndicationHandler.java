package com.android.server.neuron.publish;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.am.OppoAppStartupManager;
import com.android.server.neuron.publish.Channel.RequestSender;
import com.android.server.neuron.publish.Response.NativeIndication;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class IndicationHandler {
    private static final String TAG = "NeuronSystem";
    private Context mContext;
    private List<String> mIconApp = null;
    private RequestSender mSender;

    public IndicationHandler(Context context, RequestSender sender) {
        this.mContext = context;
        this.mSender = sender;
    }

    public void handle(NativeIndication indication) {
        switch (indication.command) {
            case 101:
                handleGetBackgroundApp();
                return;
            case 102:
                return;
            case 103:
                handleGetInstalledApp();
                return;
            case 104:
                handleGetRecentApp();
                return;
            case ProtocolConstants.UNSOL_SET_RSSI_UPDATE_FREQ /*501*/:
                handleRssiUpdateFreq(indication.arg1);
                return;
            case ProtocolConstants.UNSOL_SET_GPS_UPDATE_FREQ /*502*/:
                handleGpsUpdateFreq(indication.arg1);
                return;
            case ProtocolConstants.UNSOL_SET_SENSOR_UPDATE_FREQ /*503*/:
                handleSensorUpdateFreq(indication.arg1);
                return;
            case ProtocolConstants.UNSOL_SET_ELSA_MODE /*504*/:
                handleElsaModeUpdate(indication.arg1);
                break;
        }
        Slog.e("NeuronSystem", "IndicationHandler handle unknown command: " + indication.command);
    }

    private void handleGetBackgroundApp() {
        getAllInstalledWithIconApp();
        ActivityManager manager = (ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
        if (manager != null) {
            List<RunningAppProcessInfo> lists = manager.getRunningAppProcesses();
            if (lists != null && lists.size() != 0) {
                removeNonIconApp(lists);
                removeForegroundApp(lists);
                Request req = Request.obtain();
                Parcel parcel = req.prepare();
                parcel.writeInt(2);
                parcel.writeInt(lists.size());
                for (RunningAppProcessInfo info : lists) {
                    parcel.writeString(info.pkgList[0]);
                    parcel.writeInt(0);
                    parcel.writeString(ProtocolConstants.DEFAULT_VERSION);
                    parcel.writeInt(info.uid);
                    parcel.writeInt(info.pid);
                }
                req.commit();
                this.mSender.sendRequest(req);
            }
        }
    }

    private void handleGetInstalledApp() {
        getAllInstalledWithIconApp();
        Request req = Request.obtain();
        Parcel parcel = req.prepare();
        parcel.writeInt(13);
        parcel.writeInt(2);
        parcel.writeInt(this.mIconApp.size());
        for (String appName : this.mIconApp) {
            parcel.writeString(appName);
        }
        req.commit();
        this.mSender.sendRequest(req);
    }

    private void handleGetRecentApp() {
        ActivityManager manager = (ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
        if (manager != null) {
            List<RunningAppProcessInfo> lists = manager.getRunningAppProcesses();
            if (lists != null && lists.size() != 0) {
                removeNonIconApp(lists);
                try {
                    List<RecentTaskInfo> tasks = ActivityManagerNative.getDefault().getRecentTasks(32, 0, 0).getList();
                    ArrayList<RunningAppProcessInfo> runningRecentApp = new ArrayList();
                    for (RecentTaskInfo task : tasks) {
                        String pkg = getTaskPackageName(task);
                        if (pkg != null) {
                            for (RunningAppProcessInfo info : lists) {
                                if (info.pkgList[0].equals(pkg)) {
                                    runningRecentApp.add(info);
                                }
                            }
                        }
                    }
                    if (runningRecentApp.size() > 0) {
                        runningRecentApp.remove(0);
                    }
                    if (runningRecentApp.size() > 0) {
                        Request req = Request.obtain();
                        Parcel parcel = req.prepare();
                        parcel.writeInt(14);
                        parcel.writeInt(runningRecentApp.size());
                        for (RunningAppProcessInfo info2 : runningRecentApp) {
                            parcel.writeString(info2.pkgList[0]);
                            parcel.writeInt(0);
                            parcel.writeString(ProtocolConstants.DEFAULT_VERSION);
                            parcel.writeInt(info2.uid);
                            parcel.writeInt(info2.pid);
                        }
                        req.commit();
                        this.mSender.sendRequest(req);
                    }
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void handleRssiUpdateFreq(int freq) {
        NeuronContext.getNeoConfig().setRssiUpdatePeriod(freq);
    }

    private void handleGpsUpdateFreq(int freq) {
        NeuronContext.getNeoConfig().setGpsUpdatePeriod(freq);
    }

    private void handleSensorUpdateFreq(int freq) {
        NeuronContext.getNeoConfig().setSensorUpdatePeriod(freq);
    }

    private void handleElsaModeUpdate(int mode) {
        Slog.d("NeuronSystem", "handleElsaModeUpdate, mode:" + mode);
    }

    private void getAllInstalledWithIconApp() {
        if (this.mIconApp == null) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            List<ResolveInfo> resolvies = this.mContext.getPackageManager().queryIntentActivities(intent, 0);
            this.mIconApp = new ArrayList(resolvies.size());
            for (ResolveInfo resolve : resolvies) {
                this.mIconApp.add(resolve.activityInfo.packageName);
            }
        }
    }

    private void removeNonIconApp(List<RunningAppProcessInfo> infos) {
        Iterator<RunningAppProcessInfo> it = infos.iterator();
        while (it.hasNext()) {
            boolean hasIcon = false;
            for (String pkg : ((RunningAppProcessInfo) it.next()).pkgList) {
                if (this.mIconApp.contains(pkg)) {
                    hasIcon = true;
                    break;
                }
            }
            if (!hasIcon) {
                it.remove();
            }
        }
    }

    private void removeForegroundApp(List<RunningAppProcessInfo> infos) {
        String foregroundApp = NeuronContext.getSystemStatus().getForegroundApp();
        Iterator<RunningAppProcessInfo> it = infos.iterator();
        while (it.hasNext()) {
            for (String pkg : ((RunningAppProcessInfo) it.next()).pkgList) {
                if (foregroundApp.equals(pkg)) {
                    it.remove();
                    return;
                }
            }
        }
    }

    private String getTaskPackageName(RecentTaskInfo task) {
        if (task.id > 0) {
            if (task.origActivity != null) {
                return task.origActivity.getPackageName();
            }
            if (task.realActivity != null) {
                return task.realActivity.getPackageName();
            }
            if (task.baseActivity != null) {
                return task.baseActivity.getPackageName();
            }
            if (task.topActivity != null) {
                return task.topActivity.getPackageName();
            }
        }
        return null;
    }
}
