package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SeempLog;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.android.internal.R;
import java.util.Locale;

public class CallerInfo {
    public static final String PAYPHONE_NUMBER = "-3";
    public static final String PRIVATE_NUMBER = "-2";
    private static final String TAG = "CallerInfo";
    public static final String UNKNOWN_NUMBER = "-1";
    public static final long USER_TYPE_CURRENT = 0;
    public static final long USER_TYPE_WORK = 1;
    private static final boolean VDBG = Rlog.isLoggable(TAG, 2);
    public Drawable cachedPhoto;
    public Bitmap cachedPhotoIcon;
    public String cnapName;
    public Uri contactDisplayPhotoUri;
    public boolean contactExists;
    public long contactIdOrZero;
    public Uri contactRefUri;
    public Uri contactRingtoneUri;
    public String geoDescription;
    public boolean isCachedPhotoCurrent;
    public String lookupKey;
    private boolean mIsEmergency = false;
    private boolean mIsVoiceMail = false;
    public String name;
    public int namePresentation;
    public boolean needUpdate;
    public String normalizedNumber;
    public String numberLabel;
    public int numberPresentation;
    public int numberType;
    public String phoneLabel;
    public String phoneNumber;
    public int photoResource;
    public boolean shouldSendToVoicemail;
    public long userType = 0;

