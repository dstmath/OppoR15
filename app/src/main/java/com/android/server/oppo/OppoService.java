package com.android.server.oppo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IOppoService.Stub;
import android.os.Message;
import android.os.OppoManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.widget.Toast;
import com.android.server.LocationManagerService;
import com.android.server.SystemService;
import com.android.server.am.OppoAppStartupManager;
import com.android.server.am.OppoMultiAppManager;
import com.oppo.RomUpdateHelper;
import com.oppo.RomUpdateHelper.UpdateInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import oppo.util.OppoStatistics;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class OppoService extends Stub {
    private static final String ACTION_REDTEAMOBILE_ROAMING_MAIN = "com.redteamobile.roaming.MAIN";
    private static final String DATA_FILE_DIR = "data/system/criticallog_config.xml";
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_FLASH_LIGHT = true;
    private static final boolean DEBUG_TZUPDATE = true;
    public static final int DELAY_TIME = 36000000;
    public static final String FILTER_NAME = "criticallog_config";
    private static final int GET_IMEI_NO_DELAY = 20000;
    protected static final String KEY_SETTINGS_CHANGEOVER = "changeover_status";
    private static final int MSG_GET_IMEI_NO = 2;
    private static final int MSG_GR_CHECK_INTERNET = 9;
    private static final int MSG_GR_DOWN_INSTALL = 4;
    private static final int MSG_GR_EXIT = 8;
    private static final int MSG_GR_INIT = 3;
    private static final int MSG_GR_INSTALL_TALKBACK = 10;
    private static final int MSG_GR_REINSTALL = 5;
    private static final int MSG_GR_SHOW_EXCEPTION = 6;
    private static final int MSG_GR_SUCC = 7;
    private static final int MSG_WRITE_MM_KEY_LOG = 30;
    private static final String NAME_MMKEYLOG = "oppo_critical_log";
    private static final String SYS_FILE_DIR = "system/etc/criticallog_config.xml";
    private static final String TAG = "OppoService";
    protected static final String VALUE_CHANGEOVER = "1";
    protected static final String VALUE_NOT_CHANGEOVER = "0";
    private static final int WRITE_MM_KEY_LOG_DELAY = 10000;
    private static String curName;
    private static int curState;
    private Boolean DEBUG_GR = Boolean.valueOf(OppoManager.DEBUG_GR);
    private String grAbandon = "";
    private String grCancel = "";
    private String grDoDown = "";
    private String grDoDownDown = "";
    private String grDownTipContent = "";
    private String grDownTipContentDown = "";
    private String grExceptionContent = "";
    private String grExceptionContentDown = "";
    private String grFileName = null;
    private String grNetworkContent = "";
    private String grNeverRemind = "";
    private String grNoOppoRoamTip = "";
    private String grNotAccessTip = "";
    private String grOk = "";
    private String grOppoRoam = "";
    private String grOppoRoamTip = "";
    private String grReinstallPTipContent = "";
    private String grSpaceContent = "";
    private String grSucc = "";
    private String grSuccDown = "";
    private String grTalkbackExceptionContent = "";
    private String grTalkbackTipContent = "";
    private String grTipContent = "";
    private String grTipContentDown = "";
    private String grTipInstalling = "";
    private String grTipTitle = "";
    private Boolean hasGrInit = Boolean.valueOf(false);
    BTBRecordService mBTBRecordService;
    private AlertDialog mCheckNetworkDialog = null;
    private Context mContext;
    OppoFallingMonitor mFallingMonitor;
    private FlashLightControler mFlashLightControler = null;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            final String pkgName;
            ActivityManager am;
            PackageManager pm;
            ApplicationInfo appInfo;
            final String appName;
            final String baseCodePath;
            Builder builder;
            AlertDialog dialog;
            String eMessage;
            Bundle bundle;
            if (msg.what == 2) {
                if (OppoService.this.mRetry != 0) {
                    if (OppoService.this.isFactoryMode()) {
                        SystemProperties.set("sys.usb.config", "diag_mdm,adb");
                        SystemClock.sleep(100);
                        SystemProperties.set("sys.dial.enable", Boolean.toString(true));
                        OppoService.this.mRetry = 0;
                    } else {
                        OppoService oppoService = OppoService.this;
                        oppoService.mRetry = oppoService.mRetry - 1;
                        sendMessageDelayed(obtainMessage(2), 10000);
                    }
                } else {
                    return;
                }
            } else if (msg.what == 3) {
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we get gr init msg.");
                }
                OppoService.this.initGr();
            } else if (msg.what == 4) {
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we get gr down msg.");
                }
                if (OppoManager.canCreateDialog.booleanValue()) {
                    OppoManager.canCreateDialog = Boolean.valueOf(false);
                    pkgName = msg.getData().getString("pkgName");
                    am = (ActivityManager) OppoService.this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
                    for (String pName : OppoManager.grList) {
                        am.forceStopPackage(pName);
                    }
                    am.forceStopPackage(pkgName);
                    if (OppoService.this.DEBUG_GR.booleanValue()) {
                        Log.d(OppoService.TAG, "Geloin: we killed " + pkgName);
                    }
                    Boolean isNetworkOk = OppoService.this.isNetworkOk();
                    Boolean isSpaceOk = OppoService.this.isSpaceOk();
                    pm = OppoService.this.mContext.getPackageManager();
                    try {
                        String tipContent;
                        appInfo = pm.getApplicationInfo(pkgName, 0);
                        appName = appInfo.loadLabel(pm).toString();
                        baseCodePath = appInfo.getBaseCodePath();
                        if (!isNetworkOk.booleanValue()) {
                            tipContent = OppoService.this.grNetworkContent;
                        } else if (!isSpaceOk.booleanValue()) {
                            tipContent = OppoService.this.grSpaceContent;
                        } else if (OppoService.this.mIsGRIn.booleanValue()) {
                            tipContent = String.format(OppoService.this.grTipContent, new Object[]{appName});
                        } else {
                            tipContent = String.format(OppoService.this.grTipContentDown, new Object[]{appName});
                        }
                        if (OppoService.this.isChangeOver()) {
                            if (OppoService.this.mIsGRIn.booleanValue()) {
                                OppoService.this.notInstalls.put(pkgName, baseCodePath);
                                OppoManager.isNoDialogInstalling = Boolean.valueOf(true);
                                Log.d(OppoService.TAG, "installOnDialog: pkgName = " + pkgName);
                                OppoGrThreadFactory.executor.execute(OppoGrThreadFactory.newOppoGrThread(OppoService.this.mContext, OppoService.this.notInstalls));
                                OppoService.this.notInstalls = new HashMap();
                            }
                            return;
                        }
                        builder = new Builder(OppoService.this.mContext);
                        builder.setTitle(OppoService.this.grTipTitle);
                        builder.setMessage(tipContent);
                        if (isNetworkOk.booleanValue() && (isSpaceOk.booleanValue() ^ 1) == 0) {
                            String doDown = OppoService.this.grDoDown;
                            if (!OppoService.this.mIsGRIn.booleanValue()) {
                                doDown = OppoService.this.grDoDownDown;
                            }
                            builder.setPositiveButton(doDown, new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    OppoService.this.notInstalls.put(pkgName, baseCodePath);
                                    String eMessage = OppoService.this.grExceptionContent;
                                    if (!OppoService.this.mIsGRIn.booleanValue()) {
                                        eMessage = OppoService.this.grExceptionContentDown;
                                    }
                                    String downTipContent = OppoService.this.grDownTipContent;
                                    if (!OppoService.this.mIsGRIn.booleanValue()) {
                                        downTipContent = OppoService.this.grDownTipContentDown;
                                    }
                                    OppoGrThreadFactory.executor.execute(OppoGrThreadFactory.newOppoGrThread(OppoService.this.grFileName, OppoService.this.mContext, baseCodePath, OppoService.this.grTipTitle, downTipContent, OppoService.this.notInstalls, OppoService.this.grAbandon, OppoService.this.grOk, eMessage, appName, pkgName));
                                    OppoService.this.notInstalls = new HashMap();
                                }
                            });
                            builder.setNegativeButton(OppoService.this.grAbandon, new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    OppoService.this.notInstalls.put(pkgName, baseCodePath);
                                    OppoManager.canCreateDialog = Boolean.valueOf(true);
                                    dialog.cancel();
                                }
                            });
                        } else {
                            builder.setPositiveButton(OppoService.this.grOk, new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    OppoService.this.notInstalls.put(pkgName, baseCodePath);
                                    OppoManager.canCreateDialog = Boolean.valueOf(true);
                                    dialog.cancel();
                                }
                            });
                        }
                        dialog = builder.create();
                        dialog.getWindow().setType(2003);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        dialog.show();
                    } catch (Exception e) {
                        return;
                    }
                }
            } else if (msg.what == 5) {
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we get gr reinstall msg.");
                }
                if (OppoManager.canReinstall.booleanValue()) {
                    OppoManager.canReinstall = Boolean.valueOf(false);
                    pkgName = msg.getData().getString("pkgName");
                    am = (ActivityManager) OppoService.this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
                    for (String pName2 : OppoManager.grList) {
                        am.forceStopPackage(pName2);
                    }
                    am.forceStopPackage(pkgName);
                    if (OppoService.this.DEBUG_GR.booleanValue()) {
                        Log.d(OppoService.TAG, "Geloin: we killed " + pkgName);
                    }
                    pm = OppoService.this.mContext.getPackageManager();
                    try {
                        appInfo = pm.getApplicationInfo(pkgName, 0);
                        appName = appInfo.loadLabel(pm).toString();
                        baseCodePath = appInfo.getBaseCodePath();
                        String grReinstallPTipContentTmp = String.format(OppoService.this.grReinstallPTipContent, new Object[]{appName});
                        eMessage = OppoService.this.grExceptionContent;
                        if (!OppoService.this.mIsGRIn.booleanValue()) {
                            eMessage = OppoService.this.grExceptionContentDown;
                        }
                        OppoGrThreadFactory.executor.execute(OppoGrThreadFactory.newOppoGrThread(OppoService.this.mContext, baseCodePath, OppoService.this.grTipTitle, grReinstallPTipContentTmp, OppoService.this.grAbandon, OppoService.this.grOk, eMessage, appName, pkgName));
                    } catch (Exception e2) {
                        return;
                    }
                }
            } else if (msg.what == 6) {
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we get gr show exception msg.");
                }
                eMessage = OppoService.this.grExceptionContent;
                if (!OppoService.this.mIsGRIn.booleanValue()) {
                    eMessage = OppoService.this.grExceptionContentDown;
                }
                String exceptionType = msg.getData().getString("exceptionType");
                if (exceptionType != null) {
                    if (exceptionType.equals("NetworkError")) {
                        eMessage = OppoService.this.grNetworkContent;
                    }
                    if (exceptionType.equals("TalkbackError")) {
                        eMessage = OppoService.this.grTalkbackExceptionContent;
                    }
                }
                builder = new Builder(OppoService.this.mContext);
                builder.setTitle(OppoService.this.grTipTitle);
                builder.setMessage(eMessage);
                builder.setNegativeButton(OppoService.this.grOk, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.getWindow().setType(2003);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                dialog.show();
            } else if (msg.what == 7) {
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we get gr success msg.");
                }
                String succMsg = OppoService.this.grSucc;
                bundle = msg.getData();
                baseCodePath = bundle.getString("baseCodePath");
                appName = bundle.getString("appName");
                pkgName = bundle.getString("pkgName");
                if (appName != null) {
                    succMsg = String.format(OppoService.this.grSuccDown, new Object[]{appName});
                } else if (baseCodePath != null) {
                    succMsg = String.format(OppoService.this.grSucc, new Object[]{baseCodePath});
                }
                builder = new Builder(OppoService.this.mContext);
                builder.setTitle(OppoService.this.grTipTitle);
                builder.setMessage(succMsg);
                builder.setNegativeButton(OppoService.this.grOk, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (pkgName == null) {
                            return;
                        }
                        if (pkgName.equals("com.google.android.marvin.talkback")) {
                            Log.d(OppoService.TAG, "send broadcast for talkback install successfully");
                            OppoService.this.mContext.sendBroadcast(new Intent("com.oppo.intent.action.TALKBACK_INSTALL_SUCCESS"));
                            return;
                        }
                        Intent resolveIntent = new Intent("android.intent.action.MAIN", null);
                        resolveIntent.addCategory("android.intent.category.LAUNCHER");
                        resolveIntent.setPackage(pkgName);
                        List<ResolveInfo> apps = OppoService.this.mContext.getPackageManager().queryIntentActivities(resolveIntent, 0);
                        if (apps != null && apps.size() > 0) {
                            ResolveInfo ri = (ResolveInfo) apps.iterator().next();
                            if (ri != null) {
                                String className = ri.activityInfo.name;
                                Intent intent = new Intent("android.intent.action.MAIN");
                                intent.addCategory("android.intent.category.LAUNCHER");
                                intent.setFlags(268435456);
                                intent.setComponent(new ComponentName(pkgName, className));
                                OppoService.this.mContext.startActivity(intent);
                            }
                        }
                    }
                });
                dialog = builder.create();
                dialog.getWindow().setType(2003);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                dialog.show();
            } else if (msg.what == 8) {
                pkgName = msg.getData().getString("pkgName");
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we get gr exit msg, and we will kill " + pkgName);
                }
                am = (ActivityManager) OppoService.this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
                for (String pName22 : OppoManager.grList) {
                    am.forceStopPackage(pName22);
                }
                am.forceStopPackage(pkgName);
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we killed " + pkgName);
                }
            } else if (msg.what == 9) {
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: we will check wether can access google service");
                }
                bundle = msg.getData();
                String userInChina = bundle.getString("isInChina");
                String oppoRoamSupportStr = bundle.getString("canSupportOppoRoam");
                if (userInChina == null && oppoRoamSupportStr == null) {
                    OppoGrThreadFactory.executor.execute(OppoGrThreadFactory.newOppoGrThread(OppoService.this.mContext));
                } else {
                    if (OppoService.this.mCheckNetworkDialog != null) {
                        OppoService.this.mCheckNetworkDialog.cancel();
                    }
                    String netMsg;
                    if (userInChina != null) {
                        netMsg = OppoService.this.grNotAccessTip;
                        builder = new Builder(OppoService.this.mContext);
                        builder.setTitle(OppoService.this.grTipTitle);
                        builder.setMessage(netMsg);
                        builder.setPositiveButton(OppoService.this.grOk, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                OppoService.this.mCheckNetworkDialog = null;
                            }
                        });
                        OppoService.this.mCheckNetworkDialog = builder.create();
                        OppoService.this.mCheckNetworkDialog.getWindow().setType(2003);
                        OppoService.this.mCheckNetworkDialog.setCanceledOnTouchOutside(false);
                        OppoService.this.mCheckNetworkDialog.setCancelable(false);
                        OppoService.this.mCheckNetworkDialog.show();
                    } else {
                        Boolean valueOf = Boolean.valueOf(false);
                        try {
                            valueOf = Boolean.valueOf(oppoRoamSupportStr);
                        } catch (Exception e3) {
                            valueOf = Boolean.valueOf(false);
                        }
                        if (valueOf.booleanValue()) {
                            netMsg = OppoService.this.grOppoRoamTip;
                        } else {
                            netMsg = OppoService.this.grNoOppoRoamTip;
                        }
                        builder = new Builder(OppoService.this.mContext);
                        builder.setTitle(OppoService.this.grTipTitle);
                        builder.setMessage(netMsg);
                        builder.setNegativeButton(OppoService.this.grOk, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        if (valueOf.booleanValue()) {
                            builder.setPositiveButton(OppoService.this.grOppoRoam, new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    OppoService.this.mCheckNetworkDialog = null;
                                    Intent intent = new Intent("com.redteamobile.roaming.MAIN");
                                    intent.setFlags(268435456);
                                    OppoService.this.mContext.startActivity(intent);
                                }
                            });
                        }
                        OppoService.this.mCheckNetworkDialog = builder.create();
                        OppoService.this.mCheckNetworkDialog.getWindow().setType(2003);
                        OppoService.this.mCheckNetworkDialog.setCanceledOnTouchOutside(false);
                        OppoService.this.mCheckNetworkDialog.setCancelable(false);
                        OppoService.this.mCheckNetworkDialog.show();
                    }
                }
            } else if (msg.what == 10) {
                bundle = msg.getData();
                pkgName = bundle.getString("pkgName");
                if (OppoService.this.DEBUG_GR.booleanValue()) {
                    Log.d(OppoService.TAG, "Geloin: MSG_GR_INSTALL_TALKBACK" + pkgName);
                }
                appName = bundle.getString("appName");
                baseCodePath = bundle.getString("baseCodePath");
                eMessage = OppoService.this.grTalkbackExceptionContent;
                String str = baseCodePath;
                OppoGrThreadFactory.executor.execute(OppoGrThreadFactory.newOppoGrThread(OppoService.this.mContext, str, OppoService.this.grTipTitle, String.format(OppoService.this.grTalkbackTipContent, new Object[]{appName}), OppoService.this.grAbandon, OppoService.this.grOk, eMessage, appName, pkgName, 5));
            }
            int i = msg.what;
        }
    };
    private Boolean mIsGRIn = Boolean.valueOf(false);
    OppoLogService mLogService;
    private MMKernelKeyLogObserver mMMKernelKeyLogObserver;
    private NetWakeManager mNetWakeManager;
    private int mRetry = 7;
    CriticalLogConfigUpdateHelper mXmlHelper;
    private ZoneInfoDataFileController mZoneInfoDataFileController = null;
    private Map<String, String> notInstalls = new HashMap();

    class BTBRecordService {
        final int BTB_TYPE = 700;
        final int BTB_UPLOAD_TYPE = 701;
        final int MSG_DCS = 100001;
        final int MSG_INIT = 100002;
        private Context mContext;
        private Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100001) {
                    Slog.v(OppoService.TAG, "do in MSG_DCS");
                    OppoStatistics.onCommon(BTBRecordService.this.mContext, "BTB_System", "BTB_record", new HashMap(), true);
                    OppoManager.cleanItem(700);
                    OppoManager.writeLogToPartition(701, "upload BTB record", "KERNEL", "BTB_record", "BTBUPLOAD");
                    OppoManager.syncCacheToEmmc();
                    BTBRecordService.this.unregister();
                } else if (msg.what == 100002) {
                    BTBRecordService.this.initBTB();
                }
            }
        };
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Slog.v(OppoService.TAG, " receiver = " + intent.getAction());
                ConnectivityManager connectivityManager = (ConnectivityManager) BTBRecordService.this.mContext.getSystemService("connectivity");
                NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(0);
                NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(1);
                if (mobNetInfo.isConnected() || wifiNetInfo.isConnected()) {
                    BTBRecordService.this.mHandler.sendEmptyMessage(100001);
                } else {
                    Slog.v(OppoService.TAG, "dis connect");
                }
            }
        };

        BTBRecordService(Context context) {
            this.mContext = context;
            Slog.v(OppoService.TAG, "in BTBRecordService ");
            this.mHandler.sendEmptyMessageDelayed(100002, 20000);
        }

        void register() {
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
            Slog.v(OppoService.TAG, "register CONNECTIVITY_ACTION");
        }

        void unregister() {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        void initBTB() {
            String record = OppoManager.readCriticalData(700, 256);
            Slog.v(OppoService.TAG, "BTB_TYPE record = " + record);
            if (record != null && (record.isEmpty() ^ 1) != 0) {
                register();
            }
        }
    }

    class CriticalLogConfigUpdateHelper extends RomUpdateHelper {
        private static final int CONST_THREE = 3;
        public static final String FALLING_MONITOR = "falling_monitor_switch";
        public static final String VERSION_NAME = "version";
        CriticalLogUpdateInfo mCurrentInfo = new CriticalLogUpdateInfo();
        CriticalLogUpdateInfo mNewVersionInfo = new CriticalLogUpdateInfo();

        private class CriticalLogUpdateInfo extends UpdateInfo {
            private boolean mIsFallingSwitch = false;
            private boolean mIsUpdateToLowerVersion = false;

            public CriticalLogUpdateInfo() {
                super(CriticalLogConfigUpdateHelper.this);
            }

            public boolean getFallingSwitch() {
                Slog.v(OppoService.TAG, "mIsFallingSwitch :" + this.mIsFallingSwitch);
                return this.mIsFallingSwitch;
            }

            private void updateConfigVersion(String type, String value) {
                Slog.d(OppoService.TAG, hashCode() + " updateConfigVersion, type = " + type + ", value = " + value);
                if ("version".equals(type)) {
                    this.mVersion = (long) Integer.parseInt(value);
                }
            }

            public boolean updateToLowerVersion(String content) {
                long newVersion = getContentVersion(content);
                getFallingSwitchNewVersion(content);
                Slog.d(OppoService.TAG, "upateToLowerVersion, newVersion = " + newVersion + ", mVersion = " + this.mVersion);
                this.mIsUpdateToLowerVersion = newVersion < this.mVersion;
                return this.mIsUpdateToLowerVersion;
            }

            public boolean isUpdateToLowerVersion() {
                return this.mIsUpdateToLowerVersion;
            }

            public void parseContentFromXML(String content) {
                IOException e;
                XmlPullParserException e2;
                Throwable th;
                if (content != null) {
                    StringReader strReader = null;
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        StringReader strReader2 = new StringReader(content);
                        try {
                            parser.setInput(strReader2);
                            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                                switch (eventType) {
                                    case 2:
                                        char[] typeChar = parser.getName().toCharArray();
                                        if (typeChar.length <= 3) {
                                            break;
                                        }
                                        eventType = parser.next();
                                        updateConfigVersion(String.valueOf(typeChar), parser.getText());
                                        parserFallingMonitorSwitch(String.valueOf(typeChar), parser.getText());
                                        break;
                                    default:
                                        break;
                                }
                            }
                            if (strReader2 != null) {
                                try {
                                    strReader2.close();
                                } catch (IOException e3) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3);
                                }
                            }
                        } catch (XmlPullParserException e4) {
                            e2 = e4;
                            strReader = strReader2;
                            try {
                                CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e2);
                                if (strReader != null) {
                                    try {
                                        strReader.close();
                                    } catch (IOException e32) {
                                        CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32);
                                    }
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                if (strReader != null) {
                                    try {
                                        strReader.close();
                                    } catch (IOException e322) {
                                        CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e322);
                                    }
                                }
                                throw th;
                            }
                        } catch (IOException e5) {
                            e322 = e5;
                            strReader = strReader2;
                            CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e322);
                            if (strReader != null) {
                                try {
                                    strReader.close();
                                } catch (IOException e3222) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3222);
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            strReader = strReader2;
                            if (strReader != null) {
                                try {
                                    strReader.close();
                                } catch (IOException e32222) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32222);
                                }
                            }
                            throw th;
                        }
                    } catch (XmlPullParserException e6) {
                        e2 = e6;
                        CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e2);
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e322222) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e322222);
                            }
                        }
                    } catch (IOException e7) {
                        e322222 = e7;
                        CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e322222);
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e3222222) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3222222);
                            }
                        }
                    }
                }
            }

            void parserFallingMonitorSwitch(String type, String value) {
                if (type != null && type.equals(CriticalLogConfigUpdateHelper.FALLING_MONITOR)) {
                    if (value == null || !value.equals("true")) {
                        Slog.v(OppoService.TAG, "parserFallingMonitorSwitch false");
                        this.mIsFallingSwitch = false;
                        return;
                    }
                    Slog.v(OppoService.TAG, "parserFallingMonitorSwitch true");
                    this.mIsFallingSwitch = true;
                }
            }

            private boolean getFallingSwitchNewVersion(String content) {
                IOException e;
                XmlPullParserException e2;
                Throwable th;
                if (content == null) {
                    return false;
                }
                StringReader strReader = null;
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    StringReader strReader2 = new StringReader(content);
                    try {
                        parser.setInput(strReader2);
                        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                            switch (eventType) {
                                case 2:
                                    char[] typeChar = parser.getName().toCharArray();
                                    if (typeChar.length <= 3) {
                                        break;
                                    }
                                    eventType = parser.next();
                                    parserFallingMonitorSwitch(String.valueOf(typeChar), parser.getText());
                                    break;
                                default:
                                    break;
                            }
                        }
                        if (strReader2 != null) {
                            try {
                                strReader2.close();
                            } catch (IOException e3) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3);
                            }
                        }
                        return true;
                    } catch (XmlPullParserException e4) {
                        e2 = e4;
                        strReader = strReader2;
                        try {
                            CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e2);
                            if (strReader != null) {
                                try {
                                    strReader.close();
                                } catch (IOException e32) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32);
                                }
                            }
                            return false;
                        } catch (Throwable th2) {
                            th = th2;
                            if (strReader != null) {
                                try {
                                    strReader.close();
                                } catch (IOException e322) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e322);
                                }
                            }
                            throw th;
                        }
                    } catch (IOException e5) {
                        e322 = e5;
                        strReader = strReader2;
                        CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e322);
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e3222) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3222);
                            }
                        }
                        return false;
                    } catch (Throwable th3) {
                        th = th3;
                        strReader = strReader2;
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e32222) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32222);
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e6) {
                    e2 = e6;
                    CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e2);
                    if (strReader != null) {
                        try {
                            strReader.close();
                        } catch (IOException e322222) {
                            CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e322222);
                        }
                    }
                    return false;
                } catch (IOException e7) {
                    e322222 = e7;
                    CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e322222);
                    if (strReader != null) {
                        try {
                            strReader.close();
                        } catch (IOException e3222222) {
                            CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3222222);
                        }
                    }
                    return false;
                }
            }

            private long getContentVersion(String content) {
                IOException e;
                XmlPullParserException e2;
                Throwable th;
                long version = -1;
                if (content == null) {
                    return -1;
                }
                StringReader strReader = null;
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    StringReader strReader2 = new StringReader(content);
                    try {
                        parser.setInput(strReader2);
                        boolean found = false;
                        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                            switch (eventType) {
                                case 2:
                                    if ("version".equals(parser.getName())) {
                                        eventType = parser.next();
                                        Slog.d(OppoService.TAG, "eventType = " + eventType + ", text = " + parser.getText());
                                        version = (long) Integer.parseInt(parser.getText());
                                        found = true;
                                        break;
                                    }
                                    break;
                            }
                            if (found) {
                                if (strReader2 != null) {
                                    try {
                                        strReader2.close();
                                    } catch (IOException e3) {
                                        CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3);
                                    }
                                }
                                return version;
                            }
                        }
                        if (strReader2 != null) {
                            try {
                                strReader2.close();
                            } catch (IOException e32) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32);
                            }
                        }
                        return version;
                    } catch (XmlPullParserException e4) {
                        e2 = e4;
                        strReader = strReader2;
                        CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e2);
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e322) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e322);
                            }
                        }
                        return -1;
                    } catch (IOException e5) {
                        e322 = e5;
                        strReader = strReader2;
                        try {
                            CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e322);
                            if (strReader != null) {
                                try {
                                    strReader.close();
                                } catch (IOException e3222) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3222);
                                }
                            }
                            return -1;
                        } catch (Throwable th2) {
                            th = th2;
                            if (strReader != null) {
                                try {
                                    strReader.close();
                                } catch (IOException e32222) {
                                    CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32222);
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        strReader = strReader2;
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e322222) {
                                CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e322222);
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e6) {
                    e2 = e6;
                    CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e2);
                    if (strReader != null) {
                        try {
                            strReader.close();
                        } catch (IOException e3222222) {
                            CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e3222222);
                        }
                    }
                    return -1;
                } catch (IOException e7) {
                    e3222222 = e7;
                    CriticalLogConfigUpdateHelper.this.log("Got execption parsing permissions.", e3222222);
                    if (strReader != null) {
                        try {
                            strReader.close();
                        } catch (IOException e32222222) {
                            CriticalLogConfigUpdateHelper.this.log("Got execption close permReader.", e32222222);
                        }
                    }
                    return -1;
                }
            }
        }

        public CriticalLogConfigUpdateHelper(Context context, String filterName, String systemFile, String dataFile) {
            super(context, filterName, systemFile, dataFile);
            setUpdateInfo(this.mCurrentInfo, this.mNewVersionInfo);
        }

        public void getUpdateFromProvider() {
            super.getUpdateFromProvider();
            if (this.mCurrentInfo.isUpdateToLowerVersion()) {
                Log.v(OppoService.TAG, "update criticallog UpdateToLowerVersion do nothing");
                return;
            }
            OppoManager.updateConfig();
            if (this.mCurrentInfo.getFallingSwitch()) {
                Slog.v(OppoService.TAG, "new version falling monitor true");
                OppoService.this.startFallingMonitor();
            } else {
                Slog.v(OppoService.TAG, "new version falling monitor false");
                OppoService.this.stopFallingMonitor();
            }
            Log.v(OppoService.TAG, "update criticallog config");
        }

        public void initFallingMonitor() {
            if (this.mCurrentInfo.getFallingSwitch()) {
                Slog.v(OppoService.TAG, "initFallingMonitor start monitor ");
                OppoService.this.startFallingMonitor();
                return;
            }
            Slog.v(OppoService.TAG, "initFallingMonitor stop monitor ");
            OppoService.this.stopFallingMonitor();
        }
    }

    private class FlashLightControler {
        private static final String FLASH_LIGHT_DRIVER_NODE = "/proc/qcom_flash";
        private static final String FLASH_LIGHT_MODE_CLOSE = "0";
        private static final String FLASH_LIGHT_MODE_OPEN = "1";

        public boolean openFlashLightImpl() {
            return writeValueToFlashLightNode("1");
        }

        public boolean closeFlashLightImpl() {
            return writeValueToFlashLightNode(FLASH_LIGHT_MODE_CLOSE);
        }

        public String getFlashLightStateImpl() {
            return getCurrentFlashLightState();
        }

        private boolean writeValueToFlashLightNode(String value) {
            IOException e;
            Slog.d(OppoService.TAG, "writeValueToFlashLightNode, new value:" + value);
            if (value == null || value.length() <= 0) {
                Slog.w(OppoService.TAG, "writeValueToFlashLightNode:value unavailable!");
                return false;
            }
            try {
                FileWriter nodeFileWriter = new FileWriter(new File(FLASH_LIGHT_DRIVER_NODE));
                try {
                    nodeFileWriter.write(value);
                    nodeFileWriter.close();
                    Slog.d(OppoService.TAG, "write flashLight node succeed!");
                    return true;
                } catch (IOException e2) {
                    e = e2;
                    e.printStackTrace();
                    Slog.e(OppoService.TAG, "write flashLight node failed!");
                    return false;
                }
            } catch (IOException e3) {
                e = e3;
                e.printStackTrace();
                Slog.e(OppoService.TAG, "write flashLight node failed!");
                return false;
            }
        }

        private String getCurrentFlashLightState() {
            char[] valueArray = new char[10];
            String result = "";
            try {
                FileReader nodeFileReader = new FileReader(new File(FLASH_LIGHT_DRIVER_NODE));
                nodeFileReader.read(valueArray);
                result = new String(valueArray).trim();
                nodeFileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
                Slog.e(OppoService.TAG, "read flashLight node failed!");
            }
            Slog.d(OppoService.TAG, "getCurrentFlashLightState:" + result);
            return result;
        }
    }

    class MMKernelKeyLogObserver extends UEventObserver {
        private static final String MULTIMEDIA_TAG = "MULTIMEDIA";
        private static final int TYPE_ADSP_CLK_OPEN_TIMEOUT = 311;
        private static final int TYPE_ADSP_LOAD_FAIL = 301;
        private static final int TYPE_BL_EXCEPTION = 310;
        private static final int TYPE_ESD_EXCEPTION = 306;
        private static final int TYPE_FENCE_TIMEOUT = 309;
        private static final int TYPE_GPU_EXCEPTION = 307;
        private static final int TYPE_HP_PA_EXCEPTION = 312;
        private static final int TYPE_IOMMU_ERROR = 308;
        private static final int TYPE_KGSL_EXCEPTION = 304;
        private static final int TYPE_NO_DATA_TO_SHOW = 303;
        private static final int TYPE_SMART_PA_EXCEPTION = 302;
        private static final int TYPE_SOUND_CARD_REGISTER_FAIL = 300;
        private static final int TYPE_VSYNC_EXCEPTION = 305;
        private final Object mLock = new Object();
        private final UEventInfo mUEventInfo = makeObservedUEvent();

        private final class UEventInfo {
            private final String mDevName;

            public UEventInfo(String devName) {
                this.mDevName = devName;
            }

            public String getDevName() {
                return this.mDevName;
            }

            public String getDevPath() {
                return String.format(Locale.US, "/devices/virtual/switch/%s", new Object[]{this.mDevName});
            }

            public String getSwitchStatePath() {
                return String.format(Locale.US, "/sys/class/switch/%s/state", new Object[]{this.mDevName});
            }

            public String getSwitchNamePath() {
                return String.format(Locale.US, "/sys/class/switch/%s/name", new Object[]{this.mDevName});
            }

            public boolean checkSwitchExists() {
                return new File(getSwitchStatePath()).exists();
            }
        }

        private String getIssueCause(int id) {
            switch (id) {
                case 300:
                    return "sound_card_register_fail";
                case TYPE_ADSP_LOAD_FAIL /*301*/:
                    return "adps_load_fail";
                case TYPE_SMART_PA_EXCEPTION /*302*/:
                    return "smart_pa_exception";
                case TYPE_NO_DATA_TO_SHOW /*303*/:
                    return "no_data_to_show";
                case TYPE_KGSL_EXCEPTION /*304*/:
                    return "kgsl_exception";
                case TYPE_VSYNC_EXCEPTION /*305*/:
                    return "vsync_exception";
                case TYPE_ESD_EXCEPTION /*306*/:
                    return "esd_exception";
                case TYPE_GPU_EXCEPTION /*307*/:
                    return "gpu_exception";
                case TYPE_IOMMU_ERROR /*308*/:
                    return "iommu_error";
                case TYPE_FENCE_TIMEOUT /*309*/:
                    return "fence_timeout";
                case TYPE_BL_EXCEPTION /*310*/:
                    return "bl_exception";
                case TYPE_ADSP_CLK_OPEN_TIMEOUT /*311*/:
                    return "adsp clk open time out";
                case TYPE_HP_PA_EXCEPTION /*312*/:
                    return "headphones pa excetion";
                case 803:
                    return "symbol_version_disagree";
                case 804:
                    return "wdi_exception";
                default:
                    return "";
            }
        }

        private String getIssueDesc(int id) {
            int resId;
            switch (id) {
                case 300:
                case TYPE_ADSP_LOAD_FAIL /*301*/:
                case TYPE_SMART_PA_EXCEPTION /*302*/:
                case TYPE_ADSP_CLK_OPEN_TIMEOUT /*311*/:
                case TYPE_HP_PA_EXCEPTION /*312*/:
                    resId = 17040998;
                    break;
                case TYPE_NO_DATA_TO_SHOW /*303*/:
                case TYPE_KGSL_EXCEPTION /*304*/:
                case TYPE_VSYNC_EXCEPTION /*305*/:
                case TYPE_ESD_EXCEPTION /*306*/:
                case TYPE_GPU_EXCEPTION /*307*/:
                case TYPE_IOMMU_ERROR /*308*/:
                case TYPE_FENCE_TIMEOUT /*309*/:
                case TYPE_BL_EXCEPTION /*310*/:
                    resId = 17040988;
                    break;
                case 803:
                    resId = 17041000;
                    break;
                case 804:
                    resId = 17041008;
                    break;
                default:
                    resId = 17041002;
                    break;
            }
            return OppoService.this.mContext.getString(resId);
        }

        void init() {
            if (this.mUEventInfo == null) {
                Slog.d("mUEventInfo is null, should not be here!", "init()");
                return;
            }
            synchronized (this.mLock) {
                Slog.d("MMKernelKeyLogObserver", "init()");
                char[] buffer = new char[1024];
                try {
                    FileReader file = new FileReader(this.mUEventInfo.getSwitchStatePath());
                    int len = file.read(buffer, 0, 1024);
                    file.close();
                    OppoService.curState = Integer.valueOf(new String(buffer, 0, len).trim()).intValue();
                    FileReader fileName = new FileReader(this.mUEventInfo.getSwitchNamePath());
                    len = fileName.read(buffer, 0, 1024);
                    fileName.close();
                    OppoService.curName = new String(buffer, 0, len).trim();
                    Slog.e("MMKernelKeyLogObserver", "curName:" + OppoService.curName + "curState:" + OppoService.curState);
                    if (OppoService.curState < 0) {
                        OppoService.curState = 0 - OppoService.curState;
                    }
                    if (OppoService.curState >= 1 && OppoService.curName != null) {
                        OppoService.this.mHandler.sendMessageDelayed(OppoService.this.mHandler.obtainMessage(30), 10000);
                    }
                } catch (FileNotFoundException e) {
                    Slog.w("MMKernelKeyLogObserver", this.mUEventInfo.getSwitchStatePath() + " not found while attempting to determine initial switch state");
                } catch (Exception e2) {
                    Slog.e("MMKernelKeyLogObserver", "", e2);
                }
            }
            startObserving("DEVPATH=" + this.mUEventInfo.getDevPath());
            return;
        }

        private UEventInfo makeObservedUEvent() {
            UEventInfo uei = new UEventInfo(OppoService.NAME_MMKEYLOG);
            if (uei.checkSwitchExists()) {
                return uei;
            }
            Slog.w("MMKernelKeyLogObserver", "This kernel does not have mm key log support");
            return null;
        }

        public void onUEvent(UEvent event) {
            Log.d(OppoService.TAG, "MM Key LogEvent UEVENT: " + event.toString());
            try {
                String name = event.get("SWITCH_NAME");
                int state = Integer.parseInt(event.get("SWITCH_STATE"));
                synchronized (this.mLock) {
                    Log.d(OppoService.TAG, "onUEvent: start write log");
                    writeMMKeyLog(name, state);
                    if (state < 800 || state > OppoMultiAppManager.USER_ID) {
                        if (state == 1001) {
                            OppoService.this.recordSubSystemCrash(name);
                        } else {
                            writeMMKeyLog(name, state);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                Slog.e("MMKernelKeyLogObserver", "Could not parse switch state from event " + event);
            }
        }

        private void writeMMKeyLog(String name, int state) {
            Log.d(OppoService.TAG, "writeMMKeyLog: name = " + name + "\n type index = " + state);
            if (state < 300 || state > 399) {
                Log.e("MMKernelKeyLogObserver", "ingore switch state: " + state);
                return;
            }
            Log.d(OppoService.TAG, "Desc: " + getIssueDesc(state));
            int ret = OppoManager.writeLogToPartition(state, name, MULTIMEDIA_TAG, getIssueCause(state), getIssueDesc(state));
            if (ret == -1) {
                Slog.v("MMKernelKeyLogObserver", "failed to OppoManager.writeLogToPartition");
            } else {
                Slog.v("MMKernelKeyLogObserver", "has write :" + ret + " bytes to critical log partition!");
            }
        }

        private void writeConnKeyLog(String name, int state) {
            Log.d(OppoService.TAG, "writeConnKeyLog, Desc: " + getIssueDesc(state));
            int ret = OppoManager.writeLogToPartition(state, name, "CONNECTIVITY", getIssueCause(state), getIssueDesc(state));
            if (ret == -1) {
                Slog.v(OppoService.TAG, "writeConnKeyLog, failed to OppoManager.writeLogToPartition");
            } else {
                Slog.v(OppoService.TAG, "writeConnKeyLog, has write :" + ret + " bytes to critical log partition!");
            }
        }
    }

    private class ZoneInfoDataFileController {
        private static final String TZ_DATA_FILE_DIRECTORY = "/data/misc/zoneinfo/";

        public boolean copyFile(String destPath, String srcPath) {
            return copyTzDataFile(destPath, srcPath);
        }

        public boolean deleteFile(String path) {
            return deleteTzDataFile(path);
        }

        private boolean deleteTzDataFile(String path) {
            Slog.d(OppoService.TAG, "deleteTzDataFile, delete path:" + path);
            if (TextUtils.isEmpty(path)) {
                Slog.w(OppoService.TAG, "deleteTzDataFile path is null!");
                return false;
            }
            File file = new File(path);
            if (!path.startsWith(TZ_DATA_FILE_DIRECTORY)) {
                Slog.w(OppoService.TAG, "delete this path is not allowed!");
                return false;
            } else if (!file.exists() || file.isDirectory()) {
                Slog.w(OppoService.TAG, "file is not exist or file is a directory!");
                return false;
            } else {
                try {
                    return file.delete();
                } catch (Exception e) {
                    Slog.e(OppoService.TAG, "deleteTzDataFile failed!");
                    return false;
                }
            }
        }

        private boolean copyTzDataFile(String destPath, String srcPath) {
            IOException e;
            Throwable th;
            Slog.d(OppoService.TAG, "copyTzDataFile, new path:" + destPath);
            if (TextUtils.isEmpty(destPath) || TextUtils.isEmpty(srcPath)) {
                Slog.w(OppoService.TAG, "copyTzDataFile path unavailable or input is null!");
                return false;
            } else if (destPath.startsWith(TZ_DATA_FILE_DIRECTORY)) {
                File inFile = new File(srcPath);
                if (!inFile.exists()) {
                    return false;
                }
                FileInputStream fileInputStream = null;
                File file = new File(destPath);
                FileOutputStream destFileOutputStream = null;
                byte[] outputBuffer = new byte[1024];
                try {
                    File parent = file.getParentFile();
                    if (parent.exists() || parent.mkdirs()) {
                        try {
                            Runtime.getRuntime().exec("chmod 777 " + parent.getAbsolutePath()).waitFor();
                            FileInputStream fileInputStream2 = new FileInputStream(inFile);
                            try {
                                FileOutputStream destFileOutputStream2 = new FileOutputStream(file);
                                while (true) {
                                    try {
                                        int len = fileInputStream2.read(outputBuffer);
                                        if (len == -1) {
                                            break;
                                        }
                                        destFileOutputStream2.write(outputBuffer, 0, len);
                                    } catch (IOException e2) {
                                        e = e2;
                                        destFileOutputStream = destFileOutputStream2;
                                        fileInputStream = fileInputStream2;
                                        try {
                                            Slog.e(OppoService.TAG, "write tzdata failed!" + e);
                                            if (destFileOutputStream != null) {
                                                try {
                                                    destFileOutputStream.close();
                                                } catch (IOException e3) {
                                                    Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e3);
                                                }
                                            }
                                            if (fileInputStream != null) {
                                                try {
                                                    fileInputStream.close();
                                                } catch (IOException e32) {
                                                    Slog.e(OppoService.TAG, "close fileInputStream error!" + e32);
                                                }
                                            }
                                            return false;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            if (destFileOutputStream != null) {
                                                try {
                                                    destFileOutputStream.close();
                                                } catch (IOException e322) {
                                                    Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e322);
                                                }
                                            }
                                            if (fileInputStream != null) {
                                                try {
                                                    fileInputStream.close();
                                                } catch (IOException e3222) {
                                                    Slog.e(OppoService.TAG, "close fileInputStream error!" + e3222);
                                                }
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        destFileOutputStream = destFileOutputStream2;
                                        fileInputStream = fileInputStream2;
                                        if (destFileOutputStream != null) {
                                            try {
                                                destFileOutputStream.close();
                                            } catch (IOException e32222) {
                                                Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e32222);
                                            }
                                        }
                                        if (fileInputStream != null) {
                                            try {
                                                fileInputStream.close();
                                            } catch (IOException e322222) {
                                                Slog.e(OppoService.TAG, "close fileInputStream error!" + e322222);
                                            }
                                        }
                                        throw th;
                                    }
                                }
                                destFileOutputStream2.flush();
                                FileUtils.sync(destFileOutputStream2);
                                Slog.d(OppoService.TAG, "write tzdata succeed!");
                                try {
                                    Runtime.getRuntime().exec("chmod 666 " + destPath).waitFor();
                                    if (destFileOutputStream2 != null) {
                                        try {
                                            destFileOutputStream2.close();
                                        } catch (IOException e3222222) {
                                            Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e3222222);
                                        }
                                    }
                                    if (fileInputStream2 != null) {
                                        try {
                                            fileInputStream2.close();
                                        } catch (IOException e32222222) {
                                            Slog.e(OppoService.TAG, "close fileInputStream error!" + e32222222);
                                        }
                                    }
                                    return true;
                                } catch (Exception e4) {
                                    Log.e(OppoService.TAG, "change tzdate file mode error!");
                                    if (destFileOutputStream2 != null) {
                                        try {
                                            destFileOutputStream2.close();
                                        } catch (IOException e322222222) {
                                            Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e322222222);
                                        }
                                    }
                                    if (fileInputStream2 != null) {
                                        try {
                                            fileInputStream2.close();
                                        } catch (IOException e3222222222) {
                                            Slog.e(OppoService.TAG, "close fileInputStream error!" + e3222222222);
                                        }
                                    }
                                    return false;
                                }
                            } catch (IOException e5) {
                                e3222222222 = e5;
                                fileInputStream = fileInputStream2;
                                Slog.e(OppoService.TAG, "write tzdata failed!" + e3222222222);
                                if (destFileOutputStream != null) {
                                    try {
                                        destFileOutputStream.close();
                                    } catch (IOException e32222222222) {
                                        Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e32222222222);
                                    }
                                }
                                if (fileInputStream != null) {
                                    try {
                                        fileInputStream.close();
                                    } catch (IOException e322222222222) {
                                        Slog.e(OppoService.TAG, "close fileInputStream error!" + e322222222222);
                                    }
                                }
                                return false;
                            } catch (Throwable th4) {
                                th = th4;
                                fileInputStream = fileInputStream2;
                                if (destFileOutputStream != null) {
                                    try {
                                        destFileOutputStream.close();
                                    } catch (IOException e3222222222222) {
                                        Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e3222222222222);
                                    }
                                }
                                if (fileInputStream != null) {
                                    try {
                                        fileInputStream.close();
                                    } catch (IOException e32222222222222) {
                                        Slog.e(OppoService.TAG, "close fileInputStream error!" + e32222222222222);
                                    }
                                }
                                throw th;
                            }
                        } catch (Exception e6) {
                            Log.e(OppoService.TAG, "change current file mode error!");
                            return false;
                        }
                    }
                    Slog.w(OppoService.TAG, "create parent path failed!");
                    return false;
                } catch (IOException e7) {
                    e32222222222222 = e7;
                    Slog.e(OppoService.TAG, "write tzdata failed!" + e32222222222222);
                    if (destFileOutputStream != null) {
                        try {
                            destFileOutputStream.close();
                        } catch (IOException e322222222222222) {
                            Slog.e(OppoService.TAG, "close destFileOutputStream error!" + e322222222222222);
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e3222222222222222) {
                            Slog.e(OppoService.TAG, "close fileInputStream error!" + e3222222222222222);
                        }
                    }
                    return false;
                }
            } else {
                Slog.w(OppoService.TAG, "copy to this path is not allowed!");
                return false;
            }
        }
    }

    private native void native_finalizeRawPartition();

    private native boolean native_initRawPartition();

    private native String native_readCriticalData(int i, int i2);

    private native String native_readRawPartition(int i, int i2);

    private native int native_writeCriticalData(int i, String str);

    private native int native_writeRawPartition(String str);

    private Boolean isNetworkOk() {
        return Boolean.valueOf(true);
    }

    private Boolean isSpaceOk() {
        StatFs fs = new StatFs("/data/system");
        return Boolean.valueOf(fs.getAvailableBlocksLong() * fs.getBlockSizeLong() > 262144000);
    }

    private void checkIfGrIn() {
        this.mIsGRIn = Boolean.valueOf(true);
        if (isSpaceOk().booleanValue()) {
            for (String fp : OppoManager.mGrApkPathList) {
                if (!new File(fp).exists()) {
                    this.mIsGRIn = Boolean.valueOf(false);
                    return;
                }
            }
            return;
        }
        this.mIsGRIn = Boolean.valueOf(false);
    }

    private void initGr() {
        String bitDesc = "";
        String densityDesc = "";
        String sdkVersion = "";
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
            bitDesc = "_arm64";
        }
        switch (Integer.valueOf(SystemProperties.getInt("ro.sf.lcd_density", 160)).intValue()) {
            case 240:
                densityDesc = "_hdpi";
                break;
            case 320:
                densityDesc = "_xhdpi";
                break;
            case SystemService.PHASE_LOCK_SETTINGS_READY /*480*/:
                densityDesc = "_xxhdpi";
                break;
            default:
                densityDesc = "_alldpi";
                break;
        }
        sdkVersion = LocationManagerService.OPPO_FAKE_LOCATION_SPLIT + SystemProperties.get("ro.build.version.sdk", "21");
        Resources rs = this.mContext.getResources();
        this.grTipTitle = rs.getString(17039990);
        this.grTipContent = rs.getString(17039987);
        this.grTipContentDown = rs.getString(17039988);
        this.grReinstallPTipContent = rs.getString(17039982);
        this.grTalkbackTipContent = rs.getString(17039986);
        this.grTalkbackExceptionContent = rs.getString(17039974);
        this.grOk = rs.getString(17039979);
        this.grCancel = rs.getString(17039967);
        this.grDownTipContent = rs.getString(17039970);
        this.grDownTipContentDown = rs.getString(17039971);
        this.grAbandon = rs.getString(17039966);
        this.grNeverRemind = rs.getString(17039976);
        this.grDoDown = rs.getString(17039968);
        this.grDoDownDown = rs.getString(17039969);
        this.grNetworkContent = rs.getString(17039975);
        this.grSpaceContent = rs.getString(17039983);
        this.grSuccDown = rs.getString(17039985);
        this.grExceptionContent = rs.getString(17039972);
        this.grExceptionContentDown = rs.getString(17039973);
        this.grSucc = rs.getString(17039984);
        this.grNotAccessTip = rs.getString(17039978);
        this.grOppoRoamTip = rs.getString(17039981);
        this.grNoOppoRoamTip = rs.getString(17039977);
        this.grOppoRoam = rs.getString(17039980);
        this.grTipInstalling = rs.getString(17039989);
        this.grFileName = "gr" + sdkVersion + bitDesc + densityDesc + ".zip";
        this.hasGrInit = Boolean.valueOf(true);
    }

    private void doTalkbackInstall(String baseCodePath, String appName, String pkgName) {
        Message msg = this.mHandler.obtainMessage(Integer.valueOf(10).intValue());
        Bundle bundle = new Bundle();
        if (appName != null) {
            bundle.putString("appName", appName);
        }
        if (baseCodePath != null) {
            bundle.putString("baseCodePath", baseCodePath);
        }
        if (pkgName != null) {
            bundle.putString("pkgName", pkgName);
        }
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    public void doGr(String baseCodePath, String appName, String pkgName, String action) {
        Message msg;
        Bundle bundle;
        checkIfGrIn();
        if (!this.hasGrInit.booleanValue()) {
            initGr();
        }
        Integer what = null;
        if (action != null) {
            if ("DO_GR_SHOW_EXCEPTION".equals(action)) {
                msg = this.mHandler.obtainMessage(Integer.valueOf(6).intValue());
                if (pkgName != null) {
                    bundle = new Bundle();
                    bundle.putString("exceptionType", pkgName);
                    msg.setData(bundle);
                }
                this.mHandler.sendMessage(msg);
                return;
            } else if ("DO_GR_SUCC".equals(action)) {
                msg = this.mHandler.obtainMessage(Integer.valueOf(7).intValue());
                bundle = new Bundle();
                if (appName != null) {
                    bundle.putString("appName", appName);
                }
                if (baseCodePath != null) {
                    bundle.putString("baseCodePath", baseCodePath);
                }
                if (pkgName != null) {
                    bundle.putString("pkgName", pkgName);
                }
                msg.setData(bundle);
                this.mHandler.sendMessage(msg);
                return;
            } else if ("DO_GR_CHECK_INTERNET".equals(action)) {
                msg = this.mHandler.obtainMessage(Integer.valueOf(9).intValue());
                bundle = new Bundle();
                if (baseCodePath != null) {
                    bundle.putString("isInChina", baseCodePath);
                }
                if (appName != null) {
                    bundle.putString("canSupportOppoRoam", appName);
                }
                msg.setData(bundle);
                this.mHandler.sendMessage(msg);
                return;
            }
        }
        if (action != null) {
            if ("DO_GR_INSTALL_TALKBACK".equals(action)) {
                Log.d(TAG, "doTalkbackInstall");
                doTalkbackInstall(baseCodePath, appName, pkgName);
            } else if (!OppoManager.canCreateDialog.booleanValue() || (OppoManager.canReinstall.booleanValue() ^ 1) != 0) {
            } else {
                if (OppoManager.isNoDialogInstalling.booleanValue()) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(OppoService.this.mContext, OppoService.this.grTipInstalling, 0).show();
                        }
                    });
                    Log.d(TAG, "checkIfNoDialogInstallingGr isNoDialogInstalling = " + OppoManager.isNoDialogInstalling);
                    return;
                }
                if ("DO_GR_DOWN_INSTALL".equals(action)) {
                    what = Integer.valueOf(4);
                } else if ("DO_GR_REINSTALL".equals(action)) {
                    what = Integer.valueOf(5);
                } else if ("DO_GR_EXIT".equals(action) && pkgName != null) {
                    what = Integer.valueOf(8);
                }
                if (what != null) {
                    msg = this.mHandler.obtainMessage(what.intValue());
                    bundle = new Bundle();
                    bundle.putString("baseCodePath", baseCodePath);
                    bundle.putString("appName", appName);
                    bundle.putString("pkgName", pkgName);
                    msg.setData(bundle);
                    this.mHandler.sendMessage(msg);
                }
            }
        }
    }

    private boolean isFactoryMode() {
        boolean result = false;
        TelephonyManager manager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (manager == null) {
            Log.e(TAG, "TelephonyManager service is not ready!");
            return false;
        }
        String imei = manager.getDeviceId();
        if (imei == null || (imei != null && VALUE_NOT_CHANGEOVER.equals(imei))) {
            result = true;
        }
        return result;
    }

    public OppoService(Context context) {
        this.mContext = context;
        if (OppoManager.willUseGrLeader.booleanValue() && (this.hasGrInit.booleanValue() ^ 1) != 0) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3));
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                    Uri data = intent.getData();
                    if (data != null) {
                        String pkgName = data.getSchemeSpecificPart();
                        if (OppoManager.isNeedLeader(pkgName).booleanValue() && OppoManager.canShowDialog(pkgName).booleanValue() && (OppoManager.grExists().booleanValue() ^ 1) != 0) {
                            if (OppoManager.DEBUG_GR) {
                                Log.d(OppoService.TAG, "Geloin: Will leader when installed " + pkgName);
                            }
                            OppoService.this.doGr(null, null, pkgName, "DO_GR_DOWN_INSTALL");
                        }
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(receiver, intentFilter);
        BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                try {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                        OppoService.this.hasGrInit = Boolean.valueOf(false);
                    }
                } catch (Exception e) {
                    Log.d(OppoService.TAG, "Geloin: Exception in mLocaleChangeReceiver.onReceive" + e);
                }
            }
        };
        IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction("android.intent.action.LOCALE_CHANGED");
        localeFilter.setPriority(1000);
        this.mContext.registerReceiver(mLocaleChangeReceiver, localeFilter);
        this.mMMKernelKeyLogObserver = new MMKernelKeyLogObserver();
        this.mMMKernelKeyLogObserver.init();
        this.mFallingMonitor = new OppoFallingMonitor(this.mContext);
        this.mNetWakeManager = new NetWakeManager(context);
        this.mNetWakeManager.CoverObservse_init();
        RegisterXmlUpdate(this.mContext);
        SyncCacheToEmmcTimmer();
        this.mLogService = new OppoLogService(this.mContext);
        if (SystemProperties.getBoolean("persist.sys.assert.panic", false)) {
            startSensorLog(true);
            try {
                if (this.mContext.getPackageManager().getPackageInfo("com.oppo.stethoscope", 0) != null) {
                    Slog.v(TAG, "has stethoscope");
                    startLogSizeMonitor();
                }
            } catch (Exception e) {
                Slog.v(TAG, "get stethoscope error: " + e.toString());
            }
        }
        this.mBTBRecordService = new BTBRecordService(context);
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void recordCriticalEvent(int msg, int pid, String log) {
        switch (msg) {
            case 1004:
                OppoManager.writeLogToPartition(OppoManager.TYPE_ANDROID_INPUTMETHOD_FAIL, log, "ANDROID", "inputmethod_fail", this.mContext.getString(17040989));
                return;
            default:
                return;
        }
    }

    public String readRawPartition(int offset, int size) {
        return native_readRawPartition(offset, size);
    }

    public int writeRawPartition(String content) {
        return native_writeRawPartition(content);
    }

    public String readCriticalData(int id, int size) {
        return native_readCriticalData(id, size);
    }

    public int writeCriticalData(int id, String content) {
        return native_writeCriticalData(id, content);
    }

    void recordSubSystemCrash(String name) {
        try {
            Slog.v(TAG, "recordSubSystemCrash name = " + name);
            name = name.trim();
            String[] logSplit = name.split(":");
            if (logSplit.length != 3) {
                Log.v(TAG, "log is unknown :" + name);
            } else if (logSplit[2].contains("modem")) {
                OppoManager.writeLogToPartition(OppoManager.TYPE_MODERN, name, "ANDROID", "modem_crash", this.mContext.getString(17040992));
            } else if (logSplit[2].contains("adsp")) {
                OppoManager.writeLogToPartition(OppoManager.TYPE_ANDROID_ADSP_CRASH, name, "ANDROID", "adsp_crash", this.mContext.getString(17040987));
            } else if (logSplit[2].contains("venus")) {
                OppoManager.writeLogToPartition(OppoManager.TYPE_ANDROID_VENUS_CRASH, name, "ANDROID", "venus_crash", this.mContext.getString(17041004));
            } else if (logSplit[2].contains("wcn")) {
                OppoManager.writeLogToPartition(OppoManager.TYPE_ANDROID_WCN_CRASH, name, "ANDROID", "wcn_crash", this.mContext.getString(17041005));
            } else {
                Log.v(TAG, "record subSystem crash unknown tag :" + name);
            }
        } catch (Exception e) {
            Log.v(TAG, "record subSystem crash error e = " + e.toString());
        }
    }

    void startFallingMonitor() {
        if (this.mFallingMonitor != null) {
            this.mFallingMonitor.startMonitor();
        }
    }

    void stopFallingMonitor() {
        if (this.mFallingMonitor != null) {
            this.mFallingMonitor.stopMonitor();
        }
    }

    void RegisterXmlUpdate(Context c) {
        this.mXmlHelper = new CriticalLogConfigUpdateHelper(c, FILTER_NAME, SYS_FILE_DIR, DATA_FILE_DIR);
        this.mXmlHelper.init();
        this.mXmlHelper.initUpdateBroadcastReceiver();
        this.mXmlHelper.initFallingMonitor();
    }

    void SyncCacheToEmmcTimmer() {
        Log.v(TAG, "syncCacheToEmmc , start timmer sync ");
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                Log.v(OppoService.TAG, "syncCacheToEmmc , timmer sync ");
                OppoManager.syncCacheToEmmc();
                OppoService.this.mHandler.postDelayed(this, 36000000);
            }
        }, 36000000);
    }

    public void systemReady() {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                Log.v(OppoService.TAG, "systemReady initLogCoreService");
                if (SystemProperties.getBoolean("persist.sys.assert.panic", false)) {
                    OppoService.this.mLogService.initLogCoreService();
                }
            }
        }, 20000);
    }

    public String getOppoLogInfoString(int index) {
        if (Binder.getCallingUid() != 1000) {
            return null;
        }
        return this.mLogService.getOppoLogInfoString(index);
    }

    public void deleteSystemLogFile() {
        if (Binder.getCallingUid() == 1000) {
            this.mLogService.deleteSystemLogFile();
        }
    }

    public boolean iScoreLogServiceRunning() {
        if (this.mLogService == null) {
            return false;
        }
        boolean result = this.mLogService.isLogCoreServiceRunning();
        Log.v(TAG, "LogCoreService Running : " + result);
        return result;
    }

    public void StartLogCoreService() {
        Log.v(TAG, "StartLogCoreService : " + this.mLogService);
        if (this.mLogService == null) {
            this.mLogService = new OppoLogService(this.mContext);
        }
        this.mLogService.initLogCoreService();
    }

    public void unbindCoreLogService() {
        this.mLogService.unbindService();
    }

    public void startSensorLog(boolean isOutPutFile) {
        this.mLogService.startSensorLog(isOutPutFile);
    }

    public void stopSensorLog() {
        this.mLogService.stopSensorLog();
    }

    public void startLogSizeMonitor() {
        this.mLogService.startLogSizeMonitor();
    }

    public void stopLogSizeMonitor() {
        this.mLogService.stopLogSizeMonitor();
    }

    private boolean isChangeOver() {
        String value = Secure.getString(this.mContext.getContentResolver(), KEY_SETTINGS_CHANGEOVER);
        return value != null ? value.equals("1") : false;
    }

    public boolean openFlashLight() {
        return getFlashLightControler().openFlashLightImpl();
    }

    public boolean closeFlashLight() {
        return getFlashLightControler().closeFlashLightImpl();
    }

    public String getFlashLightState() {
        return getFlashLightControler().getFlashLightStateImpl();
    }

    private FlashLightControler getFlashLightControler() {
        if (this.mFlashLightControler == null) {
            this.mFlashLightControler = new FlashLightControler();
        }
        return this.mFlashLightControler;
    }

    public void iotop() {
    }

    public boolean copyFile(String destPath, String srcPath) {
        if (this.mContext == null) {
            return false;
        }
        this.mContext.enforceCallingPermission("oppo.permission.OPPO_COMPONENT_SAFE", "copyFile");
        return getZoneInfoFileControler().copyFile(destPath, srcPath);
    }

    public boolean deleteFile(String path) {
        if (this.mContext == null) {
            return false;
        }
        this.mContext.enforceCallingPermission("oppo.permission.OPPO_COMPONENT_SAFE", "deleteFile");
        return getZoneInfoFileControler().deleteFile(path);
    }

    private ZoneInfoDataFileController getZoneInfoFileControler() {
        if (this.mZoneInfoDataFileController == null) {
            this.mZoneInfoDataFileController = new ZoneInfoDataFileController();
        }
        return this.mZoneInfoDataFileController;
    }
}
