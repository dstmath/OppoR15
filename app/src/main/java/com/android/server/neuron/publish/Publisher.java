package com.android.server.neuron.publish;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.display.OppoBrightUtils;
import com.android.server.neuron.publish.Channel.RequestSender;
import com.oppo.neuron.NeuronSystemManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class Publisher {
    private static final String TAG = "NeuronSystem";
    private Context mContext;
    private IEventDispatcher[] mRegisterTable = new IEventDispatcher[32];
    private RequestSender mRequestSender;
    private HandlerThread mThread;
    private boolean sPropertyOn = SystemProperties.getBoolean("persist.sys.ns.publish", true);

    public interface IEventDispatcher {
        void dispatch(int i, ContentValues contentValues);
    }

    public class AlarmEventDispatcher implements IEventDispatcher {
        private static final String BROACAST_ACTION = "oppo.intent.action.anomaly.alarm";
        private static final int MAX_ALARM_COUNT = 5;
        private static final int MAX_ALARM_COUNT_PER_APP = 3;
        private static final String PAYLOAD = "payload";
        private static final String RECEIVER_PERMISSION = "oppo.permission.OPPO_COMPONENT_SAFE";
        private final long TIME_PERIOD = ((long) SystemProperties.getInt("persist.sys.ns_alarmperiod", 600000));
        private LinkedList<AlarmEvent> mAlarmQueue = new LinkedList();

        private class AlarmEvent {
            String mName;
            long mTimestamp;

            /* synthetic */ AlarmEvent(AlarmEventDispatcher this$1, AlarmEvent -this1) {
                this();
            }

            private AlarmEvent() {
            }
        }

        private class AlarmRecord {
            int mCount = 0;
            String mName;

            public AlarmRecord(String pkg) {
                this.mName = pkg;
            }
        }

        public void dispatch(int eventType, ContentValues contentValues) {
            AlarmEvent alarmEvent = new AlarmEvent(this, null);
            alarmEvent.mName = contentValues.getAsString("pkgname");
            alarmEvent.mTimestamp = contentValues.getAsLong("timestamp").longValue();
            this.mAlarmQueue.add(alarmEvent);
            long current = System.currentTimeMillis();
            while (this.mAlarmQueue.size() > 0 && current - ((AlarmEvent) this.mAlarmQueue.getFirst()).mTimestamp >= this.TIME_PERIOD) {
                this.mAlarmQueue.removeFirst();
            }
            if (this.mAlarmQueue.size() >= 5) {
                AlarmRecord record;
                ArrayList<AlarmRecord> records = new ArrayList();
                Iterator<AlarmEvent> it = this.mAlarmQueue.iterator();
                while (it.hasNext()) {
                    AlarmEvent tempAlarmEvent = (AlarmEvent) it.next();
                    record = null;
                    for (int i = 0; i < records.size(); i++) {
                        AlarmRecord tempRecord = (AlarmRecord) records.get(i);
                        if (tempRecord.mName.equals(tempAlarmEvent.mName)) {
                            record = tempRecord;
                            break;
                        }
                    }
                    if (record == null) {
                        record = new AlarmRecord(tempAlarmEvent.mName);
                        records.add(record);
                    }
                    record.mCount++;
                }
                ArrayList<String> pkgs = new ArrayList();
                ArrayList<Integer> counts = new ArrayList();
                boolean alarmExceed = false;
                for (AlarmRecord record2 : records) {
                    if (record2.mCount > 3) {
                        alarmExceed = true;
                        pkgs.add(record2.mName);
                        counts.add(Integer.valueOf(record2.mCount));
                        if (NeuronSystemManager.LOG_ON) {
                            Slog.d("NeuronSystem", "Publisher AlarmEventDispatcher alarms count too much:" + record2.mCount + " pkg:" + record2.mName);
                        }
                    }
                }
                if (alarmExceed) {
                    Intent intent = new Intent(BROACAST_ACTION);
                    intent.putExtra("package", pkgs);
                    intent.putExtra("number", counts);
                    Publisher.this.mContext.sendBroadcast(intent, RECEIVER_PERMISSION);
                }
            }
        }
    }

    public class AppEventDispatcher implements IEventDispatcher {
        private static final String DEFAULT_VERSION = "-1";
        private String mLastUsingApp = "";

        public void dispatch(int eventType, ContentValues contentValues) {
            String name = contentValues.getAsString("pkgname");
            int uid = contentValues.getAsInteger("uid").intValue();
            int pid = contentValues.getAsInteger("pid").intValue();
            if (!name.equals(this.mLastUsingApp)) {
                if (NeuronSystemManager.LOG_ON) {
                    Slog.d("NeuronSystem", "Publisher AppEventDispatcher::dispatch eventType:" + eventType + " name: " + name);
                }
                NeuronContext.getSystemStatus().setForegroundApp(name);
                Request req = Request.obtain();
                Parcel parcel = req.prepare();
                parcel.writeInt(1);
                parcel.writeString(name);
                parcel.writeInt(1);
                parcel.writeString("-1");
                parcel.writeInt(uid);
                parcel.writeInt(pid);
                req.commit();
                Publisher.this.mRequestSender.sendRequest(req);
                this.mLastUsingApp = name;
            }
        }
    }

    public class BatteryEventDispatcher implements IEventDispatcher {
        public void dispatch(int eventType, ContentValues contentValues) {
            int i = 0;
            boolean charging = contentValues.getAsInteger("charger").intValue() != 0;
            int level = contentValues.getAsInteger("level").intValue();
            NeuronContext.getSystemStatus().setChargingState(charging);
            NeuronContext.getSystemStatus().setBatteryLevel(level);
            Request req = Request.obtain();
            Parcel parcel = req.prepare();
            parcel.writeInt(8);
            if (charging) {
                i = 1;
            }
            parcel.writeInt(i);
            parcel.writeFloat(((float) level) / 100.0f);
            req.commit();
            Publisher.this.mRequestSender.sendRequest(req);
            if (NeuronSystemManager.LOG_ON) {
                Slog.d("NeuronSystem", "Publisher BatteryEventDispatcher battary_level:" + level + " percent:" + (((float) level) / 100.0f));
            }
        }
    }

    public class GpsEventDispatcher implements IEventDispatcher {
        private static final String BROACAST_ACTION = "oppo.intent.action.anomaly.gps";
        private static final int MAX_WIFI_CANDIDATE = 10;
        private static final int MESSAGE_CHECK_TIMEOUT = 1;
        private static final String PAYLOAD_PACKAGE = "payload.package";
        private static final String PAYLOAD_PID = "payload.pid";
        private static final String RECEIVER_PERMISSION = "oppo.permission.OPPO_COMPONENT_SAFE";
        private static final int TYPE_CDMA = 2;
        private static final int TYPE_GSM = 1;
        private static final int TYPE_LTE = 3;
        private static final int TYPE_WCDMA = 4;
        private final int GPS_USE_TIME_THRESHOLD = SystemProperties.getInt("persist.sys.ns_maxgpstime", OppoBrightUtils.HIGH_BRIGHTNESS_MAX_LUX);
        private long mLastLocationUpdateTime = 0;
        private Handler mTimeoutCheckHandler = new Handler(Publisher.this.mThread.getLooper()) {
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        boolean isPackageUsingGpsTimeout;
                        String packageName = message.obj;
                        synchronized (GpsEventDispatcher.this.mUsingGpsPackages) {
                            isPackageUsingGpsTimeout = GpsEventDispatcher.this.mUsingGpsPackages.containsKey(packageName);
                        }
                        if (isPackageUsingGpsTimeout) {
                            Intent intent = new Intent(GpsEventDispatcher.BROACAST_ACTION);
                            intent.putExtra(GpsEventDispatcher.PAYLOAD_PACKAGE, packageName);
                            intent.putExtra(GpsEventDispatcher.PAYLOAD_PID, message.arg2);
                            Publisher.this.mContext.sendBroadcast(intent, GpsEventDispatcher.RECEIVER_PERMISSION);
                            sendMessageDelayed(Message.obtain(message), (long) GpsEventDispatcher.this.GPS_USE_TIME_THRESHOLD);
                            if (NeuronSystemManager.LOG_ON) {
                                Slog.d("NeuronSystem", "Publisher GpsEventDispatcher hold gps too long:" + packageName);
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        private ArrayMap<String, Long> mUsingGpsPackages = new ArrayMap();

        public void dispatch(int eventType, ContentValues contentValues) {
            int gpsEvent = contentValues.getAsInteger("gps_event").intValue();
            String packageName = contentValues.getAsString("pkgname");
            if (gpsEvent == 1) {
                synchronized (this.mUsingGpsPackages) {
                    if (!this.mUsingGpsPackages.containsKey(packageName)) {
                        this.mUsingGpsPackages.put(packageName, Long.valueOf(System.currentTimeMillis()));
                        Message msg = this.mTimeoutCheckHandler.obtainMessage(1, 0, contentValues.getAsInteger("pid").intValue(), packageName);
                        this.mTimeoutCheckHandler.sendMessageDelayed(msg, (long) this.GPS_USE_TIME_THRESHOLD);
                    }
                }
            } else if (gpsEvent == 2) {
                synchronized (this.mUsingGpsPackages) {
                    this.mUsingGpsPackages.remove(packageName);
                }
                this.mTimeoutCheckHandler.removeMessages(1, packageName);
            } else if (gpsEvent == 3) {
                long period = (long) NeuronContext.getNeoConfig().getGpsUpdatePeriod();
                long now = System.currentTimeMillis();
                if (!NeuronContext.getSystemStatus().getScreenOn()) {
                    period *= 2;
                } else if (now - this.mLastLocationUpdateTime >= period) {
                    this.mLastLocationUpdateTime = now;
                    int type = contentValues.getAsInteger("location_provider_type").intValue();
                    float accuracy = contentValues.getAsFloat("gps_location_accuracy").floatValue();
                    double altitude = contentValues.getAsDouble("gps_location_attitude").doubleValue();
                    double latitude = contentValues.getAsDouble("gps_location_latitude").doubleValue();
                    double longitude = contentValues.getAsDouble("gps_location_longitude").doubleValue();
                    Request req = Request.obtain();
                    Parcel parcel = req.prepare();
                    parcel.writeInt(10);
                    parcel.writeInt(type);
                    parcel.writeFloat(accuracy);
                    parcel.writeDouble(altitude);
                    parcel.writeDouble(latitude);
                    parcel.writeDouble(longitude);
                    appendWifiRequest(parcel);
                    appendCellRequest(parcel);
                    req.commit();
                    Publisher.this.mRequestSender.sendRequest(req);
                    if (NeuronSystemManager.LOG_ON) {
                        Slog.d("NeuronSystem", "GpsEventDispatcher dispatch location update");
                    }
                }
            }
        }

        private void appendWifiRequest(Parcel parcel) {
            int networkType = NeuronContext.getSystemStatus().getNetworkType();
            parcel.writeInt(networkType);
            String ifname = "unknown_iface";
            if (networkType != -1) {
                parcel.writeInt(1);
                ConnectivityManager connectivityManager = (ConnectivityManager) Publisher.this.mContext.getSystemService("connectivity");
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    String tempIface = connectivityManager.getLinkProperties(network).getInterfaceName();
                    if (tempIface != null) {
                        ifname = tempIface;
                    }
                }
            } else {
                parcel.writeInt(0);
            }
            parcel.writeString(ifname);
            WifiManager wm = (WifiManager) Publisher.this.mContext.getSystemService("wifi");
            WifiInfo wifiinfo = wm.getConnectionInfo();
            String ssid = Shell.NIGHT_MODE_STR_UNKNOWN;
            String bssid = Shell.NIGHT_MODE_STR_UNKNOWN;
            if (wifiinfo != null) {
                ssid = wifiinfo.getSSID();
                bssid = wifiinfo.getBSSID();
            }
            parcel.writeString(ssid);
            parcel.writeString(bssid);
            List<ScanResult> result = wm.getScanResults();
            if (result == null || result.size() == 0) {
                parcel.writeInt(0);
                return;
            }
            result = sortAndTruncate(result);
            parcel.writeInt(result.size());
            for (ScanResult temp : result) {
                parcel.writeString(temp.SSID);
                parcel.writeString(temp.BSSID);
            }
        }

        private void appendCellRequest(Parcel parcel) {
            List<CellInfo> cells = ((TelephonyManager) Publisher.this.mContext.getSystemService("phone")).getAllCellInfo();
            ArrayList<CellInfo> cellInfos = new ArrayList();
            for (CellInfo cell : cells) {
                if (cell.isRegistered()) {
                    cellInfos.add(cell);
                }
            }
            parcel.writeInt(cellInfos.size());
            for (CellInfo cell2 : cellInfos) {
                if (cell2 instanceof CellInfoGsm) {
                    CellIdentityGsm identity = ((CellInfoGsm) cell2).getCellIdentity();
                    parcel.writeInt(1);
                    parcel.writeInt(identity.getMcc());
                    parcel.writeInt(identity.getMnc());
                    parcel.writeInt(identity.getLac());
                    parcel.writeInt(identity.getCid());
                } else if (cell2 instanceof CellInfoCdma) {
                    CellIdentityCdma identity2 = ((CellInfoCdma) cell2).getCellIdentity();
                    parcel.writeInt(2);
                    parcel.writeInt(identity2.getNetworkId());
                    parcel.writeInt(identity2.getSystemId());
                    parcel.writeInt(identity2.getBasestationId());
                    parcel.writeInt(identity2.getLongitude());
                    parcel.writeInt(identity2.getLatitude());
                } else if (cell2 instanceof CellInfoLte) {
                    CellIdentityLte identity3 = ((CellInfoLte) cell2).getCellIdentity();
                    parcel.writeInt(3);
                    parcel.writeInt(identity3.getMcc());
                    parcel.writeInt(identity3.getMnc());
                    parcel.writeInt(identity3.getCi());
                    parcel.writeInt(identity3.getPci());
                    parcel.writeInt(identity3.getTac());
                } else if (cell2 instanceof CellInfoWcdma) {
                    CellIdentityWcdma identity4 = ((CellInfoWcdma) cell2).getCellIdentity();
                    parcel.writeInt(4);
                    parcel.writeInt(identity4.getMcc());
                    parcel.writeInt(identity4.getMnc());
                    parcel.writeInt(identity4.getLac());
                    parcel.writeInt(identity4.getCid());
                    parcel.writeInt(identity4.getPsc());
                }
            }
        }

        private List<ScanResult> sortAndTruncate(List<ScanResult> results) {
            Collections.sort(results, new Comparator<ScanResult>() {
                public int compare(ScanResult r1, ScanResult r2) {
                    return r2.level - r1.level;
                }
            });
            if (results.size() > 10) {
                return results.subList(0, 10);
            }
            return results;
        }
    }

    public class HotAppEventDispatcher implements IEventDispatcher {
        public void dispatch(int eventType, ContentValues contentValues) {
        }
    }

    public class NetworkEventDispatcher implements IEventDispatcher {
        public void dispatch(int eventType, ContentValues contentValues) {
            int networkType = contentValues.getAsInteger("netwok_type").intValue();
            boolean status = contentValues.getAsBoolean("network_status").booleanValue();
            if (ConnectivityManager.isNetworkTypeValid(networkType)) {
                NeuronContext.getSystemStatus().setNetworkType(networkType);
                Request req = Request.obtain();
                Parcel parcel = req.prepare();
                parcel.writeInt(7);
                parcel.writeInt(networkType);
                if (status) {
                    parcel.writeInt(1);
                } else {
                    parcel.writeInt(0);
                }
                String ifname = "unknown_iface";
                ConnectivityManager connectivityManager = (ConnectivityManager) Publisher.this.mContext.getSystemService("connectivity");
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    String tempIface = connectivityManager.getLinkProperties(network).getInterfaceName();
                    if (tempIface != null) {
                        ifname = tempIface;
                    }
                }
                parcel.writeString(ifname);
                if (status && networkType == 1) {
                    WifiManager wm = (WifiManager) Publisher.this.mContext.getSystemService("wifi");
                    WifiInfo wifiinfo = wm.getConnectionInfo();
                    NeuronContext.getSystemStatus().setWifissid(wifiinfo.getSSID());
                    NeuronContext.getSystemStatus().setWifiBssid(wifiinfo.getBSSID());
                    parcel.writeString(wifiinfo.getSSID());
                    parcel.writeString(wifiinfo.getBSSID());
                    Iterable result = null;
                    try {
                        result = wm.getScanResults();
                    } catch (RuntimeException e) {
                        Slog.e("NeuronSystem", "call WifiManager getScanResults err:" + e);
                    }
                    if (result == null || result.size() == 0) {
                        parcel.writeInt(0);
                    } else {
                        parcel.writeInt(result.size());
                        for (ScanResult temp : result) {
                            parcel.writeString(temp.SSID);
                            parcel.writeString(temp.BSSID);
                        }
                    }
                }
                req.commit();
                Publisher.this.mRequestSender.sendRequest(req);
            }
        }
    }

    public class PhoneEventDispatcher implements IEventDispatcher {
        private long mLastRssiTimestamp = 0;
        private int mSignalStrength = -1;

        public void dispatch(int eventType, ContentValues contentValues) {
            Request req;
            Parcel parcel;
            if (12 == eventType) {
                int phoneType = ((TelephonyManager) Publisher.this.mContext.getSystemService("phone")).getPhoneType();
                String plmn = contentValues.getAsString("PLMN");
                int csType = contentValues.getAsInteger("network_type").intValue();
                long csTime = contentValues.getAsLong("cs_time").longValue();
                int psType = contentValues.getAsInteger("mobile_data_type").intValue();
                int psClass = contentValues.getAsInteger("mobile_data_class").intValue();
                long psTime = contentValues.getAsLong("ps_time").longValue();
                if (NeuronSystemManager.LOG_ON) {
                    Slog.d("NeuronSystem", "PhoneEventDispatcher dispatch phoneType:" + phoneType + " PLMN:" + plmn + " csType:" + csType + " psType:" + psType);
                }
                req = Request.obtain();
                parcel = req.prepare();
                parcel.writeInt(5);
                parcel.writeInt(phoneType);
                if (plmn != null) {
                    parcel.writeString(plmn);
                } else {
                    parcel.writeString("");
                }
                parcel.writeInt(csType);
                parcel.writeLong(csTime);
                parcel.writeInt(psType);
                parcel.writeInt(psClass);
                parcel.writeLong(psTime);
                req.commit();
                Publisher.this.mRequestSender.sendRequest(req);
            } else if (13 == eventType && NeuronContext.getSystemStatus().getScreenOn()) {
                int rssitype = ((TelephonyManager) Publisher.this.mContext.getSystemService("phone")).getNetworkType();
                int strength = contentValues.getAsInteger("signal_strength").intValue();
                long period = (long) NeuronContext.getNeoConfig().getRssiUpdatePeriod();
                long now = System.currentTimeMillis();
                if (this.mSignalStrength != strength && now - this.mLastRssiTimestamp > period) {
                    this.mSignalStrength = strength;
                    req = Request.obtain();
                    parcel = req.prepare();
                    parcel.writeInt(6);
                    parcel.writeInt(rssitype);
                    parcel.writeInt(strength);
                    req.commit();
                    Publisher.this.mRequestSender.sendRequest(req);
                    if (NeuronSystemManager.LOG_ON) {
                        Slog.d("NeuronSystem", "PhoneEventDispatcher dispatch rssitype:" + rssitype + " strength:" + strength);
                    }
                }
            }
        }
    }

    public class ScreenEventDispatcher implements IEventDispatcher {
        public void dispatch(int eventType, ContentValues contentValues) {
            int screenOn = contentValues.getAsInteger("screen_on").intValue();
            Request req = Request.obtain();
            Parcel parcel = req.prepare();
            parcel.writeInt(12);
            if (screenOn != 0) {
                NeuronContext.getSystemStatus().setScreenOn(true);
                parcel.writeInt(0);
            } else {
                NeuronContext.getSystemStatus().setScreenOn(false);
                parcel.writeInt(1);
            }
            req.commit();
            Publisher.this.mRequestSender.sendRequest(req);
            if (NeuronSystemManager.LOG_ON) {
                Slog.d("NeuronSystem", "Publisher ScreenEventDispatcher screen state:" + screenOn);
            }
        }
    }

    public class WakelockEventDispatcher implements IEventDispatcher {
        private static final String BROACAST_ACTION = "oppo.intent.action.anomaly.wakelock";
        private static final int MESSAGE_CHECK_TIMEOUT = 1;
        private static final String PAYLOAD_PACKAGE = "payload.package";
        private static final String PAYLOAD_PID = "payload.pid";
        private static final String RECEIVER_PERMISSION = "oppo.permission.OPPO_COMPONENT_SAFE";
        private final int WAKELOCK_USE_TIME_THRESHOLD = SystemProperties.getInt("persist.sys.ns_maxwakelocktime", OppoBrightUtils.SPECIAL_AMBIENT_LIGHT_HORIZON);
        private Handler mTimeoutCheckHandler = new Handler(Publisher.this.mThread.getLooper()) {
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        boolean isPackageUsingWakelockTimeout;
                        String packageName = message.obj;
                        synchronized (WakelockEventDispatcher.this.mUsingWakelockPackages) {
                            isPackageUsingWakelockTimeout = WakelockEventDispatcher.this.mUsingWakelockPackages.containsKey(packageName);
                        }
                        if (isPackageUsingWakelockTimeout) {
                            int pid = message.arg2;
                            Intent intent = new Intent(WakelockEventDispatcher.BROACAST_ACTION);
                            intent.putExtra(WakelockEventDispatcher.PAYLOAD_PACKAGE, packageName);
                            intent.putExtra(WakelockEventDispatcher.PAYLOAD_PID, pid);
                            Publisher.this.mContext.sendBroadcast(intent, WakelockEventDispatcher.RECEIVER_PERMISSION);
                            WakelockEventDispatcher.this.mTimeoutCheckHandler.sendMessageDelayed(Message.obtain(message), (long) WakelockEventDispatcher.this.WAKELOCK_USE_TIME_THRESHOLD);
                            if (NeuronSystemManager.LOG_ON) {
                                Slog.d("NeuronSystem", "Publisher WakelockEventDispatcher hold lock too long:" + packageName + " pid" + pid);
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        private ArrayMap<String, Long> mUsingWakelockPackages = new ArrayMap();

        public void dispatch(int eventType, ContentValues contentValues) {
            String packageName = contentValues.getAsString("pkgname");
            if (packageName != null) {
                if (contentValues.getAsBoolean("wakelock_event").booleanValue()) {
                    synchronized (this.mUsingWakelockPackages) {
                        if (!this.mUsingWakelockPackages.containsKey(packageName)) {
                            this.mUsingWakelockPackages.put(packageName, Long.valueOf(System.currentTimeMillis()));
                            this.mTimeoutCheckHandler.sendMessageDelayed(this.mTimeoutCheckHandler.obtainMessage(1, 0, contentValues.getAsInteger("pid").intValue(), packageName), (long) this.WAKELOCK_USE_TIME_THRESHOLD);
                        }
                    }
                } else {
                    synchronized (this.mUsingWakelockPackages) {
                        this.mUsingWakelockPackages.remove(packageName);
                    }
                    this.mTimeoutCheckHandler.removeMessages(1, packageName);
                }
            }
        }
    }

    public Publisher(Context context, HandlerThread thread) {
        this.mContext = context;
        this.mThread = thread;
        initChannel();
        initDispatcher();
    }

    private void initChannel() {
        Channel channel = new Channel();
        this.mRequestSender = channel.getRequestSender();
        ChannelListener listener = new ChannelListener();
        listener.setIndicationHandler(new IndicationHandler(this.mContext, this.mRequestSender));
        channel.setEventListener(listener);
        channel.triggerInit();
    }

    private void initDispatcher() {
        registerDispatcher(1, new AppEventDispatcher());
        registerDispatcher(3, new ScreenEventDispatcher());
        registerDispatcher(5, new BatteryEventDispatcher());
        registerDispatcher(8, new NetworkEventDispatcher());
        registerDispatcher(9, new AlarmEventDispatcher());
        registerDispatcher(10, new WakelockEventDispatcher());
        registerDispatcher(11, new GpsEventDispatcher());
        PhoneEventDispatcher phoneEventDispatcher = new PhoneEventDispatcher();
        registerDispatcher(12, phoneEventDispatcher);
        registerDispatcher(13, phoneEventDispatcher);
    }

    public boolean registerDispatcher(int eventType, IEventDispatcher dispatcher) {
        if (eventType <= 0 || eventType >= 32 || dispatcher == null) {
            Slog.w("NeuronSystem", "Publisher registerDispatcher para err. type:" + eventType);
            return false;
        }
        synchronized (this.mRegisterTable) {
            if (this.mRegisterTable[eventType] != null) {
                Slog.w("NeuronSystem", "Publisher registerPublisher override old dispatcher");
            }
            this.mRegisterTable[eventType] = dispatcher;
        }
        return true;
    }

    public IEventDispatcher unregisterDispatcher(int eventType) {
        if (eventType <= 0 || eventType >= 32) {
            Slog.w("NeuronSystem", "Publisher registerDispatcher para err. type:" + eventType);
            return null;
        }
        IEventDispatcher dis;
        synchronized (this.mRegisterTable) {
            dis = this.mRegisterTable[eventType];
            this.mRegisterTable[eventType] = null;
        }
        return dis;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void publishEvent(int eventType, ContentValues contentValues) {
        if (!this.sPropertyOn) {
            return;
        }
        if (eventType <= 0 || eventType >= 32 || contentValues == null) {
            Slog.w("NeuronSystem", "Publisher publishEvent para err. type:" + eventType);
            return;
        }
        synchronized (this.mRegisterTable) {
            if (this.mRegisterTable[eventType] == null) {
                return;
            }
            IEventDispatcher dispatcher = this.mRegisterTable[eventType];
        }
    }
}
