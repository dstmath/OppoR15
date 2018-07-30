package com.android.server;

import android.annotation.OppoHook;
import android.annotation.OppoHook.OppoHookType;
import android.annotation.OppoHook.OppoRomType;
import android.app.ActivityManager;
import android.app.ActivityManager.OnUidImportanceListener;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OnOpChangedInternalListener;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.PendingIntent.OnFinished;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager.OnPermissionsChangedListener;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerInternal.PackagesProvider;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.location.ActivityRecognitionHardware;
import android.location.Address;
import android.location.Criteria;
import android.location.GeocoderParams;
import android.location.Geofence;
import android.location.IBatchedLocationCallback;
import android.location.IFusedGeofenceHardware;
import android.location.IGnssMeasurementsListener;
import android.location.IGnssNavigationMessageListener;
import android.location.IGnssStatusListener;
import android.location.IGnssStatusProvider;
import android.location.IGpsGeofenceHardware;
import android.location.ILocationListener;
import android.location.ILocationManager.Stub;
import android.location.INetInitiatedListener;
import android.location.LocAppsOp;
import android.location.Location;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.am.OppoAppStartupManager;
import com.android.server.am.OppoMultiAppManager;
import com.android.server.am.OppoPermissionConstants;
import com.android.server.location.ActivityRecognitionProxy;
import com.android.server.location.FlpHardwareProvider;
import com.android.server.location.FusedProxy;
import com.android.server.location.GeocoderProxy;
import com.android.server.location.GeofenceManager;
import com.android.server.location.GeofenceProxy;
import com.android.server.location.GnssLocationProvider;
import com.android.server.location.GnssLocationProvider.GnssBatchingProvider;
import com.android.server.location.GnssLocationProvider.GnssMetricsProvider;
import com.android.server.location.GnssLocationProvider.GnssSystemInfoProvider;
import com.android.server.location.GnssMeasurementsProvider;
import com.android.server.location.GnssNavigationMessageProvider;
import com.android.server.location.LocationBlacklist;
import com.android.server.location.LocationFudger;
import com.android.server.location.LocationProviderInterface;
import com.android.server.location.LocationProviderProxy;
import com.android.server.location.LocationRequestStatistics;
import com.android.server.location.LocationRequestStatistics.PackageProviderKey;
import com.android.server.location.LocationRequestStatistics.PackageStatistics;
import com.android.server.location.MockProvider;
import com.android.server.location.NavigationStatusMonitor;
import com.android.server.location.OppoGnssWhiteListProxy;
import com.android.server.location.OppoLocationBlacklist;
import com.android.server.location.PassiveProvider;
import com.oppo.neuron.NeuronSystemManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

