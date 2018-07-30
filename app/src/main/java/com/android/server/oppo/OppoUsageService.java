package com.android.server.oppo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.engineer.OppoEngineerManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IOppoUsageService.Stub;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.ColorOSTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Base64;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import oppo.util.OppoStatistics;

public final class OppoUsageService extends Stub {
    private static final int DATA_TYPE_APK_DEL_EVENT = 22;
    private static final int DATA_TYPE_APK_INSTALL_EVENT = 23;
    private static final int DATA_TYPE_APP_USAGE = 9;
    private static final int DATA_TYPE_BOOT_TIME = 1;
    private static final int DATA_TYPE_DIAL_OUT_DURATION = 6;
    private static final int DATA_TYPE_IMEI_NO = 2;
    private static final int DATA_TYPE_INCOMING_DURATION = 7;
    private static final int DATA_TYPE_MAX = 25;
    private static final int DATA_TYPE_MAX_CHARGE_CURRENT_CONFIG = 17;
    private static final int DATA_TYPE_MAX_CHARGE_TEMPERATURE_CONFIG = 19;
    private static final int DATA_TYPE_MCS_CONNECTID = 24;
    private static final int DATA_TYPE_MIN_CHARGE_TEMPERATURE_CONFIG = 18;
    private static final int DATA_TYPE_MOS_CONFIG = 16;
    private static final int DATA_TYPE_MSG_RECEIVE = 5;
    private static final int DATA_TYPE_MSG_SEND = 4;
    private static final int DATA_TYPE_ORIGINAL_SIM_DATA = 25;
    private static final int DATA_TYPE_PCBA_NO = 3;
    private static final int DATA_TYPE_PHONE_CALL_RECORD = 8;
    private static final int DATA_TYPE_PRODUCTLINE_LAST_TEST_FLAG = 21;
    private static final int DATA_TYPE_SECRECY_CONFIG = 20;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_D = false;
    private static final boolean DEBUG_E = false;
    private static final boolean DEBUG_I = false;
    private static boolean DEBUG_SCORE_M = false;
    private static final boolean DEBUG_W = false;
    private static final int MAX_BATCH_COUNT = 10;
    private static final int MSG_GET_IMEI_NO = 1;
    private static final int MSG_GET_PCBA_NO = 2;
    private static final int MSG_SAVE_APK_INSTALL = 4;
    private static final int MSG_SAVE_BOOT_TIME = 3;
    private static final int MSG_SAVE_EMMC_INFO = 5;
    private static final int NORMAL_MSG_DELAY = 10000;
    private static final String PROP_NAME_PCBA_NO = "gsm.serial";
    private static final String TAG = "OppoUsageService";
    private static final String mConnectorForPkgNameAndTime = "|";
    private Context mContext = null;
    private int mCurrentCountOfReceivedMsg = 0;
    private int mCurrentCountOfSendedMsg = 0;
    private int mCurrentDialOutDuration = 0;
    private int mCurrentIncomingDuration = 0;
    private String mCurrentPcbaNO = null;
    private SimCardData mCurrentSimCardData = null;
    private EmmcUsageCollector mEmmcInfoCollector = null;
    private int mGetImeiNORetry = 7;
    private int mGetPcbaNORetry = 7;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            OppoUsageService oppoUsageService;
            switch (msg.what) {
                case 1:
                    if (OppoUsageService.this.mGetImeiNORetry != 0) {
                        if (!OppoUsageService.this.getImeiNoFromPhone()) {
                            oppoUsageService = OppoUsageService.this;
                            oppoUsageService.mGetImeiNORetry = oppoUsageService.mGetImeiNORetry - 1;
                            sendMessageDelayed(obtainMessage(1), 10000);
                            break;
                        }
                        OppoUsageService.this.mGetImeiNORetry = 0;
                        OppoUsageService.this.saveCurrentSimCardData();
                        sendMessageDelayed(obtainMessage(2), 10000);
                        break;
                    }
                    sendMessageDelayed(obtainMessage(2), 10000);
                    return;
                case 2:
                    if (OppoUsageService.this.mGetPcbaNORetry != 0) {
                        if (!OppoUsageService.this.getPcbaNoFromPhone()) {
                            oppoUsageService = OppoUsageService.this;
                            oppoUsageService.mGetPcbaNORetry = oppoUsageService.mGetPcbaNORetry - 1;
                            sendMessageDelayed(obtainMessage(2), 10000);
                            break;
                        }
                        OppoUsageService.this.mGetPcbaNORetry = 0;
                        OppoUsageService.this.savePcbaNoIfNew(OppoUsageService.this.mCurrentPcbaNO);
                        break;
                    }
                    return;
                case 3:
                    OppoUsageService.this.saveCurrentBootTime("startUp:" + OppoUsageService.this.getCurrentDateStr());
                    break;
                case 4:
                    String pkgName = msg.obj;
                    if (pkgName != null) {
                        OppoUsageService.this.saveApkInstallEvent(pkgName);
                        break;
                    }
                    break;
                case 5:
                    Slog.d(OppoUsageService.TAG, "msg to readEmmcInfoLable..");
                    Slog.d(OppoUsageService.TAG, "emmc info:" + OppoUsageService.this.mEmmcInfoCollector.readEmmcInfoLable(false));
                    break;
            }
        }
    };
    private boolean mHasGotDialOutDuration = false;
    private boolean mHasGotHistoryCountOfReceivedMsg = false;
    private boolean mHasGotHistoryCountOfSendedMsg = false;
    private boolean mHasGotIncomingDuration = false;
    private IntergrateReserveManager mIntergrateReserveManager = null;
    private Kahaleesi mKahaleesi = null;
    private BroadcastReceiver mPkgMsgReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                Uri data = intent.getData();
                if (data != null) {
                    String pkgName = data.getSchemeSpecificPart();
                    if (pkgName != null) {
                        Message apkInstallMsg = OppoUsageService.this.mHandler.obtainMessage(4);
                        apkInstallMsg.obj = pkgName;
                        OppoUsageService.this.mHandler.sendMessageDelayed(apkInstallMsg, 20);
                    }
                }
            }
        }
    };
    private boolean mRawPartionInitOk = false;
    private String mRecordStrSlitter = Pattern.quote("#");
    private ScoreMonitor mScoreMonitor = null;
    private Time mTimeObj = new Time();

    private class UsageDataRecorder {
        protected Context mContext = null;
        protected Kahaleesi mLocalKahaleesi = null;
        protected File mUsageCacheFileDir = null;
        protected File mUsagePersistFileDir = null;

        public UsageDataRecorder(Context context, Kahaleesi localKahaleesi) {
            this.mContext = context;
            this.mLocalKahaleesi = localKahaleesi;
            this.mUsageCacheFileDir = new File("/opporeserve/media/log/usage/cache");
            this.mUsagePersistFileDir = new File("/opporeserve/media/log/usage/persist");
        }

        public boolean saveDataToFile() {
            return false;
        }

        public ArrayList<String> loadHistoryDataFromFile(File srcFile) {
            IOException ioe;
            FileInputStream fileInputStream;
            if (srcFile == null || (srcFile.exists() ^ 1) != 0) {
                return null;
            }
            BufferedReader inputBuffReader = null;
            ArrayList<String> tmpStrList = new ArrayList();
            try {
                FileInputStream srcFileIpStream = new FileInputStream(srcFile);
                try {
                    InputStreamReader inputStrReader = new InputStreamReader(srcFileIpStream);
                    try {
                        BufferedReader inputBuffReader2 = new BufferedReader(inputStrReader);
                        while (true) {
                            try {
                                String readStr = inputBuffReader2.readLine();
                                if (readStr == null) {
                                    break;
                                } else if (!readStr.isEmpty()) {
                                    tmpStrList.add(this.mLocalKahaleesi.unfrozenFromFire(readStr));
                                }
                            } catch (IOException e) {
                                ioe = e;
                                inputBuffReader = inputBuffReader2;
                                fileInputStream = srcFileIpStream;
                                Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream IO failed.", ioe);
                                if (inputBuffReader != null) {
                                    try {
                                        inputBuffReader.close();
                                    } catch (IOException ioclose) {
                                        Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream inputBuffReader close failed.", ioclose);
                                    }
                                }
                                return tmpStrList;
                            }
                        }
                        inputBuffReader2.close();
                    } catch (IOException e2) {
                        ioe = e2;
                        fileInputStream = srcFileIpStream;
                        Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream IO failed.", ioe);
                        if (inputBuffReader != null) {
                            try {
                                inputBuffReader.close();
                            } catch (IOException ioclose2) {
                                Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream inputBuffReader close failed.", ioclose2);
                            }
                        }
                        return tmpStrList;
                    }
                } catch (IOException e3) {
                    ioe = e3;
                    Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream IO failed.", ioe);
                    if (inputBuffReader != null) {
                        try {
                            inputBuffReader.close();
                        } catch (IOException ioclose22) {
                            Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream inputBuffReader close failed.", ioclose22);
                        }
                    }
                    return tmpStrList;
                }
            } catch (IOException e4) {
                ioe = e4;
                Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream IO failed.", ioe);
                if (inputBuffReader != null) {
                    try {
                        inputBuffReader.close();
                    } catch (IOException ioclose222) {
                        Slog.w(OppoUsageService.TAG, "sd orgSrcFileIpStream inputBuffReader close failed.", ioclose222);
                    }
                }
                return tmpStrList;
            }
            return tmpStrList;
        }

        protected boolean doSaveData(ArrayList<String> contentList, File destFile, boolean append, boolean frozen) {
            if (contentList == null || contentList.size() <= 0) {
                return false;
            }
            if (destFile == null || (destFile.exists() ^ 1) != 0) {
                return false;
            }
            BufferedWriter destBuffWriter = null;
            try {
                BufferedWriter destBuffWriter2;
                FileWriter destFileWriter = new FileWriter(destFile, append);
                try {
                    destBuffWriter2 = new BufferedWriter(destFileWriter);
                } catch (IOException e) {
                    Slog.w(OppoUsageService.TAG, "sd destFileOpStream IO failed.");
                    if (destBuffWriter != null) {
                        try {
                            destBuffWriter.close();
                        } catch (IOException e2) {
                            Slog.w(OppoUsageService.TAG, "destFileOpStream close failed.");
                        }
                    }
                    return false;
                }
                try {
                    for (String contentStr : contentList) {
                        if (!(contentStr == null || (contentStr.isEmpty() ^ 1) == 0)) {
                            destBuffWriter2.write(frozen ? this.mLocalKahaleesi.frozenIntoIce(contentStr) : contentStr);
                            destBuffWriter2.newLine();
                        }
                    }
                    destBuffWriter2.flush();
                    destBuffWriter2.close();
                    return true;
                } catch (IOException e3) {
                    destBuffWriter = destBuffWriter2;
                    FileWriter fileWriter = destFileWriter;
                    Slog.w(OppoUsageService.TAG, "sd destFileOpStream IO failed.");
                    if (destBuffWriter != null) {
                        try {
                            destBuffWriter.close();
                        } catch (IOException e22) {
                            Slog.w(OppoUsageService.TAG, "destFileOpStream close failed.");
                        }
                    }
                    return false;
                }
            } catch (IOException e4) {
                Slog.w(OppoUsageService.TAG, "sd destFileOpStream IO failed.");
                if (destBuffWriter != null) {
                    try {
                        destBuffWriter.close();
                    } catch (IOException e222) {
                        Slog.w(OppoUsageService.TAG, "destFileOpStream close failed.");
                    }
                }
                return false;
            }
        }
    }

    private class CacheRecordsRecorder extends UsageDataRecorder {
        private static final boolean DBG_SD = false;
        private static final String LOGTAG = "CacheRecordsRecorder";
        private static final long MAX_LEN_FOR_ONE_FILE = 2097152;
        private File mCurHistoryFile = null;
        protected String mFileName = null;
        private File mPreHistoryFile = null;

        public CacheRecordsRecorder(Context context, Kahaleesi localKahaleesi, String fileName) {
            super(context, localKahaleesi);
            if (fileName == null || (fileName.isEmpty() ^ 1) == 0) {
                fileName = "default";
            }
            this.mFileName = fileName;
            this.mCurHistoryFile = new File(this.mUsageCacheFileDir, this.mFileName + ".dat");
            this.mPreHistoryFile = new File(this.mUsageCacheFileDir, this.mFileName + "-pre.dat");
        }

        public boolean saveContentList(ArrayList<String> contentList) {
            if (contentList == null || contentList.size() <= 0) {
                return false;
            }
            if (!this.mCurHistoryFile.exists()) {
                try {
                    this.mCurHistoryFile.createNewFile();
                } catch (IOException ioe) {
                    Slog.e(LOGTAG, "create history file failed!", ioe);
                    return false;
                }
            } else if (this.mCurHistoryFile.length() > MAX_LEN_FOR_ONE_FILE && clearOldDataFile()) {
                this.mCurHistoryFile = new File(this.mUsageCacheFileDir, this.mFileName + ".dat");
                this.mPreHistoryFile = new File(this.mUsageCacheFileDir, this.mFileName + "-pre.dat");
            }
            return doSaveData(contentList, this.mCurHistoryFile, true, true);
        }

        private boolean clearOldDataFile() {
            if (this.mPreHistoryFile.exists()) {
                this.mPreHistoryFile.delete();
            }
            return this.mCurHistoryFile.renameTo(this.mPreHistoryFile);
        }

        public List<String> getCurHistoryFileInfoList() {
            if (this.mCurHistoryFile.exists()) {
                return loadHistoryDataFromFile(this.mCurHistoryFile);
            }
            return null;
        }

        public List<String> getPreHistoryFileInfoList() {
            if (this.mPreHistoryFile.exists()) {
                return loadHistoryDataFromFile(this.mPreHistoryFile);
            }
            return null;
        }
    }

    private class EmmcUsageCollector {
        private static final String EMMC_INFO_FILE_SUFFIX = ".txt";
        private static final String PATH_EMMC_INFO_FILE = "/data/system/dropbox/emmcInfo-";
        private static final int READ_COUNT = 256;
        private static final int READ_OFFSET_INTERGRATE = 4300800;
        private static final int READ_OFFSET_UNINTERGRATE = 15729664;
        private static final boolean SAVE_AS_FILE = false;
        private String mCurrentEmmcInfo = null;
        boolean mDebugEmmcInfo = false;
        private boolean mIsUserDebugVersion = false;
        private Context mLocalContext = null;

        public EmmcUsageCollector(Context context) {
            this.mLocalContext = context;
            this.mIsUserDebugVersion = "userdebug".equals(SystemProperties.get("ro.build.type"));
        }

        public boolean readEmmcInfoLable(boolean ignoreVersion) {
            if (ignoreVersion || !this.mIsUserDebugVersion) {
                return getEmmcInfoLableImplIntergrate();
            }
            return false;
        }

        private boolean getEmmcInfoLableImplIntergrate() {
            return encodeAndSend(OppoEngineerManager.getEmmcHealthInfo());
        }

        private boolean encodeAndSend(byte[] emmcInfoByteArray) {
            if (emmcInfoByteArray == null || emmcInfoByteArray.length <= 0) {
                Slog.e(OppoUsageService.TAG, "readEmmcInfoLable failed, res empty.");
                return false;
            }
            int maxLen = emmcInfoByteArray.length >= 256 ? 256 : emmcInfoByteArray.length;
            if (this.mDebugEmmcInfo) {
                Slog.d(OppoUsageService.TAG, "readEmmcInfoLable, beging decode");
            }
            String resInBase64 = new String(Base64.encode(emmcInfoByteArray, 0, maxLen, 2));
            if (this.mDebugEmmcInfo) {
                Slog.d(OppoUsageService.TAG, "readEmmcInfoLable, after resInBase64:" + resInBase64);
            }
            sendDcsMsg(resInBase64);
            if (this.mDebugEmmcInfo) {
                byte[] decodeByteArray = Base64.decode(resInBase64, 2);
                StringBuilder sb = new StringBuilder();
                int sbCount = 0;
                for (byte b : decodeByteArray) {
                    sb.append(Integer.toHexString(b & 255));
                    sbCount++;
                    if (sbCount == 10) {
                        Slog.d(OppoUsageService.TAG, "readEmmcInfoLable, decode:" + sb.toString());
                        sb = new StringBuilder();
                        sbCount = 0;
                    }
                }
                Slog.d(OppoUsageService.TAG, "readEmmcInfoLable, decode at last:" + sb.toString());
            }
            if (this.mDebugEmmcInfo) {
                Slog.d(OppoUsageService.TAG, "readEmmcInfoLable:01");
            }
            return true;
        }

        private String getCurrentDateStrForEmmcFile() {
            OppoUsageService.this.mTimeObj.setToNow();
            return OppoUsageService.this.mTimeObj.format("%Y-%m-%d-%H-%M-%S");
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private synchronized void sendDcsMsg(String strMsg) {
            if (strMsg != null) {
                if (!strMsg.isEmpty()) {
                    try {
                        Map<String, String> logMap = new HashMap();
                        logMap.put("EmmcInfo", strMsg);
                        OppoStatistics.onCommon(this.mLocalContext, "EmmcInfo", "EmmcInfoID", logMap, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
    }

    private class IntergrateReserveManager {
        private static final String APP_DEL_FILE_NAME = "apd";
        private static final String APP_INSTALL_FILE_NAME = "api";
        private static final String APP_USAGE_FILE_NAME = "apu";
        private static final String BOOT_TIME_FILE_NAME = "bt";
        public static final boolean DBG_ITRM = false;
        public static final int ERRO_NO_ITEGRATE = -10;
        public static final String ITRM_TAG = "IntergrateReserveManager";
        public static final String PROP_STR_ITRM = "ro.sys.reserve.integrate";
        public static final String STR_RESERVE_DIR = "/opporeserve/media/log/usage/";
        public static final String STR_USAGE_CACHE = "cache";
        public static final String STR_USAGE_PERSIST = "persist";
        private static final String TEL_CALL_FILE_NAME = "tc";
        private CacheRecordsRecorder mAppDeleteRecorder = null;
        private CacheRecordsRecorder mAppInstallRecorder = null;
        private CacheRecordsRecorder mAppUsageRecorder = null;
        private CacheRecordsRecorder mBootTimeRecorder = null;
        private CacheRecordsRecorder mComInfoRecorder = null;
        private Context mLocalContext = null;
        private Kahaleesi mLocalKahaleesi = null;
        private McsDataRecorder mMcsDataRecorder = null;
        private PcbaDataRecorder mPcbaDataRecorder = null;
        private SimCardDataRecorder mSimCardDataRecorder = null;
        private StatsticDataRecorder mStatsticDataRecorder = null;
        private boolean mSupportIntergrateReserve = false;

        public IntergrateReserveManager(Context context, Kahaleesi kahaleesi) {
            this.mLocalContext = context;
            this.mLocalKahaleesi = kahaleesi;
            this.mSupportIntergrateReserve = SystemProperties.getBoolean(PROP_STR_ITRM, false);
            if (this.mSupportIntergrateReserve) {
                File persistFileDir = new File("/opporeserve/media/log/usage/persist");
                if (!persistFileDir.exists()) {
                    boolean mkdir = persistFileDir.mkdir();
                }
                File cacheFileDir = new File("/opporeserve/media/log/usage/cache");
                if (!cacheFileDir.exists()) {
                    boolean mkdir2 = cacheFileDir.mkdir();
                }
                this.mSimCardDataRecorder = new SimCardDataRecorder(this.mLocalContext, this.mLocalKahaleesi);
                this.mPcbaDataRecorder = new PcbaDataRecorder(this.mLocalContext, this.mLocalKahaleesi);
                this.mMcsDataRecorder = new McsDataRecorder(this.mLocalContext, this.mLocalKahaleesi);
                this.mStatsticDataRecorder = new StatsticDataRecorder(this.mLocalContext, this.mLocalKahaleesi);
                this.mAppUsageRecorder = new CacheRecordsRecorder(this.mLocalContext, this.mLocalKahaleesi, APP_USAGE_FILE_NAME);
                this.mAppInstallRecorder = new CacheRecordsRecorder(this.mLocalContext, this.mLocalKahaleesi, APP_INSTALL_FILE_NAME);
                this.mAppDeleteRecorder = new CacheRecordsRecorder(this.mLocalContext, this.mLocalKahaleesi, APP_DEL_FILE_NAME);
                this.mComInfoRecorder = new CacheRecordsRecorder(this.mLocalContext, this.mLocalKahaleesi, TEL_CALL_FILE_NAME);
                this.mBootTimeRecorder = new CacheRecordsRecorder(this.mLocalContext, this.mLocalKahaleesi, BOOT_TIME_FILE_NAME);
            }
        }

        public void setKahaleesi(Kahaleesi kahaleesi) {
            this.mLocalKahaleesi = kahaleesi;
        }

        public boolean isSupportIntergrateReserve() {
            return this.mSupportIntergrateReserve;
        }

        public SimCardDataRecorder getSimCardDataRecorder() {
            return this.mSimCardDataRecorder;
        }

        public PcbaDataRecorder getPcbaDataRecorder() {
            return this.mPcbaDataRecorder;
        }

        public McsDataRecorder getMcsDataRecorder() {
            return this.mMcsDataRecorder;
        }

        public CacheRecordsRecorder getAppUsageRecorder() {
            return this.mAppUsageRecorder;
        }

        public CacheRecordsRecorder getAppInstallRecorder() {
            return this.mAppInstallRecorder;
        }

        public CacheRecordsRecorder getAppUninstallRecorder() {
            return this.mAppDeleteRecorder;
        }

        public CacheRecordsRecorder getComInfoRecorder() {
            return this.mComInfoRecorder;
        }

        public CacheRecordsRecorder getBootTimeRecorder() {
            return this.mBootTimeRecorder;
        }

        public StatsticDataRecorder getStaticDataRecorder() {
            return this.mStatsticDataRecorder;
        }
    }

    private class Kahaleesi {
        private static final String KEY_ALGORITHM = "DES";
        private static final String KEY_STRORE_OF_K = "AndroidKeyStore";
        private byte[] mIv = new byte[]{(byte) 6, (byte) 5, (byte) 6, (byte) 5, (byte) 7, (byte) 3, (byte) 6, (byte) 9};
        private String mNameOfKahaleesi = "Kahalees";
        private Cipher mPowerOfFire = null;
        private Cipher mPowerOfIce = null;
        private boolean mfrozen = false;

        public Kahaleesi() {
            try {
                IvParameterSpec zeroIv = new IvParameterSpec(this.mIv);
                SecretKey scKey = new SecretKeySpec(this.mNameOfKahaleesi.getBytes(), KEY_ALGORITHM);
                this.mPowerOfIce = Cipher.getInstance(KEY_ALGORITHM);
                this.mPowerOfIce.init(1, scKey, zeroIv);
                this.mPowerOfFire = Cipher.getInstance(KEY_ALGORITHM);
                this.mPowerOfFire.init(2, scKey, zeroIv);
            } catch (Exception e) {
                Slog.e(OppoUsageService.TAG, "Failed to encrypt key", e);
                this.mfrozen = false;
            }
        }

        public String frozenIntoIce(String sword) {
            if (!this.mfrozen || sword == null || sword.isEmpty()) {
                return sword;
            }
            try {
                return StringFactory.newStringFromBytes(this.mPowerOfIce.doFinal(sword.getBytes()));
            } catch (Exception e) {
                Slog.w(OppoUsageService.TAG, "Failed frozenIntoIce ", e);
                return sword;
            }
        }

        public String unfrozenFromFire(String ice) {
            if (!this.mfrozen || ice == null || ice.isEmpty()) {
                return ice;
            }
            try {
                return StringFactory.newStringFromBytes(this.mPowerOfFire.doFinal(ice.getBytes()));
            } catch (Exception e) {
                Slog.w(OppoUsageService.TAG, "Failed unfrozenFromFire ", e);
                return ice;
            }
        }
    }

    private class McsDataRecorder extends UsageDataRecorder {
        private static final boolean DBG_SD = false;
        private static final String LOGTAG = "McsDataRecorder";
        private static final String STR_USAGE_MCS = "mcs.data";
        private File mHistoryFile;

        public McsDataRecorder(Context context, Kahaleesi localKahaleesi) {
            super(context, localKahaleesi);
            this.mHistoryFile = null;
            this.mHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_MCS);
        }

        public boolean saveMcsInfo(String numberStr) {
            if (numberStr == null || numberStr.isEmpty()) {
                return false;
            }
            if (!this.mHistoryFile.exists()) {
                try {
                    this.mHistoryFile.createNewFile();
                } catch (IOException e) {
                    Slog.e(LOGTAG, "create history file failed!");
                    return false;
                }
            }
            ArrayList<String> saveStrList = new ArrayList();
            saveStrList.add(numberStr);
            return doSaveData(saveStrList, this.mHistoryFile, false, true);
        }

        public String getMcsInfo() {
            if (!this.mHistoryFile.exists()) {
                return null;
            }
            ArrayList<String> resStrList = loadHistoryDataFromFile(this.mHistoryFile);
            if (resStrList == null || resStrList.size() <= 0) {
                return null;
            }
            return (String) resStrList.get(0);
        }
    }

    private class PcbaDataRecorder extends UsageDataRecorder {
        private static final boolean DBG_SD = false;
        private static final String LOGTAG = "PcbaDataRecorder";
        private static final String STR_USAGE_PCBA_HISTORY = "pb.data";
        private File mHistoryFile;

        public PcbaDataRecorder(Context context, Kahaleesi localKahaleesi) {
            super(context, localKahaleesi);
            this.mHistoryFile = null;
            this.mHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_PCBA_HISTORY);
        }

        public boolean savePcbaInfo(String numberStr) {
            if (numberStr == null || numberStr.isEmpty()) {
                return false;
            }
            if (!this.mHistoryFile.exists()) {
                try {
                    this.mHistoryFile.createNewFile();
                } catch (IOException e) {
                    Slog.e(LOGTAG, "create history file failed!");
                    return false;
                }
            }
            ArrayList<String> curHistoryList = loadHistoryDataFromFile(this.mHistoryFile);
            boolean isTheSame = false;
            if (curHistoryList != null) {
                for (String historyStr : curHistoryList) {
                    if (numberStr.equals(historyStr)) {
                        isTheSame = true;
                        break;
                    }
                }
            }
            if (isTheSame) {
                return false;
            }
            ArrayList<String> saveStrList = new ArrayList();
            saveStrList.add(numberStr);
            return doSaveData(saveStrList, this.mHistoryFile, true, true);
        }

        public ArrayList<String> getHistoryPcbaInfoList() {
            if (this.mHistoryFile.exists()) {
                return loadHistoryDataFromFile(this.mHistoryFile);
            }
            return null;
        }
    }

    private class ScoreMonitor {
        private static final String ACTION_MONITOR_TIMER = "com.oppo.ScoreAppMonitor.MONITOR_TIMER";
        private static final String BROADCAST_ACTION_INFO_UPLOAD = "com.oppo.ScoreAppMonitor.UPLOAD";
        private static final int DEFAULT_START_TIME = 17;
        private static final String EXTRA_KEY_FILE_NAME = "filename";
        private static final String PERMISSION_START_MONITOR = "com.oppo.ScoreAppMonitor.permission.START_MONITOR";
        private static final String PROP_CONTROL_VERSION = "persist.version.confidential";
        private static final String SCORE_MONITOR_PACKAGE = "com.oppo.ScoreAppMonitor";
        private static final String SCORE_MONITOR_SERVICE = "com.oppo.ScoreAppMonitor.MonitorService";
        private AlarmManager mAlarmManager = null;
        private boolean mIsControlVersion = false;
        private Context mLocalContext = null;
        private BroadcastReceiver mScoreMonitorReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (OppoUsageService.DEBUG_SCORE_M) {
                    Slog.d(OppoUsageService.TAG, "mScoreMonitorReceiver onReceive:" + action);
                }
                if (action != null) {
                    if (action.equals(ScoreMonitor.BROADCAST_ACTION_INFO_UPLOAD)) {
                        String uploadFileName = intent.getStringExtra(ScoreMonitor.EXTRA_KEY_FILE_NAME);
                        if (OppoUsageService.DEBUG_SCORE_M) {
                            Slog.d(OppoUsageService.TAG, "mScoreMonitorReceiver onReceive:" + uploadFileName);
                        }
                        if (uploadFileName != null) {
                            ScoreMonitor.this.onFileUpload(uploadFileName);
                        }
                    } else if (action.equals(ScoreMonitor.ACTION_MONITOR_TIMER)) {
                        ScoreMonitor.this.startScoreMonitor();
                    }
                }
            }
        };

        public ScoreMonitor(Context context) {
            this.mLocalContext = context;
            initScoreMonitor();
        }

        private void initScoreMonitor() {
            this.mIsControlVersion = SystemProperties.getBoolean(PROP_CONTROL_VERSION, false);
            if (OppoUsageService.DEBUG_SCORE_M) {
                Slog.d(OppoUsageService.TAG, "initScoreMonitor, mIsControlVersion:" + this.mIsControlVersion);
            }
            if (this.mIsControlVersion) {
                regMonitorReceiver();
                initMonitorTimer();
            }
        }

        private void regMonitorReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_MONITOR_TIMER);
            intentFilter.addAction(BROADCAST_ACTION_INFO_UPLOAD);
            this.mLocalContext.registerReceiver(this.mScoreMonitorReceiver, intentFilter);
        }

        private void initMonitorTimer() {
            this.mAlarmManager = (AlarmManager) this.mLocalContext.getSystemService("alarm");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mLocalContext, 0, new Intent(ACTION_MONITOR_TIMER, null), 0);
            int startTimeHour = 17;
            int startTimeMinute = 0;
            long interval = 86400000;
            try {
                if (SystemProperties.getBoolean("persist.sys.assert.panic", false)) {
                    startTimeHour = SystemProperties.getInt("persist.sys.sc.h", 17);
                    startTimeMinute = SystemProperties.getInt("persist.sys.sc.m", 0);
                    interval = Long.valueOf(SystemProperties.getLong("persist.sys.sc.i", 86400000)).longValue();
                }
            } catch (Exception e) {
            }
            if (OppoUsageService.DEBUG_SCORE_M) {
                Slog.d(OppoUsageService.TAG, "initMonitorTimer, startTimeHour:" + startTimeHour + ", startTimeMinute:" + startTimeMinute + ", interval:" + interval);
            }
            this.mAlarmManager.setRepeating(0, getTime(startTimeHour, startTimeMinute), interval, pendingIntent);
            if (OppoUsageService.DEBUG_SCORE_M) {
                Slog.d(OppoUsageService.TAG, "initMonitorTimer, start timer.");
            }
        }

        public boolean isControlVersion() {
            return this.mIsControlVersion;
        }

        void emulateScoreMonitorStart() {
            if (OppoUsageService.DEBUG_SCORE_M) {
                Slog.d(OppoUsageService.TAG, "emulateScoreMonitorStart ...");
            }
            this.mLocalContext.sendBroadcast(new Intent(ACTION_MONITOR_TIMER, null));
            if (OppoUsageService.DEBUG_SCORE_M) {
                Slog.d(OppoUsageService.TAG, "emulateScoreMonitorStart end.");
            }
        }

        private void startScoreMonitor() {
            if (OppoUsageService.DEBUG_SCORE_M) {
                Slog.d(OppoUsageService.TAG, "startScoreMonitor, enable:" + this.mIsControlVersion);
            }
            if (this.mIsControlVersion) {
                ComponentName scoreMonitorServiceComponent = new ComponentName(SCORE_MONITOR_PACKAGE, SCORE_MONITOR_SERVICE);
                try {
                    Intent serviceIntent = new Intent();
                    serviceIntent.setComponent(scoreMonitorServiceComponent);
                    this.mLocalContext.startService(serviceIntent);
                } catch (Exception e) {
                    Slog.w(OppoUsageService.TAG, "startScoreMonitor failed!", e);
                }
                if (OppoUsageService.DEBUG_SCORE_M) {
                    Slog.d(OppoUsageService.TAG, "startScoreMonitor, send start action.");
                }
            }
        }

        private long getTime(int hourOfDay, int minute) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(11, hourOfDay);
            calendar.set(12, minute);
            calendar.set(13, 0);
            calendar.set(14, 0);
            long time = calendar.getTimeInMillis();
            if (System.currentTimeMillis() <= time) {
                return time;
            }
            calendar.add(5, 1);
            return calendar.getTimeInMillis();
        }

        private void onFileUpload(String fileName) {
        }
    }

    private class SimCardData {
        String mImeiStrSlot0 = null;
        String mImeiStrSlot1 = null;
        String mMeidStrSlot0 = null;
        String mMeidStrSlot1 = null;
        int mSimCardCount = 0;

        public String toString() {
            return "[c:" + this.mSimCardCount + ", I0:" + this.mImeiStrSlot0 + ", M0:" + this.mMeidStrSlot0 + ", I1:" + this.mImeiStrSlot1 + ", M1:" + this.mMeidStrSlot1 + "]";
        }

        public boolean isValid() {
            return (this.mImeiStrSlot0 == null && this.mImeiStrSlot1 == null) ? false : true;
        }
    }

    private class SimCardDataRecorder extends UsageDataRecorder {
        private static final boolean DBG_SD = false;
        private static final String LOGTAG = "SimCardDataRecorder";
        private static final String STR_USAGE_SIMCARD_HISTORY = "sdh.data";
        private static final String STR_USAGE_SIMCARD_ORIGINAL = "sdo.data";
        private File mSDHistoryFile;
        private File mSDOriginalFile;
        private SimCardData mSimCardData;
        private boolean mUpdateOriginalSD;

        public SimCardDataRecorder(Context context, Kahaleesi localKahaleesi) {
            super(context, localKahaleesi);
            this.mSDOriginalFile = null;
            this.mSDHistoryFile = null;
            this.mSimCardData = null;
            this.mUpdateOriginalSD = false;
            this.mSDOriginalFile = new File(this.mUsagePersistFileDir, STR_USAGE_SIMCARD_ORIGINAL);
            this.mSDHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_SIMCARD_HISTORY);
        }

        public boolean saveSimCardInfo(SimCardData simcardData) {
            if (simcardData == null || !simcardData.isValid()) {
                return false;
            }
            ArrayList<String> recordList = new ArrayList();
            if (simcardData.mImeiStrSlot0 != null) {
                recordList.add("0|" + simcardData.mImeiStrSlot0 + OppoUsageService.mConnectorForPkgNameAndTime + simcardData.mMeidStrSlot0);
            }
            if (simcardData.mImeiStrSlot1 != null) {
                recordList.add("1|" + simcardData.mImeiStrSlot1 + OppoUsageService.mConnectorForPkgNameAndTime + simcardData.mMeidStrSlot1);
            }
            boolean saveOrigScInfoRes = saveOriginalSimCardInfo(recordList);
            boolean saveHistoryScInfoRes = saveSimCardInfoToHistory(recordList);
            if (!saveOrigScInfoRes) {
                saveHistoryScInfoRes = false;
            }
            return saveHistoryScInfoRes;
        }

        private boolean saveOriginalSimCardInfo(ArrayList<String> recordList) {
            if (!this.mSDOriginalFile.exists()) {
                try {
                    this.mSDOriginalFile.createNewFile();
                } catch (IOException e) {
                    Slog.e(LOGTAG, "create sdo file failed!");
                    return false;
                }
            } else if (isExistDataRight()) {
                return true;
            }
            return doSaveSimCardData(recordList, this.mSDOriginalFile, false, false);
        }

        private boolean saveSimCardInfoToHistory(ArrayList<String> recordList) {
            if (!this.mSDHistoryFile.exists()) {
                try {
                    this.mSDHistoryFile.createNewFile();
                } catch (IOException e) {
                    Slog.e(LOGTAG, "create sdh file failed!");
                    return false;
                }
            }
            return doSaveSimCardData(recordList, this.mSDHistoryFile, true, true);
        }

        private boolean doSaveSimCardData(ArrayList<String> recordList, File destFile, boolean append, boolean checkRepeat) {
            if (recordList.size() <= 0) {
                return false;
            }
            Iterable curHistoryList = null;
            if (checkRepeat) {
                curHistoryList = getHistorySimCardInfoList();
            }
            BufferedWriter destBuffWriter = null;
            try {
                BufferedWriter destBuffWriter2;
                boolean isFirstLine;
                FileWriter destFileWriter = new FileWriter(destFile, append);
                try {
                    destBuffWriter2 = new BufferedWriter(destFileWriter);
                    isFirstLine = true;
                } catch (IOException e) {
                    Slog.w(OppoUsageService.TAG, "sd destFileOpStream IO failed.");
                    if (destBuffWriter != null) {
                        try {
                            destBuffWriter.close();
                        } catch (IOException e2) {
                            Slog.w(OppoUsageService.TAG, "destFileOpStream close failed.");
                        }
                    }
                    return false;
                }
                try {
                    for (String str : recordList) {
                        if (str != null && str.length() > 0) {
                            boolean isTheSame = false;
                            if (checkRepeat) {
                                for (String historyStr : curHistoryList) {
                                    if (str.equals(historyStr)) {
                                        isTheSame = true;
                                        break;
                                    }
                                }
                                if (isTheSame) {
                                }
                            }
                            if (isFirstLine) {
                                isFirstLine = false;
                            } else {
                                destBuffWriter2.newLine();
                            }
                            destBuffWriter2.write(this.mLocalKahaleesi.frozenIntoIce(str));
                        }
                    }
                    destBuffWriter2.flush();
                    destBuffWriter2.close();
                    return true;
                } catch (IOException e3) {
                    destBuffWriter = destBuffWriter2;
                    FileWriter fileWriter = destFileWriter;
                    Slog.w(OppoUsageService.TAG, "sd destFileOpStream IO failed.");
                    if (destBuffWriter != null) {
                        try {
                            destBuffWriter.close();
                        } catch (IOException e22) {
                            Slog.w(OppoUsageService.TAG, "destFileOpStream close failed.");
                        }
                    }
                    return false;
                }
            } catch (IOException e4) {
                Slog.w(OppoUsageService.TAG, "sd destFileOpStream IO failed.");
                if (destBuffWriter != null) {
                    try {
                        destBuffWriter.close();
                    } catch (IOException e222) {
                        Slog.w(OppoUsageService.TAG, "destFileOpStream close failed.");
                    }
                }
                return false;
            }
        }

        public ArrayList<String> getOriginalSimCardData() {
            if (this.mSDOriginalFile.exists()) {
                return loadHistoryDataFromFile(this.mSDOriginalFile);
            }
            return null;
        }

        public ArrayList<String> getHistorySimCardInfoList() {
            if (this.mSDHistoryFile.exists()) {
                return loadHistoryDataFromFile(this.mSDHistoryFile);
            }
            return null;
        }

        private boolean isExistDataRight() {
            return true;
        }
    }

    private class StatsticDataRecorder extends UsageDataRecorder {
        private static final boolean DBG_SD = false;
        private static final String LOGTAG = "StatsticDataRecorder";
        private static final String STR_USAGE_DAIL_IN_DURATION = "did.data";
        private static final String STR_USAGE_DAIL_OUT_DURATION = "dod.data";
        private static final String STR_USAGE_SMS_RECEIVE_COUNT = "smsr.data";
        private static final String STR_USAGE_SMS_SEND_COUNT = "smss.data";
        private int mDailInDuration = 0;
        private File mDailInDurationHistoryFile = null;
        private int mDailOutDuration = 0;
        private File mDailOutDurationHistoryFile = null;
        private int mSmsRecCount = 0;
        private File mSmsRecCountHistoryFile = null;
        private int mSmsSendCount = 0;
        private File mSmsSendCountHistoryFile = null;

        public StatsticDataRecorder(Context context, Kahaleesi localKahaleesi) {
            super(context, localKahaleesi);
            updateCurDataFromFiles();
        }

        private void updateCurDataFromFiles() {
            int i = 0;
            if (this.mSmsRecCountHistoryFile == null) {
                this.mSmsRecCountHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_SMS_RECEIVE_COUNT);
            }
            int readRes = readSignalCountFromFile(this.mSmsRecCountHistoryFile);
            if (readRes < 0) {
                readRes = 0;
            }
            this.mSmsRecCount = readRes;
            if (this.mSmsSendCountHistoryFile == null) {
                this.mSmsSendCountHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_SMS_SEND_COUNT);
            }
            readRes = readSignalCountFromFile(this.mSmsSendCountHistoryFile);
            if (readRes < 0) {
                readRes = 0;
            }
            this.mSmsSendCount = readRes;
            if (this.mDailOutDurationHistoryFile == null) {
                this.mDailOutDurationHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_DAIL_OUT_DURATION);
            }
            readRes = readSignalCountFromFile(this.mDailOutDurationHistoryFile);
            if (readRes < 0) {
                readRes = 0;
            }
            this.mDailOutDuration = readRes;
            if (this.mDailInDurationHistoryFile == null) {
                this.mDailInDurationHistoryFile = new File(this.mUsagePersistFileDir, STR_USAGE_DAIL_IN_DURATION);
            }
            readRes = readSignalCountFromFile(this.mDailInDurationHistoryFile);
            if (readRes >= 0) {
                i = readRes;
            }
            this.mDailInDuration = i;
        }

        private int readSignalCountFromFile(File srcFile) {
            if (srcFile == null || (srcFile.exists() ^ 1) != 0) {
                return 0;
            }
            ArrayList<String> resStrList = loadHistoryDataFromFile(srcFile);
            if (resStrList == null || resStrList.size() <= 0) {
                return 0;
            }
            String countStr = (String) resStrList.get(0);
            if (countStr == null || (countStr.isEmpty() ^ 1) == 0) {
                return 0;
            }
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                Slog.e(OppoUsageService.TAG, "parser failed num.");
                return 0;
            } catch (Exception e2) {
                Slog.e(OppoUsageService.TAG, "parser failed.");
                return 0;
            }
        }

        private boolean saveCountInfoToFile(File destFile, int count, boolean append) {
            if (count < 0 || destFile == null) {
                return false;
            }
            if (!destFile.exists()) {
                try {
                    destFile.createNewFile();
                } catch (IOException e) {
                    Slog.e(LOGTAG, "create history file failed!");
                    return false;
                }
            }
            String saveStr = Integer.toString(count);
            ArrayList<String> saveStrList = new ArrayList();
            saveStrList.add(saveStr);
            return doSaveData(saveStrList, destFile, append, true);
        }

        public boolean accumulateSmsRecCount(int delataRecCount) {
            if (delataRecCount <= 0) {
                return false;
            }
            this.mSmsRecCount += delataRecCount;
            return saveCountInfoToFile(this.mSmsRecCountHistoryFile, this.mSmsRecCount, false);
        }

        public int getHistorySmsRecCount() {
            return readSignalCountFromFile(this.mSmsRecCountHistoryFile);
        }

        public boolean accumulateSmsSendCount(int delataSendCount) {
            if (delataSendCount <= 0) {
                return false;
            }
            this.mSmsSendCount += delataSendCount;
            return saveCountInfoToFile(this.mSmsSendCountHistoryFile, this.mSmsSendCount, false);
        }

        public int getHistorySmsSendCount() {
            return readSignalCountFromFile(this.mSmsSendCountHistoryFile);
        }

        public boolean accumulateDailOutDuration(int delataDailOutDuration) {
            if (delataDailOutDuration <= 0) {
                return false;
            }
            this.mDailOutDuration += delataDailOutDuration;
            return saveCountInfoToFile(this.mDailOutDurationHistoryFile, this.mDailOutDuration, false);
        }

        public int getHistoryDailOutDuration() {
            return readSignalCountFromFile(this.mDailOutDurationHistoryFile);
        }

        public boolean accumulateDailInDuration(int delataDailInDuration) {
            if (delataDailInDuration <= 0) {
                return false;
            }
            this.mDailInDuration += delataDailInDuration;
            return saveCountInfoToFile(this.mDailInDurationHistoryFile, this.mDailInDuration, false);
        }

        public int getHistoryDailInDuration() {
            return readSignalCountFromFile(this.mDailInDurationHistoryFile);
        }
    }

    private native byte[] native_engineer_read_dev_block(String str, int i, int i2);

    private native int native_engineer_write_dev_block(String str, byte[] bArr, int i);

    private native void native_finalizeRawPartition();

    private native String native_get_download_status(int i);

    private native boolean native_initUsageRawPartition();

    private native int native_readDataRecordCount(int i);

    private native String native_readDataStrContent(int i, int i2, int i3);

    private native String native_readDataStrContentForSingleRecord(int i);

    private native byte[] native_read_emmc_info(int i, int i2);

    private native int native_writeStringContentData(int i, String str, int i2);

    public OppoUsageService(Context context) {
        this.mContext = context;
        this.mKahaleesi = new Kahaleesi();
        this.mIntergrateReserveManager = new IntergrateReserveManager(this.mContext, this.mKahaleesi);
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            this.mRawPartionInitOk = true;
        } else {
            this.mRawPartionInitOk = native_initUsageRawPartition();
        }
    }

    public void systemReady() {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 20000);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 30000);
        initPkgBroadcastReceive();
        this.mScoreMonitor = new ScoreMonitor(this.mContext);
        this.mEmmcInfoCollector = new EmmcUsageCollector(this.mContext);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 40000);
    }

    private boolean isSupportIntergratedReserve() {
        return this.mIntergrateReserveManager.isSupportIntergrateReserve();
    }

    private void saveCurrentBootTime(String bootTimeDateStr) {
        if (bootTimeDateStr != null && !bootTimeDateStr.isEmpty()) {
            if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
                ArrayList<String> contentList = new ArrayList();
                contentList.add(bootTimeDateStr);
                boolean saveRes = this.mIntergrateReserveManager.getBootTimeRecorder().saveContentList(contentList);
                return;
            }
            int saveRes2 = goToKahaleesi(1, bootTimeDateStr, 0, true);
        }
    }

    private String getCurrentDateStr() {
        this.mTimeObj.setToNow();
        return this.mTimeObj.format("%Y-%m-%d %H:%M:%S");
    }

    private List<String> getAllHistoryRecordData(int dataType) {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        int recordCount = native_readDataRecordCount(dataType);
        if (recordCount <= 0) {
            return null;
        }
        ArrayList<String> recordList = new ArrayList();
        int readBatchCount = recordCount / 10;
        for (int index = 0; index < readBatchCount; index++) {
            splitStr(comeBackFromKahaleesi(dataType, (index * 10) + 1, (index + 1) * 10, true), this.mRecordStrSlitter, recordList);
        }
        if (recordCount - (readBatchCount * 10) > 0) {
            splitStr(comeBackFromKahaleesi(dataType, (readBatchCount * 10) + 1, recordCount, true), this.mRecordStrSlitter, recordList);
        }
        return recordList;
    }

    private List<String> getHistoryRecordByIndex(int startIndex, int endIndex, int dataType, String logTag) {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (startIndex < 1 || endIndex < 1 || startIndex > endIndex) {
            return null;
        }
        int recordCount = native_readDataRecordCount(dataType);
        if (startIndex > recordCount) {
            return null;
        }
        if (endIndex > recordCount) {
            endIndex = recordCount;
        }
        ArrayList<String> recordList = new ArrayList();
        int attemptReadCount = (endIndex - startIndex) + 1;
        int readBatchCount = attemptReadCount / 10;
        for (int index = 0; index < readBatchCount; index++) {
            int tmpStartIndex = startIndex + (index * 10);
            splitStr(comeBackFromKahaleesi(dataType, tmpStartIndex, (tmpStartIndex + 10) - 1, true), this.mRecordStrSlitter, recordList);
        }
        if (attemptReadCount - (readBatchCount * 10) > 0) {
            splitStr(comeBackFromKahaleesi(dataType, (readBatchCount * 10) + startIndex, endIndex, true), this.mRecordStrSlitter, recordList);
        }
        return recordList;
    }

    public void testSaveSomeData(int dataType, String dataContent) {
        if (!checkOppoUsagePermission()) {
        }
    }

    public List<String> getHistoryBootTime() {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getBootTimeRecorder().getCurHistoryFileInfoList();
        }
        return getAllHistoryRecordData(1);
    }

    public List<String> getHistoryImeiNO() {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getSimCardDataRecorder().getHistorySimCardInfoList();
        }
        return getAllHistoryRecordData(2);
    }

    public List<String> getOriginalSimcardData() {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getSimCardDataRecorder().getOriginalSimCardData();
        }
        return getAllHistoryRecordData(25);
    }

    public List<String> getHistoryPcbaNO() {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getPcbaDataRecorder().getHistoryPcbaInfoList();
        }
        return getAllHistoryRecordData(3);
    }

    public int getAppUsageHistoryRecordCount() {
        if (checkOppoUsagePermission() && !this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return native_readDataRecordCount(9);
        }
        return 0;
    }

    private boolean writeHistoryRecord(String contentStr, String dateTimeStr, int dataType, int isSingleRecord, String logTag) {
        boolean z = true;
        if ((isSingleRecord != 0 && isSingleRecord != 1) || !isValidDataType(dataType) || contentStr == null || contentStr.isEmpty()) {
            return false;
        }
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            dateTimeStr = getCurrentDateStr();
            if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                return false;
            }
        }
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(contentStr).append(mConnectorForPkgNameAndTime).append(dateTimeStr);
        String lastContentStr = strBuilder.toString();
        if (lastContentStr == null || lastContentStr.isEmpty()) {
            return false;
        }
        if (goToKahaleesi(dataType, lastContentStr, isSingleRecord, isSingleRecord != 1) <= 0) {
            z = false;
        }
        return z;
    }

    public List<String> getAppUsageHistoryRecords(int startIndex, int endIndex) {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (!this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return getHistoryRecordByIndex(startIndex, endIndex, 9, "AppUsage");
        }
        if (startIndex == 0 && endIndex == 0) {
            return this.mIntergrateReserveManager.getAppUsageRecorder().getCurHistoryFileInfoList();
        }
        if (1 == startIndex && 1 == endIndex) {
            return this.mIntergrateReserveManager.getAppUsageRecorder().getPreHistoryFileInfoList();
        }
        return null;
    }

    public boolean writeAppUsageHistoryRecord(String appName, String dateTime) {
        if (!checkOppoUsagePermission()) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            String contentStr = appName;
            String dateTimeStr = dateTime;
            if (appName == null || appName.isEmpty()) {
                return false;
            }
            if (dateTime == null || dateTime.isEmpty()) {
                dateTimeStr = getCurrentDateStr();
                if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                    return false;
                }
            }
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(appName).append(mConnectorForPkgNameAndTime).append(dateTimeStr);
            String lastContentStr = strBuilder.toString();
            if (lastContentStr == null || lastContentStr.isEmpty()) {
                return false;
            }
            ArrayList<String> contentList = new ArrayList();
            contentList.add(lastContentStr);
            return this.mIntergrateReserveManager.getAppUsageRecorder().saveContentList(contentList);
        }
        return writeHistoryRecord(appName, dateTime, 9, 0, "AppUsage");
    }

    public int getHistoryCountOfSendedMsg() {
        if (!checkOppoUsagePermission()) {
            return 0;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().getHistorySmsSendCount();
        }
        return strValueToIntValue(comeBackFromKahaleesiSingle(4, false), 0);
    }

    public int getHistoryCountOfReceivedMsg() {
        if (!checkOppoUsagePermission()) {
            return 0;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().getHistorySmsRecCount();
        }
        return strValueToIntValue(comeBackFromKahaleesiSingle(5, false), 0);
    }

    public boolean accumulateHistoryCountOfSendedMsg(int newCountIncrease) {
        if (!checkOppoUsagePermission() || newCountIncrease <= 0) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().accumulateSmsSendCount(newCountIncrease);
        }
        if (!this.mHasGotHistoryCountOfSendedMsg) {
            this.mCurrentCountOfSendedMsg = getHistoryCountOfSendedMsg();
            this.mHasGotHistoryCountOfSendedMsg = true;
        }
        this.mCurrentCountOfSendedMsg += newCountIncrease;
        return doSaveHistoryCount(4, this.mCurrentCountOfSendedMsg, true, "SendedMsg");
    }

    public boolean accumulateHistoryCountOfReceivedMsg(int newCountIncrease) {
        if (!checkOppoUsagePermission() || newCountIncrease <= 0) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().accumulateSmsRecCount(newCountIncrease);
        }
        if (!this.mHasGotHistoryCountOfReceivedMsg) {
            this.mCurrentCountOfReceivedMsg = getHistoryCountOfReceivedMsg();
            this.mHasGotHistoryCountOfReceivedMsg = true;
        }
        this.mCurrentCountOfReceivedMsg += newCountIncrease;
        return doSaveHistoryCount(5, this.mCurrentCountOfReceivedMsg, true, "ReceivedMsg");
    }

    private boolean doSaveHistoryCount(int dataType, int saveValue, boolean isSingleRecord, String logTag) {
        boolean z = true;
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(saveValue);
        String contentStr = strBuilder.toString();
        if (contentStr == null || contentStr.isEmpty()) {
            return false;
        }
        int i;
        if (isSingleRecord) {
            i = 1;
        } else {
            i = 0;
        }
        if (goToKahaleesi(dataType, contentStr, i, false) <= 0) {
            z = false;
        }
        return z;
    }

    public int getDialOutDuration() {
        if (!checkOppoUsagePermission()) {
            return 0;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().getHistoryDailOutDuration();
        }
        return strValueToIntValue(comeBackFromKahaleesiSingle(6, false), 0);
    }

    public int getInComingCallDuration() {
        if (!checkOppoUsagePermission()) {
            return 0;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().getHistoryDailInDuration();
        }
        return strValueToIntValue(comeBackFromKahaleesiSingle(7, false), 0);
    }

    private int strValueToIntValue(String strValue, int defaultValue) {
        if (strValue == null || strValue.isEmpty()) {
            return defaultValue;
        }
        try {
            int intValue = Integer.parseInt(strValue);
            if (intValue < 0) {
                intValue = 0;
            }
            return intValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean accumulateDialOutDuration(int durationInMinute) {
        if (!checkOppoUsagePermission()) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().accumulateDailOutDuration(durationInMinute);
        }
        if (!this.mHasGotDialOutDuration) {
            this.mCurrentDialOutDuration = getDialOutDuration();
            this.mHasGotDialOutDuration = true;
        }
        this.mCurrentDialOutDuration += durationInMinute;
        return doSaveHistoryCount(6, this.mCurrentDialOutDuration, true, "DialOutDuration");
    }

    public boolean accumulateInComingCallDuration(int durationInMinute) {
        if (!checkOppoUsagePermission()) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getStaticDataRecorder().accumulateDailInDuration(durationInMinute);
        }
        if (!this.mHasGotIncomingDuration) {
            this.mCurrentIncomingDuration = getInComingCallDuration();
            this.mHasGotIncomingDuration = true;
        }
        this.mCurrentIncomingDuration += durationInMinute;
        return doSaveHistoryCount(7, this.mCurrentIncomingDuration, true, "IncomingDuration");
    }

    public int getHistoryRecordsCountOfPhoneCalls() {
        if (checkOppoUsagePermission() && !this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return native_readDataRecordCount(8);
        }
        return 0;
    }

    public List<String> getPhoneCallHistoryRecords(int startIndex, int endIndex) {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (!this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return getHistoryRecordByIndex(startIndex, endIndex, 8, "PhoneCall");
        }
        if (startIndex == 0 && endIndex == 0) {
            return this.mIntergrateReserveManager.getComInfoRecorder().getCurHistoryFileInfoList();
        }
        if (1 == startIndex && 1 == endIndex) {
            return this.mIntergrateReserveManager.getComInfoRecorder().getPreHistoryFileInfoList();
        }
        return null;
    }

    public boolean writePhoneCallHistoryRecord(String phoneNoStr, String dateTime) {
        if (!checkOppoUsagePermission()) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            ArrayList<String> contentList = new ArrayList();
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(phoneNoStr).append(mConnectorForPkgNameAndTime).append(dateTime);
            String lastContentStr = strBuilder.toString();
            if (lastContentStr == null || lastContentStr.isEmpty()) {
                return false;
            }
            contentList.add(lastContentStr);
            return this.mIntergrateReserveManager.getComInfoRecorder().saveContentList(contentList);
        }
        return writeHistoryRecord(phoneNoStr, dateTime, 8, 0, "PhoneCall");
    }

    public void shutDown() {
        if (checkOppoUsagePermission()) {
            saveCurrentBootTime("shutDown:" + getCurrentDateStr());
        }
    }

    private boolean isValidDataType(int dataType) {
        if (dataType < 1 || dataType > 25) {
            return false;
        }
        return true;
    }

    private boolean getImeiNoFromPhone() {
        boolean result;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            if (telephonyManager == null) {
                return false;
            }
            int simCardCount = telephonyManager.getSimCount();
            ColorOSTelephonyManager mColorTelMgr = ColorOSTelephonyManager.getDefault(this.mContext);
            this.mCurrentSimCardData = new SimCardData();
            this.mCurrentSimCardData.mSimCardCount = simCardCount;
            this.mCurrentSimCardData.mImeiStrSlot0 = mColorTelMgr.colorGetImei(0);
            this.mCurrentSimCardData.mMeidStrSlot0 = mColorTelMgr.colorGetMeid(0);
            if (this.mCurrentSimCardData.mSimCardCount > 1) {
                this.mCurrentSimCardData.mImeiStrSlot1 = mColorTelMgr.colorGetImei(1);
                this.mCurrentSimCardData.mMeidStrSlot1 = mColorTelMgr.colorGetMeid(1);
            }
            result = true;
            return result;
        } catch (Exception e) {
            result = false;
        }
    }

    private void saveCurrentSimCardData() {
        if (this.mCurrentSimCardData != null) {
            if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
                boolean saveRes = this.mIntergrateReserveManager.getSimCardDataRecorder().saveSimCardInfo(this.mCurrentSimCardData);
                return;
            }
            int saveRes2;
            int originalDataCount = native_readDataRecordCount(25);
            int numberRecordCount = native_readDataRecordCount(2);
            List numberList = null;
            boolean isSaveAsOriginalData = false;
            if (originalDataCount <= 0) {
                isSaveAsOriginalData = true;
            }
            if (numberRecordCount > 0) {
                numberList = getHistoryImeiNO();
            }
            if (this.mCurrentSimCardData.mImeiStrSlot0 != null) {
                String slot0NoStr = "0|" + this.mCurrentSimCardData.mImeiStrSlot0 + mConnectorForPkgNameAndTime + this.mCurrentSimCardData.mMeidStrSlot0;
                if (isNewNumber(slot0NoStr, numberList)) {
                    saveRes2 = goToKahaleesi(2, slot0NoStr, 0, true);
                }
                if (isSaveAsOriginalData) {
                    saveRes2 = goToKahaleesi(25, slot0NoStr, 0, true);
                }
            }
            if (this.mCurrentSimCardData.mImeiStrSlot1 != null) {
                String slot1NoStr = "1|" + this.mCurrentSimCardData.mImeiStrSlot1 + mConnectorForPkgNameAndTime + this.mCurrentSimCardData.mMeidStrSlot1;
                if (isNewNumber(slot1NoStr, numberList)) {
                    saveRes2 = goToKahaleesi(2, slot1NoStr, 0, true);
                }
                if (isSaveAsOriginalData) {
                    saveRes2 = goToKahaleesi(25, slot1NoStr, 0, true);
                }
            }
        }
    }

    private boolean isNewNumber(String valueNoStr, List<String> numberListForComp) {
        if (valueNoStr == null) {
            return false;
        }
        if (numberListForComp == null) {
            return true;
        }
        for (String numberStrInList : numberListForComp) {
            if (valueNoStr.equals(numberStrInList)) {
                return false;
            }
        }
        return true;
    }

    private void savePcbaNoIfNew(String numberStr) {
        if (numberStr != null && !numberStr.isEmpty()) {
            if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
                boolean saveRes = this.mIntergrateReserveManager.getPcbaDataRecorder().savePcbaInfo(numberStr);
                return;
            }
            boolean isNewNumber = true;
            if (native_readDataRecordCount(3) > 0) {
                List<String> numberList = getHistoryPcbaNO();
                if (numberList != null && numberList.size() > 0) {
                    for (String numberInList : numberList) {
                        if (numberStr.equals(numberInList)) {
                            isNewNumber = false;
                            break;
                        }
                    }
                }
                return;
            }
            isNewNumber = true;
            if (isNewNumber) {
                int goToKahaleesi = goToKahaleesi(3, numberStr, 0, true);
            }
        }
    }

    private boolean getPcbaNoFromPhone() {
        String pcbaNOStr = SystemProperties.get(PROP_NAME_PCBA_NO);
        if (pcbaNOStr == null || (pcbaNOStr.isEmpty() ^ 1) == 0) {
            return false;
        }
        this.mCurrentPcbaNO = pcbaNOStr;
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean splitStr(String contentStr, String strSlitter, ArrayList<String> recordList) {
        if (contentStr == null || contentStr.isEmpty() || strSlitter == null || recordList == null) {
            return false;
        }
        String[] tmpResArray = contentStr.split(strSlitter);
        if (tmpResArray != null && tmpResArray.length > 0) {
            for (Object add : tmpResArray) {
                recordList.add(add);
            }
        }
        return true;
    }

    protected void finalize() throws Throwable {
        native_finalizeRawPartition();
        super.finalize();
    }

    private boolean updateChargeInfomation(int dataType, int value) {
        boolean z = true;
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(value);
        String contentStr = strBuilder.toString();
        if (contentStr == null || contentStr.isEmpty()) {
            return false;
        }
        if (goToKahaleesi(dataType, contentStr, 1, false) <= 0) {
            z = false;
        }
        return z;
    }

    public boolean updateMaxChargeCurrent(int current) {
        return updateChargeInfomation(17, current);
    }

    public boolean updateMaxChargeTemperature(int temp) {
        return updateChargeInfomation(19, temp);
    }

    public boolean updateMinChargeTemperature(int temp) {
        return updateChargeInfomation(18, temp);
    }

    private int getChargeInformation(int dataType, int default_value) {
        return strValueToIntValue(comeBackFromKahaleesiSingle(dataType, false), default_value);
    }

    public int getMaxChargeCurrent() {
        return getChargeInformation(17, Integer.MIN_VALUE);
    }

    public int getMaxChargeTemperature() {
        return getChargeInformation(19, Integer.MIN_VALUE);
    }

    public int getMinChargeTemperature() {
        return getChargeInformation(18, Integer.MAX_VALUE);
    }

    public byte[] engineerReadDevBlock(String partion, int offset, int count) {
        if (partion == null || (partion.isEmpty() ^ 1) == 0 || count <= 0) {
            return null;
        }
        return native_engineer_read_dev_block(partion, offset, count);
    }

    public int engineerWriteDevBlock(String partion, byte[] content, int offset) {
        if (partion == null || (partion.isEmpty() ^ 1) == 0 || content == null) {
            return -1;
        }
        return native_engineer_write_dev_block(partion, content, offset);
    }

    public String getDownloadStatusString(int part) {
        return native_get_download_status(part);
    }

    public String loadSecrecyConfig() {
        return comeBackFromKahaleesiSingle(20, false);
    }

    public int saveSecrecyConfig(String content) {
        return goToKahaleesi(20, content, 1, false);
    }

    public int getProductLineLastTestFlag() {
        return strValueToIntValue(comeBackFromKahaleesiSingle(21, false), -1);
    }

    public boolean setProductLineLastTestFlag(int flag) {
        if (goToKahaleesi(21, Integer.toString(flag), 1, false) > 0) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean recordApkDeleteEvent(String deleteAppPkgName, String callerAppPkgName, String dateTime) {
        if (!checkOppoUsagePermission() || deleteAppPkgName == null || deleteAppPkgName.isEmpty() || callerAppPkgName == null || callerAppPkgName.isEmpty()) {
            return false;
        }
        String content = deleteAppPkgName + "/" + callerAppPkgName;
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            ArrayList<String> contentList = new ArrayList();
            contentList.add(content);
            return this.mIntergrateReserveManager.getAppUninstallRecorder().saveContentList(contentList);
        }
        return writeHistoryRecord(content, dateTime, 22, 0, "ApkDelEvent");
    }

    public int getApkDeleteEventRecordCount() {
        if (checkOppoUsagePermission() && !this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return native_readDataRecordCount(22);
        }
        return 0;
    }

    public List<String> getApkDeleteEventRecords(int startIndex, int endIndex) {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (!this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return getHistoryRecordByIndex(startIndex, endIndex, 22, "ApkDelEvent");
        }
        if (startIndex == 0 && endIndex == 0) {
            return this.mIntergrateReserveManager.getAppUninstallRecorder().getCurHistoryFileInfoList();
        }
        if (1 == startIndex && 1 == endIndex) {
            return this.mIntergrateReserveManager.getAppUninstallRecorder().getPreHistoryFileInfoList();
        }
        return null;
    }

    private void initPkgBroadcastReceive() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPkgMsgReceiver, intentFilter);
    }

    private void saveApkInstallEvent(String pkgName) {
        boolean saveRes = recordApkInstallEvent(pkgName, "installer", null);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean recordApkInstallEvent(String installAppPkgName, String callerAppPkgName, String dateTime) {
        if (!checkOppoUsagePermission() || installAppPkgName == null || installAppPkgName.isEmpty() || callerAppPkgName == null || callerAppPkgName.isEmpty()) {
            return false;
        }
        String content = installAppPkgName + "/" + callerAppPkgName;
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            ArrayList<String> contentList = new ArrayList();
            contentList.add(content);
            return this.mIntergrateReserveManager.getAppInstallRecorder().saveContentList(contentList);
        }
        return writeHistoryRecord(content, dateTime, 23, 0, "ApkInstallEvent");
    }

    public int getApkInstallEventRecordCount() {
        if (checkOppoUsagePermission() && !this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return native_readDataRecordCount(23);
        }
        return 0;
    }

    public List<String> getApkInstallEventRecords(int startIndex, int endIndex) {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (!this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return getHistoryRecordByIndex(startIndex, endIndex, 23, "ApkInstallEvent");
        }
        if (startIndex == 0 && endIndex == 0) {
            return this.mIntergrateReserveManager.getAppInstallRecorder().getCurHistoryFileInfoList();
        }
        if (1 == startIndex && 1 == endIndex) {
            return this.mIntergrateReserveManager.getAppInstallRecorder().getPreHistoryFileInfoList();
        }
        return null;
    }

    public boolean recordMcsConnectID(String connectID) {
        boolean z = false;
        if (!checkOppoUsagePermission() || connectID == null || connectID.isEmpty()) {
            return false;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getMcsDataRecorder().saveMcsInfo(connectID);
        }
        if (goToKahaleesi(24, connectID, 1, false) > 0) {
            z = true;
        }
        return z;
    }

    public String getMcsConnectID() {
        if (!checkOppoUsagePermission()) {
            return null;
        }
        if (this.mIntergrateReserveManager.isSupportIntergrateReserve()) {
            return this.mIntergrateReserveManager.getMcsDataRecorder().getMcsInfo();
        }
        return comeBackFromKahaleesiSingle(24, false);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        HashSet<String> argSet = new HashSet();
        for (String arg : args) {
            argSet.add(arg);
        }
        if (!SystemProperties.getBoolean("persist.sys.assert.panic", false)) {
            return;
        }
        if (argSet.contains("--openScLog") && this.mScoreMonitor != null) {
            pw.println("oppo usage state:open sc log");
            DEBUG_SCORE_M = true;
        } else if (argSet.contains("--emulateSc") && this.mScoreMonitor != null) {
            pw.println("oppo usage state:emulateSc");
            this.mScoreMonitor.emulateScoreMonitorStart();
        } else if (argSet.contains("--emmcD") && this.mEmmcInfoCollector != null) {
            pw.println("oppo usage state:open emmcD");
            this.mEmmcInfoCollector.mDebugEmmcInfo = true;
        } else if (argSet.contains("--emmc") && this.mEmmcInfoCollector != null) {
            boolean res2 = this.mEmmcInfoCollector.readEmmcInfoLable(true);
            Slog.d(TAG, "emmc info byte:" + res2);
            pw.println("emmc byte state:" + res2);
        }
    }

    private boolean checkOppoUsagePermission() {
        return isSystemAppByUid(Binder.getCallingUid());
    }

    private boolean isSystemAppByUid(int uid) {
        if (uid < 10000) {
            return true;
        }
        PackageManager pm = this.mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            return true;
        }
        for (String packageName : packages) {
            if (isSystemAppByPkgName(packageName, pm)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSystemAppByPkgName(String packageName, PackageManager pm) {
        if (packageName != null) {
            try {
                PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);
                ApplicationInfo info = pkgInfo != null ? pkgInfo.applicationInfo : null;
                if (!(info == null || (info.flags & 1) == 0)) {
                    return true;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    private int goToKahaleesi(int dataType, String dataContent, int isSingleRecord, boolean frozen) {
        if (dataContent == null) {
            return 0;
        }
        return native_writeStringContentData(dataType, frozen ? this.mKahaleesi.frozenIntoIce(dataContent) : dataContent, isSingleRecord);
    }

    private String comeBackFromKahaleesi(int dataType, int startIndex, int endIndex, boolean unfrozen) {
        String res = native_readDataStrContent(dataType, startIndex, endIndex);
        return unfrozen ? this.mKahaleesi.unfrozenFromFire(res) : res;
    }

    private String comeBackFromKahaleesiSingle(int dataType, boolean unfrozen) {
        String res = native_readDataStrContentForSingleRecord(dataType);
        return unfrozen ? this.mKahaleesi.unfrozenFromFire(res) : res;
    }
}
