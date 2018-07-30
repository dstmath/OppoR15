package com.android.server.am;

import android.os.SystemClock;
import android.util.Slog;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oppo.util.OppoStatistics;

public class OppoBpmMonitorManager {
    static final long INIT_LIFE_TIME = 0;
    static final int INIT_PERCENT = 100;
    static final String TAG = "OppoProcessManager";
    static final int UPLOAD_COUNT_NUM = 300;
    static final String UPLOAD_EVENTID = "bpm_usage";
    static final String UPLOAD_KEY_PKGNAME = "proc";
    static final String UPLOAD_KEY_RESUME_PRE = "r_";
    static final String UPLOAD_KEY_SUSPEND_COUNT = "s_num";
    static final String UPLOAD_KEY_SUSPEND_PERCENT = "per_";
    static final String UPLOAD_KEY_SUSPEND_REAL_PRECENT = "realPer_";
    static final String UPLOAD_KEY_SUSPEND_TIME = "s_time";
    static final String UPLOAD_KEY_UNSUSPEND_PRE = "us_";
    static final String UPLOAD_LOGTAG = "20120";
    private static OppoBpmMonitorManager sOppoBpmMonitorManager = null;
    private List<OppoBpmAppInfo> mBpmAppInfoList = new ArrayList();
    private boolean mDebugDetail = OppoProcessManager.sDebugDetail;
    private long mMonitorStartTime = 0;
    private HashMap<String, Long> mResumeTimeMap = new HashMap();
    private HashMap<String, Long> mSuspendTimeMap = new HashMap();
    private List<String> mTmpBmpProcessNameList = new ArrayList();
    private List<OppoBpmAppInfo> mTmpBpmAppInfoList = new ArrayList();
    private HashMap<String, Long> mUnSuspendTimeMap = new HashMap();

    protected OppoBpmMonitorManager() {
    }

    public static final OppoBpmMonitorManager getInstance() {
        if (sOppoBpmMonitorManager == null) {
            sOppoBpmMonitorManager = new OppoBpmMonitorManager();
        }
        return sOppoBpmMonitorManager;
    }

