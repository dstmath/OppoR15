package com.android.internal.os;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.os.Bundle;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseLongArray;
import com.android.internal.R;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.util.ArrayUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BatteryStatsHelper {
    static final boolean DEBUG = false;
    private static final String TAG = BatteryStatsHelper.class.getSimpleName();
    private static Intent sBatteryBroadcastXfer;
    private static ArrayMap<File, BatteryStats> sFileXfer = new ArrayMap();
    private static BatteryStats sStatsXfer;
    private Intent mBatteryBroadcast;
    private IBatteryStats mBatteryInfo;
    long mBatteryRealtimeUs;
    long mBatteryTimeRemainingUs;
    long mBatteryUptimeUs;
    PowerCalculator mBluetoothPowerCalculator;
    private final List<BatterySipper> mBluetoothSippers;
    PowerCalculator mCameraPowerCalculator;
    long mChargeTimeRemainingUs;
    private final boolean mCollectBatteryBroadcast;
    private double mComputedPower;
    private final Context mContext;
    PowerCalculator mCpuPowerCalculator;
    PowerCalculator mFlashlightPowerCalculator;
    boolean mHasBluetoothPowerReporting;
    boolean mHasWifiPowerReporting;
    private double mMaxDrainedPower;
    private double mMaxPower;
    private double mMaxRealPower;
    PowerCalculator mMemoryPowerCalculator;
    private double mMinDrainedPower;
    MobileRadioPowerCalculator mMobileRadioPowerCalculator;
    private final List<BatterySipper> mMobilemsppList;
    private PackageManager mPackageManager;
    private PowerProfile mPowerProfile;
    long mRawRealtimeUs;
    long mRawUptimeUs;
    PowerCalculator mSensorPowerCalculator;
    private String[] mServicepackageArray;
    private BatteryStats mStats;
    private long mStatsPeriod;
    private int mStatsType;
    private String[] mSystemPackageArray;
    private double mTotalPower;
    long mTypeBatteryRealtimeUs;
    long mTypeBatteryUptimeUs;
    private final List<BatterySipper> mUsageList;
    private final SparseArray<List<BatterySipper>> mUserSippers;
    PowerCalculator mWakelockPowerCalculator;
    private final boolean mWifiOnly;
    PowerCalculator mWifiPowerCalculator;
    private final List<BatterySipper> mWifiSippers;

    public static boolean checkWifiOnly(Context context) {
        return ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0) ^ 1;
    }

    public static boolean checkHasWifiPowerReporting(BatteryStats stats, PowerProfile profile) {
        if (!stats.hasWifiActivityReporting() || profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE) == 0.0d || profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX) == 0.0d || profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX) == 0.0d) {
            return false;
        }
        return true;
    }

    public static boolean checkHasBluetoothPowerReporting(BatteryStats stats, PowerProfile profile) {
        if (!stats.hasBluetoothActivityReporting() || profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_IDLE) == 0.0d || profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_RX) == 0.0d || profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_TX) == 0.0d) {
            return false;
        }
        return true;
    }

    public BatteryStatsHelper(Context context) {
        this(context, true);
    }

    public BatteryStatsHelper(Context context, boolean collectBatteryBroadcast) {
        this(context, collectBatteryBroadcast, checkWifiOnly(context));
    }

    public BatteryStatsHelper(Context context, boolean collectBatteryBroadcast, boolean wifiOnly) {
        this.mUsageList = new ArrayList();
        this.mWifiSippers = new ArrayList();
        this.mBluetoothSippers = new ArrayList();
        this.mUserSippers = new SparseArray();
        this.mMobilemsppList = new ArrayList();
        this.mStatsType = 0;
        this.mStatsPeriod = 0;
        this.mMaxPower = 1.0d;
        this.mMaxRealPower = 1.0d;
        this.mHasWifiPowerReporting = false;
        this.mHasBluetoothPowerReporting = false;
        this.mContext = context;
        this.mCollectBatteryBroadcast = collectBatteryBroadcast;
        this.mWifiOnly = wifiOnly;
        this.mPackageManager = context.getPackageManager();
        Resources resources = context.getResources();
        this.mSystemPackageArray = resources.getStringArray(R.array.config_batteryPackageTypeSystem);
        this.mServicepackageArray = resources.getStringArray(R.array.config_batteryPackageTypeService);
    }

    public void storeStatsHistoryInFile(String fname) {
        IOException e;
        Throwable th;
        synchronized (sFileXfer) {
            File path = makeFilePath(this.mContext, fname);
            sFileXfer.put(path, getStats());
            FileOutputStream fout = null;
            try {
                FileOutputStream fout2 = new FileOutputStream(path);
                try {
                    Parcel hist = Parcel.obtain();
                    getStats().writeToParcelWithoutUids(hist, 0);
                    fout2.write(hist.marshall());
                    if (fout2 != null) {
                        try {
                            fout2.close();
                        } catch (IOException e2) {
                        }
                    }
                    fout = fout2;
                } catch (IOException e3) {
                    e = e3;
                    fout = fout2;
                    try {
                        Log.w(TAG, "Unable to write history to file", e);
                        if (fout != null) {
                            try {
                                fout.close();
                            } catch (IOException e4) {
                            }
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        if (fout != null) {
                            try {
                                fout.close();
                            } catch (IOException e5) {
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fout = fout2;
                    if (fout != null) {
                        try {
                            fout.close();
                        } catch (IOException e52) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                Log.w(TAG, "Unable to write history to file", e);
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e42) {
                    }
                }
                return;
            }
        }
        return;
    }

    public static BatteryStats statsFromFile(Context context, String fname) {
        BatteryStats batteryStats;
        IOException e;
        Throwable th;
        synchronized (sFileXfer) {
            File path = makeFilePath(context, fname);
            BatteryStats stats = (BatteryStats) sFileXfer.get(path);
            if (stats != null) {
                return stats;
            }
            FileInputStream fin = null;
            try {
                FileInputStream fin2 = new FileInputStream(path);
                try {
                    byte[] data = readFully(fin2);
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(data, 0, data.length);
                    parcel.setDataPosition(0);
                    batteryStats = (BatteryStats) BatteryStatsImpl.CREATOR.createFromParcel(parcel);
                    if (fin2 != null) {
                        try {
                            fin2.close();
                        } catch (IOException e2) {
                        }
                    }
                } catch (IOException e3) {
                    e = e3;
                    fin = fin2;
                    try {
                        Log.w(TAG, "Unable to read history to file", e);
                        if (fin != null) {
                            try {
                                fin.close();
                            } catch (IOException e4) {
                            }
                        }
                        return getStats(Stub.asInterface(ServiceManager.getService("batterystats")));
                    } catch (Throwable th2) {
                        th = th2;
                        if (fin != null) {
                            try {
                                fin.close();
                            } catch (IOException e5) {
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fin = fin2;
                    if (fin != null) {
                        try {
                            fin.close();
                        } catch (IOException e52) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                Log.w(TAG, "Unable to read history to file", e);
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e42) {
                    }
                }
                return getStats(Stub.asInterface(ServiceManager.getService("batterystats")));
            }
        }
        return batteryStats;
    }

    public static void dropFile(Context context, String fname) {
        makeFilePath(context, fname).delete();
    }

    private static File makeFilePath(Context context, String fname) {
        return new File(context.getFilesDir(), fname);
    }

    public void clearStats() {
        this.mStats = null;
    }

    public BatteryStats getStats() {
        if (this.mStats == null) {
            load();
        }
        return this.mStats;
    }

    public Intent getBatteryBroadcast() {
        if (this.mBatteryBroadcast == null && this.mCollectBatteryBroadcast) {
            load();
        }
        return this.mBatteryBroadcast;
    }

    public PowerProfile getPowerProfile() {
        return this.mPowerProfile;
    }

    public void create(BatteryStats stats) {
        this.mPowerProfile = new PowerProfile(this.mContext);
        this.mStats = stats;
    }

    public void create(Bundle icicle) {
        if (icicle != null) {
            this.mStats = sStatsXfer;
            this.mBatteryBroadcast = sBatteryBroadcastXfer;
        }
        this.mBatteryInfo = Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mPowerProfile = new PowerProfile(this.mContext);
    }

    public void storeState() {
        sStatsXfer = this.mStats;
        sBatteryBroadcastXfer = this.mBatteryBroadcast;
    }

    public static String makemAh(double power) {
        if (power == 0.0d) {
            return "0";
        }
        String format;
        if (power < 1.0E-5d) {
            format = "%.8f";
        } else if (power < 1.0E-4d) {
            format = "%.7f";
        } else if (power < 0.001d) {
            format = "%.6f";
        } else if (power < 0.01d) {
            format = "%.5f";
        } else if (power < 0.1d) {
            format = "%.4f";
        } else if (power < 1.0d) {
            format = "%.3f";
        } else if (power < 10.0d) {
            format = "%.2f";
        } else if (power < 100.0d) {
            format = "%.1f";
        } else {
            format = "%.0f";
        }
        return String.format(Locale.ENGLISH, format, new Object[]{Double.valueOf(power)});
    }

    public void refreshStats(int statsType, int asUser) {
        SparseArray users = new SparseArray(1);
        users.put(asUser, new UserHandle(asUser));
        refreshStats(statsType, users);
    }

    public void refreshStats(int statsType, List<UserHandle> asUsers) {
        int n = asUsers.size();
        SparseArray users = new SparseArray(n);
        for (int i = 0; i < n; i++) {
            UserHandle userHandle = (UserHandle) asUsers.get(i);
            users.put(userHandle.getIdentifier(), userHandle);
        }
        refreshStats(statsType, users);
    }

    public void refreshStats(int statsType, SparseArray<UserHandle> asUsers) {
        refreshStats(statsType, asUsers, SystemClock.elapsedRealtime() * 1000, SystemClock.uptimeMillis() * 1000);
    }

    public void refreshStats(int statsType, SparseArray<UserHandle> asUsers, long rawRealtimeUs, long rawUptimeUs) {
        getStats();
        this.mMaxPower = 0.0d;
        this.mMaxRealPower = 0.0d;
        this.mComputedPower = 0.0d;
        this.mTotalPower = 0.0d;
        this.mUsageList.clear();
        this.mWifiSippers.clear();
        this.mBluetoothSippers.clear();
        this.mUserSippers.clear();
        this.mMobilemsppList.clear();
        if (this.mStats != null) {
            int i;
            BatterySipper bs;
            if (this.mCpuPowerCalculator == null) {
                this.mCpuPowerCalculator = new CpuPowerCalculator(this.mPowerProfile);
            }
            this.mCpuPowerCalculator.reset();
            if (this.mMemoryPowerCalculator == null) {
                this.mMemoryPowerCalculator = new MemoryPowerCalculator(this.mPowerProfile);
            }
            this.mMemoryPowerCalculator.reset();
            if (this.mWakelockPowerCalculator == null) {
                this.mWakelockPowerCalculator = new WakelockPowerCalculator(this.mPowerProfile);
            }
            this.mWakelockPowerCalculator.reset();
            if (this.mMobileRadioPowerCalculator == null) {
                this.mMobileRadioPowerCalculator = new MobileRadioPowerCalculator(this.mPowerProfile, this.mStats);
            }
            this.mMobileRadioPowerCalculator.reset(this.mStats);
            boolean hasWifiPowerReporting = checkHasWifiPowerReporting(this.mStats, this.mPowerProfile);
            if (this.mWifiPowerCalculator == null || hasWifiPowerReporting != this.mHasWifiPowerReporting) {
                PowerCalculator wifiPowerCalculator;
                if (hasWifiPowerReporting) {
                    wifiPowerCalculator = new WifiPowerCalculator(this.mPowerProfile);
                } else {
                    wifiPowerCalculator = new WifiPowerEstimator(this.mPowerProfile);
                }
                this.mWifiPowerCalculator = wifiPowerCalculator;
                this.mHasWifiPowerReporting = hasWifiPowerReporting;
            }
            this.mWifiPowerCalculator.reset();
            boolean hasBluetoothPowerReporting = checkHasBluetoothPowerReporting(this.mStats, this.mPowerProfile);
            if (this.mBluetoothPowerCalculator == null || hasBluetoothPowerReporting != this.mHasBluetoothPowerReporting) {
                this.mBluetoothPowerCalculator = new BluetoothPowerCalculator(this.mPowerProfile);
                this.mHasBluetoothPowerReporting = hasBluetoothPowerReporting;
            }
            this.mBluetoothPowerCalculator.reset();
            if (this.mSensorPowerCalculator == null) {
                this.mSensorPowerCalculator = new SensorPowerCalculator(this.mPowerProfile, (SensorManager) this.mContext.getSystemService("sensor"));
            }
            this.mSensorPowerCalculator.reset();
            if (this.mCameraPowerCalculator == null) {
                this.mCameraPowerCalculator = new CameraPowerCalculator(this.mPowerProfile);
            }
            this.mCameraPowerCalculator.reset();
            if (this.mFlashlightPowerCalculator == null) {
                this.mFlashlightPowerCalculator = new FlashlightPowerCalculator(this.mPowerProfile);
            }
            this.mFlashlightPowerCalculator.reset();
            this.mStatsType = statsType;
            this.mRawUptimeUs = rawUptimeUs;
            this.mRawRealtimeUs = rawRealtimeUs;
            this.mBatteryUptimeUs = this.mStats.getBatteryUptime(rawUptimeUs);
            this.mBatteryRealtimeUs = this.mStats.getBatteryRealtime(rawRealtimeUs);
            this.mTypeBatteryUptimeUs = this.mStats.computeBatteryUptime(rawUptimeUs, this.mStatsType);
            this.mTypeBatteryRealtimeUs = this.mStats.computeBatteryRealtime(rawRealtimeUs, this.mStatsType);
            this.mBatteryTimeRemainingUs = this.mStats.computeBatteryTimeRemaining(rawRealtimeUs);
            this.mChargeTimeRemainingUs = this.mStats.computeChargeTimeRemaining(rawRealtimeUs);
            this.mMinDrainedPower = (((double) this.mStats.getLowDischargeAmountSinceCharge()) * this.mPowerProfile.getBatteryCapacity()) / 100.0d;
            this.mMaxDrainedPower = (((double) this.mStats.getHighDischargeAmountSinceCharge()) * this.mPowerProfile.getBatteryCapacity()) / 100.0d;
            processAppUsage(asUsers);
            for (i = 0; i < this.mUsageList.size(); i++) {
                bs = (BatterySipper) this.mUsageList.get(i);
                bs.computeMobilemspp();
                if (bs.mobilemspp != 0.0d) {
                    this.mMobilemsppList.add(bs);
                }
            }
            for (i = 0; i < this.mUserSippers.size(); i++) {
                List<BatterySipper> user = (List) this.mUserSippers.valueAt(i);
                for (int j = 0; j < user.size(); j++) {
                    bs = (BatterySipper) user.get(j);
                    bs.computeMobilemspp();
                    if (bs.mobilemspp != 0.0d) {
                        this.mMobilemsppList.add(bs);
                    }
                }
            }
            Collections.sort(this.mMobilemsppList, new Comparator<BatterySipper>() {
                public int compare(BatterySipper lhs, BatterySipper rhs) {
                    return Double.compare(rhs.mobilemspp, lhs.mobilemspp);
                }
            });
            processMiscUsage();
            Collections.sort(this.mUsageList);
            if (!this.mUsageList.isEmpty()) {
                double d = ((BatterySipper) this.mUsageList.get(0)).totalPowerMah;
                this.mMaxPower = d;
                this.mMaxRealPower = d;
                int usageListCount = this.mUsageList.size();
                for (i = 0; i < usageListCount; i++) {
                    this.mComputedPower = ((BatterySipper) this.mUsageList.get(i)).totalPowerMah + this.mComputedPower;
                }
            }
            this.mTotalPower = this.mComputedPower;
            if (this.mStats.getLowDischargeAmountSinceCharge() > 1) {
                double amount;
                int index;
                if (this.mMinDrainedPower > this.mComputedPower) {
                    amount = this.mMinDrainedPower - this.mComputedPower;
                    this.mTotalPower = this.mMinDrainedPower;
                    bs = new BatterySipper(DrainType.UNACCOUNTED, null, amount);
                    index = Collections.binarySearch(this.mUsageList, bs);
                    if (index < 0) {
                        index = -(index + 1);
                    }
                    this.mUsageList.add(index, bs);
                    this.mMaxPower = Math.max(this.mMaxPower, amount);
                } else if (this.mMaxDrainedPower < this.mComputedPower) {
                    amount = this.mComputedPower - this.mMaxDrainedPower;
                    bs = new BatterySipper(DrainType.OVERCOUNTED, null, amount);
                    index = Collections.binarySearch(this.mUsageList, bs);
                    if (index < 0) {
                        index = -(index + 1);
                    }
                    this.mUsageList.add(index, bs);
                    this.mMaxPower = Math.max(this.mMaxPower, amount);
                }
            }
            double hiddenPowerMah = removeHiddenBatterySippers(this.mUsageList);
            double totalRemainingPower = getTotalPower() - hiddenPowerMah;
            if (Math.abs(totalRemainingPower) > 0.001d) {
                int size = this.mUsageList.size();
                for (i = 0; i < size; i++) {
                    BatterySipper sipper = (BatterySipper) this.mUsageList.get(i);
                    if (!sipper.shouldHide) {
                        sipper.proportionalSmearMah = ((sipper.totalPowerMah + sipper.screenPowerMah) / totalRemainingPower) * hiddenPowerMah;
                        sipper.sumPower();
                    }
                }
            }
        }
    }

    private void processAppUsage(SparseArray<UserHandle> asUsers) {
        boolean forAllUsers = asUsers.get(-1) != null;
        this.mStatsPeriod = this.mTypeBatteryRealtimeUs;
        BatterySipper osSipper = null;
        SparseArray<? extends Uid> uidStats = this.mStats.getUidStats();
        int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = (Uid) uidStats.valueAt(iu);
            BatterySipper app = new BatterySipper(DrainType.APP, u, 0.0d);
            this.mCpuPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mWakelockPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mMobileRadioPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mWifiPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mBluetoothPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mSensorPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mCameraPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mFlashlightPowerCalculator.calculateApp(app, u, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            if (app.sumPower() != 0.0d || u.getUid() == 0) {
                int uid = app.getUid();
                int userId = UserHandle.getUserId(uid);
                if (uid == 1010) {
                    this.mWifiSippers.add(app);
                } else if (uid == 1002) {
                    this.mBluetoothSippers.add(app);
                } else if (forAllUsers || asUsers.get(userId) != null || UserHandle.getAppId(uid) < 10000) {
                    this.mUsageList.add(app);
                } else {
                    List<BatterySipper> list = (List) this.mUserSippers.get(userId);
                    if (list == null) {
                        list = new ArrayList();
                        this.mUserSippers.put(userId, list);
                    }
                    list.add(app);
                }
                if (uid == 0) {
                    osSipper = app;
                }
            }
        }
        if (osSipper != null) {
            this.mWakelockPowerCalculator.calculateRemaining(osSipper, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            osSipper.sumPower();
        }
    }

    private void addPhoneUsage() {
        long phoneOnTimeMs = this.mStats.getPhoneOnTime(this.mRawRealtimeUs, this.mStatsType) / 1000;
        double phoneOnPower = (this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) * ((double) phoneOnTimeMs)) / 3600000.0d;
        if (phoneOnPower != 0.0d) {
            addEntry(DrainType.PHONE, phoneOnTimeMs, phoneOnPower);
        }
    }

    private void addScreenUsage() {
        long screenOnTimeMs = this.mStats.getScreenOnTime(this.mRawRealtimeUs, this.mStatsType) / 1000;
        double power = 0.0d + (((double) screenOnTimeMs) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON));
        double screenFullPower = this.mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        for (int i = 0; i < 5; i++) {
            power += ((((double) (((float) i) + 0.5f)) * screenFullPower) / 5.0d) * ((double) (this.mStats.getScreenBrightnessTime(i, this.mRawRealtimeUs, this.mStatsType) / 1000));
        }
        power /= 3600000.0d;
        if (power != 0.0d) {
            addEntry(DrainType.SCREEN, screenOnTimeMs, power);
        }
    }

    private void addRadioUsage() {
        BatterySipper radio = new BatterySipper(DrainType.CELL, null, 0.0d);
        this.mMobileRadioPowerCalculator.calculateRemaining(radio, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        radio.sumPower();
        if (radio.totalPowerMah > 0.0d) {
            this.mUsageList.add(radio);
        }
    }

    private void aggregateSippers(BatterySipper bs, List<BatterySipper> from, String tag) {
        for (int i = 0; i < from.size(); i++) {
            bs.add((BatterySipper) from.get(i));
        }
        bs.computeMobilemspp();
        bs.sumPower();
    }

    private void addIdleUsage() {
        double totalPowerMah = ((((double) (this.mTypeBatteryRealtimeUs / 1000)) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE)) + (((double) (this.mTypeBatteryUptimeUs / 1000)) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE))) / 3600000.0d;
        if (totalPowerMah != 0.0d) {
            addEntry(DrainType.IDLE, this.mTypeBatteryRealtimeUs / 1000, totalPowerMah);
        }
    }

    private void addWiFiUsage() {
        BatterySipper bs = new BatterySipper(DrainType.WIFI, null, 0.0d);
        this.mWifiPowerCalculator.calculateRemaining(bs, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        aggregateSippers(bs, this.mWifiSippers, "WIFI");
        if (bs.totalPowerMah > 0.0d) {
            this.mUsageList.add(bs);
        }
    }

    private void addBluetoothUsage() {
        BatterySipper bs = new BatterySipper(DrainType.BLUETOOTH, null, 0.0d);
        this.mBluetoothPowerCalculator.calculateRemaining(bs, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        aggregateSippers(bs, this.mBluetoothSippers, "Bluetooth");
        if (bs.totalPowerMah > 0.0d) {
            this.mUsageList.add(bs);
        }
    }

    private void addUserUsage() {
        for (int i = 0; i < this.mUserSippers.size(); i++) {
            int userId = this.mUserSippers.keyAt(i);
            BatterySipper bs = new BatterySipper(DrainType.USER, null, 0.0d);
            bs.userId = userId;
            aggregateSippers(bs, (List) this.mUserSippers.valueAt(i), "User");
            this.mUsageList.add(bs);
        }
    }

    private void addMemoryUsage() {
        BatterySipper memory = new BatterySipper(DrainType.MEMORY, null, 0.0d);
        this.mMemoryPowerCalculator.calculateRemaining(memory, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        memory.sumPower();
        if (memory.totalPowerMah > 0.0d) {
            this.mUsageList.add(memory);
        }
    }

    private void processMiscUsage() {
        addUserUsage();
        addPhoneUsage();
        addScreenUsage();
        addWiFiUsage();
        addBluetoothUsage();
        addMemoryUsage();
        addIdleUsage();
        if (!this.mWifiOnly) {
            addRadioUsage();
        }
    }

    private BatterySipper addEntry(DrainType drainType, long time, double power) {
        BatterySipper bs = new BatterySipper(drainType, null, 0.0d);
        bs.usagePowerMah = power;
        bs.usageTimeMs = time;
        bs.sumPower();
        this.mUsageList.add(bs);
        return bs;
    }

    public List<BatterySipper> getUsageList() {
        return this.mUsageList;
    }

    public List<BatterySipper> getMobilemsppList() {
        return this.mMobilemsppList;
    }

    public long getStatsPeriod() {
        return this.mStatsPeriod;
    }

    public int getStatsType() {
        return this.mStatsType;
    }

    public double getMaxPower() {
        return this.mMaxPower;
    }

    public double getMaxRealPower() {
        return this.mMaxRealPower;
    }

    public double getTotalPower() {
        return this.mTotalPower;
    }

    public double getComputedPower() {
        return this.mComputedPower;
    }

    public double getMinDrainedPower() {
        return this.mMinDrainedPower;
    }

    public double getMaxDrainedPower() {
        return this.mMaxDrainedPower;
    }

    public static byte[] readFully(FileInputStream stream) throws IOException {
        return readFully(stream, stream.available());
    }

    public static byte[] readFully(FileInputStream stream, int avail) throws IOException {
        int pos = 0;
        byte[] data = new byte[avail];
        while (true) {
            int amt = stream.read(data, pos, data.length - pos);
            if (amt <= 0) {
                return data;
            }
            pos += amt;
            avail = stream.available();
            if (avail > data.length - pos) {
                byte[] newData = new byte[(pos + avail)];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }

    public double removeHiddenBatterySippers(List<BatterySipper> sippers) {
        double proportionalSmearPowerMah = 0.0d;
        BatterySipper screenSipper = null;
        for (int i = sippers.size() - 1; i >= 0; i--) {
            BatterySipper sipper = (BatterySipper) sippers.get(i);
            sipper.shouldHide = shouldHideSipper(sipper);
            if (!(!sipper.shouldHide || sipper.drainType == DrainType.OVERCOUNTED || sipper.drainType == DrainType.SCREEN || sipper.drainType == DrainType.UNACCOUNTED || sipper.drainType == DrainType.BLUETOOTH || sipper.drainType == DrainType.WIFI || sipper.drainType == DrainType.IDLE)) {
                proportionalSmearPowerMah += sipper.totalPowerMah;
            }
            if (sipper.drainType == DrainType.SCREEN) {
                screenSipper = sipper;
            }
        }
        if (screenSipper != null) {
            smearScreenBatterySipper(sippers, screenSipper);
        }
        return proportionalSmearPowerMah;
    }

    public void smearScreenBatterySipper(List<BatterySipper> sippers, BatterySipper screenSipper) {
        int i;
        long totalActivityTimeMs = 0;
        SparseLongArray activityTimeArray = new SparseLongArray();
        int size = sippers.size();
        for (i = 0; i < size; i++) {
            Uid uid = ((BatterySipper) sippers.get(i)).uidObj;
            if (uid != null) {
                long timeMs = getProcessForegroundTimeMs(uid, 0);
                activityTimeArray.put(uid.getUid(), timeMs);
                totalActivityTimeMs += timeMs;
            }
        }
        if (screenSipper != null && totalActivityTimeMs >= 600000) {
            double screenPowerMah = screenSipper.totalPowerMah;
            size = sippers.size();
            for (i = 0; i < size; i++) {
                BatterySipper sipper = (BatterySipper) sippers.get(i);
                sipper.screenPowerMah = (((double) activityTimeArray.get(sipper.getUid(), 0)) * screenPowerMah) / ((double) totalActivityTimeMs);
            }
        }
    }

    public boolean shouldHideSipper(BatterySipper sipper) {
        DrainType drainType = sipper.drainType;
        if (drainType == DrainType.IDLE || drainType == DrainType.CELL || drainType == DrainType.SCREEN || drainType == DrainType.UNACCOUNTED || drainType == DrainType.OVERCOUNTED || isTypeService(sipper)) {
            return true;
        }
        return isTypeSystem(sipper);
    }

    public boolean isTypeService(BatterySipper sipper) {
        String[] packages = this.mPackageManager.getPackagesForUid(sipper.getUid());
        if (packages == null) {
            return false;
        }
        for (Object packageName : packages) {
            if (ArrayUtils.contains(this.mServicepackageArray, packageName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTypeSystem(BatterySipper sipper) {
        int uid = sipper.uidObj == null ? -1 : sipper.getUid();
        sipper.mPackages = this.mPackageManager.getPackagesForUid(uid);
        if (uid >= 0 && uid < 10000) {
            return true;
        }
        if (sipper.mPackages != null) {
            for (Object packageName : sipper.mPackages) {
                if (ArrayUtils.contains(this.mSystemPackageArray, packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public long convertUsToMs(long timeUs) {
        return timeUs / 1000;
    }

    public long convertMsToUs(long timeMs) {
        return 1000 * timeMs;
    }

    public long getForegroundActivityTotalTimeUs(Uid uid, long rawRealtimeUs) {
        Timer timer = uid.getForegroundActivityTimer();
        if (timer != null) {
            return timer.getTotalTimeLocked(rawRealtimeUs, 0);
        }
        return 0;
    }

    public long getProcessForegroundTimeMs(Uid uid, int which) {
        int i = 0;
        long rawRealTimeUs = convertMsToUs(SystemClock.elapsedRealtime());
        int[] foregroundTypes = new int[]{0};
        long timeUs = 0;
        while (i < foregroundTypes.length) {
            timeUs += uid.getProcessStateTime(foregroundTypes[i], rawRealTimeUs, which);
            i++;
        }
        return convertUsToMs(Math.min(timeUs, getForegroundActivityTotalTimeUs(uid, rawRealTimeUs)));
    }

    public void setPackageManager(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    public void setSystemPackageArray(String[] array) {
        this.mSystemPackageArray = array;
    }

    public void setServicePackageArray(String[] array) {
        this.mServicepackageArray = array;
    }

    private void load() {
        if (this.mBatteryInfo != null) {
            this.mStats = getStats(this.mBatteryInfo);
            if (this.mCollectBatteryBroadcast) {
                this.mBatteryBroadcast = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            }
        }
    }

    private static BatteryStatsImpl getStats(IBatteryStats service) {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        try {
            ParcelFileDescriptor pfd = service.getStatisticsStream();
            if (pfd != null) {
                FileInputStream fis = null;
                try {
                    FileInputStream fis2 = new AutoCloseInputStream(pfd);
                    try {
                        byte[] data = readFully(fis2, MemoryFile.getSize(pfd.getFileDescriptor()));
                        Parcel parcel = Parcel.obtain();
                        parcel.unmarshall(data, 0, data.length);
                        parcel.setDataPosition(0);
                        BatteryStatsImpl stats = (BatteryStatsImpl) BatteryStatsImpl.CREATOR.createFromParcel(parcel);
                        if (fis2 != null) {
                            try {
                                fis2.close();
                            } catch (Throwable th3) {
                                th2 = th3;
                            }
                        }
                        if (th2 == null) {
                            return stats;
                        }
                        try {
                            throw th2;
                        } catch (IOException e2) {
                            e = e2;
                            fis = fis2;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        fis = fis2;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (Throwable th5) {
                                if (th2 == null) {
                                    th2 = th5;
                                } else if (th2 != th5) {
                                    th2.addSuppressed(th5);
                                }
                            }
                        }
                        if (th2 == null) {
                            try {
                                throw th2;
                            } catch (IOException e3) {
                                e = e3;
                            }
                        } else {
                            throw th;
                        }
                    }
                } catch (Throwable th6) {
                    th = th6;
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable th52) {
                            if (th2 == null) {
                                th2 = th52;
                            } else if (th2 != th52) {
                                th2.addSuppressed(th52);
                            }
                        }
                    }
                    if (th2 == null) {
                        throw th;
                    } else {
                        try {
                            throw th2;
                        } catch (IOException e32) {
                            e = e32;
                        }
                    }
                }
            }
        } catch (RemoteException e4) {
            Log.w(TAG, "RemoteException:", e4);
        }
        return new BatteryStatsImpl();
        Log.w(TAG, "Unable to read statistics stream", e);
        return new BatteryStatsImpl();
    }
}
