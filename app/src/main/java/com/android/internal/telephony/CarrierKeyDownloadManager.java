package com.android.internal.telephony;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.telephony.uicc.SpnOverride;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.zip.GZIPInputStream;

public class CarrierKeyDownloadManager {
    private static final int[] CARRIER_KEY_TYPES = new int[]{1, 2};
    private static final int DAY_IN_MILLIS = 86400000;
    private static final int DEFAULT_RENEWAL_WINDOW_DAYS = 7;
    private static final String INTENT_KEY_RENEWAL_ALARM_PREFIX = "com.android.internal.telephony.carrier_key_download_alarm";
    private static final String JSON_CARRIER_KEYS = "carrier-keys";
    private static final String JSON_CERTIFICATE = "certificate";
    private static final String JSON_CERTIFICATE_ALTERNATE = "public-key";
    private static final String JSON_IDENTIFIER = "key-identifier";
    private static final String JSON_TYPE = "key-type";
    private static final String JSON_TYPE_VALUE_EPDG = "EPDG";
    private static final String JSON_TYPE_VALUE_WLAN = "WLAN";
    private static final String LOG_TAG = "CarrierKeyDownloadManager";
    public static final String MCC = "MCC";
    private static final String MCC_MNC_PREF_TAG = "CARRIER_KEY_DM_MCC_MNC";
    public static final String MNC = "MNC";
    private static final String SEPARATOR = ":";
    private static final int UNINITIALIZED_KEY_TYPE = -1;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int slotId = CarrierKeyDownloadManager.this.mPhone.getPhoneId();
            if (action.equals(CarrierKeyDownloadManager.INTENT_KEY_RENEWAL_ALARM_PREFIX + slotId)) {
                Log.d(CarrierKeyDownloadManager.LOG_TAG, "Handling key renewal alarm: " + action);
                CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
            } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                if (slotId == intent.getIntExtra("phone", -1)) {
                    Log.d(CarrierKeyDownloadManager.LOG_TAG, "Carrier Config changed: " + action);
                    CarrierKeyDownloadManager.this.handleAlarmOrConfigChange();
                }
            } else if (action.equals("android.intent.action.DOWNLOAD_COMPLETE")) {
                Log.d(CarrierKeyDownloadManager.LOG_TAG, "Download Complete");
                long carrierKeyDownloadIdentifier = intent.getLongExtra("extra_download_id", 0);
                String mccMnc = CarrierKeyDownloadManager.this.getMccMncSetFromPref();
                if (CarrierKeyDownloadManager.this.isValidDownload(mccMnc)) {
                    CarrierKeyDownloadManager.this.onDownloadComplete(carrierKeyDownloadIdentifier, mccMnc);
                    CarrierKeyDownloadManager.this.onPostDownloadProcessing(carrierKeyDownloadIdentifier);
                }
            }
        }
    };
    private final Context mContext;
    public final DownloadManager mDownloadManager;
    public int mKeyAvailability = 0;
    private final Phone mPhone;
    private String mURL;

    public CarrierKeyDownloadManager(Phone phone) {
        this.mPhone = phone;
        this.mContext = phone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        filter.addAction(INTENT_KEY_RENEWAL_ALARM_PREFIX + this.mPhone.getPhoneId());
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, phone);
        this.mDownloadManager = (DownloadManager) this.mContext.getSystemService("download");
    }

    private void onPostDownloadProcessing(long carrierKeyDownloadIdentifier) {
        resetRenewalAlarm();
        cleanupDownloadPreferences(carrierKeyDownloadIdentifier);
    }

    private void handleAlarmOrConfigChange() {
        if (!carrierUsesKeys()) {
            cleanupRenewalAlarms();
        } else if (areCarrierKeysAbsentOrExpiring() && !downloadKey()) {
            resetRenewalAlarm();
        }
    }

    private void cleanupDownloadPreferences(long carrierKeyDownloadIdentifier) {
        Log.d(LOG_TAG, "Cleaning up download preferences: " + carrierKeyDownloadIdentifier);
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editor.remove(String.valueOf(carrierKeyDownloadIdentifier));
        editor.commit();
    }

    private void cleanupRenewalAlarms() {
        Log.d(LOG_TAG, "Cleaning up existing renewal alarms");
        ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX + this.mPhone.getPhoneId()), 134217728));
    }

    public long getExpirationDate() {
        long minExpirationDate = Long.MAX_VALUE;
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                ImsiEncryptionInfo imsiEncryptionInfo = this.mPhone.getCarrierInfoForImsiEncryption(key_type);
                if (!(imsiEncryptionInfo == null || imsiEncryptionInfo.getExpirationTime() == null || minExpirationDate <= imsiEncryptionInfo.getExpirationTime().getTime())) {
                    minExpirationDate = imsiEncryptionInfo.getExpirationTime().getTime();
                }
            }
        }
        if (minExpirationDate == Long.MAX_VALUE || minExpirationDate < System.currentTimeMillis() + 604800000) {
            return System.currentTimeMillis() + 86400000;
        }
        return minExpirationDate - 604800000;
    }

    public void resetRenewalAlarm() {
        cleanupRenewalAlarms();
        int slotId = this.mPhone.getPhoneId();
        long minExpirationDate = getExpirationDate();
        Log.d(LOG_TAG, "minExpirationDate: " + new Date(minExpirationDate));
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent intent = new Intent(INTENT_KEY_RENEWAL_ALARM_PREFIX + slotId);
        alarmManager.set(2, minExpirationDate, PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728));
        Log.d(LOG_TAG, "setRenewelAlarm: action=" + intent.getAction() + " time=" + new Date(minExpirationDate));
    }

    private String getMccMncSetFromPref() {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(MCC_MNC_PREF_TAG + this.mPhone.getPhoneId(), null);
    }

    public String getSimOperator() {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getSimOperator(this.mPhone.getSubId());
    }

    public boolean isValidDownload(String mccMnc) {
        String mccCurrent = SpnOverride.MVNO_TYPE_NONE;
        String mncCurrent = SpnOverride.MVNO_TYPE_NONE;
        String mccSource = SpnOverride.MVNO_TYPE_NONE;
        String mncSource = SpnOverride.MVNO_TYPE_NONE;
        String simOperator = getSimOperator();
        if (TextUtils.isEmpty(simOperator) || TextUtils.isEmpty(mccMnc)) {
            Log.e(LOG_TAG, "simOperator or mcc/mnc is empty");
            return false;
        }
        String[] splitValue = mccMnc.split(SEPARATOR);
        mccSource = splitValue[0];
        mncSource = splitValue[1];
        Log.d(LOG_TAG, "values from sharedPrefs mcc, mnc: " + mccSource + "," + mncSource);
        mccCurrent = simOperator.substring(0, 3);
        mncCurrent = simOperator.substring(3);
        Log.d(LOG_TAG, "using values for mcc, mnc: " + mccCurrent + "," + mncCurrent);
        return TextUtils.equals(mncSource, mncCurrent) && TextUtils.equals(mccSource, mccCurrent);
    }

    private void onDownloadComplete(long carrierKeyDownloadIdentifier, String mccMnc) {
        Exception e;
        Throwable th;
        Log.d(LOG_TAG, "onDownloadComplete: " + carrierKeyDownloadIdentifier);
        Query query = new Query();
        query.setFilterById(new long[]{carrierKeyDownloadIdentifier});
        Cursor cursor = this.mDownloadManager.query(query);
        InputStream source = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                if (8 == cursor.getInt(cursor.getColumnIndex("status"))) {
                    try {
                        InputStream source2 = new FileInputStream(this.mDownloadManager.openDownloadedFile(carrierKeyDownloadIdentifier).getFileDescriptor());
                        try {
                            parseJsonAndPersistKey(convertToString(source2), mccMnc);
                            this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                            try {
                                source2.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                            source = source2;
                        } catch (Exception e3) {
                            e = e3;
                            source = source2;
                            try {
                                Log.e(LOG_TAG, "Error in download:" + carrierKeyDownloadIdentifier + ". " + e);
                                this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                                try {
                                    source.close();
                                } catch (IOException e22) {
                                    e22.printStackTrace();
                                }
                                Log.d(LOG_TAG, "Completed downloading keys");
                                cursor.close();
                            } catch (Throwable th2) {
                                th = th2;
                                this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                                try {
                                    source.close();
                                } catch (IOException e222) {
                                    e222.printStackTrace();
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            source = source2;
                            this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                            source.close();
                            throw th;
                        }
                    } catch (Exception e4) {
                        e = e4;
                        Log.e(LOG_TAG, "Error in download:" + carrierKeyDownloadIdentifier + ". " + e);
                        this.mDownloadManager.remove(new long[]{carrierKeyDownloadIdentifier});
                        source.close();
                        Log.d(LOG_TAG, "Completed downloading keys");
                        cursor.close();
                    }
                }
                Log.d(LOG_TAG, "Completed downloading keys");
            }
            cursor.close();
        }
    }

    private boolean carrierUsesKeys() {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            return false;
        }
        PersistableBundle b = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (b == null) {
            return false;
        }
        this.mKeyAvailability = b.getInt("imsi_key_availability_int");
        this.mURL = b.getString("imsi_key_download_url_string");
        if (TextUtils.isEmpty(this.mURL) || this.mKeyAvailability == 0) {
            Log.d(LOG_TAG, "Carrier not enabled or invalid values");
            return false;
        }
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                return true;
            }
        }
        return false;
    }

    private static String convertToString(InputStream is) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return sb.toString();
                }
                sb.append(line).append(10);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void parseJsonAndPersistKey(java.lang.String r25, java.lang.String r26) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r20_2 'reader' com.android.org.bouncycastle.util.io.pem.PemReader) in PHI: PHI: (r20_9 'reader' com.android.org.bouncycastle.util.io.pem.PemReader) = (r20_0 'reader' com.android.org.bouncycastle.util.io.pem.PemReader), (r20_0 'reader' com.android.org.bouncycastle.util.io.pem.PemReader), (r20_2 'reader' com.android.org.bouncycastle.util.io.pem.PemReader), (r20_2 'reader' com.android.org.bouncycastle.util.io.pem.PemReader) binds: {(r20_0 'reader' com.android.org.bouncycastle.util.io.pem.PemReader)=B:7:0x0018, (r20_0 'reader' com.android.org.bouncycastle.util.io.pem.PemReader)=B:8:?, (r20_2 'reader' com.android.org.bouncycastle.util.io.pem.PemReader)=B:20:0x009c, (r20_2 'reader' com.android.org.bouncycastle.util.io.pem.PemReader)=B:64:0x013e}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r24 = this;
        r2 = android.text.TextUtils.isEmpty(r25);
        if (r2 != 0) goto L_0x000c;
    L_0x0006:
        r2 = android.text.TextUtils.isEmpty(r26);
        if (r2 == 0) goto L_0x0016;
    L_0x000c:
        r2 = "CarrierKeyDownloadManager";
        r3 = "jsonStr or mcc, mnc: is empty";
        android.util.Log.e(r2, r3);
        return;
    L_0x0016:
        r20 = 0;
        r8 = "";	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r9 = "";	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = ":";	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r0 = r26;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r22 = r0.split(r2);	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = 0;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r8 = r22[r2];	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = 1;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r9 = r22[r2];	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r16 = new org.json.JSONObject;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r0 = r16;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r1 = r25;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r0.<init>(r1);	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = "carrier-keys";	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r0 = r16;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r19 = r0.getJSONArray(r2);	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r14 = 0;
        r21 = r20;
    L_0x0042:
        r2 = r19.length();	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        if (r14 >= r2) goto L_0x00dd;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
    L_0x0048:
        r0 = r19;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r17 = r0.getJSONObject(r14);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r10 = 0;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = "certificate";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r17;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = r0.has(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        if (r2 == 0) goto L_0x00c6;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
    L_0x005a:
        r2 = "certificate";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r17;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r10 = r0.getString(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
    L_0x0063:
        r2 = "key-type";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r17;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r23 = r0.getString(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r4 = -1;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = "WLAN";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r23;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = r0.equals(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        if (r2 == 0) goto L_0x00d0;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
    L_0x0078:
        r4 = 2;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
    L_0x0079:
        r2 = "key-identifier";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r17;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r5 = r0.getString(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r15 = new java.io.ByteArrayInputStream;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = r10.getBytes();	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r15.<init>(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r13 = new java.io.BufferedReader;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = new java.io.InputStreamReader;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2.<init>(r15);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r13.<init>(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r20 = new com.android.org.bouncycastle.util.io.pem.PemReader;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r20;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0.<init>(r13);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = r20.readPemObject();	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = r2.getContent();	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r18 = getKeyInformation(r2);	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r20.close();	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r0 = r18;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r3 = r0.first;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r3 = (java.security.PublicKey) r3;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r0 = r18;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = r0.second;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = (java.lang.Long) r2;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r6 = r2.longValue();	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2 = r24;	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r2.savePublicKey(r3, r4, r5, r6, r8, r9);	 Catch:{ JSONException -> 0x013e, Exception -> 0x0101 }
        r14 = r14 + 1;
        r21 = r20;
        goto L_0x0042;
    L_0x00c6:
        r2 = "public-key";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r17;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r10 = r0.getString(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        goto L_0x0063;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
    L_0x00d0:
        r2 = "EPDG";	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r0 = r23;	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        r2 = r0.equals(r2);	 Catch:{ JSONException -> 0x01a7, Exception -> 0x01ab, all -> 0x01a3 }
        if (r2 == 0) goto L_0x0079;
    L_0x00db:
        r4 = 1;
        goto L_0x0079;
    L_0x00dd:
        if (r21 == 0) goto L_0x00e2;
    L_0x00df:
        r21.close();	 Catch:{ Exception -> 0x00e5 }
    L_0x00e2:
        r20 = r21;
    L_0x00e4:
        return;
    L_0x00e5:
        r11 = move-exception;
        r2 = "CarrierKeyDownloadManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r6 = "Exception getting certificate: ";
        r3 = r3.append(r6);
        r3 = r3.append(r11);
        r3 = r3.toString();
        android.util.Log.e(r2, r3);
        goto L_0x00e2;
    L_0x0101:
        r11 = move-exception;
    L_0x0102:
        r2 = "CarrierKeyDownloadManager";	 Catch:{ all -> 0x0180 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0180 }
        r3.<init>();	 Catch:{ all -> 0x0180 }
        r6 = "Exception getting certificate: ";	 Catch:{ all -> 0x0180 }
        r3 = r3.append(r6);	 Catch:{ all -> 0x0180 }
        r3 = r3.append(r11);	 Catch:{ all -> 0x0180 }
        r3 = r3.toString();	 Catch:{ all -> 0x0180 }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x0180 }
        if (r20 == 0) goto L_0x00e4;
    L_0x011e:
        r20.close();	 Catch:{ Exception -> 0x0122 }
        goto L_0x00e4;
    L_0x0122:
        r11 = move-exception;
        r2 = "CarrierKeyDownloadManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r6 = "Exception getting certificate: ";
        r3 = r3.append(r6);
        r3 = r3.append(r11);
        r3 = r3.toString();
        android.util.Log.e(r2, r3);
        goto L_0x00e4;
    L_0x013e:
        r12 = move-exception;
    L_0x013f:
        r2 = "CarrierKeyDownloadManager";	 Catch:{ all -> 0x0180 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0180 }
        r3.<init>();	 Catch:{ all -> 0x0180 }
        r6 = "Json parsing error: ";	 Catch:{ all -> 0x0180 }
        r3 = r3.append(r6);	 Catch:{ all -> 0x0180 }
        r6 = r12.getMessage();	 Catch:{ all -> 0x0180 }
        r3 = r3.append(r6);	 Catch:{ all -> 0x0180 }
        r3 = r3.toString();	 Catch:{ all -> 0x0180 }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x0180 }
        if (r20 == 0) goto L_0x00e4;
    L_0x015f:
        r20.close();	 Catch:{ Exception -> 0x0163 }
        goto L_0x00e4;
    L_0x0163:
        r11 = move-exception;
        r2 = "CarrierKeyDownloadManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r6 = "Exception getting certificate: ";
        r3 = r3.append(r6);
        r3 = r3.append(r11);
        r3 = r3.toString();
        android.util.Log.e(r2, r3);
        goto L_0x00e4;
    L_0x0180:
        r2 = move-exception;
    L_0x0181:
        if (r20 == 0) goto L_0x0186;
    L_0x0183:
        r20.close();	 Catch:{ Exception -> 0x0187 }
    L_0x0186:
        throw r2;
    L_0x0187:
        r11 = move-exception;
        r3 = "CarrierKeyDownloadManager";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Exception getting certificate: ";
        r6 = r6.append(r7);
        r6 = r6.append(r11);
        r6 = r6.toString();
        android.util.Log.e(r3, r6);
        goto L_0x0186;
    L_0x01a3:
        r2 = move-exception;
        r20 = r21;
        goto L_0x0181;
    L_0x01a7:
        r12 = move-exception;
        r20 = r21;
        goto L_0x013f;
    L_0x01ab:
        r11 = move-exception;
        r20 = r21;
        goto L_0x0102;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.CarrierKeyDownloadManager.parseJsonAndPersistKey(java.lang.String, java.lang.String):void");
    }

    public boolean isKeyEnabled(int keyType) {
        if (((this.mKeyAvailability >> (keyType - 1)) & 1) == 1) {
            return true;
        }
        return false;
    }

    public boolean areCarrierKeysAbsentOrExpiring() {
        boolean z = true;
        for (int key_type : CARRIER_KEY_TYPES) {
            if (isKeyEnabled(key_type)) {
                ImsiEncryptionInfo imsiEncryptionInfo = this.mPhone.getCarrierInfoForImsiEncryption(key_type);
                if (imsiEncryptionInfo == null) {
                    Log.d(LOG_TAG, "Key not found for: " + key_type);
                    return true;
                }
                if (imsiEncryptionInfo.getExpirationTime().getTime() - System.currentTimeMillis() >= 604800000) {
                    z = false;
                }
                return z;
            }
        }
        return false;
    }

    private boolean downloadKey() {
        Log.d(LOG_TAG, "starting download from: " + this.mURL);
        String mcc = SpnOverride.MVNO_TYPE_NONE;
        String mnc = SpnOverride.MVNO_TYPE_NONE;
        String simOperator = getSimOperator();
        if (TextUtils.isEmpty(simOperator)) {
            Log.e(LOG_TAG, "mcc, mnc: is empty");
            return false;
        }
        mcc = simOperator.substring(0, 3);
        mnc = simOperator.substring(3);
        Log.d(LOG_TAG, "using values for mcc, mnc: " + mcc + "," + mnc);
        try {
            Request request = new Request(Uri.parse(this.mURL));
            request.setAllowedOverMetered(false);
            request.setVisibleInDownloadsUi(false);
            Long carrierKeyDownloadRequestId = Long.valueOf(this.mDownloadManager.enqueue(request));
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            String mccMnc = mcc + SEPARATOR + mnc;
            int slotId = this.mPhone.getPhoneId();
            Log.d(LOG_TAG, "storing values in sharedpref mcc, mnc, days: " + mcc + "," + mnc + "," + carrierKeyDownloadRequestId);
            editor.putString(MCC_MNC_PREF_TAG + slotId, mccMnc);
            editor.commit();
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "exception trying to dowload key from url: " + this.mURL);
            return false;
        }
    }

    public static Pair<PublicKey, Long> getKeyInformation(byte[] certificate) throws Exception {
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate));
        return new Pair(cert.getPublicKey(), Long.valueOf(cert.getNotAfter().getTime()));
    }

    public void savePublicKey(PublicKey publicKey, int type, String identifier, long expirationDate, String mcc, String mnc) {
        this.mPhone.setCarrierInfoForImsiEncryption(new ImsiEncryptionInfo(mcc, mnc, type, identifier, publicKey, new Date(expirationDate)));
    }
}