    public void increaseSuspendTime(String processName) {
        long currentTime = getCurrentTime();
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().equals(processName)) {
                increaseTime(appInfo, processName, currentTime);
                setBeginLifeTime(appInfo, currentTime);
                this.mSuspendTimeMap.put(processName, Long.valueOf(currentTime));
                appInfo.setCurrentProcessState("suspend");
                return;
            }
        }
        OppoBpmAppInfo appInfo2 = OppoBpmAppInfo.builder(processName);
        setBeginLifeTime(appInfo2, currentTime);
        this.mSuspendTimeMap.put(processName, Long.valueOf(currentTime));
        appInfo2.setCurrentProcessState("suspend");
        this.mBpmAppInfoList.add(appInfo2);
    }

    protected void increaseUnSuspendTime(String processName) {
        long currentTime = getCurrentTime();
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().equals(processName)) {
                increaseTime(appInfo, processName, currentTime);
                setBeginLifeTime(appInfo, currentTime);
                this.mUnSuspendTimeMap.put(processName, Long.valueOf(currentTime));
                appInfo.setCurrentProcessState("unsuspend");
                return;
            }
        }
        OppoBpmAppInfo appInfo2 = OppoBpmAppInfo.builder(processName);
        setBeginLifeTime(appInfo2, currentTime);
        this.mUnSuspendTimeMap.put(processName, Long.valueOf(currentTime));
        appInfo2.setCurrentProcessState("unsuspend");
        this.mBpmAppInfoList.add(appInfo2);
    }

    protected void increaseResumeTime(String processName, boolean isDelay) {
        long currentTime = getCurrentTime();
        if (this.mSuspendTimeMap.containsKey(processName)) {
            long intervalTime = currentTime - ((Long) this.mSuspendTimeMap.get(processName)).longValue();
            if (isDelay) {
                intervalTime -= 10000;
            }
            if (intervalTime <= 0) {
                if (this.mDebugDetail) {
                    Slog.i("OppoProcessManager", "increaseSuspendTime w resume before suspend2");
                }
                return;
            }
            for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
                if (appInfo.getProcessName().equals(processName)) {
                    increaseTime(appInfo, processName, currentTime);
                    setBeginLifeTime(appInfo, currentTime);
                    this.mResumeTimeMap.put(processName, Long.valueOf(currentTime));
                    appInfo.setCurrentProcessState("resume");
                    return;
                }
            }
            OppoBpmAppInfo appInfo2 = OppoBpmAppInfo.builder(processName);
            setBeginLifeTime(appInfo2, currentTime);
            this.mResumeTimeMap.put(processName, Long.valueOf(currentTime));
            appInfo2.setCurrentProcessState("resume");
            this.mBpmAppInfoList.add(appInfo2);
            return;
        }
        if (this.mDebugDetail) {
            Slog.i("OppoProcessManager", "increaseSuspendTime w resume before suspend1");
        }
    }

    private void increaseTime(OppoBpmAppInfo appInfo, String processName, long currentTime) {
        if (appInfo.getCurrentProcessState().equals("unsuspend")) {
            if (this.mUnSuspendTimeMap.containsKey(processName)) {
                appInfo.increaseUnSuspendTime(currentTime - ((Long) this.mUnSuspendTimeMap.get(processName)).longValue());
            }
        } else if (appInfo.getCurrentProcessState().equals("resume")) {
            if (this.mResumeTimeMap.containsKey(processName)) {
                appInfo.increaseResumeTime(currentTime - ((Long) this.mResumeTimeMap.get(processName)).longValue());
            }
        } else if (appInfo.getCurrentProcessState().equals("suspend") && this.mSuspendTimeMap.containsKey(processName)) {
            appInfo.increaseSuspendTime(currentTime - ((Long) this.mSuspendTimeMap.get(processName)).longValue());
        }
    }

    protected void handleAppDied(String processName) {
        long currentTime = getCurrentTime();
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().equals(processName)) {
                appInfo.setEndLifeTime(currentTime);
                if (appInfo.getCurrentProcessState().equals("unsuspend")) {
                    if (this.mUnSuspendTimeMap.containsKey(processName)) {
                        appInfo.increaseUnSuspendTime(currentTime - ((Long) this.mUnSuspendTimeMap.get(processName)).longValue());
                    }
                } else if (appInfo.getCurrentProcessState().equals("suspend")) {
                    if (this.mSuspendTimeMap.containsKey(processName)) {
                        appInfo.increaseSuspendTime(currentTime - ((Long) this.mSuspendTimeMap.get(processName)).longValue());
                    }
                } else if (appInfo.getCurrentProcessState().equals("resume") && this.mResumeTimeMap.containsKey(processName)) {
                    appInfo.increaseResumeTime(currentTime - ((Long) this.mResumeTimeMap.get(processName)).longValue());
                }
                appInfo.setCurrentProcessState("killed");
                return;
            }
        }
    }

    public void increaseSuspendCount(String processName) {
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().equals(processName)) {
                appInfo.increaseSuspendCount();
                return;
            }
        }
        OppoBpmAppInfo appInfo2 = OppoBpmAppInfo.builder(processName);
        appInfo2.increaseSuspendCount();
        this.mBpmAppInfoList.add(appInfo2);
    }

    public void increaseUnSuspendInfo(String processName, Integer reason) {
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().equals(processName)) {
                appInfo.increaseUnSuspendInfo(reason);
                return;
            }
        }
        OppoBpmAppInfo appInfo2 = OppoBpmAppInfo.builder(processName);
        appInfo2.increaseUnSuspendInfo(reason);
        this.mBpmAppInfoList.add(appInfo2);
    }

    public void increaseResumeInfo(String processName, Integer reason) {
        if (this.mBpmAppInfoList.size() >= 300) {
            OppoProcessManager.getInstance().handleUploadInfo();
            return;
        }
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().equals(processName)) {
                appInfo.increaseResumeInfo(reason);
                return;
            }
        }
        OppoBpmAppInfo appInfo2 = OppoBpmAppInfo.builder(processName);
        appInfo2.increaseResumeInfo(reason);
        this.mBpmAppInfoList.add(appInfo2);
    }

    protected long getCurrentTime() {
        return SystemClock.elapsedRealtime();
    }

    private void setBeginLifeTime(OppoBpmAppInfo appInfo, long time) {
        if (appInfo.getCurrentProcessState().equals("init") || appInfo.getCurrentProcessState().equals("killed")) {
            appInfo.setBeginLifeTime(time);
        }
    }

    public void handleUploadInfo(ActivityManagerService mActivityManager, boolean isPowerConn) {
        stopTime();
        List<Map<String, String>> uploadList = new ArrayList();
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        String suspendPercent = "0";
        String realSuspendPercent = "0";
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            HashMap<String, String> appMap = appInfo.getBpmAppMap();
            long lifeTimeLong = appInfo.getLifeTimeLong();
            long realLifeTimeLong = appInfo.getLifeTimeLong() - appInfo.getUnSuspendTimeLong();
            if (lifeTimeLong > 0) {
                suspendPercent = numberFormat.format((double) ((((float) appInfo.getSuspendTimeLong()) / ((float) lifeTimeLong)) * 100.0f));
                if (realLifeTimeLong > 0) {
                    realSuspendPercent = numberFormat.format((double) ((((float) appInfo.getSuspendTimeLong()) / ((float) realLifeTimeLong)) * 100.0f));
                }
                appInfo.setSuspendPercent(suspendPercent);
                appInfo.setRealSuspendPercent(realSuspendPercent);
                appMap.put(UPLOAD_KEY_SUSPEND_PERCENT, suspendPercent);
                appMap.put(UPLOAD_KEY_SUSPEND_REAL_PRECENT, realSuspendPercent);
                uploadList.add(appMap);
                suspendPercent = "0";
                realSuspendPercent = "0";
            }
        }
        OppoStatistics.onCommon(mActivityManager.mContext, "20120", UPLOAD_EVENTID, uploadList, false);
        if (OppoProcessManager.getInstance().mDynamicDebug) {
            dumpInfo();
        }
        clear();
        initTime(mActivityManager, isPowerConn);
    }

    public void clear() {
        this.mBpmAppInfoList.clear();
        this.mSuspendTimeMap.clear();
        this.mUnSuspendTimeMap.clear();
        this.mResumeTimeMap.clear();
    }

    protected void resetUploadInfo() {
        clear();
    }

    private void stopTime() {
        long currentTime = getCurrentTime();
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (!(appInfo.getCurrentProcessState().equals("killed") || appInfo.getCurrentProcessState().equals("init"))) {
                appInfo.setEndLifeTime(currentTime);
                if (appInfo.getCurrentProcessState().equals("suspend")) {
                    if (this.mSuspendTimeMap.containsKey(appInfo.getProcessName())) {
                        appInfo.increaseSuspendTime(currentTime - ((Long) this.mSuspendTimeMap.get(appInfo.getProcessName())).longValue());
                    }
                } else if (appInfo.getCurrentProcessState().equals("unsuspend")) {
                    if (this.mUnSuspendTimeMap.containsKey(appInfo.getProcessName())) {
                        appInfo.increaseUnSuspendTime(currentTime - ((Long) this.mUnSuspendTimeMap.get(appInfo.getProcessName())).longValue());
                    }
                } else if (appInfo.getCurrentProcessState().equals("resume") && this.mResumeTimeMap.containsKey(appInfo.getProcessName())) {
                    appInfo.increaseResumeTime(currentTime - ((Long) this.mResumeTimeMap.get(appInfo.getProcessName())).longValue());
                }
                this.mTmpBmpProcessNameList.add(appInfo.getProcessName());
                this.mTmpBpmAppInfoList.add(appInfo);
            }
        }
    }

    private void initTime(ActivityManagerService mActivityManager, boolean isPowerConn) {
        if (isPowerConn) {
            this.mTmpBmpProcessNameList.clear();
            this.mTmpBpmAppInfoList.clear();
            return;
        }
        List<String> processNameList = new ArrayList();
        for (int i = mActivityManager.mLruProcesses.size() - 1; i >= 0; i--) {
            ProcessRecord rec = (ProcessRecord) mActivityManager.mLruProcesses.get(i);
            if (!(rec == null || rec.thread == null || !this.mTmpBmpProcessNameList.contains(rec.processName))) {
                processNameList.add(rec.processName);
            }
        }
        long currentTime = getCurrentTime();
        for (OppoBpmAppInfo appInfo : this.mTmpBpmAppInfoList) {
            if (processNameList.contains(appInfo.getProcessName())) {
                appInfo.resetTime();
                appInfo.setBeginLifeTime(currentTime);
                if (appInfo.getCurrentProcessState().equals("suspend")) {
                    this.mSuspendTimeMap.put(appInfo.getProcessName(), Long.valueOf(currentTime));
                } else if (appInfo.getCurrentProcessState().equals("unsuspend")) {
                    this.mUnSuspendTimeMap.put(appInfo.getProcessName(), Long.valueOf(currentTime));
                } else if (appInfo.getCurrentProcessState().equals("resume")) {
                    this.mResumeTimeMap.put(appInfo.getProcessName(), Long.valueOf(currentTime));
                }
                this.mBpmAppInfoList.add(appInfo);
            } else {
                return;
            }
        }
        processNameList.clear();
        this.mTmpBmpProcessNameList.clear();
        this.mTmpBpmAppInfoList.clear();
    }

    protected void removeRecordInfo(String packageName) {
        List<OppoBpmAppInfo> removeList = new ArrayList();
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            if (appInfo.getProcessName().contains(packageName)) {
                removeList.add(appInfo);
                this.mUnSuspendTimeMap.remove(appInfo.getProcessName());
                this.mResumeTimeMap.remove(appInfo.getProcessName());
                this.mSuspendTimeMap.remove(appInfo.getProcessName());
            }
        }
        this.mBpmAppInfoList.removeAll(removeList);
    }

    public void dumpInfo() {
        Slog.i("OppoProcessManager", "--------------------------OppoBpmMonitorManager start---------------------------");
        Slog.i("OppoProcessManager", "dumpInfo mBpmAppInfoList 's size is " + this.mBpmAppInfoList.size());
        for (OppoBpmAppInfo appInfo : this.mBpmAppInfoList) {
            Slog.i("OppoProcessManager", appInfo.formatToString());
        }
        Slog.i("OppoProcessManager", "--------------------------OppoBpmMonitorManager end---------------------------");
    }
}