    public static CallerInfo getCallerInfo(Context context, Uri contactRef, Cursor cursor) {
        SeempLog.record_uri(12, contactRef);
        CallerInfo info = new CallerInfo();
        info.photoResource = 0;
        info.phoneLabel = null;
        info.numberType = 0;
        info.numberLabel = null;
        info.cachedPhoto = null;
        info.isCachedPhotoCurrent = false;
        info.contactExists = false;
        info.userType = 0;
        if (VDBG) {
            Rlog.v(TAG, "getCallerInfo() based on cursor...");
        }
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("display_name");
                if (columnIndex != -1) {
                    info.name = cursor.getString(columnIndex);
                }
                columnIndex = cursor.getColumnIndex("number");
                if (columnIndex != -1) {
                    info.phoneNumber = cursor.getString(columnIndex);
                }
                columnIndex = cursor.getColumnIndex("normalized_number");
                if (columnIndex != -1) {
                    info.normalizedNumber = cursor.getString(columnIndex);
                }
                columnIndex = cursor.getColumnIndex("label");
                if (columnIndex != -1) {
                    int typeColumnIndex = cursor.getColumnIndex("type");
                    if (typeColumnIndex != -1) {
                        info.numberType = cursor.getInt(typeColumnIndex);
                        info.numberLabel = cursor.getString(columnIndex);
                        info.phoneLabel = Phone.getDisplayLabel(context, info.numberType, info.numberLabel).toString();
                    }
                }
                columnIndex = getColumnIndexForPersonId(contactRef, cursor);
                if (columnIndex != -1) {
                    long contactId = cursor.getLong(columnIndex);
                    if (!(contactId == 0 || (Contacts.isEnterpriseContactId(contactId) ^ 1) == 0)) {
                        info.contactIdOrZero = contactId;
                        if (VDBG) {
                            Rlog.v(TAG, "==> got info.contactIdOrZero: " + info.contactIdOrZero);
                        }
                    }
                    if (Contacts.isEnterpriseContactId(contactId)) {
                        info.userType = 1;
                    }
                } else {
                    Rlog.w(TAG, "Couldn't find contact_id column for " + contactRef);
                }
                columnIndex = cursor.getColumnIndex(ContactsColumns.LOOKUP_KEY);
                if (columnIndex != -1) {
                    info.lookupKey = cursor.getString(columnIndex);
                }
                columnIndex = cursor.getColumnIndex("photo_uri");
                if (columnIndex == -1 || cursor.getString(columnIndex) == null) {
                    info.contactDisplayPhotoUri = null;
                } else {
                    info.contactDisplayPhotoUri = Uri.parse(cursor.getString(columnIndex));
                }
                columnIndex = cursor.getColumnIndex("custom_ringtone");
                if (columnIndex == -1 || cursor.getString(columnIndex) == null) {
                    info.contactRingtoneUri = null;
                } else if (TextUtils.isEmpty(cursor.getString(columnIndex))) {
                    info.contactRingtoneUri = Uri.EMPTY;
                } else {
                    info.contactRingtoneUri = Uri.parse(cursor.getString(columnIndex));
                }
                columnIndex = cursor.getColumnIndex("send_to_voicemail");
                boolean z = columnIndex != -1 ? cursor.getInt(columnIndex) == 1 : false;
                info.shouldSendToVoicemail = z;
                info.contactExists = true;
            }
            cursor.close();
        }
        info.needUpdate = false;
        info.name = normalize(info.name);
        info.contactRefUri = contactRef;
        return info;
    }

    public static CallerInfo getCallerInfo(Context context, Uri contactRef) {
        CallerInfo info = null;
        ContentResolver cr = CallerInfoAsyncQuery.getCurrentProfileContentResolver(context);
        if (cr == null) {
            return info;
        }
        try {
            return getCallerInfo(context, contactRef, cr.query(contactRef, null, null, null, null));
        } catch (RuntimeException re) {
            Rlog.e(TAG, "Error getting caller info.", re);
            return info;
        }
    }

    public static CallerInfo getCallerInfo(Context context, String number) {
        if (VDBG) {
            Rlog.v(TAG, "getCallerInfo() based on number...");
        }
        return getCallerInfo(context, number, SubscriptionManager.getDefaultSubscriptionId());
    }

    public static CallerInfo getCallerInfo(Context context, String number, int subId) {
        SeempLog.record_str(12, "number=" + number + ",subId=" + subId);
        if (TextUtils.isEmpty(number)) {
            return null;
        }
        if (PhoneNumberUtils.isLocalEmergencyNumber(context, number)) {
            return new CallerInfo().markAsEmergency(context);
        }
        if (PhoneNumberUtils.isVoiceMailNumber(subId, number)) {
            return new CallerInfo().markAsVoiceMail();
        }
        CallerInfo info = doSecondaryLookupIfNecessary(context, number, getCallerInfo(context, Uri.withAppendedPath(PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(number))));
        if (TextUtils.isEmpty(info.phoneNumber)) {
            info.phoneNumber = number;
        }
        return info;
    }

    static CallerInfo doSecondaryLookupIfNecessary(Context context, String number, CallerInfo previousResult) {
        if (previousResult.contactExists || !PhoneNumberUtils.isUriNumber(number)) {
            return previousResult;
        }
        String username = PhoneNumberUtils.getUsernameFromUriNumber(number);
        if (PhoneNumberUtils.isGlobalPhoneNumber(username)) {
            return getCallerInfo(context, Uri.withAppendedPath(PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(username)));
        }
        return previousResult;
    }

    public boolean isEmergencyNumber() {
        return this.mIsEmergency;
    }

    public boolean isVoiceMailNumber() {
        return this.mIsVoiceMail;
    }

    CallerInfo markAsEmergency(Context context) {
        this.phoneNumber = context.getString(R.string.emergency_call_dialog_number_for_display);
        this.photoResource = R.drawable.picture_emergency;
        this.mIsEmergency = true;
        return this;
    }

    CallerInfo markAsVoiceMail() {
        return markAsVoiceMail(SubscriptionManager.getDefaultSubscriptionId());
    }

    CallerInfo markAsVoiceMail(int subId) {
        this.mIsVoiceMail = true;
        try {
            this.phoneNumber = TelephonyManager.getDefault().getVoiceMailAlphaTag(subId);
        } catch (SecurityException se) {
            Rlog.e(TAG, "Cannot access VoiceMail.", se);
        }
        return this;
    }

    private static String normalize(String s) {
        if (s == null || s.length() > 0) {
            return s;
        }
        return null;
    }

    private static int getColumnIndexForPersonId(Uri contactRef, Cursor cursor) {
        if (VDBG) {
            Rlog.v(TAG, "- getColumnIndexForPersonId: contactRef URI = '" + contactRef + "'...");
        }
        String url = contactRef.toString();
        String columnName = null;
        if (url.startsWith("content://com.android.contacts/data/phones")) {
            if (VDBG) {
                Rlog.v(TAG, "'data/phones' URI; using RawContacts.CONTACT_ID");
            }
            columnName = "contact_id";
        } else if (url.startsWith("content://com.android.contacts/data")) {
            if (VDBG) {
                Rlog.v(TAG, "'data' URI; using Data.CONTACT_ID");
            }
            columnName = "contact_id";
        } else if (url.startsWith("content://com.android.contacts/phone_lookup")) {
            if (VDBG) {
                Rlog.v(TAG, "'phone_lookup' URI; using PhoneLookup._ID");
            }
            columnName = "_id";
        } else {
            Rlog.w(TAG, "Unexpected prefix for contactRef '" + url + "'");
        }
        int columnIndex = columnName != null ? cursor.getColumnIndex(columnName) : -1;
        if (VDBG) {
            Rlog.v(TAG, "==> Using column '" + columnName + "' (columnIndex = " + columnIndex + ") for person_id lookup...");
        }
        return columnIndex;
    }

    public void updateGeoDescription(Context context, String fallbackNumber) {
        this.geoDescription = getGeoDescription(context, TextUtils.isEmpty(this.phoneNumber) ? fallbackNumber : this.phoneNumber);
    }

    public static String getGeoDescription(Context context, String number) {
        if (VDBG) {
            Rlog.v(TAG, "getGeoDescription('" + number + "')...");
        }
        if (TextUtils.isEmpty(number)) {
            return null;
        }
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
        Locale locale = context.getResources().getConfiguration().locale;
        String countryIso = getCurrentCountryIso(context, locale);
        PhoneNumber pn = null;
        try {
            if (VDBG) {
                Rlog.v(TAG, "parsing '" + number + "' for countryIso '" + countryIso + "'...");
            }
            pn = util.parse(number, countryIso);
            if (VDBG) {
                Rlog.v(TAG, "- parsed number: " + pn);
            }
        } catch (NumberParseException e) {
            Rlog.w(TAG, "getGeoDescription: NumberParseException for incoming number '" + Rlog.pii(TAG, (Object) number) + "'");
        }
        if (pn == null) {
            return null;
        }
        String description;
        if (SystemProperties.get("ro.oppo.version", "CN").equalsIgnoreCase("US")) {
            description = getCountryNameForNumber(pn, locale);
        } else {
            description = geocoder.getDescriptionForNumber(pn, locale);
        }
        if (VDBG) {
            Rlog.v(TAG, "- got description: '" + description + "'");
        }
        return description;
    }

    private static String getCurrentCountryIso(Context context, Locale locale) {
        String countryIso = null;
        CountryDetector detector = (CountryDetector) context.getSystemService("country_detector");
        if (detector != null) {
            Country country = detector.detectCountry();
            if (country != null) {
                countryIso = country.getCountryIso();
            } else {
                Rlog.e(TAG, "CountryDetector.detectCountry() returned null.");
            }
        }
        if (countryIso != null) {
            return countryIso;
        }
        countryIso = locale.getCountry();
        Rlog.w(TAG, "No CountryDetector; falling back to countryIso based on locale: " + countryIso);
        return countryIso;
    }

    protected static String getCurrentCountryIso(Context context) {
        return getCurrentCountryIso(context, Locale.getDefault());
    }

    public String toString() {
        return super.toString() + " { " + "name " + (this.name == null ? "null" : "non-null") + ", phoneNumber " + (this.phoneNumber == null ? "null" : "non-null") + " }";
    }

    public static String getCountryNameForNumber(PhoneNumber number, Locale language) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        if (phoneUtil.getNumberType(number) == PhoneNumberType.UNKNOWN) {
            return "";
        }
        String regionCode = phoneUtil.getRegionCodeForNumber(number);
        String displayCountry = (regionCode == null || regionCode.equals("ZZ") || regionCode.equals("001")) ? "" : new Locale("", regionCode).getDisplayCountry(language);
        return displayCountry;
    }
}