public class LocationManagerService extends Stub {
    private static final String ACCESS_LOCATION_EXTRA_COMMANDS = "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS";
    private static final String ACCESS_MOCK_LOCATION = "android.permission.ACCESS_MOCK_LOCATION";
    private static final String ADMIN_PERM = "android.permission.WRITE_SECURE_SETTINGS";
    private static final String ADMIN_PERM_ERROR = "WRITE_SECURE_SETTINGS permission required";
    public static boolean D = Log.isLoggable("LocationManagerService", 3);
    private static final long DEFAULT_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final LocationRequest DEFAULT_LOCATION_REQUEST = new LocationRequest();
    private static final String EXTRA_GPS_PKGS = "pkgs";
    private static final int FOREGROUND_IMPORTANCE_CUTOFF = 125;
    private static final String FUSED_LOCATION_SERVICE_ACTION = "com.android.location.service.FusedLocationProvider";
    public static final String GPS_DEBUG_SWITCH = "gps_debug_switch";
    public static final String GPS_OPCUSTOM_FEATURE = "persist.sys.gps_disable";
    private static final String GPS_WORKSOURCE_CHANGE_ACTION = "android.location.GPS_WORKSOURCE_CHANGE";
    private static final long HIGH_POWER_INTERVAL_MS = 300000;
    private static final String INSTALL_LOCATION_PROVIDER = "android.permission.INSTALL_LOCATION_PROVIDER";
    private static final int MAX_PROVIDER_SCHEDULING_JITTER_MS = 100;
    private static final int MAX_SIZE = 3;
    private static final int MSG_LOCATION_CHANGED = 1;
    private static final int MSG_OP_CHANGED = 2;
    private static final long NANOS_PER_MILLI = 1000000;
    private static final String NETWORK_LOCATION_SERVICE_ACTION = "com.android.location.service.v3.NetworkLocationProvider";
    public static final int OPPO_FAKE_LOCATION_DATA_LENGTH = 3;
    public static final String OPPO_FAKE_LOCATION_SPLIT = "_";
    public static final String OPPO_FAKE_LOCATION_TEST = "oppo.locationtest.data";
    public static final String OPPO_FAKE_LOCATOIN_SWITCH_ON = "1";
    public static final String OPPO_FAKE_LOCATOIN_TEST_SWITCH = "oppo.locationtest.switch";
    private static final int RESOLUTION_LEVEL_COARSE = 1;
    private static final int RESOLUTION_LEVEL_FINE = 2;
    private static final int RESOLUTION_LEVEL_NONE = 0;
    private static final String TAG = "LocationManagerService";
    private static final String WAKELOCK_KEY = "LocationManagerService";
    private boolean isFakeLocationreporting = false;
    private boolean isSytemPropRooted = false;
    private ActivityManager mActivityManager;
    private IActivityManager mAm;
    private final AppOpsManager mAppOps;
    private final ArraySet<String> mBackgroundThrottlePackageWhitelist = new ArraySet();
    private LocationBlacklist mBlacklist;
    private String mComboNlpPackageName;
    private String mComboNlpReadyMarker;
    private String mComboNlpScreenMarker;
    private final Context mContext;
    private int mCurrentUserId = 0;
    private int[] mCurrentUserProfiles = new int[]{0};
    private final Set<String> mDisabledProviders = new HashSet();
    private final Set<String> mEnabledProviders = new HashSet();
    private GeocoderProxy mGeocodeProvider;
    private GeofenceManager mGeofenceManager;
    private IBatchedLocationCallback mGnssBatchingCallback;
    private LinkedCallback mGnssBatchingDeathCallback;
    private boolean mGnssBatchingInProgress = false;
    private GnssBatchingProvider mGnssBatchingProvider;
    private final ArrayMap<IGnssMeasurementsListener, Identity> mGnssMeasurementsListeners = new ArrayMap();
    private GnssMeasurementsProvider mGnssMeasurementsProvider;
    private GnssMetricsProvider mGnssMetricsProvider;
    private final ArrayMap<IGnssNavigationMessageListener, Identity> mGnssNavigationMessageListeners = new ArrayMap();
    private GnssNavigationMessageProvider mGnssNavigationMessageProvider;
    private GnssLocationProvider mGnssProvider;
    private IGnssStatusProvider mGnssStatusProvider;
    private GnssSystemInfoProvider mGnssSystemInfoProvider;
    private OppoGnssWhiteListProxy mGnssWhiteListProxy;
    private IGpsGeofenceHardware mGpsGeofenceProxy;
    private ArrayList<String> mGpsPkgs = new ArrayList(3);
    private boolean mIsRegister = false;
    private final HashMap<String, Location> mLastLocation = new HashMap();
    private final HashMap<String, Location> mLastLocationCoarseInterval = new HashMap();
    private boolean mLastNLPSetting = false;
    private LocationFudger mLocationFudger;
    private LocationWorkerHandler mLocationHandler;
    private final Object mLock = new Object();
    private final HashMap<String, MockProvider> mMockProviders = new HashMap();
    private INetInitiatedListener mNetInitiatedListener;
    private boolean mNetworkEnabled = true;
    private OppoLocationBlacklist mOppoBlackList;
    private PackageManager mPackageManager;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onPackageDisappeared(String packageName, int reason) {
            Throwable th;
            synchronized (LocationManagerService.this.mLock) {
                ArrayList<Receiver> deadReceivers = null;
                try {
                    Iterator receiver$iterator = LocationManagerService.this.mReceivers.values().iterator();
                    while (true) {
                        ArrayList<Receiver> deadReceivers2;
                        try {
                            deadReceivers2 = deadReceivers;
                            if (!receiver$iterator.hasNext()) {
                                break;
                            }
                            Receiver receiver = (Receiver) receiver$iterator.next();
                            if (receiver.mIdentity.mPackageName.equals(packageName)) {
                                if (deadReceivers2 == null) {
                                    deadReceivers = new ArrayList();
                                } else {
                                    deadReceivers = deadReceivers2;
                                }
                                deadReceivers.add(receiver);
                            } else {
                                deadReceivers = deadReceivers2;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            deadReceivers = deadReceivers2;
                            throw th;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }
    };
    private PassiveProvider mPassiveProvider;
    private PowerManager mPowerManager;
    private final IProcessObserver mProcessObserver = new IProcessObserver.Stub() {
        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (LocationManagerService.D) {
                Log.d("LocationManagerService", "pid:" + pid + ",uid:" + uid + ",fg:" + foregroundActivities);
            }
            synchronized (LocationManagerService.this.mLock) {
                if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked("gps")) {
                    ArrayList<UpdateRecord> records = (ArrayList) LocationManagerService.this.mRecordsByProvider.get("gps");
                    if (records == null || records.size() == 0) {
                    } else {
                        boolean needUpdate = false;
                        for (UpdateRecord record : records) {
                            if (record.mOp == 2 && record.mReceiver.mIdentity.mUid == uid) {
                                needUpdate = true;
                                break;
                            }
                        }
                        if (needUpdate) {
                            LocationManagerService.this.applyRequirementsLocked("gps");
                        }
                    }
                }
            }
        }

        public void onProcessDied(int pid, int uid) {
        }
    };
    private final ArrayList<LocationProviderInterface> mProviders = new ArrayList();
    private final HashMap<String, LocationProviderInterface> mProvidersByName = new HashMap();
    private final ArrayList<LocationProviderProxy> mProxyProviders = new ArrayList();
    private final HashMap<String, LocationProviderInterface> mRealProviders = new HashMap();
    private final HashMap<Object, Receiver> mReceivers = new HashMap();
    private final HashMap<String, ArrayList<UpdateRecord>> mRecordsByProvider = new HashMap();
    private final LocationRequestStatistics mRequestStatistics = new LocationRequestStatistics();
    private String mTopAppName = null;
    private UserManager mUserManager;

    private static final class Identity {
        final String mPackageName;
        final int mPid;
        final int mUid;

        Identity(int uid, int pid, String packageName) {
            this.mUid = uid;
            this.mPid = pid;
            this.mPackageName = packageName;
        }
    }

    private class LinkedCallback implements DeathRecipient {
        private final IBatchedLocationCallback mCallback;

        public LinkedCallback(IBatchedLocationCallback callback) {
            this.mCallback = callback;
        }

        public IBatchedLocationCallback getUnderlyingListener() {
            return this.mCallback;
        }

        public void binderDied() {
            Log.d("LocationManagerService", "Remote Batching Callback died: " + this.mCallback);
            LocationManagerService.this.stopGnssBatch();
            LocationManagerService.this.removeGnssBatchingCallback();
        }
    }

    private class LocationWorkerHandler extends Handler {
        public LocationWorkerHandler(Looper looper) {
            super(looper, null, true);
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 1:
                    LocationManagerService locationManagerService = LocationManagerService.this;
                    Location location = (Location) msg.obj;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    locationManagerService.handleLocationChanged(location, z);
                    break;
                case 2:
                    synchronized (LocationManagerService.this.mLock) {
                        ArrayList<UpdateRecord> records = (ArrayList) LocationManagerService.this.mRecordsByProvider.get("gps");
                        if (records != null && records.size() != 0) {
                            boolean needUpdate = false;
                            for (UpdateRecord record : records) {
                                int op = LocationManagerService.this.mOppoBlackList.getAppOp(record.mReceiver.mIdentity.mPackageName);
                                if (record.mOp != op) {
                                    needUpdate = true;
                                }
                                record.mOp = op;
                            }
                            if (needUpdate) {
                                if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked("gps")) {
                                    LocationManagerService.this.applyRequirementsLocked("gps");
                                    break;
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    private final class Receiver implements DeathRecipient, OnFinished {
        final int mAllowedResolutionLevel;
        final boolean mHideFromAppOps;
        final Identity mIdentity;
        final Object mKey;
        final ILocationListener mListener;
        boolean mOpHighPowerMonitoring;
        boolean mOpMonitoring;
        int mPendingBroadcasts;
        final PendingIntent mPendingIntent;
        final HashMap<String, UpdateRecord> mUpdateRecords = new HashMap();
        WakeLock mWakeLock;
        final WorkSource mWorkSource;

        Receiver(ILocationListener listener, PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
            this.mListener = listener;
            this.mPendingIntent = intent;
            if (listener != null) {
                this.mKey = listener.asBinder();
            } else {
                this.mKey = intent;
            }
            this.mAllowedResolutionLevel = LocationManagerService.this.getAllowedResolutionLevel(pid, uid);
            this.mIdentity = new Identity(uid, pid, packageName);
            if (workSource != null && workSource.size() <= 0) {
                workSource = null;
            }
            this.mWorkSource = workSource;
            this.mHideFromAppOps = hideFromAppOps;
            updateMonitoring(true);
            this.mWakeLock = LocationManagerService.this.mPowerManager.newWakeLock(1, "LocationManagerService");
            if (workSource == null) {
                workSource = new WorkSource(this.mIdentity.mUid, this.mIdentity.mPackageName);
            }
            this.mWakeLock.setWorkSource(workSource);
        }

        public boolean equals(Object otherObj) {
            return otherObj instanceof Receiver ? this.mKey.equals(((Receiver) otherObj).mKey) : false;
        }

        public int hashCode() {
            return this.mKey.hashCode();
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("Reciever[");
            s.append(Integer.toHexString(System.identityHashCode(this)));
            if (this.mListener != null) {
                s.append(" listener");
            } else {
                s.append(" intent");
            }
            for (String p : this.mUpdateRecords.keySet()) {
                s.append(" ").append(((UpdateRecord) this.mUpdateRecords.get(p)).toString());
            }
            s.append("]");
            return s.toString();
        }

        public void updateMonitoring(boolean allow) {
            if (!this.mHideFromAppOps) {
                boolean requestingLocation = false;
                boolean requestingHighPowerLocation = false;
                if (allow) {
                    for (UpdateRecord updateRecord : this.mUpdateRecords.values()) {
                        if (LocationManagerService.this.isAllowedByCurrentUserSettingsLocked(updateRecord.mProvider)) {
                            requestingLocation = true;
                            LocationProviderInterface locationProvider = (LocationProviderInterface) LocationManagerService.this.mProvidersByName.get(updateRecord.mProvider);
                            ProviderProperties properties = locationProvider != null ? locationProvider.getProperties() : null;
                            if (properties != null && properties.mPowerRequirement == 3 && updateRecord.mRequest.getInterval() < 300000) {
                                requestingHighPowerLocation = true;
                                break;
                            }
                        }
                    }
                }
                this.mOpMonitoring = updateMonitoring(requestingLocation, this.mOpMonitoring, 41);
                boolean wasHighPowerMonitoring = this.mOpHighPowerMonitoring;
                this.mOpHighPowerMonitoring = updateMonitoring(requestingHighPowerLocation, this.mOpHighPowerMonitoring, 42);
                if (this.mOpHighPowerMonitoring != wasHighPowerMonitoring) {
                    LocationManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.location.HIGH_POWER_REQUEST_CHANGE"), UserHandle.ALL);
                }
            }
        }

        private boolean updateMonitoring(boolean allowMonitoring, boolean currentlyMonitoring, int op) {
            boolean z = false;
            if (currentlyMonitoring) {
                if (!(allowMonitoring && LocationManagerService.this.mAppOps.checkOpNoThrow(op, this.mIdentity.mUid, this.mIdentity.mPackageName) == 0)) {
                    LocationManagerService.this.mAppOps.finishOp(op, this.mIdentity.mUid, this.mIdentity.mPackageName);
                    return false;
                }
            } else if (allowMonitoring) {
                if (LocationManagerService.this.mAppOps.startOpNoThrow(op, this.mIdentity.mUid, this.mIdentity.mPackageName) == 0) {
                    z = true;
                }
                return z;
            }
            return currentlyMonitoring;
        }

        public boolean isListener() {
            return this.mListener != null;
        }

        public boolean isPendingIntent() {
            return this.mPendingIntent != null;
        }

        public ILocationListener getListener() {
            if (this.mListener != null) {
                return this.mListener;
            }
            throw new IllegalStateException("Request for non-existent listener");
        }

        public boolean callStatusChangedLocked(String provider, int status, Bundle extras) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        this.mListener.onStatusChanged(provider, status, extras);
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent statusChanged = new Intent();
            statusChanged.putExtras(new Bundle(extras));
            statusChanged.putExtra("status", status);
            try {
                synchronized (this) {
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, statusChanged, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel));
                    incrementPendingBroadcastsLocked();
                }
            } catch (CanceledException e2) {
                return false;
            }
            return true;
        }

        public boolean callLocationChangedLocked(Location location) {
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        this.mListener.onLocationChanged(new Location(location));
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent locationChanged = new Intent();
            locationChanged.putExtra("location", new Location(location));
            try {
                synchronized (this) {
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, locationChanged, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel));
                    incrementPendingBroadcastsLocked();
                }
            } catch (CanceledException e2) {
                return false;
            }
            return true;
        }

        public boolean callProviderEnabledLocked(String provider, boolean enabled) {
            updateMonitoring(true);
            if (this.mListener != null) {
                try {
                    synchronized (this) {
                        if (enabled) {
                            this.mListener.onProviderEnabled(provider);
                        } else {
                            this.mListener.onProviderDisabled(provider);
                        }
                        incrementPendingBroadcastsLocked();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            Intent providerIntent = new Intent();
            providerIntent.putExtra("providerEnabled", enabled);
            try {
                synchronized (this) {
                    this.mPendingIntent.send(LocationManagerService.this.mContext, 0, providerIntent, this, LocationManagerService.this.mLocationHandler, LocationManagerService.this.getResolutionPermission(this.mAllowedResolutionLevel));
                    incrementPendingBroadcastsLocked();
                }
            } catch (CanceledException e2) {
                return false;
            }
            return true;
        }

        public void binderDied() {
            if (LocationManagerService.D) {
                Log.d("LocationManagerService", "Location listener died");
            }
            synchronized (LocationManagerService.this.mLock) {
                LocationManagerService.this.removeUpdatesLocked(this);
            }
            synchronized (this) {
                clearPendingBroadcastsLocked();
            }
        }

        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (this) {
                decrementPendingBroadcastsLocked();
            }
        }

        private void incrementPendingBroadcastsLocked() {
            int i = this.mPendingBroadcasts;
            this.mPendingBroadcasts = i + 1;
            if (i == 0) {
                this.mWakeLock.acquire();
            }
        }

        private void decrementPendingBroadcastsLocked() {
            int i = this.mPendingBroadcasts - 1;
            this.mPendingBroadcasts = i;
            if (i == 0 && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }

        public void clearPendingBroadcastsLocked() {
            if (this.mPendingBroadcasts > 0) {
                this.mPendingBroadcasts = 0;
                if (this.mWakeLock.isHeld()) {
                    this.mWakeLock.release();
                }
            }
        }
    }

    private class UpdateRecord {
        boolean mIsForegroundUid;
        Location mLastFixBroadcast;
        long mLastStatusBroadcast;
        int mOp;
        final String mProvider;
        final LocationRequest mRealRequest;
        final Receiver mReceiver;
        LocationRequest mRequest;

        UpdateRecord(String provider, LocationRequest request, Receiver receiver) {
            this.mProvider = provider;
            this.mRealRequest = request;
            this.mRequest = request;
            this.mReceiver = receiver;
            if ("gps".equals(request.getProvider())) {
                String packageName = this.mReceiver.mIdentity.mPackageName;
                if (LocationManagerService.this.isSystemApp(packageName) || LocationManagerService.this.mBackgroundThrottlePackageWhitelist.contains(packageName)) {
                    this.mOp = 1;
                } else {
                    this.mOp = LocationManagerService.this.mOppoBlackList.getAppOp(packageName);
                }
                if (LocationManagerService.D) {
                    Log.d("LocationManagerService", "pkg:" + packageName + ",Op:" + this.mOp);
                }
            }
            this.mIsForegroundUid = LocationManagerService.isImportanceForeground(LocationManagerService.this.mActivityManager.getPackageImportance(this.mReceiver.mIdentity.mPackageName));
            ArrayList<UpdateRecord> records = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(provider);
            if (records == null) {
                records = new ArrayList();
                LocationManagerService.this.mRecordsByProvider.put(provider, records);
            }
            if (!records.contains(this)) {
                records.add(this);
            }
            LocationManagerService.this.mRequestStatistics.startRequesting(this.mReceiver.mIdentity.mPackageName, provider, request.getInterval());
        }

        boolean isBlock() {
            if ("gps".equals(this.mRequest.getProvider())) {
                switch (this.mOp) {
                    case 2:
                        if (LocationManagerService.this.mIsRegister) {
                            if (LocationManagerService.this.mTopAppName == null) {
                                try {
                                    LocationManagerService.this.mTopAppName = LocationManagerService.this.mAm.getTopAppName().getPackageName();
                                    if (LocationManagerService.D) {
                                        Log.d("LocationManagerService", "topApp:" + LocationManagerService.this.mTopAppName);
                                    }
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                            return this.mReceiver.mIdentity.mPackageName.equals(LocationManagerService.this.mTopAppName) ^ 1;
                        }
                        break;
                    case 3:
                        return true;
                }
            }
            return false;
        }

        void disposeLocked(boolean removeReceiver) {
            LocationManagerService.this.mRequestStatistics.stopRequesting(this.mReceiver.mIdentity.mPackageName, this.mProvider);
            ArrayList<UpdateRecord> globalRecords = (ArrayList) LocationManagerService.this.mRecordsByProvider.get(this.mProvider);
            if (globalRecords != null) {
                globalRecords.remove(this);
            }
            if (removeReceiver) {
                HashMap<String, UpdateRecord> receiverRecords = this.mReceiver.mUpdateRecords;
                if (receiverRecords != null) {
                    receiverRecords.remove(this.mProvider);
                    if (receiverRecords.size() == 0) {
                        LocationManagerService.this.removeUpdatesLocked(this.mReceiver);
                    }
                }
            }
        }

        public String toString() {
            return "UpdateRecord[" + this.mProvider + " " + this.mReceiver.mIdentity.mPackageName + "(" + this.mReceiver.mIdentity.mUid + (this.mIsForegroundUid ? " foreground" : " background") + ")" + " " + this.mRealRequest + "]";
        }
    }

    public LocationManagerService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setLocationPackagesProvider(new PackagesProvider() {
            public String[] getPackages(int userId) {
                return LocationManagerService.this.mContext.getResources().getStringArray(17236016);
            }
        });
        if (D) {
            Log.d("LocationManagerService", "Constructed");
        }
    }

    public void systemRunning() {
        synchronized (this.mLock) {
            boolean z;
            if (D) {
                Log.d("LocationManagerService", "systemRunning()");
            }
            this.mPackageManager = this.mContext.getPackageManager();
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mActivityManager = (ActivityManager) this.mContext.getSystemService(OppoAppStartupManager.TYPE_ACTIVITY);
            this.mLocationHandler = new LocationWorkerHandler(BackgroundThread.get().getLooper());
            this.mLocationFudger = new LocationFudger(this.mContext, this.mLocationHandler);
            this.mBlacklist = new LocationBlacklist(this.mContext, this.mLocationHandler);
            this.mBlacklist.init();
            this.mGeofenceManager = new GeofenceManager(this.mContext, this.mBlacklist);
            this.mAm = ActivityManagerNative.getDefault();
            this.mOppoBlackList = new OppoLocationBlacklist(this.mContext, this.mLocationHandler);
            this.mOppoBlackList.init();
            this.mAppOps.startWatchingMode(0, null, new OnOpChangedInternalListener() {
                public void onOpChanged(int op, String packageName) {
                    synchronized (LocationManagerService.this.mLock) {
                        for (Receiver receiver : LocationManagerService.this.mReceivers.values()) {
                            receiver.updateMonitoring(true);
                        }
                        LocationManagerService.this.applyAllProviderRequirementsLocked();
                    }
                }
            });
            this.mPackageManager.addOnPermissionsChangeListener(new OnPermissionsChangedListener() {
                public void onPermissionsChanged(int uid) {
                    synchronized (LocationManagerService.this.mLock) {
                        LocationManagerService.this.applyAllProviderRequirementsLocked();
                    }
                }
            });
            this.mActivityManager.addOnUidImportanceListener(new OnUidImportanceListener() {
                public void onUidImportance(final int uid, final int importance) {
                    LocationManagerService.this.mLocationHandler.post(new Runnable() {
                        public void run() {
                            LocationManagerService.this.onUidImportanceChanged(uid, importance);
                        }
                    });
                }
            }, FOREGROUND_IMPORTANCE_CUTOFF);
            this.mUserManager = (UserManager) this.mContext.getSystemService("user");
            updateUserProfiles(this.mCurrentUserId);
            updateBackgroundThrottlingWhitelistLocked();
            loadProvidersLocked();
            updateProvidersLocked();
            if (SystemProperties.getInt("ro.secure", 1) == 0) {
                z = true;
            } else {
                z = false;
            }
            this.isSytemPropRooted = z;
        }
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("location_providers_allowed"), true, new ContentObserver(this.mLocationHandler) {
            public void onChange(boolean selfChange) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("location_background_throttle_interval_ms"), true, new ContentObserver(this.mLocationHandler) {
            public void onChange(boolean selfChange) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("location_background_throttle_package_whitelist"), true, new ContentObserver(this.mLocationHandler) {
            public void onChange(boolean selfChange) {
                synchronized (LocationManagerService.this.mLock) {
                    LocationManagerService.this.updateBackgroundThrottlingWhitelistLocked();
                    LocationManagerService.this.updateProvidersLocked();
                }
            }
        }, -1);
        this.mPackageMonitor.register(this.mContext, this.mLocationHandler.getLooper(), true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    LocationManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action) || "android.intent.action.MANAGED_PROFILE_REMOVED".equals(action)) {
                    LocationManagerService.this.updateUserProfiles(LocationManagerService.this.mCurrentUserId);
                } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                    if (LocationManagerService.D) {
                        Log.d("LocationManagerService", "Shutdown received with UserId: " + getSendingUserId());
                    }
                    if (getSendingUserId() == -1) {
                        LocationManagerService.this.shutdownComponents();
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mLocationHandler);
    }

    private void onUidImportanceChanged(int uid, int importance) {
        boolean foreground = isImportanceForeground(importance);
        HashSet<String> affectedProviders = new HashSet(this.mRecordsByProvider.size());
        synchronized (this.mLock) {
            String provider;
            for (Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                provider = (String) entry.getKey();
                for (UpdateRecord record : (ArrayList) entry.getValue()) {
                    if (record.mReceiver.mIdentity.mUid == uid && record.mIsForegroundUid != foreground) {
                        if (D) {
                            Log.d("LocationManagerService", "request from uid " + uid + " is now " + (foreground ? "foreground" : "background)"));
                        }
                        record.mIsForegroundUid = foreground;
                        if (!isThrottlingExemptLocked(record.mReceiver.mIdentity)) {
                            affectedProviders.add(provider);
                        }
                    }
                }
            }
            for (String provider2 : affectedProviders) {
                applyRequirementsLocked(provider2);
            }
            for (Entry<IGnssMeasurementsListener, Identity> entry2 : this.mGnssMeasurementsListeners.entrySet()) {
                if (((Identity) entry2.getValue()).mUid == uid) {
                    if (D) {
                        Log.d("LocationManagerService", "gnss measurements listener from uid " + uid + " is now " + (foreground ? "foreground" : "background)"));
                    }
                    if (foreground || isThrottlingExemptLocked((Identity) entry2.getValue())) {
                        this.mGnssMeasurementsProvider.addListener((IGnssMeasurementsListener) entry2.getKey());
                    } else {
                        this.mGnssMeasurementsProvider.removeListener((IGnssMeasurementsListener) entry2.getKey());
                    }
                }
            }
            for (Entry<IGnssNavigationMessageListener, Identity> entry3 : this.mGnssNavigationMessageListeners.entrySet()) {
                if (((Identity) entry3.getValue()).mUid == uid) {
                    if (D) {
                        Log.d("LocationManagerService", "gnss navigation message listener from uid " + uid + " is now " + (foreground ? "foreground" : "background)"));
                    }
                    if (foreground || isThrottlingExemptLocked((Identity) entry3.getValue())) {
                        this.mGnssNavigationMessageProvider.addListener((IGnssNavigationMessageListener) entry3.getKey());
                    } else {
                        this.mGnssNavigationMessageProvider.removeListener((IGnssNavigationMessageListener) entry3.getKey());
                    }
                }
            }
        }
    }

    private static boolean isImportanceForeground(int importance) {
        return importance <= FOREGROUND_IMPORTANCE_CUTOFF;
    }

    private void shutdownComponents() {
        if (D) {
            Log.d("LocationManagerService", "Shutting down components...");
        }
        LocationProviderInterface gpsProvider = (LocationProviderInterface) this.mProvidersByName.get("gps");
        if (gpsProvider != null && gpsProvider.isEnabled()) {
            gpsProvider.disable();
        }
        if (FlpHardwareProvider.isSupported()) {
            FlpHardwareProvider.getInstance(this.mContext).cleanup();
        }
        this.mOppoBlackList.shutdown();
    }

    void updateUserProfiles(int currentUserId) {
        int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(currentUserId);
        synchronized (this.mLock) {
            this.mCurrentUserProfiles = profileIds;
        }
    }

    private boolean isCurrentProfile(int userId) {
        if (OppoMultiAppManager.getInstance().isCurrentProfile(userId)) {
            return true;
        }
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mCurrentUserProfiles, userId);
        }
        return contains;
    }

    private void ensureFallbackFusedProviderPresentLocked(ArrayList<String> pkgs) {
        PackageManager pm = this.mContext.getPackageManager();
        String systemPackageName = this.mContext.getPackageName();
        ArrayList<HashSet<Signature>> sigSets = ServiceWatcher.getSignatureSets(this.mContext, pkgs);
        for (ResolveInfo rInfo : pm.queryIntentServicesAsUser(new Intent(FUSED_LOCATION_SERVICE_ACTION), 128, this.mCurrentUserId)) {
            String packageName = rInfo.serviceInfo.packageName;
            try {
                if (!ServiceWatcher.isSignatureMatch(pm.getPackageInfo(packageName, 64).signatures, sigSets)) {
                    Log.w("LocationManagerService", packageName + " resolves service " + FUSED_LOCATION_SERVICE_ACTION + ", but has wrong signature, ignoring");
                } else if (rInfo.serviceInfo.metaData == null) {
                    Log.w("LocationManagerService", "Found fused provider without metadata: " + packageName);
                } else if (rInfo.serviceInfo.metaData.getInt(ServiceWatcher.EXTRA_SERVICE_VERSION, -1) == 0) {
                    if ((rInfo.serviceInfo.applicationInfo.flags & 1) == 0) {
                        if (D) {
                            Log.d("LocationManagerService", "Fallback candidate not in /system: " + packageName);
                        }
                    } else if (pm.checkSignatures(systemPackageName, packageName) != 0) {
                        if (D) {
                            Log.d("LocationManagerService", "Fallback candidate not signed the same as system: " + packageName);
                        }
                    } else if (D) {
                        Log.d("LocationManagerService", "Found fallback provider: " + packageName);
                        return;
                    } else {
                        return;
                    }
                } else if (D) {
                    Log.d("LocationManagerService", "Fallback candidate not version 0: " + packageName);
                }
            } catch (NameNotFoundException e) {
                Log.e("LocationManagerService", "missing package: " + packageName);
            }
        }
        throw new IllegalStateException("Unable to find a fused location provider that is in the system partition with version 0 and signed with the platform certificate. Such a package is needed to provide a default fused location provider in the event that no other fused location provider has been installed or is currently available. For example, coreOnly boot mode when decrypting the data partition. The fallback must also be marked coreApp=\"true\" in the manifest");
    }

    private void loadProvidersLocked() {
        FlpHardwareProvider flpHardwareProvider;
        IFusedGeofenceHardware geofenceHardware;
        PassiveProvider passiveProvider = new PassiveProvider(this);
        addProviderLocked(passiveProvider);
        this.mEnabledProviders.add(passiveProvider.getName());
        this.mPassiveProvider = passiveProvider;
        if (GnssLocationProvider.isSupported()) {
            GnssLocationProvider gnssLocationProvider = new GnssLocationProvider(this.mContext, this, this.mOppoBlackList, this.mLocationHandler.getLooper());
            this.mGnssProvider = gnssLocationProvider;
            this.mGnssSystemInfoProvider = gnssLocationProvider.getGnssSystemInfoProvider();
            this.mGnssBatchingProvider = gnssLocationProvider.getGnssBatchingProvider();
            this.mGnssMetricsProvider = gnssLocationProvider.getGnssMetricsProvider();
            this.mGnssStatusProvider = gnssLocationProvider.getGnssStatusProvider();
            this.mNetInitiatedListener = gnssLocationProvider.getNetInitiatedListener();
            addProviderLocked(gnssLocationProvider);
            this.mRealProviders.put("gps", gnssLocationProvider);
            this.mGnssMeasurementsProvider = gnssLocationProvider.getGnssMeasurementsProvider();
            this.mGnssNavigationMessageProvider = gnssLocationProvider.getGnssNavigationMessageProvider();
            this.mGpsGeofenceProxy = gnssLocationProvider.getGpsGeofenceProxy();
        }
        Resources resources = this.mContext.getResources();
        ArrayList<String> providerPackageNames = new ArrayList();
        String[] pkgs = resources.getStringArray(17236016);
        if (D) {
            Log.d("LocationManagerService", "certificates for location providers pulled from: " + Arrays.toString(pkgs));
        }
        if (pkgs != null) {
            providerPackageNames.addAll(Arrays.asList(pkgs));
        }
        ensureFallbackFusedProviderPresentLocked(providerPackageNames);
        LocationProviderProxy networkProvider = LocationProviderProxy.createAndBind(this.mContext, "network", NETWORK_LOCATION_SERVICE_ACTION, 17956959, 17039702, 17236016, this.mLocationHandler);
        if (networkProvider != null) {
            this.mRealProviders.put("network", networkProvider);
            this.mProxyProviders.add(networkProvider);
            addProviderLocked(networkProvider);
        } else {
            Slog.w("LocationManagerService", "no network location provider found");
        }
        LocationProviderProxy fusedLocationProvider = LocationProviderProxy.createAndBind(this.mContext, "fused", FUSED_LOCATION_SERVICE_ACTION, 17956950, 17039681, 17236016, this.mLocationHandler);
        if (fusedLocationProvider != null) {
            addProviderLocked(fusedLocationProvider);
            this.mProxyProviders.add(fusedLocationProvider);
            this.mEnabledProviders.add(fusedLocationProvider.getName());
            this.mRealProviders.put("fused", fusedLocationProvider);
        } else {
            Slog.e("LocationManagerService", "no fused location provider found", new IllegalStateException("Location service needs a fused location provider"));
        }
        this.mGeocodeProvider = GeocoderProxy.createAndBind(this.mContext, 17956959, 17039702, 17236016, this.mLocationHandler);
        if (this.mGeocodeProvider == null) {
            Slog.e("LocationManagerService", "no geocoder provider found");
        }
        if (FlpHardwareProvider.isSupported()) {
            flpHardwareProvider = FlpHardwareProvider.getInstance(this.mContext);
            if (FusedProxy.createAndBind(this.mContext, this.mLocationHandler, flpHardwareProvider.getLocationHardware(), 17956954, 17039684, 17236016) == null) {
                Slog.d("LocationManagerService", "Unable to bind FusedProxy.");
            }
        } else {
            flpHardwareProvider = null;
            Slog.d("LocationManagerService", "FLP HAL not supported");
        }
        Context context = this.mContext;
        Handler handler = this.mLocationHandler;
        IGpsGeofenceHardware iGpsGeofenceHardware = this.mGpsGeofenceProxy;
        if (flpHardwareProvider != null) {
            geofenceHardware = flpHardwareProvider.getGeofenceHardware();
        } else {
            geofenceHardware = null;
        }
        if (GeofenceProxy.createAndBind(context, 17956952, 17039683, 17236016, handler, iGpsGeofenceHardware, geofenceHardware) == null) {
            Slog.d("LocationManagerService", "Unable to bind FLP Geofence proxy.");
        }
        boolean activityRecognitionHardwareIsSupported = ActivityRecognitionHardware.isSupported();
        ActivityRecognitionHardware activityRecognitionHardware = null;
        if (activityRecognitionHardwareIsSupported) {
            activityRecognitionHardware = ActivityRecognitionHardware.getInstance(this.mContext);
        } else {
            Slog.d("LocationManagerService", "Hardware Activity-Recognition not supported.");
        }
        if (ActivityRecognitionProxy.createAndBind(this.mContext, this.mLocationHandler, activityRecognitionHardwareIsSupported, activityRecognitionHardware, 17956944, 17039645, 17236016) == null) {
            Slog.d("LocationManagerService", "Unable to bind ActivityRecognitionProxy.");
        }
        this.mComboNlpPackageName = resources.getString(17039652);
        if (this.mComboNlpPackageName != null) {
            this.mComboNlpReadyMarker = this.mComboNlpPackageName + ".nlp:ready";
            this.mComboNlpScreenMarker = this.mComboNlpPackageName + ".nlp:screen";
        }
        for (String split : resources.getStringArray(17236041)) {
            String[] fragments = split.split(",");
            String name = fragments[0].trim();
            if (this.mProvidersByName.get(name) != null) {
                throw new IllegalArgumentException("Provider \"" + name + "\" already exists");
            }
            addTestProviderLocked(name, new ProviderProperties(Boolean.parseBoolean(fragments[1]), Boolean.parseBoolean(fragments[2]), Boolean.parseBoolean(fragments[3]), Boolean.parseBoolean(fragments[4]), Boolean.parseBoolean(fragments[5]), Boolean.parseBoolean(fragments[6]), Boolean.parseBoolean(fragments[7]), Integer.parseInt(fragments[8]), Integer.parseInt(fragments[9])));
        }
        this.mGnssWhiteListProxy = OppoGnssWhiteListProxy.getInstall(this.mContext);
        this.mGnssWhiteListProxy.setIsDebug(D);
        enableLogInternal(getVerboseLoggingLevel());
    }

    private void switchUser(int userId) {
        if (this.mCurrentUserId != userId) {
            this.mBlacklist.switchUser(userId);
            this.mLocationHandler.removeMessages(1);
            synchronized (this.mLock) {
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
                for (LocationProviderInterface p : this.mProviders) {
                    updateProviderListenersLocked(p.getName(), false);
                }
                this.mCurrentUserId = userId;
                updateUserProfiles(userId);
                updateProvidersLocked();
            }
        }
    }

    public void locationCallbackFinished(ILocationListener listener) {
        synchronized (this.mLock) {
            Receiver receiver = (Receiver) this.mReceivers.get(listener.asBinder());
            if (receiver != null) {
                synchronized (receiver) {
                    long identity = Binder.clearCallingIdentity();
                    receiver.decrementPendingBroadcastsLocked();
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    public int getGnssYearOfHardware() {
        if (this.mGnssSystemInfoProvider != null) {
            return this.mGnssSystemInfoProvider.getGnssYearOfHardware();
        }
        return 0;
    }

    private boolean hasGnssPermissions(String packageName) {
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, "gps");
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            boolean hasLocationAccess = checkLocationAccess(pid, uid, packageName, allowedResolutionLevel);
            return hasLocationAccess;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getGnssBatchSize(String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(packageName) || this.mGnssBatchingProvider == null) {
            return 0;
        }
        return this.mGnssBatchingProvider.getSize();
    }

    public boolean addGnssBatchingCallback(IBatchedLocationCallback callback, String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(packageName) || this.mGnssBatchingProvider == null) {
            return false;
        }
        this.mGnssBatchingCallback = callback;
        this.mGnssBatchingDeathCallback = new LinkedCallback(callback);
        try {
            callback.asBinder().linkToDeath(this.mGnssBatchingDeathCallback, 0);
            return true;
        } catch (RemoteException e) {
            Log.e("LocationManagerService", "Remote listener already died.", e);
            return false;
        }
    }

    public void removeGnssBatchingCallback() {
        try {
            this.mGnssBatchingCallback.asBinder().unlinkToDeath(this.mGnssBatchingDeathCallback, 0);
        } catch (NoSuchElementException e) {
            Log.e("LocationManagerService", "Couldn't unlink death callback.", e);
        }
        this.mGnssBatchingCallback = null;
        this.mGnssBatchingDeathCallback = null;
    }

    public boolean startGnssBatch(long periodNanos, boolean wakeOnFifoFull, String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (!hasGnssPermissions(packageName) || this.mGnssBatchingProvider == null) {
            return false;
        }
        if (this.mGnssBatchingInProgress) {
            Log.e("LocationManagerService", "startGnssBatch unexpectedly called w/o stopping prior batch");
            stopGnssBatch();
        }
        this.mGnssBatchingInProgress = true;
        return this.mGnssBatchingProvider.start(periodNanos, wakeOnFifoFull);
    }

    public void flushGnssBatch(String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (hasGnssPermissions(packageName)) {
            if (!this.mGnssBatchingInProgress) {
                Log.w("LocationManagerService", "flushGnssBatch called with no batch in progress");
            }
            if (this.mGnssBatchingProvider != null) {
                this.mGnssBatchingProvider.flush();
            }
            return;
        }
        Log.e("LocationManagerService", "flushGnssBatch called without GNSS permissions");
    }

    public boolean stopGnssBatch() {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "Location Hardware permission not granted to access hardware batching");
        if (this.mGnssBatchingProvider == null) {
            return false;
        }
        this.mGnssBatchingInProgress = false;
        return this.mGnssBatchingProvider.stop();
    }

    public void reportLocationBatch(List<Location> locations) {
        checkCallerIsProvider();
        if (!isAllowedByCurrentUserSettingsLocked("gps")) {
            Slog.w("LocationManagerService", "reportLocationBatch() called without user permission, locations blocked");
        } else if (this.mGnssBatchingCallback == null) {
            Slog.e("LocationManagerService", "reportLocationBatch() called without active Callback");
        } else {
            try {
                this.mGnssBatchingCallback.onLocationBatch(locations);
            } catch (RemoteException e) {
                Slog.e("LocationManagerService", "mGnssBatchingCallback.onLocationBatch failed", e);
            }
        }
    }

    public int getVerboseLoggingLevel() {
        return Global.getInt(this.mContext.getContentResolver(), GPS_DEBUG_SWITCH, 0);
    }

    public void enableVerboseLogging(int verbose) {
        Global.putInt(this.mContext.getContentResolver(), GPS_DEBUG_SWITCH, verbose);
        SystemProperties.set("persist.sys.oppo.chatty", verbose > 0 ? "0" : OPPO_FAKE_LOCATOIN_SWITCH_ON);
        enableLogInternal(verbose);
    }

    private void enableLogInternal(int verbose) {
        boolean z = false;
        if (verbose > 0) {
            z = true;
        }
        D = z;
        this.mGnssProvider.enableVerboseLogging(verbose);
        this.mOppoBlackList.enableLog(verbose);
    }

    public List<String> getNavigationPackagesList() {
        Throwable th;
        List<String> list = null;
        int mode = NavigationStatusMonitor.getNavigateMode();
        if (1 == mode) {
            synchronized (this.mLock) {
                try {
                    ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get("gps");
                    if (records != null) {
                        ArrayList<String> pkgList = new ArrayList();
                        try {
                            for (UpdateRecord r : records) {
                                pkgList.add(r.mReceiver.mIdentity.mPackageName);
                            }
                            list = pkgList;
                        } catch (Throwable th2) {
                            th = th2;
                            ArrayList<String> arrayList = pkgList;
                            throw th;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
        if (D) {
            Log.d("LocationManagerService", "PF03:NAV Mode:" + mode + ",pkg:" + list);
        }
        return list;
    }

    private void addProviderLocked(LocationProviderInterface provider) {
        this.mProviders.add(provider);
        this.mProvidersByName.put(provider.getName(), provider);
    }

    private void removeProviderLocked(LocationProviderInterface provider) {
        provider.disable();
        this.mProviders.remove(provider);
        this.mProvidersByName.remove(provider.getName());
    }

    private boolean isAllowedByCurrentUserSettingsLocked(String provider) {
        if (this.mEnabledProviders.contains(provider)) {
            return true;
        }
        if (this.mDisabledProviders.contains(provider)) {
            return false;
        }
        return Secure.isLocationProviderEnabledForUser(this.mContext.getContentResolver(), provider, this.mCurrentUserId);
    }

    private boolean isAllowedByCurrentUserSettingsLockedForOppo(String provider) {
        if (this.mEnabledProviders.contains(provider)) {
            return true;
        }
        if (this.mDisabledProviders.contains(provider)) {
            return false;
        }
        boolean isEnabled = Secure.isLocationProviderEnabledForUser(this.mContext.getContentResolver(), provider, this.mCurrentUserId);
        if (this.mGnssWhiteListProxy != null && this.mGnssWhiteListProxy.isNetworkLocationAlwayOn() && provider.equals("network")) {
            this.mNetworkEnabled = isEnabled;
            isEnabled = true;
            if (!this.mNetworkEnabled && D) {
                Log.d("LocationManagerService", "--allow starting --");
            }
        }
        return isEnabled;
    }

    private boolean isAllowedByUserSettingsLockedForOppo(String provider, int uid) {
        if (isCurrentProfile(UserHandle.getUserId(uid)) || (isUidALocationProvider(uid) ^ 1) == 0) {
            return isAllowedByCurrentUserSettingsLockedForOppo(provider);
        }
        return false;
    }

    private boolean isAllowedLocationRequested(String provider, int uid, String packageName) {
        boolean isAllowed = isAllowedByUserSettingsLockedForOppo(provider, uid);
        if (provider.equals("network")) {
            isAllowed = !this.mNetworkEnabled ? isNetworkWhiteList(packageName) : true;
            if (!this.mNetworkEnabled && D) {
                Log.d("LocationManagerService", "allow network request--" + packageName + ", " + isNetworkWhiteList(packageName));
            }
        }
        return isAllowed;
    }

    private boolean isAllowedLocationChanged(String provider, String packageName) {
        boolean z = true;
        if (!provider.equals("network")) {
            return true;
        }
        if (!this.mNetworkEnabled && D) {
            Log.d("LocationManagerService", "allow network location--" + packageName + ", " + isNetworkWhiteList(packageName));
        }
        if (!this.mNetworkEnabled) {
            z = isNetworkWhiteList(packageName);
        }
        return z;
    }

    private boolean isNetworkWhiteList(String packageName) {
        if (packageName == null || this.mGnssWhiteListProxy == null) {
            return false;
        }
        return this.mGnssWhiteListProxy.inNetworkLocationWhiteList(packageName);
    }

    private boolean isAllowedByUserSettingsLocked(String provider, int uid) {
        if (isCurrentProfile(UserHandle.getUserId(uid)) || (isUidALocationProvider(uid) ^ 1) == 0) {
            return isAllowedByCurrentUserSettingsLocked(provider);
        }
        return false;
    }

    private String getResolutionPermission(int resolutionLevel) {
        switch (resolutionLevel) {
            case 1:
                return "android.permission.ACCESS_COARSE_LOCATION";
            case 2:
                return OppoPermissionConstants.PERMISSION_ACCESS;
            default:
                return null;
        }
    }

    private int getAllowedResolutionLevel(int pid, int uid) {
        if (this.mContext.checkPermission(OppoPermissionConstants.PERMISSION_ACCESS, pid, uid) == 0) {
            return 2;
        }
        if (this.mContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid) == 0) {
            return 1;
        }
        return 0;
    }

    private int getCallerAllowedResolutionLevel() {
        return getAllowedResolutionLevel(Binder.getCallingPid(), Binder.getCallingUid());
    }

    private void checkResolutionLevelIsSufficientForGeofenceUse(int allowedResolutionLevel) {
        if (allowedResolutionLevel < 2) {
            throw new SecurityException("Geofence usage requires ACCESS_FINE_LOCATION permission");
        }
    }

    private int getMinimumResolutionLevelForProviderUse(String provider) {
        if ("gps".equals(provider) || "passive".equals(provider)) {
            return 2;
        }
        if ("network".equals(provider) || "fused".equals(provider)) {
            return 1;
        }
        LocationProviderInterface lp = (LocationProviderInterface) this.mMockProviders.get(provider);
        if (lp != null) {
            ProviderProperties properties = lp.getProperties();
            if (properties == null || properties.mRequiresSatellite) {
                return 2;
            }
            if (properties.mRequiresNetwork || properties.mRequiresCell) {
                return 1;
            }
        }
        return 2;
    }

    private void checkResolutionLevelIsSufficientForProviderUse(int allowedResolutionLevel, String providerName) {
        int requiredResolutionLevel = getMinimumResolutionLevelForProviderUse(providerName);
        if (allowedResolutionLevel < requiredResolutionLevel) {
            switch (requiredResolutionLevel) {
                case 1:
                    throw new SecurityException("\"" + providerName + "\" location provider " + "requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission.");
                case 2:
                    throw new SecurityException("\"" + providerName + "\" location provider " + "requires ACCESS_FINE_LOCATION permission.");
                default:
                    throw new SecurityException("Insufficient permission for \"" + providerName + "\" location provider.");
            }
        }
    }

    private void checkDeviceStatsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
    }

    private void checkUpdateAppOpsAllowed() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_APP_OPS_STATS", null);
    }

    public static int resolutionLevelToOp(int allowedResolutionLevel) {
        if (allowedResolutionLevel != 0) {
            return allowedResolutionLevel == 1 ? 0 : 1;
        } else {
            return -1;
        }
    }

    boolean reportLocationAccessNoThrow(int pid, int uid, String packageName, int allowedResolutionLevel) {
        boolean z = false;
        int op = resolutionLevelToOp(allowedResolutionLevel);
        if (op >= 0 && this.mAppOps.noteOpNoThrow(op, uid, packageName) != 0) {
            return false;
        }
        if (getAllowedResolutionLevel(pid, uid) >= allowedResolutionLevel) {
            z = true;
        }
        return z;
    }

    boolean checkLocationAccess(int pid, int uid, String packageName, int allowedResolutionLevel) {
        boolean z = false;
        int op = resolutionLevelToOp(allowedResolutionLevel);
        if (op >= 0 && this.mAppOps.checkOp(op, uid, packageName) != 0) {
            return false;
        }
        if (getAllowedResolutionLevel(pid, uid) >= allowedResolutionLevel) {
            z = true;
        }
        return z;
    }

    public List<String> getAllProviders() {
        ArrayList<String> out;
        synchronized (this.mLock) {
            out = new ArrayList(this.mProviders.size());
            for (LocationProviderInterface provider : this.mProviders) {
                String name = provider.getName();
                if (!"fused".equals(name)) {
                    out.add(name);
                }
            }
        }
        if (D) {
            Log.d("LocationManagerService", "getAllProviders()=" + out);
        }
        return out;
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            ArrayList<String> out;
            synchronized (this.mLock) {
                out = new ArrayList(this.mProviders.size());
                for (LocationProviderInterface provider : this.mProviders) {
                    String name = provider.getName();
                    if (!"fused".equals(name) && allowedResolutionLevel >= getMinimumResolutionLevelForProviderUse(name)) {
                        if ((!enabledOnly || (isAllowedByUserSettingsLocked(name, uid) ^ 1) == 0) && (criteria == null || (LocationProvider.propertiesMeetCriteria(name, provider.getProperties(), criteria) ^ 1) == 0)) {
                            out.add(name);
                        }
                    }
                }
            }
            if (D) {
                Log.d("LocationManagerService", "getProviders()=" + out);
            }
            return out;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        List<String> providers = getProviders(criteria, enabledOnly);
        String result;
        if (providers.isEmpty()) {
            providers = getProviders(null, enabledOnly);
            if (providers.isEmpty()) {
                if (D) {
                    Log.d("LocationManagerService", "getBestProvider(" + criteria + ", " + enabledOnly + ")=" + null);
                }
                return null;
            }
            result = pickBest(providers);
            if (D) {
                Log.d("LocationManagerService", "getBestProvider(" + criteria + ", " + enabledOnly + ")=" + result);
            }
            return result;
        }
        result = pickBest(providers);
        if (D) {
            Log.d("LocationManagerService", "getBestProvider(" + criteria + ", " + enabledOnly + ")=" + result);
        }
        return result;
    }

    private String pickBest(List<String> providers) {
        if (providers.contains("gps")) {
            return "gps";
        }
        if (providers.contains("network")) {
            return "network";
        }
        return (String) providers.get(0);
    }

    public boolean providerMeetsCriteria(String provider, Criteria criteria) {
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p == null) {
            throw new IllegalArgumentException("provider=" + provider);
        }
        boolean result = LocationProvider.propertiesMeetCriteria(p.getName(), p.getProperties(), criteria);
        if (D) {
            Log.d("LocationManagerService", "providerMeetsCriteria(" + provider + ", " + criteria + ")=" + result);
        }
        return result;
    }

    private void updateProvidersLocked() {
        boolean changesMade = false;
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            LocationProviderInterface p = (LocationProviderInterface) this.mProviders.get(i);
            boolean isEnabled = p.isEnabled();
            String name = p.getName();
            boolean shouldBeEnabled = isAllowedByCurrentUserSettingsLockedForOppo(name);
            if (isEnabled && (shouldBeEnabled ^ 1) != 0) {
                updateProviderListenersLocked(name, false);
                this.mLastLocation.clear();
                this.mLastLocationCoarseInterval.clear();
                changesMade = true;
            } else if (!isEnabled && shouldBeEnabled) {
                updateProviderListenersLocked(name, true);
                changesMade = true;
            }
        }
        boolean nLPSetting = isAllowedByCurrentUserSettingsLocked("network");
        if (D) {
            Log.d("LocationManagerService", "mLastNLPSet:" + this.mLastNLPSetting + ",nLPSet:" + nLPSetting);
        }
        if (this.mLastNLPSetting != nLPSetting) {
            this.mLastNLPSetting = nLPSetting;
            changesMade = true;
        }
        if (changesMade) {
            this.mContext.sendBroadcastAsUser(new Intent("android.location.PROVIDERS_CHANGED"), UserHandle.ALL);
            this.mContext.sendBroadcastAsUser(new Intent("android.location.MODE_CHANGED"), UserHandle.ALL);
        }
    }

    private void updateProviderListenersLocked(String provider, boolean enabled) {
        int listeners = 0;
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p != null) {
            ArrayList arrayList = null;
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get(provider);
            if (records != null) {
                for (UpdateRecord record : records) {
                    if (isCurrentProfile(UserHandle.getUserId(record.mReceiver.mIdentity.mUid))) {
                        if (!record.mReceiver.callProviderEnabledLocked(provider, enabled)) {
                            if (arrayList == null) {
                                arrayList = new ArrayList();
                            }
                            arrayList.add(record.mReceiver);
                        }
                        listeners++;
                    }
                }
            }
            if (arrayList != null) {
                for (int i = arrayList.size() - 1; i >= 0; i--) {
                    removeUpdatesLocked((Receiver) arrayList.get(i));
                }
            }
            if (enabled) {
                p.enable();
                if (listeners > 0) {
                    applyRequirementsLocked(provider);
                }
            } else {
                p.disable();
            }
        }
    }

    private void broadcastGpsWorkSourceChange() {
        ArrayList<String> gpsPkgs = getLastWorkSource();
        if (D) {
            Log.d("LocationManagerService", "BF01 new:" + gpsPkgs);
        }
        if (!this.mGpsPkgs.equals(gpsPkgs)) {
            this.mGpsPkgs = gpsPkgs;
            Intent intent = new Intent(GPS_WORKSOURCE_CHANGE_ACTION);
            if (this.mGpsPkgs.size() > 0) {
                intent.putStringArrayListExtra(EXTRA_GPS_PKGS, this.mGpsPkgs);
            }
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private ArrayList<String> getLastWorkSource() {
        ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get("gps");
        ArrayList<String> gpsPkgs = new ArrayList(3);
        if (records != null) {
            int i = records.size() - 1;
            while (i >= 0) {
                Receiver receiver = ((UpdateRecord) records.get(i)).mReceiver;
                if (!(1000 == receiver.mIdentity.mUid || ((UpdateRecord) records.get(i)).isBlock() || !isCurrentProfile(UserHandle.getUserId(receiver.mIdentity.mUid)))) {
                    WorkSource worksource = receiver.mWorkSource;
                    if (worksource != null && worksource.size() > 0 && worksource.getName(0) != null) {
                        for (int k = 0; k < worksource.size(); k++) {
                            String pkg = worksource.getName(k);
                            if (!gpsPkgs.contains(pkg)) {
                                gpsPkgs.add(pkg);
                            }
                            if (gpsPkgs.size() >= 3) {
                                break;
                            }
                        }
                    } else if (!gpsPkgs.contains(receiver.mIdentity.mPackageName)) {
                        gpsPkgs.add(receiver.mIdentity.mPackageName);
                    }
                    if (gpsPkgs.size() >= 3) {
                        break;
                    }
                }
                i--;
            }
        }
        return gpsPkgs;
    }

    private void applyRequirementsLocked(String provider) {
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p != null) {
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get(provider);
            WorkSource worksource = new WorkSource();
            ProviderRequest providerRequest = new ProviderRequest();
            long backgroundThrottleInterval = Global.getLong(this.mContext.getContentResolver(), "location_background_throttle_interval_ms", 1800000);
            this.mTopAppName = null;
            if (records != null) {
                LocationRequest locationRequest;
                for (UpdateRecord record : records) {
                    if (isCurrentProfile(UserHandle.getUserId(record.mReceiver.mIdentity.mUid))) {
                        if (checkLocationAccess(record.mReceiver.mIdentity.mPid, record.mReceiver.mIdentity.mUid, record.mReceiver.mIdentity.mPackageName, record.mReceiver.mAllowedResolutionLevel)) {
                            if (!record.isBlock()) {
                                locationRequest = record.mRealRequest;
                                long interval = locationRequest.getInterval();
                                if (!isThrottlingExemptLocked(record.mReceiver.mIdentity)) {
                                    if (!record.mIsForegroundUid) {
                                        interval = Math.max(interval, backgroundThrottleInterval);
                                    }
                                    if (interval != locationRequest.getInterval()) {
                                        LocationRequest locationRequest2 = new LocationRequest(locationRequest);
                                        locationRequest2.setInterval(interval);
                                        locationRequest = locationRequest2;
                                    }
                                }
                                record.mRequest = locationRequest;
                                providerRequest.locationRequests.add(locationRequest);
                                if (interval < providerRequest.interval) {
                                    providerRequest.reportLocation = true;
                                    providerRequest.interval = interval;
                                }
                            } else if (D) {
                                Log.d("LocationManagerService", "block:" + record.mReceiver.mIdentity.mPackageName);
                            }
                        }
                    }
                }
                if (providerRequest.reportLocation) {
                    long thresholdInterval = ((providerRequest.interval + 1000) * 3) / 2;
                    for (UpdateRecord record2 : records) {
                        if (isCurrentProfile(UserHandle.getUserId(record2.mReceiver.mIdentity.mUid))) {
                            locationRequest = record2.mRequest;
                            if (providerRequest.locationRequests.contains(locationRequest) && locationRequest.getInterval() <= thresholdInterval) {
                                if (record2.mReceiver.mWorkSource == null || record2.mReceiver.mWorkSource.size() <= 0 || record2.mReceiver.mWorkSource.getName(0) == null) {
                                    worksource.add(record2.mReceiver.mIdentity.mUid, record2.mReceiver.mIdentity.mPackageName);
                                } else {
                                    worksource.add(record2.mReceiver.mWorkSource);
                                }
                            }
                        }
                    }
                }
            }
            if (D) {
                Log.d("LocationManagerService", "provider request: " + provider + " " + providerRequest);
            }
            if (this.isFakeLocationreporting) {
                fakeReportLocation(provider);
            } else {
                p.setRequest(providerRequest, worksource);
            }
            if ("gps".equals(provider)) {
                broadcastGpsWorkSourceChange();
            }
        }
    }

    public String[] getBackgroundThrottlingWhitelist() {
        String[] strArr;
        synchronized (this.mLock) {
            strArr = (String[]) this.mBackgroundThrottlePackageWhitelist.toArray(new String[this.mBackgroundThrottlePackageWhitelist.size()]);
        }
        return strArr;
    }

    private void updateBackgroundThrottlingWhitelistLocked() {
        String setting = Global.getString(this.mContext.getContentResolver(), "location_background_throttle_package_whitelist");
        if (setting == null) {
            setting = "";
        }
        this.mBackgroundThrottlePackageWhitelist.clear();
        this.mBackgroundThrottlePackageWhitelist.addAll(SystemConfig.getInstance().getAllowUnthrottledLocation());
        this.mBackgroundThrottlePackageWhitelist.addAll(Arrays.asList(setting.split(",")));
    }

    private boolean isThrottlingExemptLocked(Identity identity) {
        if (identity.mUid == 1000 || this.mBackgroundThrottlePackageWhitelist.contains(identity.mPackageName)) {
            return true;
        }
        for (LocationProviderProxy provider : this.mProxyProviders) {
            if (identity.mPackageName.equals(provider.getConnectedPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo info = this.mContext.getPackageManager().getPackageInfo(packageName, 0).applicationInfo;
            if (info == null || (info.flags & 1) == 0) {
                return false;
            }
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public void getLocAppsOp(int flag, LocAppsOp locAppsOp) {
        this.mContext.enforceCallingOrSelfPermission(ADMIN_PERM, ADMIN_PERM_ERROR);
        if (locAppsOp == null || locAppsOp.getAppsOp() == null) {
            throw new IllegalArgumentException("invalid LocAppsOp");
        }
        this.mOppoBlackList.getLocAppsOp(flag, locAppsOp);
    }

    public void setLocAppsOp(int cmd, LocAppsOp locAppsOp) {
        this.mContext.enforceCallingOrSelfPermission(ADMIN_PERM, ADMIN_PERM_ERROR);
        if (locAppsOp == null || locAppsOp.getAppsOp() == null) {
            throw new IllegalArgumentException("invalid LocAppsOp");
        }
        this.mOppoBlackList.setLocAppsOp(cmd, locAppsOp);
        this.mLocationHandler.removeMessages(2);
        this.mLocationHandler.sendMessage(Message.obtain(this.mLocationHandler, 2));
    }

    private Receiver getReceiverLocked(ILocationListener listener, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        IBinder binder = listener.asBinder();
        Receiver receiver = (Receiver) this.mReceivers.get(binder);
        if (receiver == null) {
            receiver = new Receiver(listener, null, pid, uid, packageName, workSource, hideFromAppOps);
            try {
                receiver.getListener().asBinder().linkToDeath(receiver, 0);
                this.mReceivers.put(binder, receiver);
            } catch (RemoteException e) {
                Slog.e("LocationManagerService", "linkToDeath failed:", e);
                return null;
            }
        }
        return receiver;
    }

    private Receiver getReceiverLocked(PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        Receiver receiver = (Receiver) this.mReceivers.get(intent);
        if (receiver != null) {
            return receiver;
        }
        receiver = new Receiver(null, intent, pid, uid, packageName, workSource, hideFromAppOps);
        this.mReceivers.put(intent, receiver);
        return receiver;
    }

    private LocationRequest createSanitizedRequest(LocationRequest request, int resolutionLevel) {
        LocationRequest sanitizedRequest = new LocationRequest(request);
        if (resolutionLevel < 2) {
            switch (sanitizedRequest.getQuality()) {
                case 100:
                    sanitizedRequest.setQuality(102);
                    break;
                case 203:
                    sanitizedRequest.setQuality(201);
                    break;
            }
            if (sanitizedRequest.getInterval() < LocationFudger.FASTEST_INTERVAL_MS) {
                sanitizedRequest.setInterval(LocationFudger.FASTEST_INTERVAL_MS);
            }
            if (sanitizedRequest.getFastestInterval() < LocationFudger.FASTEST_INTERVAL_MS) {
                sanitizedRequest.setFastestInterval(LocationFudger.FASTEST_INTERVAL_MS);
            }
        }
        if (sanitizedRequest.getFastestInterval() > sanitizedRequest.getInterval()) {
            request.setFastestInterval(request.getInterval());
        }
        return sanitizedRequest;
    }

    private void checkPackageName(String packageName) {
        if (packageName == null) {
            throw new SecurityException("invalid package name: " + packageName);
        }
        int uid = Binder.getCallingUid();
        String[] packages = this.mPackageManager.getPackagesForUid(uid);
        if (packages == null) {
            throw new SecurityException("invalid UID " + uid);
        }
        int i = 0;
        int length = packages.length;
        while (i < length) {
            if (!packageName.equals(packages[i])) {
                i++;
            } else {
                return;
            }
        }
        throw new SecurityException("invalid package name: " + packageName);
    }

    private void checkPendingIntent(PendingIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("invalid pending intent: " + intent);
        }
    }

    private Receiver checkListenerOrIntentLocked(ILocationListener listener, PendingIntent intent, int pid, int uid, String packageName, WorkSource workSource, boolean hideFromAppOps) {
        if (intent == null && listener == null) {
            throw new IllegalArgumentException("need either listener or intent");
        } else if (intent != null && listener != null) {
            throw new IllegalArgumentException("cannot register both listener and intent");
        } else if (intent == null) {
            return getReceiverLocked(listener, pid, uid, packageName, workSource, hideFromAppOps);
        } else {
            checkPendingIntent(intent);
            return getReceiverLocked(intent, pid, uid, packageName, workSource, hideFromAppOps);
        }
    }

    public void requestLocationUpdates(LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName) {
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        checkPackageName(packageName);
        if (SystemProperties.getInt(GPS_OPCUSTOM_FEATURE, 0) != 1) {
            int allowedResolutionLevel = getCallerAllowedResolutionLevel();
            checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
            WorkSource workSource = request.getWorkSource();
            if (workSource != null && workSource.size() > 0) {
                checkDeviceStatsAllowed();
            }
            boolean hideFromAppOps = request.getHideFromAppOps();
            if (hideFromAppOps) {
                checkUpdateAppOpsAllowed();
            }
            LocationRequest sanitizedRequest = createSanitizedRequest(request, allowedResolutionLevel);
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                checkLocationAccess(pid, uid, packageName, allowedResolutionLevel);
                synchronized (this.mLock) {
                    Receiver recevier = checkListenerOrIntentLocked(listener, intent, pid, uid, packageName, workSource, hideFromAppOps);
                    if (recevier != null) {
                        if ("gps".equals(request.getProvider()) && (this.mIsRegister ^ 1) != 0) {
                            try {
                                this.mAm.registerProcessObserver(this.mProcessObserver);
                                this.mIsRegister = true;
                            } catch (RemoteException e) {
                            }
                        }
                        requestLocationUpdatesLocked(sanitizedRequest, recevier, pid, uid, packageName);
                    } else {
                        Log.e("LocationManagerService", "request from " + packageName + " failed.");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void requestLocationUpdatesLocked(LocationRequest request, Receiver receiver, int pid, int uid, String packageName) {
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        String name = request.getProvider();
        if (name == null) {
            throw new IllegalArgumentException("provider name must not be null");
        } else if (((LocationProviderInterface) this.mProvidersByName.get(name)) == null) {
            throw new IllegalArgumentException("provider doesn't exist: " + name);
        } else {
            boolean isProviderEnabled;
            UpdateRecord record = new UpdateRecord(name, request, receiver);
            Log.d("LocationManagerService", "request " + Integer.toHexString(System.identityHashCode(receiver)) + " " + name + " " + request + " from " + packageName + "(" + uid + " " + (record.mIsForegroundUid ? "foreground" : "background") + (isThrottlingExemptLocked(receiver.mIdentity) ? " [whitelisted]" : "") + ")");
            UpdateRecord oldRecord = (UpdateRecord) receiver.mUpdateRecords.put(name, record);
            if (oldRecord != null) {
                oldRecord.disposeLocked(false);
            }
            if (this.isSytemPropRooted && fakeLocationFeatureEnable()) {
                this.isFakeLocationreporting = true;
            }
            if (this.mGnssWhiteListProxy == null || !this.mGnssWhiteListProxy.isNetworkLocationAlwayOn()) {
                isProviderEnabled = isAllowedByUserSettingsLocked(name, uid);
            } else {
                isProviderEnabled = isAllowedLocationRequested(name, uid, packageName);
            }
            if (isProviderEnabled) {
                applyRequirementsLocked(name);
            } else {
                receiver.callProviderEnabledLocked(name, false);
            }
            receiver.updateMonitoring(true);
        }
    }

    public void removeUpdates(ILocationListener listener, PendingIntent intent, String packageName) {
        checkPackageName(packageName);
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        synchronized (this.mLock) {
            Receiver receiver = checkListenerOrIntentLocked(listener, intent, pid, uid, packageName, null, false);
            long identity = Binder.clearCallingIdentity();
            try {
                removeUpdatesLocked(receiver);
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void removeUpdatesLocked(Receiver receiver) {
        Log.i("LocationManagerService", "remove " + Integer.toHexString(System.identityHashCode(receiver)));
        if (fakeLocationFeatureEnable()) {
            this.isFakeLocationreporting = false;
        }
        if (this.mReceivers.remove(receiver.mKey) != null && receiver.isListener()) {
            receiver.getListener().asBinder().unlinkToDeath(receiver, 0);
            synchronized (receiver) {
                receiver.clearPendingBroadcastsLocked();
            }
        }
        receiver.updateMonitoring(false);
        HashSet<String> providers = new HashSet();
        HashMap<String, UpdateRecord> oldRecords = receiver.mUpdateRecords;
        if (oldRecords != null) {
            for (UpdateRecord record : oldRecords.values()) {
                record.disposeLocked(false);
            }
            providers.addAll(oldRecords.keySet());
        }
        if (this.mIsRegister) {
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get("gps");
            if (records == null || records.size() == 0) {
                try {
                    this.mAm.unregisterProcessObserver(this.mProcessObserver);
                    this.mIsRegister = false;
                } catch (RemoteException e) {
                }
            }
        }
        for (String provider : providers) {
            if (isAllowedByCurrentUserSettingsLockedForOppo(provider)) {
                applyRequirementsLocked(provider);
            }
        }
    }

    private void applyAllProviderRequirementsLocked() {
        for (LocationProviderInterface p : this.mProviders) {
            if (isAllowedByCurrentUserSettingsLocked(p.getName())) {
                applyRequirementsLocked(p.getName());
            }
        }
    }

    public Location getLastLocation(LocationRequest request, String packageName) {
        if (D) {
            Log.d("LocationManagerService", "getLastLocation: " + request);
        }
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkPackageName(packageName);
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            if (this.mBlacklist.isBlacklisted(packageName)) {
                if (D) {
                    Log.d("LocationManagerService", "not returning last loc for blacklisted app: " + packageName);
                }
                Binder.restoreCallingIdentity(identity);
                return null;
            } else if (reportLocationAccessNoThrow(pid, uid, packageName, allowedResolutionLevel)) {
                synchronized (this.mLock) {
                    String name = request.getProvider();
                    if (name == null) {
                        name = "fused";
                    }
                    if (((LocationProviderInterface) this.mProvidersByName.get(name)) == null) {
                        Binder.restoreCallingIdentity(identity);
                        return null;
                    } else if (isAllowedByUserSettingsLocked(name, uid)) {
                        Location location;
                        if (allowedResolutionLevel < 2) {
                            location = (Location) this.mLastLocationCoarseInterval.get(name);
                        } else {
                            location = (Location) this.mLastLocation.get(name);
                        }
                        Location location2;
                        if (location == null) {
                            Binder.restoreCallingIdentity(identity);
                            return null;
                        } else if (allowedResolutionLevel < 2) {
                            Location noGPSLocation = location.getExtraLocation("noGPSLocation");
                            if (noGPSLocation != null) {
                                location2 = new Location(this.mLocationFudger.getOrCreate(noGPSLocation));
                                Binder.restoreCallingIdentity(identity);
                                return location2;
                            }
                            Binder.restoreCallingIdentity(identity);
                            return null;
                        } else {
                            location2 = new Location(location);
                            Binder.restoreCallingIdentity(identity);
                            return location2;
                        }
                    } else {
                        Binder.restoreCallingIdentity(identity);
                        return null;
                    }
                }
            } else {
                if (D) {
                    Log.d("LocationManagerService", "not returning last loc for no op app: " + packageName);
                }
                Binder.restoreCallingIdentity(identity);
                return null;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void requestGeofence(LocationRequest request, Geofence geofence, PendingIntent intent, String packageName) {
        if (request == null) {
            request = DEFAULT_LOCATION_REQUEST;
        }
        int allowedResolutionLevel = getCallerAllowedResolutionLevel();
        checkResolutionLevelIsSufficientForGeofenceUse(allowedResolutionLevel);
        checkPendingIntent(intent);
        checkPackageName(packageName);
        checkResolutionLevelIsSufficientForProviderUse(allowedResolutionLevel, request.getProvider());
        LocationRequest sanitizedRequest = createSanitizedRequest(request, allowedResolutionLevel);
        if (D) {
            Log.d("LocationManagerService", "requestGeofence: " + sanitizedRequest + " " + geofence + " " + intent);
        }
        int uid = Binder.getCallingUid();
        if (UserHandle.getUserId(uid) != 0) {
            Log.w("LocationManagerService", "proximity alerts are currently available only to the primary user");
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mGeofenceManager.addFence(sanitizedRequest, geofence, intent, allowedResolutionLevel, uid, packageName);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void removeGeofence(Geofence geofence, PendingIntent intent, String packageName) {
        checkPendingIntent(intent);
        checkPackageName(packageName);
        if (D) {
            Log.d("LocationManagerService", "removeGeofence: " + geofence + " " + intent);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mGeofenceManager.removeFence(geofence, intent);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean registerGnssStatusCallback(IGnssStatusListener callback, String packageName) {
        if (!hasGnssPermissions(packageName) || this.mGnssStatusProvider == null) {
            return false;
        }
        try {
            this.mGnssStatusProvider.registerGnssStatusCallback(callback);
            return true;
        } catch (RemoteException e) {
            Slog.e("LocationManagerService", "mGpsStatusProvider.registerGnssStatusCallback failed", e);
            return false;
        }
    }

    public void unregisterGnssStatusCallback(IGnssStatusListener callback) {
        synchronized (this.mLock) {
            try {
                this.mGnssStatusProvider.unregisterGnssStatusCallback(callback);
            } catch (Exception e) {
                Slog.e("LocationManagerService", "mGpsStatusProvider.unregisterGnssStatusCallback failed", e);
            }
        }
        return;
    }

    public boolean addGnssMeasurementsListener(IGnssMeasurementsListener listener, String packageName) {
        if (!hasGnssPermissions(packageName) || this.mGnssMeasurementsProvider == null) {
            return false;
        }
        synchronized (this.mLock) {
            Identity callerIdentity = new Identity(Binder.getCallingUid(), Binder.getCallingPid(), packageName);
            this.mGnssMeasurementsListeners.put(listener, callerIdentity);
            long identity = Binder.clearCallingIdentity();
            try {
                if (isThrottlingExemptLocked(callerIdentity) || isImportanceForeground(this.mActivityManager.getPackageImportance(packageName))) {
                    boolean addListener = this.mGnssMeasurementsProvider.addListener(listener);
                    Binder.restoreCallingIdentity(identity);
                    return addListener;
                }
                Binder.restoreCallingIdentity(identity);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void removeGnssMeasurementsListener(IGnssMeasurementsListener listener) {
        if (this.mGnssMeasurementsProvider != null) {
            synchronized (this.mLock) {
                this.mGnssMeasurementsListeners.remove(listener);
                this.mGnssMeasurementsProvider.removeListener(listener);
            }
        }
    }

    public boolean addGnssNavigationMessageListener(IGnssNavigationMessageListener listener, String packageName) {
        if (!hasGnssPermissions(packageName) || this.mGnssNavigationMessageProvider == null) {
            return false;
        }
        synchronized (this.mLock) {
            Identity callerIdentity = new Identity(Binder.getCallingUid(), Binder.getCallingPid(), packageName);
            this.mGnssNavigationMessageListeners.put(listener, callerIdentity);
            long identity = Binder.clearCallingIdentity();
            try {
                if (isThrottlingExemptLocked(callerIdentity) || isImportanceForeground(this.mActivityManager.getPackageImportance(packageName))) {
                    boolean addListener = this.mGnssNavigationMessageProvider.addListener(listener);
                    Binder.restoreCallingIdentity(identity);
                    return addListener;
                }
                Binder.restoreCallingIdentity(identity);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void removeGnssNavigationMessageListener(IGnssNavigationMessageListener listener) {
        if (this.mGnssNavigationMessageProvider != null) {
            synchronized (this.mLock) {
                this.mGnssNavigationMessageListeners.remove(listener);
                this.mGnssNavigationMessageProvider.removeListener(listener);
            }
        }
    }

    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        if (provider == null) {
            throw new NullPointerException();
        }
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), provider);
        if (this.mContext.checkCallingOrSelfPermission(ACCESS_LOCATION_EXTRA_COMMANDS) != 0) {
            throw new SecurityException("Requires ACCESS_LOCATION_EXTRA_COMMANDS permission");
        }
        synchronized (this.mLock) {
            LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
            if (p == null) {
                return false;
            }
            boolean sendExtraCommand = p.sendExtraCommand(command, extras);
            return sendExtraCommand;
        }
    }

    public boolean sendNiResponse(int notifId, int userResponse) {
        if (Binder.getCallingUid() != Process.myUid()) {
            throw new SecurityException("calling sendNiResponse from outside of the system is not allowed");
        }
        try {
            return this.mNetInitiatedListener.sendNiResponse(notifId, userResponse);
        } catch (RemoteException e) {
            Slog.e("LocationManagerService", "RemoteException in LocationManagerService.sendNiResponse");
            return false;
        }
    }

    public ProviderProperties getProviderProperties(String provider) {
        if (this.mProvidersByName.get(provider) == null) {
            return null;
        }
        LocationProviderInterface p;
        checkResolutionLevelIsSufficientForProviderUse(getCallerAllowedResolutionLevel(), provider);
        synchronized (this.mLock) {
            p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        }
        if (p == null) {
            return null;
        }
        return p.getProperties();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getNetworkProviderPackage() {
        synchronized (this.mLock) {
            if (this.mProvidersByName.get("network") == null) {
                return null;
            }
            LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get("network");
        }
    }

    public boolean isProviderEnabled(String provider) {
        boolean z = false;
        if ("fused".equals(provider)) {
            return false;
        }
        int uid = Binder.getCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (((LocationProviderInterface) this.mProvidersByName.get(provider)) != null) {
                    z = isAllowedByUserSettingsLocked(provider, uid);
                }
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean isUidALocationProvider(int uid) {
        if (uid == 1000 || doesUidHavePackage(uid, "com.google.android.gms")) {
            return true;
        }
        if (this.mGeocodeProvider != null && doesUidHavePackage(uid, this.mGeocodeProvider.getConnectedPackageName())) {
            return true;
        }
        for (LocationProviderProxy proxy : this.mProxyProviders) {
            if (doesUidHavePackage(uid, proxy.getConnectedPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void checkCallerIsProvider() {
        if (this.mContext.checkCallingOrSelfPermission(INSTALL_LOCATION_PROVIDER) != 0 && !isUidALocationProvider(Binder.getCallingUid())) {
            throw new SecurityException("need INSTALL_LOCATION_PROVIDER permission, or UID of a currently bound location provider");
        }
    }

    private boolean doesUidHavePackage(int uid, String packageName) {
        if (packageName == null) {
            return false;
        }
        String[] packageNames = this.mPackageManager.getPackagesForUid(uid);
        if (packageNames == null) {
            return false;
        }
        for (String name : packageNames) {
            if (packageName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void reportLocation(Location location, boolean passive) {
        int i = 1;
        checkCallerIsProvider();
        if (location.isComplete()) {
            this.mLocationHandler.removeMessages(1, location);
            Message m = Message.obtain(this.mLocationHandler, 1, location);
            if (!passive) {
                i = 0;
            }
            m.arg1 = i;
            this.mLocationHandler.sendMessageAtFrontOfQueue(m);
            return;
        }
        Log.w("LocationManagerService", "Dropping incomplete location: " + location);
    }

    private void fakeReportLocation(String provider) {
        String location_prop = SystemProperties.get(OPPO_FAKE_LOCATION_TEST);
        if (location_prop != null) {
            String[] split = location_prop.split(OPPO_FAKE_LOCATION_SPLIT);
            if (split.length <= 0 || split.length != 3) {
                Log.e("LocationManagerService", "fakeReportLocation parameter is abnormal ");
                return;
            }
            Location fakeLoation = new Location(provider);
            try {
                fakeLoation.setLatitude(Double.parseDouble(split[1]));
                fakeLoation.setLongitude(Double.parseDouble(split[0]));
            } catch (NumberFormatException e) {
            }
            this.mLocationHandler.removeMessages(1, fakeLoation);
            Message m = Message.obtain(this.mLocationHandler, 1, fakeLoation);
            m.arg1 = 0;
            Log.d("LocationManagerService", "fakeReportLocation Longitude = " + split[0] + " Altitude = " + split[1] + " inchina = " + split[2]);
            this.mLocationHandler.sendMessageDelayed(m, 500);
        }
    }

    private boolean fakeLocationFeatureEnable() {
        String location_prop = SystemProperties.get(OPPO_FAKE_LOCATOIN_TEST_SWITCH);
        if (location_prop == null || !location_prop.equals(OPPO_FAKE_LOCATOIN_SWITCH_ON)) {
            return false;
        }
        return true;
    }

    private static boolean shouldBroadcastSafe(Location loc, Location lastLoc, UpdateRecord record, long now) {
        if (lastLoc == null) {
            return true;
        }
        if ((loc.getElapsedRealtimeNanos() - lastLoc.getElapsedRealtimeNanos()) / NANOS_PER_MILLI < record.mRealRequest.getFastestInterval() - 100) {
            return false;
        }
        double minDistance = (double) record.mRealRequest.getSmallestDisplacement();
        if (minDistance > 0.0d && ((double) loc.distanceTo(lastLoc)) <= minDistance) {
            return false;
        }
        if (record.mRealRequest.getNumUpdates() <= 0) {
            return false;
        }
        return record.mRealRequest.getExpireAt() >= now;
    }

    private void handleLocationChangedLocked(Location location, boolean passive) {
        if (D) {
            Log.d("LocationManagerService", "incoming location: " + location);
        }
        long now = SystemClock.elapsedRealtime();
        String provider = passive ? "passive" : location.getProvider();
        LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(provider);
        if (p != null) {
            Location noGPSLocation = location.getExtraLocation("noGPSLocation");
            Location lastLocation = (Location) this.mLastLocation.get(provider);
            if (lastLocation == null) {
                lastLocation = new Location(provider);
                this.mLastLocation.put(provider, lastLocation);
            } else {
                Location lastNoGPSLocation = lastLocation.getExtraLocation("noGPSLocation");
                if (noGPSLocation == null && lastNoGPSLocation != null) {
                    location.setExtraLocation("noGPSLocation", lastNoGPSLocation);
                }
            }
            lastLocation.set(location);
            Location lastLocationCoarseInterval = (Location) this.mLastLocationCoarseInterval.get(provider);
            if (lastLocationCoarseInterval == null) {
                lastLocationCoarseInterval = new Location(location);
                this.mLastLocationCoarseInterval.put(provider, lastLocationCoarseInterval);
            }
            if (location.getElapsedRealtimeNanos() - lastLocationCoarseInterval.getElapsedRealtimeNanos() > 600000000000L) {
                lastLocationCoarseInterval.set(location);
            }
            noGPSLocation = lastLocationCoarseInterval.getExtraLocation("noGPSLocation");
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get(provider);
            if (records != null && records.size() != 0) {
                Receiver receiver;
                Location coarseLocation = null;
                if (noGPSLocation != null) {
                    coarseLocation = this.mLocationFudger.getOrCreate(noGPSLocation);
                }
                long newStatusUpdateTime = p.getStatusUpdateTime();
                Bundle extras = new Bundle();
                int status = p.getStatus(extras);
                Iterable deadReceivers = null;
                Iterable deadUpdateRecords = null;
                for (UpdateRecord r : records) {
                    receiver = r.mReceiver;
                    boolean receiverDead = false;
                    int receiverUserId = UserHandle.getUserId(receiver.mIdentity.mUid);
                    if (!isCurrentProfile(receiverUserId)) {
                        if ((isUidALocationProvider(receiver.mIdentity.mUid) ^ 1) != 0) {
                            if (D) {
                                Log.d("LocationManagerService", "skipping loc update for background user " + receiverUserId + " (current user: " + this.mCurrentUserId + ", app: " + receiver.mIdentity.mPackageName + ")");
                            }
                        }
                    }
                    if (!this.mBlacklist.isBlacklisted(receiver.mIdentity.mPackageName)) {
                        if (this.mGnssWhiteListProxy != null && this.mGnssWhiteListProxy.isNetworkLocationAlwayOn()) {
                            if (!isAllowedLocationChanged(provider, receiver.mIdentity.mPackageName)) {
                            }
                        }
                        if (reportLocationAccessNoThrow(receiver.mIdentity.mPid, receiver.mIdentity.mUid, receiver.mIdentity.mPackageName, receiver.mAllowedResolutionLevel)) {
                            Location notifyLocation;
                            if (receiver.mAllowedResolutionLevel < 2) {
                                notifyLocation = coarseLocation;
                            } else {
                                notifyLocation = lastLocation;
                            }
                            if (notifyLocation != null) {
                                Location lastLoc = r.mLastFixBroadcast;
                                if (lastLoc == null || shouldBroadcastSafe(notifyLocation, lastLoc, r, now)) {
                                    if (lastLoc == null) {
                                        r.mLastFixBroadcast = new Location(notifyLocation);
                                    } else {
                                        lastLoc.set(notifyLocation);
                                    }
                                    if (!receiver.callLocationChangedLocked(notifyLocation)) {
                                        Slog.w("LocationManagerService", "RemoteException calling onLocationChanged on " + receiver);
                                        receiverDead = true;
                                    }
                                    r.mRealRequest.decrementNumUpdates();
                                }
                            }
                            long prevStatusUpdateTime = r.mLastStatusBroadcast;
                            if (newStatusUpdateTime > prevStatusUpdateTime && !(prevStatusUpdateTime == 0 && status == 2)) {
                                r.mLastStatusBroadcast = newStatusUpdateTime;
                                if (!receiver.callStatusChangedLocked(provider, status, extras)) {
                                    receiverDead = true;
                                    Slog.w("LocationManagerService", "RemoteException calling onStatusChanged on " + receiver);
                                }
                            }
                            if (r.mRealRequest.getNumUpdates() <= 0 || r.mRealRequest.getExpireAt() < now) {
                                if (deadUpdateRecords == null) {
                                    deadUpdateRecords = new ArrayList();
                                }
                                deadUpdateRecords.add(r);
                            }
                            if (receiverDead) {
                                if (deadReceivers == null) {
                                    deadReceivers = new ArrayList();
                                }
                                if (!deadReceivers.contains(receiver)) {
                                    deadReceivers.add(receiver);
                                }
                            }
                        } else if (D) {
                            Log.d("LocationManagerService", "skipping loc update for no op app: " + receiver.mIdentity.mPackageName);
                        }
                    } else if (D) {
                        Log.d("LocationManagerService", "skipping loc update for blacklisted app: " + receiver.mIdentity.mPackageName);
                    }
                }
                if (deadReceivers != null) {
                    for (Receiver receiver2 : deadReceivers) {
                        removeUpdatesLocked(receiver2);
                    }
                }
                if (deadUpdateRecords != null) {
                    for (UpdateRecord r2 : deadUpdateRecords) {
                        r2.disposeLocked(true);
                    }
                    applyRequirementsLocked(provider);
                }
                if (NeuronSystemManager.isEnable() && NeuronSystemManager.isHookEnable(1)) {
                    ContentValues contentValues = new ContentValues();
                    if ("gps".equals(provider)) {
                        contentValues.put("location_provider_type", Integer.valueOf(0));
                    } else if ("network".equals(provider)) {
                        contentValues.put("location_provider_type", Integer.valueOf(1));
                    } else if ("passive".equals(provider)) {
                        contentValues.put("location_provider_type", Integer.valueOf(2));
                    } else {
                        contentValues.put("location_provider_type", Integer.valueOf(3));
                    }
                    contentValues.put("gps_location_accuracy", Float.valueOf(location.getAccuracy()));
                    contentValues.put("gps_location_attitude", Double.valueOf(location.getAltitude()));
                    contentValues.put("gps_location_latitude", Double.valueOf(location.getLatitude()));
                    contentValues.put("gps_location_longitude", Double.valueOf(location.getLongitude()));
                    contentValues.put("gps_event", Integer.valueOf(3));
                    NeuronSystemManager.getInstance().publishEvent(11, contentValues);
                }
            }
        }
    }

    private boolean isMockProvider(String provider) {
        boolean containsKey;
        synchronized (this.mLock) {
            containsKey = this.mMockProviders.containsKey(provider);
        }
        return containsKey;
    }

    private Location screenLocationLocked(Location location, String provider) {
        if (isMockProvider("network")) {
            return location;
        }
        LocationProviderProxy providerProxy = (LocationProviderProxy) this.mProvidersByName.get("network");
        if (this.mComboNlpPackageName == null || providerProxy == null || !provider.equals("network") || isMockProvider("network")) {
            return location;
        }
        String connectedNlpPackage = providerProxy.getConnectedPackageName();
        if (connectedNlpPackage == null || (connectedNlpPackage.equals(this.mComboNlpPackageName) ^ 1) != 0) {
            return location;
        }
        Bundle extras = location.getExtras();
        boolean isBeingScreened = false;
        if (extras == null || (extras.containsKey(this.mComboNlpReadyMarker) ^ 1) != 0) {
            ArrayList<UpdateRecord> records = (ArrayList) this.mRecordsByProvider.get("passive");
            if (records != null) {
                for (UpdateRecord r : records) {
                    if (r.mReceiver.mIdentity.mPackageName.equals(this.mComboNlpPackageName)) {
                        if (!isBeingScreened) {
                            isBeingScreened = true;
                            if (extras == null) {
                                location.setExtras(new Bundle());
                                extras = location.getExtras();
                            }
                            extras.putBoolean(this.mComboNlpScreenMarker, true);
                        }
                        if (!r.mReceiver.callLocationChangedLocked(location)) {
                            Slog.w("LocationManagerService", "RemoteException calling onLocationChanged on " + r.mReceiver);
                        } else if (D) {
                            Log.d("LocationManagerService", "Sending location for screening");
                        }
                    }
                }
            }
            return location;
        }
        if (D) {
            Log.d("LocationManagerService", "This location is marked as ready for broadcast");
        }
        extras.remove(this.mComboNlpReadyMarker);
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleLocationChanged(Location location, boolean passive) {
        Location myLocation = new Location(location);
        String provider = myLocation.getProvider();
        if (!myLocation.isFromMockProvider() && isMockProvider(provider)) {
            myLocation.setIsFromMockProvider(true);
        }
        synchronized (this.mLock) {
            if (isAllowedByCurrentUserSettingsLockedForOppo(provider)) {
                if (!passive) {
                    if (screenLocationLocked(location, provider) == null) {
                        return;
                    }
                    this.mPassiveProvider.updateLocation(myLocation);
                }
                handleLocationChangedLocked(myLocation, passive);
            }
        }
    }

    public boolean geocoderIsPresent() {
        return this.mGeocodeProvider != null;
    }

    public String getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocation(latitude, longitude, maxResults, params, addrs);
        }
        return null;
    }

    public String getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        if (this.mGeocodeProvider != null) {
            return this.mGeocodeProvider.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, addrs);
        }
        return null;
    }

    private boolean canCallerAccessMockLocation(String opPackageName) {
        return this.mAppOps.noteOp(58, Binder.getCallingUid(), opPackageName) == 0;
    }

    public void addTestProvider(String name, ProviderProperties properties, String opPackageName) {
        if (!canCallerAccessMockLocation(opPackageName)) {
            return;
        }
        if ("passive".equals(name)) {
            throw new IllegalArgumentException("Cannot mock the passive location provider");
        }
        long identity = Binder.clearCallingIdentity();
        synchronized (this.mLock) {
            if ("gps".equals(name) || "network".equals(name) || "fused".equals(name)) {
                LocationProviderInterface p = (LocationProviderInterface) this.mProvidersByName.get(name);
                if (p != null) {
                    removeProviderLocked(p);
                }
            }
            addTestProviderLocked(name, properties);
            updateProvidersLocked();
        }
        Binder.restoreCallingIdentity(identity);
    }

    private void addTestProviderLocked(String name, ProviderProperties properties) {
        if (this.mProvidersByName.get(name) != null) {
            throw new IllegalArgumentException("Provider \"" + name + "\" already exists");
        }
        MockProvider provider = new MockProvider(name, this, properties);
        addProviderLocked(provider);
        this.mMockProviders.put(name, provider);
        this.mLastLocation.put(name, null);
        this.mLastLocationCoarseInterval.put(name, null);
    }

    public void removeTestProvider(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                clearTestProviderEnabled(provider, opPackageName);
                clearTestProviderLocation(provider, opPackageName);
                clearTestProviderStatus(provider, opPackageName);
                if (((MockProvider) this.mMockProviders.remove(provider)) == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                long identity = Binder.clearCallingIdentity();
                removeProviderLocked((LocationProviderInterface) this.mProvidersByName.get(provider));
                LocationProviderInterface realProvider = (LocationProviderInterface) this.mRealProviders.get(provider);
                if (realProvider != null) {
                    addProviderLocked(realProvider);
                }
                this.mLastLocation.put(provider, null);
                this.mLastLocationCoarseInterval.put(provider, null);
                updateProvidersLocked();
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void setTestProviderLocation(String provider, Location loc, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                Location mock = new Location(loc);
                mock.setIsFromMockProvider(true);
                if (!(TextUtils.isEmpty(loc.getProvider()) || (provider.equals(loc.getProvider()) ^ 1) == 0)) {
                    EventLog.writeEvent(1397638484, new Object[]{"33091107", Integer.valueOf(Binder.getCallingUid()), provider + "!=" + loc.getProvider()});
                }
                long identity = Binder.clearCallingIdentity();
                mockProvider.setLocation(mock);
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void clearTestProviderLocation(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                mockProvider.clearLocation();
            }
        }
    }

    public void setTestProviderEnabled(String provider, boolean enabled, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                long identity = Binder.clearCallingIdentity();
                if (enabled) {
                    mockProvider.enable();
                    this.mEnabledProviders.add(provider);
                    this.mDisabledProviders.remove(provider);
                } else {
                    mockProvider.disable();
                    this.mEnabledProviders.remove(provider);
                    this.mDisabledProviders.add(provider);
                }
                updateProvidersLocked();
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void clearTestProviderEnabled(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                if (((MockProvider) this.mMockProviders.get(provider)) == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                long identity = Binder.clearCallingIdentity();
                this.mEnabledProviders.remove(provider);
                this.mDisabledProviders.remove(provider);
                updateProvidersLocked();
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                mockProvider.setStatus(status, extras, updateTime);
            }
        }
    }

    public void clearTestProviderStatus(String provider, String opPackageName) {
        if (canCallerAccessMockLocation(opPackageName)) {
            synchronized (this.mLock) {
                MockProvider mockProvider = (MockProvider) this.mMockProviders.get(provider);
                if (mockProvider == null) {
                    throw new IllegalArgumentException("Provider \"" + provider + "\" unknown");
                }
                mockProvider.clearStatus();
            }
        }
    }

    private void log(String log) {
        if (Log.isLoggable("LocationManagerService", 2)) {
            Slog.d("LocationManagerService", log);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, "LocationManagerService", pw)) {
            synchronized (this.mLock) {
                if (args.length <= 0 || !args[0].equals("--gnssmetrics")) {
                    Location location;
                    pw.println("Current Location Manager state:");
                    pw.println("  Location Listeners:");
                    for (Receiver receiver : this.mReceivers.values()) {
                        pw.println("    " + receiver);
                    }
                    pw.println("  Active Records by Provider:");
                    for (Entry<String, ArrayList<UpdateRecord>> entry : this.mRecordsByProvider.entrySet()) {
                        pw.println("    " + ((String) entry.getKey()) + ":");
                        for (UpdateRecord record : (ArrayList) entry.getValue()) {
                            pw.println("      " + record);
                        }
                    }
                    pw.println("  Overlay Provider Packages:");
                    for (LocationProviderInterface provider : this.mProviders) {
                        if (provider instanceof LocationProviderProxy) {
                            pw.println("    " + provider.getName() + ": " + ((LocationProviderProxy) provider).getConnectedPackageName());
                        }
                    }
                    pw.println("  Historical Records by Provider:");
                    for (Entry<PackageProviderKey, PackageStatistics> entry2 : this.mRequestStatistics.statistics.entrySet()) {
                        PackageProviderKey key = (PackageProviderKey) entry2.getKey();
                        pw.println("    " + key.packageName + ": " + key.providerName + ": " + ((PackageStatistics) entry2.getValue()));
                    }
                    pw.println("  Last Known Locations:");
                    for (Entry<String, Location> entry3 : this.mLastLocation.entrySet()) {
                        location = (Location) entry3.getValue();
                        pw.println("    " + ((String) entry3.getKey()) + ": " + location);
                    }
                    pw.println("  Last Known Locations Coarse Intervals:");
                    for (Entry<String, Location> entry32 : this.mLastLocationCoarseInterval.entrySet()) {
                        location = (Location) entry32.getValue();
                        pw.println("    " + ((String) entry32.getKey()) + ": " + location);
                    }
                    this.mGeofenceManager.dump(pw);
                    if (this.mEnabledProviders.size() > 0) {
                        pw.println("  Enabled Providers:");
                        for (String i : this.mEnabledProviders) {
                            pw.println("    " + i);
                        }
                    }
                    if (this.mDisabledProviders.size() > 0) {
                        pw.println("  Disabled Providers:");
                        for (String i2 : this.mDisabledProviders) {
                            pw.println("    " + i2);
                        }
                    }
                    pw.append("  ");
                    this.mBlacklist.dump(pw);
                    if (this.mMockProviders.size() > 0) {
                        pw.println("  Mock Providers:");
                        for (Entry<String, MockProvider> i3 : this.mMockProviders.entrySet()) {
                            ((MockProvider) i3.getValue()).dump(pw, "      ");
                        }
                    }
                    if (!this.mBackgroundThrottlePackageWhitelist.isEmpty()) {
                        pw.println("  Throttling Whitelisted Packages:");
                        for (String packageName : this.mBackgroundThrottlePackageWhitelist) {
                            pw.println("    " + packageName);
                        }
                    }
                    pw.append("  fudger: ");
                    this.mLocationFudger.dump(fd, pw, args);
                    if (args.length <= 0 || !"short".equals(args[0])) {
                        for (LocationProviderInterface provider2 : this.mProviders) {
                            pw.print(provider2.getName() + " Internal State");
                            if (provider2 instanceof LocationProviderProxy) {
                                LocationProviderProxy proxy = (LocationProviderProxy) provider2;
                                pw.print(" (" + proxy.getConnectedPackageName() + ")");
                            }
                            pw.println(":");
                            provider2.dump(fd, pw, args);
                        }
                        if (this.mGnssBatchingInProgress) {
                            pw.println("  GNSS batching in progress");
                        }
                        if (D) {
                            this.mOppoBlackList.dump(pw);
                        }
                    }
                } else if (this.mGnssMetricsProvider != null) {
                    pw.append(this.mGnssMetricsProvider.getGnssMetricsAsProtoString());
                }
            }
        }
    }

    @OppoHook(level = OppoHookType.NEW_METHOD, note = "ZhiYong.Lin@Plf.Framework, add for BPM", property = OppoRomType.ROM)
    public int[] getLocationListenersUid() {
        synchronized (this.mLock) {
            ArrayList<Integer> tmp = new ArrayList();
            for (Receiver i : this.mReceivers.values()) {
                for (Entry<String, UpdateRecord> j : i.mUpdateRecords.entrySet()) {
                    tmp.add(Integer.valueOf(((UpdateRecord) j.getValue()).mReceiver.mIdentity.mUid));
                }
            }
            int size = tmp.size();
            if (size <= 0) {
                return null;
            }
            int[] res = new int[size];
            for (int i2 = 0; i2 < size; i2++) {
                res[i2] = ((Integer) tmp.get(i2)).intValue();
            }
            return res;
        }
    }
}
