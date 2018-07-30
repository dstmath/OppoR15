package com.android.server.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.OppoActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.android.server.LocationManagerService;
import com.android.server.am.OppoMultiAppManager;
import com.android.server.am.OppoMultiAppManagerUtil;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import oppo.util.OppoStatistics;
import org.xmlpull.v1.XmlPullParser;

public class OppoNotificationManager {
    private static final String DISTURB_FOR_GAME_SPACE_MODE = "disturb_for_game_space_mode";
    private static final Uri DISTURB_FOR_GAME_SPACE_MODE_URI = Global.getUriFor(DISTURB_FOR_GAME_SPACE_MODE);
    private static final String EVENTID_FOR_BLOCKED_BY_BLACKLIST = "notification_blacklist";
    private static final String EVENTID_FOR_BLOCKED_BY_DYNAMIC = "notification_dynamic";
    private static final String EVENTID_FOR_BLOCKED_BY_NORMAL = "notification_normal";
    private static final String EVENTID_FOR_BLOCKED_BY_UNNORMAL = "notification_unnormal";
    private static final String EVENT_PACKAGE = "package_name";
    private static final String[] GAME_SPACE_WHITELIST = new String[]{"com.android.calendar", "com.android.incallui", "com.coloros.alarmclock", "com.google.android.calendar"};
    private static final String[] KEEP_ALIVE_APP_WHITELIST = new String[]{"com.autonavi.minimap", "com.baidu.BaiduMap", "com.coloros.notificationdemo"};
    private static final int KEEP_ALIVE_NOTIFICATION_ID = 10000;
    private static final String[] KEEP_NOTIFICATION_APP_LIST = new String[]{"com.nearme.gamecenter", "com.nearme.instant.platform"};
    private static final String LOGTAG = "NotificationService";
    private static final int MAX_CHANNELS_PER_APP = 1000;
    private static final String MAX_CHANNELS_TITLE_NAME = "max-channels";
    private static final int MAX_NOTIFICATION_IS_BLOCKED_BY_BLACKLIST = 20;
    private static final int MAX_NOTIFICATION_IS_BLOCKED_BY_DYNAMIC = 0;
    private static final int MAX_NOTIFICATION_IS_BLOCKED_BY_NORMAL = 20;
    private static final int MAX_NOTIFICATION_IS_BLOCKED_BY_UNNORMAL = 20;
    private static final int MIN_CHANNELS_PER_APP = 100;
    private static final String NAVIGATION_SUB_TITLE_NAME = "package";
    private static final String NAVIGATION_TITLE_NAME = "navigation";
    private static final int NOTIFICATION_FILTER_SIZE = 3;
    private static final String NOTIFICATION_ITEM_URI = "deep_protect_notification";
    private static final int NOTIFICATION_PACKAGE_NAME = 0;
    private static final int NOTIFICATION_SUMMARY_NAME = 2;
    private static final int NOTIFICATION_TITLE_NAME = 1;
    private static final String NOTIFICATON_TITLE_NAME = "notification";
    private static final String NO_DISTURB_FOR_SCREEN_ASSISTANT = "no_disturb_for_screen_assistant";
    private static final Uri NO_DISTURB_FOR_SCREEN_ASSISTANT_URI = Secure.getUriFor(NO_DISTURB_FOR_SCREEN_ASSISTANT);
    private static final String OPPO_NOTIFICATION_BLACKLIST_DIRECTORY = "/data/oppo/coloros/notification";
    private static final String OPPO_NOTIFICATION_BLACKLIST_FILE_PATH = "/data/oppo/coloros/notification/sys_nms_intercept_blacklist.xml";
    private static final String OPPO_VERSION_EXP = "oppo.version.exp";
    private static final String PRIVACY_PROTECT_URI = "content://com.color.provider.SafeProvider/pp_privacy_protect";
    private static final String[] PROJECTION = new String[]{"pkg_name"};
    private static final String[] SELECTION_ARGS = new String[]{LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON, "0"};
    private static final String SELECTION_CLAUSE = "protect_type = ? AND show_notification = ?";
    private static final String SELECTION_CLAUSE_MULTI = "multi_protect_type = ? AND multi_show_notification = ?";
    private static final int STEP = 1;
    private static final String TAG = "OppoNotificationManager";
    private static final long TIME_UPLOAD_THRESHOLD = 10800000;
    private static final Uri URI_DEEP_PROTECT_NOTIFICATION_CHANGED = Uri.withAppendedPath(Uri.parse(PRIVACY_PROTECT_URI), NOTIFICATION_ITEM_URI);
    private static final Uri VIBRATE_WHEN_RINGING_URI = System.getUriFor("vibrate_when_ringing");
    private static final OppoNotificationManager sOppoNotificationManager = new OppoNotificationManager();
    private boolean DEBUG = false;
    private boolean DEBUG_INTERNAL = false;
    private ArrayList<String> mBlacklistNotificationStatisticList = new ArrayList();
    private Context mContext;
    private List<String[]> mDynamicFilterNotificationList = new ArrayList();
    private ArrayList<String> mDynamicNotificationStatisticList = new ArrayList();
    private Map<String, String> mEventMap = new HashMap();
    private boolean mGameSpaceMode;
    private HandlerThread mHandlerThread;
    private List<String> mHidePkgList;
    private final Object mHidePkgListLock = new Object();
    private List<String> mHidePkgListMulti;
    private boolean mIsCtaVersion = false;
    private boolean mIsNoDisturbForScreenAssistant;
    private boolean mIsReleaseVersion = false;
    private boolean mIsShutDown = false;
    private List<String> mKeepAliveAppWhiteList = new ArrayList();
    private Map<String, Boolean> mKeepAliveByNotificationMap = new HashMap();
    private List<String> mKeepNotificationList = new ArrayList();
    private long mLastUploadStaticsDataTime = 0;
    private int mLimitMaxChannels = 0;
    private Object mLock = new Object();
    private Handler mMainHandler;
    private ArrayList<String> mNormalNotificationStatisticList = new ArrayList();
    private FileObserverPolicy mNotificationFileObserver = null;
    private ArrayList<String> mNotificationNoClear = new ArrayList();
    private OppoSettingsObserver mOppoSettingsObserver;
    private Handler mThreadHandler;
    private ArrayList<String> mUnNormalNotificationStatisticList = new ArrayList();
    private boolean mVibrateWhenRingingEnabled;

