package com.android.server.camera;

import android.app.job.JobInfo.Builder;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
import java.util.concurrent.TimeUnit;

public class CameraStatsJobService extends JobService {
    private static final int CAMERA_REPORTING_JOB_ID = 13254266;
    private static final String TAG = "CameraStatsJobService";
    private static ComponentName sCameraStatsJobServiceName = new ComponentName("android", CameraStatsJobService.class.getName());

    public boolean onStartJob(JobParameters params) {
        CameraServiceProxy serviceProxy = (CameraServiceProxy) LocalServices.getService(CameraServiceProxy.class);
        if (serviceProxy == null) {
            Slog.w(TAG, "Can't collect camera usage stats - no camera service proxy found");
            return false;
        }
        serviceProxy.dumpUsageEvents();
        return false;
    }

    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void schedule(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        if (js == null) {
            Slog.e(TAG, "Can't collect camera usage stats - no Job Scheduler");
        } else {
            js.schedule(new Builder(CAMERA_REPORTING_JOB_ID, sCameraStatsJobServiceName).setMinimumLatency(TimeUnit.DAYS.toMillis(1)).setRequiresDeviceIdle(true).build());
        }
    }
}