    private class FileObserverPolicy extends FileObserver {
        private String mFocusPath;

        public FileObserverPolicy(String path) {
            super(path, 8);
            this.mFocusPath = path;
            if (OppoNotificationManager.this.DEBUG) {
                Log.d(OppoNotificationManager.TAG, "FileObserverPolicy_path = " + path);
            }
        }

        public void onEvent(int event, String path) {
            if (OppoNotificationManager.this.DEBUG) {
                Log.d(OppoNotificationManager.TAG, "onEvent: event = " + event + ",focusPath = " + this.mFocusPath);
            }
            if (event == 8 && this.mFocusPath.equals(OppoNotificationManager.OPPO_NOTIFICATION_BLACKLIST_FILE_PATH)) {
                if (OppoNotificationManager.this.DEBUG) {
                    Log.d(OppoNotificationManager.TAG, "onEvent: focusPath = OPPO_NOTIFICATION_BLACKLIST_FILE_PATH");
                }
                OppoNotificationManager.this.parseConfig();
            }
        }
    }

    private class OppoSettingsObserver extends ContentObserver {
        OppoSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = OppoNotificationManager.this.mContext.getContentResolver();
            try {
                resolver.registerContentObserver(OppoNotificationManager.VIBRATE_WHEN_RINGING_URI, false, this, -1);
                resolver.registerContentObserver(OppoNotificationManager.DISTURB_FOR_GAME_SPACE_MODE_URI, false, this, -1);
                resolver.registerContentObserver(OppoNotificationManager.NO_DISTURB_FOR_SCREEN_ASSISTANT_URI, false, this, -1);
                resolver.registerContentObserver(OppoNotificationManager.URI_DEEP_PROTECT_NOTIFICATION_CHANGED, false, this, -1);
                update(null);
            } catch (Exception e) {
                Log.e(OppoNotificationManager.TAG, "Exception trying to registerContentObserver on setting", e);
            }
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            Log.d(OppoNotificationManager.TAG, "mHidePkgListObserver onChange " + uri);
            ContentResolver resolver = OppoNotificationManager.this.mContext.getContentResolver();
            if (uri == null || OppoNotificationManager.VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                boolean vibreateEnabled = System.getInt(resolver, "vibrate_when_ringing", 0) != 0;
                if (OppoNotificationManager.this.mVibrateWhenRingingEnabled != vibreateEnabled) {
                    OppoNotificationManager.this.mVibrateWhenRingingEnabled = vibreateEnabled;
                }
            }
            if (uri == null || OppoNotificationManager.DISTURB_FOR_GAME_SPACE_MODE_URI.equals(uri)) {
                boolean gameSpaceMode = Global.getInt(resolver, OppoNotificationManager.DISTURB_FOR_GAME_SPACE_MODE, 1) != 1;
                if (OppoNotificationManager.this.mGameSpaceMode != gameSpaceMode) {
                    OppoNotificationManager.this.mGameSpaceMode = gameSpaceMode;
                }
            }
            if (uri == null || OppoNotificationManager.NO_DISTURB_FOR_SCREEN_ASSISTANT_URI.equals(uri)) {
                boolean isNoDisturb = Secure.getInt(resolver, OppoNotificationManager.NO_DISTURB_FOR_SCREEN_ASSISTANT, 0) == 1;
                if (OppoNotificationManager.this.mIsNoDisturbForScreenAssistant != isNoDisturb) {
                    OppoNotificationManager.this.mIsNoDisturbForScreenAssistant = isNoDisturb;
                }
            }
            if (uri == null || OppoNotificationManager.URI_DEEP_PROTECT_NOTIFICATION_CHANGED.equals(uri)) {
                OppoNotificationManager.this.mThreadHandler.post(new Runnable() {
                    public void run() {
                        OppoNotificationManager.this.initHidePkgList();
                    }
                });
            }
        }
    }

    public static OppoNotificationManager getInstance() {
        return sOppoNotificationManager;
    }

    private OppoNotificationManager() {
    }

    public void init(Context context, Handler handler) {
        this.mContext = context;
        this.mMainHandler = handler;
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mThreadHandler = new Handler(this.mHandlerThread.getLooper());
        initProperty();
        initConfig();
    }

    private void initProperty() {
        this.DEBUG = SystemProperties.getBoolean("persist.sys.assert.panic", false);
        this.mIsCtaVersion = this.mContext.getPackageManager().hasSystemFeature("oppo.cta.support");
        this.mIsReleaseVersion = SystemProperties.getBoolean("ro.build.release_type", false);
        this.mKeepNotificationList.clear();
        this.mKeepNotificationList.addAll(Arrays.asList(KEEP_NOTIFICATION_APP_LIST));
        this.mOppoSettingsObserver = new OppoSettingsObserver(this.mMainHandler);
    }

    private void initConfig() {
        File notificationBlacklistDirectory = new File(OPPO_NOTIFICATION_BLACKLIST_DIRECTORY);
        File notificationBlacklistFilePath = new File(OPPO_NOTIFICATION_BLACKLIST_FILE_PATH);
        try {
            if (!notificationBlacklistDirectory.exists()) {
                notificationBlacklistDirectory.mkdirs();
            }
            if (!notificationBlacklistFilePath.exists()) {
                notificationBlacklistFilePath.createNewFile();
            }
        } catch (IOException e) {
            Log.e(TAG, "init notificationBlacklistFilePath Dir failed!!!");
        }
        this.mNotificationFileObserver = new FileObserverPolicy(OPPO_NOTIFICATION_BLACKLIST_FILE_PATH);
        this.mNotificationFileObserver.startWatching();
        parseConfig();
    }

    private void parseConfig() {
        Throwable th;
        Exception e;
        synchronized (this.mLock) {
            this.mDynamicFilterNotificationList.clear();
            this.mKeepAliveAppWhiteList.clear();
            this.mKeepAliveAppWhiteList.addAll(Arrays.asList(KEEP_ALIVE_APP_WHITELIST));
        }
        File xmlFile = new File(OPPO_NOTIFICATION_BLACKLIST_FILE_PATH);
        if (xmlFile.exists()) {
            FileReader fileReader = null;
            try {
                XmlPullParser parser = Xml.newPullParser();
                Reader fileReader2 = new FileReader(xmlFile);
                try {
                    parser.setInput(fileReader2);
                    List<String> keepAliveList = new ArrayList();
                    boolean hasKeepAliveList = false;
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        switch (eventType) {
                            case 2:
                                if (!NOTIFICATON_TITLE_NAME.equals(parser.getName())) {
                                    if (!NAVIGATION_TITLE_NAME.equals(parser.getName())) {
                                        if (!NAVIGATION_SUB_TITLE_NAME.equals(parser.getName())) {
                                            if (!MAX_CHANNELS_TITLE_NAME.equals(parser.getName())) {
                                                break;
                                            }
                                            setLimitMaxChannels(parser.nextText());
                                            break;
                                        }
                                        String value = parser.nextText();
                                        if (!TextUtils.isEmpty(value)) {
                                            keepAliveList.add(value);
                                            break;
                                        }
                                        break;
                                    }
                                    hasKeepAliveList = true;
                                    break;
                                }
                                updateDynamicInterceptList(parser.getAttributeValue(null, "value"));
                                break;
                            default:
                                break;
                        }
                    }
                    synchronized (this.mLock) {
                        if (hasKeepAliveList) {
                            this.mKeepAliveAppWhiteList.clear();
                            this.mKeepAliveAppWhiteList.addAll(keepAliveList);
                        }
                        for (Entry entry : this.mKeepAliveByNotificationMap.entrySet()) {
                            String key = (String) entry.getKey();
                            if (!this.mKeepAliveAppWhiteList.contains(key)) {
                                this.mKeepAliveByNotificationMap.put(key, Boolean.valueOf(false));
                            }
                        }
                    }
                    if (fileReader2 != null) {
                        try {
                            fileReader2.close();
                        } catch (IOException e2) {
                            Log.w(TAG, "Got execption close permReader.", e2);
                        }
                    }
                    Reader xmlReader = fileReader2;
                } catch (FileNotFoundException e3) {
                    fileReader = fileReader2;
                    try {
                        Log.w(TAG, "Couldn't find or open alarm_filter_packages file " + xmlFile);
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (IOException e22) {
                                Log.w(TAG, "Got execption close permReader.", e22);
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (IOException e222) {
                                Log.w(TAG, "Got execption close permReader.", e222);
                            }
                        }
                        throw th;
                    }
                } catch (Exception e4) {
                    e = e4;
                    fileReader = fileReader2;
                    Log.w(TAG, "Got execption parsing permissions.", e);
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e2222) {
                            Log.w(TAG, "Got execption close permReader.", e2222);
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileReader = fileReader2;
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e22222) {
                            Log.w(TAG, "Got execption close permReader.", e22222);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e5) {
                Log.w(TAG, "Couldn't find or open alarm_filter_packages file " + xmlFile);
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e222222) {
                        Log.w(TAG, "Got execption close permReader.", e222222);
                    }
                }
            } catch (Exception e6) {
                e = e6;
                Log.w(TAG, "Got execption parsing permissions.", e);
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e2222222) {
                        Log.w(TAG, "Got execption close permReader.", e2222222);
                    }
                }
            }
        }
    }

    private void updateDynamicInterceptList(String tagName) {
        if (this.DEBUG) {
            Log.i(TAG, "updateNotifcationTitleName_tagName = " + tagName);
        }
        if (!TextUtils.isEmpty(tagName)) {
            String[] stringArray = tagName.split("/");
            if (stringArray != null && stringArray.length > 0) {
                synchronized (this.mLock) {
                    if (this.DEBUG) {
                        Log.d(TAG, "stringArray = " + Arrays.asList(stringArray));
                    }
                    this.mDynamicFilterNotificationList.add(stringArray);
                }
            }
        }
    }

    public void onPhaseThrirdPartyAppsCanStart() {
        this.mOppoSettingsObserver.observe();
    }

    public boolean getDebug() {
        return this.DEBUG;
    }

    public boolean isShutdown() {
        return this.mIsShutDown;
    }

    public void setShutdown(boolean shutdown) {
        this.mIsShutDown = shutdown;
    }

    public boolean shouldShowNotificationToast() {
        if (this.mIsCtaVersion || (this.mIsReleaseVersion ^ 1) == 0) {
            return false;
        }
        return true;
    }

    public boolean isMultiAppUserIdMatch(NotificationRecord r, int userId) {
        if (userId != OppoMultiAppManager.USER_ID || r == null || r.sbn == null || userId != r.sbn.getUserId()) {
            return false;
        }
        return true;
    }

    public boolean isAppOrMutilAppUserId() {
        return false;
    }

    public void updateNoClearNotification(Notification notification, String pkg) {
        synchronized (this.mNotificationNoClear) {
            if (!((notification.flags & 98) == 0 || (this.mNotificationNoClear.contains(pkg) ^ 1) == 0)) {
                this.mNotificationNoClear.add(pkg);
                if (this.DEBUG) {
                    Log.d(TAG, "enqueueNotificationInternal: add no clear notification : " + pkg);
                }
            }
        }
    }

    public boolean shouldInterceptSound(RankingHelper mRankingHelper, ZenModeHelper mZenModeHelper, String pkg, int uid) {
        if (isHidePkg(pkg, UserHandle.getUserId(uid))) {
            if (this.DEBUG) {
                Log.e(TAG, "shouldInterceptSound-isHidePkg intercept");
            }
            return true;
        }
        uid = getMutilAppUid(pkg, uid);
        int importance = mRankingHelper.getImportance(pkg, uid);
        NotificationChannel notificationChannel = mRankingHelper.getNotificationChannel(pkg, uid, null, false);
        if (this.DEBUG) {
            Log.e(TAG, "shouldInterceptSound-importance:" + importance + ", notificationChannel : " + notificationChannel);
        }
        if ((importance < 3 && importance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) || (notificationChannel != null && notificationChannel.getSound() == null)) {
            return true;
        }
        boolean bIsInImportantInterruptions = mZenModeHelper.isInImportantInterruptions();
        boolean bAllowReminders = mZenModeHelper.getConfig().allowReminders;
        boolean bIsPriorityInterruption = mRankingHelper.getPackagePriority(pkg, uid) != 0;
        if (this.DEBUG) {
            Log.e(TAG, "bIsInImportantInterruptions:" + bIsInImportantInterruptions + ", bAllowReminders : " + bAllowReminders + ", bIsPriorityInterruption: " + bIsPriorityInterruption);
        }
        return !(!bIsInImportantInterruptions || (bAllowReminders ^ 1) == 0 || (bIsPriorityInterruption ^ 1) == 0) || shouldSuppressEffect(pkg);
    }

    public boolean shouldSuppressEffect(String pkg) {
        boolean gameMode = SystemProperties.getBoolean("debug.gamemode.value", false);
        if (this.DEBUG) {
            Log.d(TAG, "shouldSuppressEffect-pkg:" + pkg + ",gameMode = " + gameMode);
        }
        return gameMode ? suppressedByGameSpace(pkg) : suppressedByNoDisturb();
    }

    public boolean suppressedByNoDisturb() {
        return this.mIsNoDisturbForScreenAssistant;
    }

    public void setNavigationStatus(String pkg, String channelId, int callingUid, int callingPid, int reason) {
        if (this.DEBUG) {
            Log.d(TAG, "cancelAllNotificationsInt=" + callingUid + " callingPid=" + callingPid + ",pkg:" + pkg + ",channelId:" + channelId + ",reason:" + reason);
        }
        if (reason != 17 && reason != 7 && reason != 9) {
            setKeepAliveAppIfNeed(pkg, -1, false);
        }
    }

    public boolean shouldKeepNotifcationWhenForceStop(String pkg, NotificationRecord r, int reason) {
        if (reason == 21 || reason == 20) {
            if (r.sbn.isClearable() && (isSystemApp(pkg) || this.mKeepNotificationList.contains(pkg))) {
                return true;
            }
            Notification notification = r.getNotification();
            if (notification != null) {
                String appPackage = notification.extras.getString("appPackage");
                if (pkg != null && pkg.equals(appPackage)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canListenNotificationChannelChange(String pkg) {
        return "com.coloros.notificationmanager".equals(pkg);
    }

    public void sendDataToDcs(String logTag, String eventId, ArrayList<String> dataStatisticList) {
        if (eventId.equals(EVENTID_FOR_BLOCKED_BY_BLACKLIST)) {
            for (int i = 0; i < dataStatisticList.size(); i++) {
                sendDataToDcsAfterLocalHandle(logTag, eventId, (String) dataStatisticList.get(i));
            }
            return;
        }
        Map<String, String> eventMap = new HashMap();
        for (int j = 0; j < dataStatisticList.size(); j++) {
            String strPkg = (String) dataStatisticList.get(j);
            if (eventMap.containsKey(strPkg)) {
                eventMap.put(strPkg, String.valueOf(Integer.parseInt((String) eventMap.get(strPkg)) + 1));
            } else {
                eventMap.put(strPkg, LocationManagerService.OPPO_FAKE_LOCATOIN_SWITCH_ON);
            }
        }
        if (eventId.equals(EVENTID_FOR_BLOCKED_BY_DYNAMIC)) {
            this.mDynamicNotificationStatisticList.clear();
        } else if (eventId.equals(EVENTID_FOR_BLOCKED_BY_NORMAL)) {
            this.mNormalNotificationStatisticList.clear();
        } else if (eventId.equals(EVENTID_FOR_BLOCKED_BY_UNNORMAL)) {
            this.mUnNormalNotificationStatisticList.clear();
        }
        if (this.DEBUG) {
            Log.d(TAG, "sendDataToDcs_eventId = " + eventId);
        }
        OppoStatistics.onCommon(this.mContext, logTag, eventId, eventMap, false);
    }

    public boolean shouldInterceptNotification(String pkg, Notification notification) {
        boolean isNeedBlock = false;
        if (notification != null) {
            String title = notification.extras.getString("android.title");
            String text = notification.extras.getString("android.text");
            if (!(title == null || this.mDynamicFilterNotificationList == null || this.mDynamicFilterNotificationList.size() <= 0)) {
                int index = 0;
                while (index < this.mDynamicFilterNotificationList.size()) {
                    String[] tempArray = (String[]) this.mDynamicFilterNotificationList.get(index);
                    if (tempArray != null && tempArray.length == 3 && (("null".equals(tempArray[0]) || pkg.equals(tempArray[0])) && (("null".equals(tempArray[1]) || title.equals(tempArray[1])) && ("null".equals(tempArray[2]) || text.contains(tempArray[2]))))) {
                        isNeedBlock = true;
                        if (this.DEBUG) {
                            Log.d(TAG, "notification.sendTitleData_hasMatched!");
                        }
                        if (isNeedBlock) {
                            String resultString = pkg + LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + title + LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + text;
                            if (!resultString.isEmpty()) {
                                this.mDynamicNotificationStatisticList.add(resultString);
                            }
                            if (this.mDynamicNotificationStatisticList.size() > 0) {
                                if (this.DEBUG) {
                                    Log.d(TAG, "notification.sendDynamicInterceptData");
                                }
                                sendDataToDcs(LOGTAG, EVENTID_FOR_BLOCKED_BY_DYNAMIC, this.mDynamicNotificationStatisticList);
                            }
                            if (this.DEBUG) {
                                Log.d(TAG, "we discard it, because " + pkg + " is in list of Dynamic intercept notification!");
                            }
                        }
                    } else {
                        index++;
                    }
                }
                if (isNeedBlock) {
                    String resultString2 = pkg + LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + title + LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + text;
                    if (!resultString2.isEmpty()) {
                        this.mDynamicNotificationStatisticList.add(resultString2);
                    }
                    if (this.mDynamicNotificationStatisticList.size() > 0) {
                        if (this.DEBUG) {
                            Log.d(TAG, "notification.sendDynamicInterceptData");
                        }
                        sendDataToDcs(LOGTAG, EVENTID_FOR_BLOCKED_BY_DYNAMIC, this.mDynamicNotificationStatisticList);
                    }
                    if (this.DEBUG) {
                        Log.d(TAG, "we discard it, because " + pkg + " is in list of Dynamic intercept notification!");
                    }
                }
            }
        }
        return isNeedBlock;
    }

    private int getMutilAppUid(String pkg, int uid) {
        if (UserHandle.getUserId(uid) == OppoMultiAppManager.USER_ID && pkg != null && OppoMultiAppManagerUtil.getInstance().isMultiApp(pkg)) {
            return UserHandle.getUid(0, UserHandle.getAppId(uid));
        }
        return uid;
    }

    private void sendDataToDcsAfterLocalHandle(String logTag, String eventID, String eventTag) {
        if (this.DEBUG) {
            Log.d(TAG, "sendEventDataAfterLocalHandle_eventID = " + eventID + ", eventTag = " + eventTag);
        }
        if (this.mEventMap.containsKey(eventTag)) {
            this.mEventMap.put(eventTag, String.valueOf(Integer.valueOf((String) this.mEventMap.get(eventTag)).intValue() + 1));
            if (this.DEBUG) {
                Log.d(TAG, "containsKey = " + eventTag + ", and new value = " + ((String) this.mEventMap.get(eventTag)));
            }
        } else {
            this.mEventMap.put(eventTag, String.valueOf(1));
            if (this.DEBUG) {
                Log.d(TAG, "not contains " + eventTag);
            }
        }
        if (this.DEBUG) {
            Log.d(TAG, "value =" + ((String) this.mEventMap.get(eventTag)) + ",eventMap = " + this.mEventMap);
        }
        long currentTime = System.currentTimeMillis();
        if (this.mLastUploadStaticsDataTime <= 0) {
            this.mLastUploadStaticsDataTime = currentTime;
        }
        if (this.DEBUG) {
            Log.d(TAG, " onKVEvent, durring = " + (currentTime - this.mLastUploadStaticsDataTime));
        }
        if (currentTime - this.mLastUploadStaticsDataTime > TIME_UPLOAD_THRESHOLD && this.mEventMap.size() > 0) {
            OppoStatistics.onCommon(this.mContext, logTag, eventID, this.mEventMap, false);
            this.mLastUploadStaticsDataTime = currentTime;
            this.mEventMap.clear();
        }
    }

    private boolean dumpNoClearNotification(PrintWriter pw, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (!"noClear".equals(args[0])) {
            return false;
        }
        pw.println("\n  mNotificationNoClear:");
        synchronized (this.mNotificationNoClear) {
            for (String pkg : this.mNotificationNoClear) {
                pw.println("    NoClearNotification:" + pkg);
            }
        }
        return true;
    }

    public String getForegroundPackage() {
        ComponentName cn;
        try {
            cn = new OppoActivityManager().getTopActivityComponentName();
        } catch (Exception e) {
            Log.w(TAG, "getTopActivityComponentName exception");
            cn = null;
        }
        if (cn != null) {
            return cn.getPackageName();
        }
        return null;
    }

    public boolean isSystemApp(String pkg) {
        try {
            if ((this.mContext.getPackageManager().getApplicationInfo(pkg, 0).flags & 1) != 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean suppressedByGameSpace(String pkgName) {
        if (!this.mGameSpaceMode) {
            return false;
        }
        for (String pkg : GAME_SPACE_WHITELIST) {
            if (pkg.equals(pkgName)) {
                return false;
            }
        }
        return true;
    }

    public void setKeepAliveAppIfNeed(String pkgName, int id, boolean isKeepAlive) {
        for (String appPackage : this.mKeepAliveAppWhiteList) {
            if (appPackage.equals(pkgName)) {
                if (id == 10000 || id == -1) {
                    synchronized (this.mKeepAliveByNotificationMap) {
                        this.mKeepAliveByNotificationMap.put(pkgName, Boolean.valueOf(isKeepAlive));
                    }
                    return;
                }
                return;
            }
        }
    }

    private boolean dumpKeepAliveStatus(PrintWriter pw, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (!"keepAlive".equals(args[0])) {
            return false;
        }
        synchronized (this.mKeepAliveByNotificationMap) {
            try {
                pw.println(" list:" + this.mKeepAliveAppWhiteList);
                for (Entry entry : this.mKeepAliveByNotificationMap.entrySet()) {
                    pw.println(" map:" + ((String) entry.getKey()) + "," + ((Boolean) entry.getValue()) + ",keepAlive:" + shouldKeepAlive((String) entry.getKey(), -1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean shouldKeepAlive(String pkg, int userId) {
        if (!TextUtils.isEmpty(pkg)) {
            synchronized (this.mKeepAliveByNotificationMap) {
                if (this.mKeepAliveByNotificationMap.containsKey(pkg)) {
                    boolean booleanValue = ((Boolean) this.mKeepAliveByNotificationMap.get(pkg)).booleanValue();
                    return booleanValue;
                }
            }
        }
        return false;
    }

    private void initHidePkgList() {
        List<String> hidePkgList = null;
        List<String> hidePkgListMulti = null;
        Cursor cursor = null;
        try {
            cursor = this.mContext.getContentResolver().query(Uri.parse(PRIVACY_PROTECT_URI), PROJECTION, SELECTION_CLAUSE, SELECTION_ARGS, null);
            if (cursor != null) {
                hidePkgList = getPkgNameList(cursor);
                cursor.close();
            } else {
                Log.w(TAG, "mHidePkgList: cursor is null");
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "initHidePkgList error:" + e);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
        try {
            cursor = this.mContext.getContentResolver().query(Uri.parse(PRIVACY_PROTECT_URI), PROJECTION, SELECTION_CLAUSE_MULTI, SELECTION_ARGS, null);
            if (cursor != null) {
                hidePkgListMulti = getPkgNameList(cursor);
                cursor.close();
            } else {
                Log.w(TAG, "mHidePkgListMulti: cursor is null");
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e2) {
            Log.e(TAG, "initHidePkgListMulti error:" + e2);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th2) {
            if (cursor != null) {
                cursor.close();
            }
        }
        synchronized (this.mHidePkgListLock) {
            this.mHidePkgList = hidePkgList;
            this.mHidePkgListMulti = hidePkgListMulti;
        }
        if (this.DEBUG) {
            Log.d(TAG, "initHidePkgList: mHidePkgList = " + this.mHidePkgList + " mHidePkgListMulti = " + this.mHidePkgListMulti);
        }
    }

    private List<String> getPkgNameList(Cursor c) {
        if (c == null) {
            return new ArrayList();
        }
        List<String> hidePkgList = new ArrayList();
        String pkgName = "";
        while (c.moveToNext()) {
            pkgName = c.getString(0);
            if (!TextUtils.isEmpty(pkgName)) {
                hidePkgList.add(pkgName);
            }
        }
        return hidePkgList;
    }

    private void setLimitMaxChannels(String count) {
        try {
            int max = Integer.parseInt(count);
            if (max < 100) {
                Log.d(TAG, "setLimitMaxChannels MIN_CHANNELS_PER_APP > max:" + max + ",count:" + count);
            } else {
                this.mLimitMaxChannels = max;
            }
        } catch (Exception e) {
            Log.d(TAG, "setMaxChannels e:" + e.toString());
        }
    }

    private int getLimitMaxChannels() {
        if (this.mLimitMaxChannels != 0) {
            return this.mLimitMaxChannels;
        }
        return 1000;
    }

    public boolean shouldLimitChannels(RankingHelper rankingHelper, String pkg, int uid, int channelSize) {
        try {
            int curChannelCount = rankingHelper.getNotificationChannels(pkg, uid, false).getList().size();
            if (this.DEBUG_INTERNAL) {
                Log.d(TAG, "needLimitChannels--pkg:" + pkg + ",channelSize:" + channelSize + ",curChannelCount:" + curChannelCount + ",max:" + getLimitMaxChannels());
            }
            if (curChannelCount + channelSize > getLimitMaxChannels()) {
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "shouldLimitChannels e:" + e.toString());
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isHidePkg(String pkg, int userId) {
        synchronized (this.mHidePkgListLock) {
            boolean contains;
            if (userId == 0) {
                if (this.mHidePkgList == null || this.mHidePkgList.isEmpty()) {
                } else {
                    contains = this.mHidePkgList.contains(pkg);
                    return contains;
                }
            } else if (userId != OppoMultiAppManager.USER_ID) {
                return false;
            } else if (this.mHidePkgListMulti == null || this.mHidePkgListMulti.isEmpty()) {
            } else {
                contains = this.mHidePkgListMulti.contains(pkg);
                return contains;
            }
        }
    }

    public boolean dumpOppoNotificationInfo(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (dumpNoClearNotification(pw, args) || dumpKeepAliveStatus(pw, args)) {
            return true;
        }
        return false;
    }
}
