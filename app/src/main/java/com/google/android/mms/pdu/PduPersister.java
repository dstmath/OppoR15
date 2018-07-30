package com.google.android.mms.pdu;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.drm.DrmManagerClient;
import android.hardware.radio.V1_0.RadioAccessFamily;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Draft;
import android.provider.Telephony.Mms.Inbox;
import android.provider.Telephony.Mms.Outbox;
import android.provider.Telephony.Mms.Sent;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Threads;
import android.provider.oppo.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.uicc.SpnOverride;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.util.DownloadDrmHelper;
import com.google.android.mms.util.DrmConvertSession;
import com.google.android.mms.util.PduCache;
import com.google.android.mms.util.PduCacheEntry;
import com.google.android.mms.util.SqliteWrapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class PduPersister {
    static final /* synthetic */ boolean -assertionsDisabled = (PduPersister.class.desiredAssertionStatus() ^ 1);
    private static final int[] ADDRESS_FIELDS = new int[]{129, 130, 137, 151};
    private static final HashMap<Integer, Integer> CHARSET_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> CHARSET_COLUMN_NAME_MAP = new HashMap();
    private static final boolean DEBUG = false;
    private static final long DUMMY_THREAD_ID = Long.MAX_VALUE;
    private static final HashMap<Integer, Integer> ENCODED_STRING_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> ENCODED_STRING_COLUMN_NAME_MAP = new HashMap();
    private static final boolean LOCAL_LOGV = false;
    private static final HashMap<Integer, Integer> LONG_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> LONG_COLUMN_NAME_MAP = new HashMap();
    private static final HashMap<Uri, Integer> MESSAGE_BOX_MAP = new HashMap();
    private static final HashMap<Integer, Integer> OCTET_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> OCTET_COLUMN_NAME_MAP = new HashMap();
    private static final int PART_COLUMN_CHARSET = 1;
    private static final int PART_COLUMN_CONTENT_DISPOSITION = 2;
    private static final int PART_COLUMN_CONTENT_ID = 3;
    private static final int PART_COLUMN_CONTENT_LOCATION = 4;
    private static final int PART_COLUMN_CONTENT_TYPE = 5;
    private static final int PART_COLUMN_FILENAME = 6;
    private static final int PART_COLUMN_ID = 0;
    private static final int PART_COLUMN_NAME = 7;
    private static final int PART_COLUMN_TEXT = 8;
    private static final String[] PART_PROJECTION = new String[]{"_id", "chset", "cd", "cid", "cl", "ct", "fn", Calls.CACHED_NAME, "text"};
    private static final PduCache PDU_CACHE_INSTANCE = PduCache.getInstance();
    private static final int PDU_COLUMN_CONTENT_CLASS = 11;
    private static final int PDU_COLUMN_CONTENT_LOCATION = 5;
    private static final int PDU_COLUMN_CONTENT_TYPE = 6;
    private static final int PDU_COLUMN_DATE = 21;
    private static final int PDU_COLUMN_DELIVERY_REPORT = 12;
    private static final int PDU_COLUMN_DELIVERY_TIME = 22;
    private static final int PDU_COLUMN_EXPIRY = 23;
    private static final int PDU_COLUMN_ID = 0;
    private static final int PDU_COLUMN_MESSAGE_BOX = 1;
    private static final int PDU_COLUMN_MESSAGE_CLASS = 7;
    private static final int PDU_COLUMN_MESSAGE_ID = 8;
    private static final int PDU_COLUMN_MESSAGE_SIZE = 24;
    private static final int PDU_COLUMN_MESSAGE_TYPE = 13;
    private static final int PDU_COLUMN_MMS_VERSION = 14;
    private static final int PDU_COLUMN_PRIORITY = 15;
    private static final int PDU_COLUMN_READ_REPORT = 16;
    private static final int PDU_COLUMN_READ_STATUS = 17;
    private static final int PDU_COLUMN_REPORT_ALLOWED = 18;
    private static final int PDU_COLUMN_RESPONSE_TEXT = 9;
    private static final int PDU_COLUMN_RETRIEVE_STATUS = 19;
    private static final int PDU_COLUMN_RETRIEVE_TEXT = 3;
    private static final int PDU_COLUMN_RETRIEVE_TEXT_CHARSET = 26;
    private static final int PDU_COLUMN_STATUS = 20;
    private static final int PDU_COLUMN_SUBJECT = 4;
    private static final int PDU_COLUMN_SUBJECT_CHARSET = 25;
    private static final int PDU_COLUMN_THREAD_ID = 2;
    private static final int PDU_COLUMN_TRANSACTION_ID = 10;
    private static final String[] PDU_PROJECTION = new String[]{"_id", "msg_box", "thread_id", "retr_txt", "sub", "ct_l", "ct_t", "m_cls", "m_id", "resp_txt", "tr_id", "ct_cls", "d_rpt", "m_type", "v", "pri", "rr", "read_status", "rpt_a", "retr_st", "st", Calls.DATE, "d_tm", "exp", "m_size", "sub_cs", "retr_txt_cs"};
    public static final int PROC_STATUS_COMPLETED = 3;
    public static final int PROC_STATUS_PERMANENTLY_FAILURE = 2;
    public static final int PROC_STATUS_TRANSIENT_FAILURE = 1;
    private static final String TAG = "PduPersister";
    public static final String TEMPORARY_DRM_OBJECT_URI = "content://mms/9223372036854775807/part";
    private static final HashMap<Integer, Integer> TEXT_STRING_COLUMN_INDEX_MAP = new HashMap();
    private static final HashMap<Integer, String> TEXT_STRING_COLUMN_NAME_MAP = new HashMap();
    private static PduPersister sPersister;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final DrmManagerClient mDrmManagerClient;
    private final TelephonyManager mTelephonyManager;

    static {
        MESSAGE_BOX_MAP.put(Inbox.CONTENT_URI, Integer.valueOf(1));
        MESSAGE_BOX_MAP.put(Sent.CONTENT_URI, Integer.valueOf(2));
        MESSAGE_BOX_MAP.put(Draft.CONTENT_URI, Integer.valueOf(3));
        MESSAGE_BOX_MAP.put(Outbox.CONTENT_URI, Integer.valueOf(4));
        CHARSET_COLUMN_INDEX_MAP.put(Integer.valueOf(150), Integer.valueOf(25));
        CHARSET_COLUMN_INDEX_MAP.put(Integer.valueOf(154), Integer.valueOf(26));
        CHARSET_COLUMN_NAME_MAP.put(Integer.valueOf(150), "sub_cs");
        CHARSET_COLUMN_NAME_MAP.put(Integer.valueOf(154), "retr_txt_cs");
        ENCODED_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(154), Integer.valueOf(3));
        ENCODED_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(150), Integer.valueOf(4));
        ENCODED_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(154), "retr_txt");
        ENCODED_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(150), "sub");
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(131), Integer.valueOf(5));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(132), Integer.valueOf(6));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(138), Integer.valueOf(7));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(139), Integer.valueOf(8));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(147), Integer.valueOf(9));
        TEXT_STRING_COLUMN_INDEX_MAP.put(Integer.valueOf(152), Integer.valueOf(10));
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(131), "ct_l");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(132), "ct_t");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(138), "m_cls");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(139), "m_id");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(147), "resp_txt");
        TEXT_STRING_COLUMN_NAME_MAP.put(Integer.valueOf(152), "tr_id");
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), Integer.valueOf(11));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(134), Integer.valueOf(12));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(140), Integer.valueOf(13));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(141), Integer.valueOf(14));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(143), Integer.valueOf(15));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(144), Integer.valueOf(16));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(155), Integer.valueOf(17));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(145), Integer.valueOf(18));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(153), Integer.valueOf(19));
        OCTET_COLUMN_INDEX_MAP.put(Integer.valueOf(149), Integer.valueOf(20));
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(PduHeaders.CONTENT_CLASS), "ct_cls");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(134), "d_rpt");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(140), "m_type");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(141), "v");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(143), "pri");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(144), "rr");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(155), "read_status");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(145), "rpt_a");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(153), "retr_st");
        OCTET_COLUMN_NAME_MAP.put(Integer.valueOf(149), "st");
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(133), Integer.valueOf(21));
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(135), Integer.valueOf(22));
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(136), Integer.valueOf(23));
        LONG_COLUMN_INDEX_MAP.put(Integer.valueOf(142), Integer.valueOf(24));
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(133), Calls.DATE);
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(135), "d_tm");
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(136), "exp");
        LONG_COLUMN_NAME_MAP.put(Integer.valueOf(142), "m_size");
    }

    private PduPersister(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mDrmManagerClient = new DrmManagerClient(context);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    public static PduPersister getPduPersister(Context context) {
        if (sPersister == null) {
            sPersister = new PduPersister(context);
        } else if (!context.equals(sPersister.mContext)) {
            sPersister.release();
            sPersister = new PduPersister(context);
        }
        return sPersister;
    }

    private void setEncodedStringValueToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s != null && s.length() > 0) {
            headers.setEncodedStringValue(new EncodedStringValue(c.getInt(((Integer) CHARSET_COLUMN_INDEX_MAP.get(Integer.valueOf(mapColumn))).intValue()), getBytes(s)), mapColumn);
        }
    }

    private void setTextStringToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        String s = c.getString(columnIndex);
        if (s != null) {
            headers.setTextString(getBytes(s), mapColumn);
        }
    }

    private void setOctetToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) throws InvalidHeaderValueException {
        if (!c.isNull(columnIndex)) {
            headers.setOctet(c.getInt(columnIndex), mapColumn);
        }
    }

    private void setLongToHeaders(Cursor c, int columnIndex, PduHeaders headers, int mapColumn) {
        if (!c.isNull(columnIndex)) {
            headers.setLongInteger(c.getLong(columnIndex), mapColumn);
        }
    }

    private Integer getIntegerFromPartColumn(Cursor c, int columnIndex) {
        if (c.isNull(columnIndex)) {
            return null;
        }
        return Integer.valueOf(c.getInt(columnIndex));
    }

    private byte[] getByteArrayFromPartColumn(Cursor c, int columnIndex) {
        if (c.isNull(columnIndex)) {
            return null;
        }
        return getBytes(c.getString(columnIndex));
    }

    private PduPart[] loadParts(long msgId) throws MmsException {
        Cursor c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/part"), PART_PROJECTION, null, null, null);
        if (c != null) {
            try {
                if (c.getCount() != 0) {
                    int partIdx = 0;
                    PduPart[] parts = new PduPart[c.getCount()];
                    while (true) {
                        int partIdx2 = partIdx;
                        if (c.moveToNext()) {
                            PduPart part = new PduPart();
                            Integer charset = getIntegerFromPartColumn(c, 1);
                            if (charset != null) {
                                part.setCharset(charset.intValue());
                            }
                            byte[] contentDisposition = getByteArrayFromPartColumn(c, 2);
                            if (contentDisposition != null) {
                                part.setContentDisposition(contentDisposition);
                            }
                            byte[] contentId = getByteArrayFromPartColumn(c, 3);
                            if (contentId != null) {
                                part.setContentId(contentId);
                            }
                            byte[] contentLocation = getByteArrayFromPartColumn(c, 4);
                            if (contentLocation != null) {
                                part.setContentLocation(contentLocation);
                            }
                            byte[] contentType = getByteArrayFromPartColumn(c, 5);
                            if (contentType != null) {
                                part.setContentType(contentType);
                                byte[] fileName = getByteArrayFromPartColumn(c, 6);
                                if (fileName != null) {
                                    part.setFilename(fileName);
                                }
                                byte[] name = getByteArrayFromPartColumn(c, 7);
                                if (name != null) {
                                    part.setName(name);
                                }
                                Uri partURI = Uri.parse("content://mms/part/" + c.getLong(0));
                                part.setDataUri(partURI);
                                String type = toIsoString(contentType);
                                if (!(ContentType.isImageType(type) || (ContentType.isAudioType(type) ^ 1) == 0 || (ContentType.isVideoType(type) ^ 1) == 0)) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    InputStream inputStream = null;
                                    if (ContentType.TEXT_PLAIN.equals(type) || ContentType.APP_SMIL.equals(type) || ContentType.TEXT_HTML.equals(type)) {
                                        byte[] blob = getBlob(getCharsetName(charset) != null, charset, c.getString(8));
                                        baos.write(blob, 0, blob.length);
                                    } else {
                                        try {
                                            inputStream = this.mContentResolver.openInputStream(partURI);
                                            byte[] buffer = new byte[256];
                                            for (int len = inputStream.read(buffer); len >= 0; len = inputStream.read(buffer)) {
                                                baos.write(buffer, 0, len);
                                            }
                                            if (inputStream != null) {
                                                inputStream.close();
                                            }
                                        } catch (Throwable e) {
                                            Log.e(TAG, "Failed to load part data", e);
                                            c.close();
                                            throw new MmsException(e);
                                        } catch (Throwable th) {
                                            if (inputStream != null) {
                                                try {
                                                    inputStream.close();
                                                } catch (Throwable e2) {
                                                    Log.e(TAG, "Failed to close stream", e2);
                                                }
                                            }
                                        }
                                    }
                                    part.setData(baos.toByteArray());
                                }
                                partIdx = partIdx2 + 1;
                                parts[partIdx2] = part;
                            } else {
                                throw new MmsException("Content-Type must be set.");
                            }
                        }
                        if (c != null) {
                            c.close();
                        }
                        return parts;
                    }
                }
            } catch (Throwable e22) {
                Log.e(TAG, "Failed to close stream", e22);
            } catch (Throwable th2) {
                if (c != null) {
                    c.close();
                }
            }
        }
        if (c != null) {
            c.close();
        }
        return null;
    }

    private String getCharsetName(Integer charset) {
        if (charset == null || charset.intValue() == 0) {
            return null;
        }
        String charsetName = null;
        try {
            charsetName = CharacterSets.getMimeName(charset.intValue());
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "charset " + charset + " is not supported");
        }
        return charsetName;
    }

    private byte[] getBlob(boolean hasCharset, Integer charset, String text) {
        if (hasCharset) {
            int intValue = charset.intValue();
            if (text == null) {
                text = SpnOverride.MVNO_TYPE_NONE;
            }
            return new EncodedStringValue(intValue, text).getTextString();
        }
        if (text == null) {
            text = SpnOverride.MVNO_TYPE_NONE;
        }
        return new EncodedStringValue(text).getTextString();
    }

    private void loadAddress(long msgId, PduHeaders headers) {
        Cursor c = SqliteWrapper.query(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), new String[]{"address", "charset", Calls.TYPE}, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    String addr = c.getString(0);
                    if (!TextUtils.isEmpty(addr)) {
                        int addrType = c.getInt(2);
                        switch (addrType) {
                            case 129:
                            case 130:
                            case 151:
                                headers.appendEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                                break;
                            case 137:
                                headers.setEncodedStringValue(new EncodedStringValue(c.getInt(1), getBytes(addr)), addrType);
                                break;
                            default:
                                Log.e(TAG, "Unknown address type: " + addrType);
                                break;
                        }
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    public com.google.android.mms.pdu.GenericPdu load(android.net.Uri r31) throws com.google.android.mms.MmsException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r26_1 'pdu' com.google.android.mms.pdu.GenericPdu) in PHI: PHI: (r26_2 'pdu' com.google.android.mms.pdu.GenericPdu) = (r26_1 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_3 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_4 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_5 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_6 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_7 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_8 'pdu' com.google.android.mms.pdu.GenericPdu), (r26_9 'pdu' com.google.android.mms.pdu.GenericPdu) binds: {(r26_1 'pdu' com.google.android.mms.pdu.GenericPdu)=B:97:0x0207, (r26_3 'pdu' com.google.android.mms.pdu.GenericPdu)=B:113:?, (r26_4 'pdu' com.google.android.mms.pdu.GenericPdu)=B:114:0x0237, (r26_5 'pdu' com.google.android.mms.pdu.GenericPdu)=B:115:0x0241, (r26_6 'pdu' com.google.android.mms.pdu.GenericPdu)=B:116:0x024b, (r26_7 'pdu' com.google.android.mms.pdu.GenericPdu)=B:117:0x0255, (r26_8 'pdu' com.google.android.mms.pdu.GenericPdu)=B:118:0x025f, (r26_9 'pdu' com.google.android.mms.pdu.GenericPdu)=B:119:0x0269}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r30 = this;
        r26 = 0;
        r13 = 0;
        r20 = 0;
        r28 = -1;
        r5 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x004c }
        monitor-enter(r5);	 Catch:{ all -> 0x004c }
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x0049 }
        r0 = r31;	 Catch:{ all -> 0x0049 }
        r4 = r4.isUpdating(r0);	 Catch:{ all -> 0x0049 }
        if (r4 == 0) goto L_0x0062;
    L_0x0014:
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ InterruptedException -> 0x003e }
        r4.wait();	 Catch:{ InterruptedException -> 0x003e }
    L_0x0019:
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x0049 }
        r0 = r31;	 Catch:{ all -> 0x0049 }
        r4 = r4.get(r0);	 Catch:{ all -> 0x0049 }
        r0 = r4;	 Catch:{ all -> 0x0049 }
        r0 = (com.google.android.mms.util.PduCacheEntry) r0;	 Catch:{ all -> 0x0049 }
        r13 = r0;	 Catch:{ all -> 0x0049 }
        if (r13 == 0) goto L_0x0062;	 Catch:{ all -> 0x0049 }
    L_0x0027:
        r4 = r13.getPdu();	 Catch:{ all -> 0x0049 }
        monitor-exit(r5);	 Catch:{ all -> 0x004c }
        r5 = PDU_CACHE_INSTANCE;
        monitor-enter(r5);
        r6 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x005f }
        r7 = 0;	 Catch:{ all -> 0x005f }
        r0 = r31;	 Catch:{ all -> 0x005f }
        r6.setUpdating(r0, r7);	 Catch:{ all -> 0x005f }
        r6 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x005f }
        r6.notifyAll();	 Catch:{ all -> 0x005f }
        monitor-exit(r5);
        return r4;
    L_0x003e:
        r15 = move-exception;
        r4 = "PduPersister";	 Catch:{ all -> 0x0049 }
        r6 = "load: ";	 Catch:{ all -> 0x0049 }
        android.util.Log.e(r4, r6, r15);	 Catch:{ all -> 0x0049 }
        goto L_0x0019;
    L_0x0049:
        r4 = move-exception;
    L_0x004a:
        monitor-exit(r5);	 Catch:{ all -> 0x004c }
        throw r4;	 Catch:{ all -> 0x004c }
    L_0x004c:
        r4 = move-exception;
    L_0x004d:
        r5 = PDU_CACHE_INSTANCE;
        monitor-enter(r5);
        r6 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x02b2 }
        r7 = 0;	 Catch:{ all -> 0x02b2 }
        r0 = r31;	 Catch:{ all -> 0x02b2 }
        r6.setUpdating(r0, r7);	 Catch:{ all -> 0x02b2 }
        r6 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x02b2 }
        r6.notifyAll();	 Catch:{ all -> 0x02b2 }
        monitor-exit(r5);
        throw r4;
    L_0x005f:
        r4 = move-exception;
        monitor-exit(r5);
        throw r4;
    L_0x0062:
        r14 = r13;
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x02b8 }
        r6 = 1;	 Catch:{ all -> 0x02b8 }
        r0 = r31;	 Catch:{ all -> 0x02b8 }
        r4.setUpdating(r0, r6);	 Catch:{ all -> 0x02b8 }
        monitor-exit(r5);	 Catch:{ all -> 0x00b4 }
        r0 = r30;	 Catch:{ all -> 0x00b4 }
        r4 = r0.mContext;	 Catch:{ all -> 0x00b4 }
        r0 = r30;	 Catch:{ all -> 0x00b4 }
        r5 = r0.mContentResolver;	 Catch:{ all -> 0x00b4 }
        r7 = PDU_PROJECTION;	 Catch:{ all -> 0x00b4 }
        r8 = 0;	 Catch:{ all -> 0x00b4 }
        r9 = 0;	 Catch:{ all -> 0x00b4 }
        r10 = 0;	 Catch:{ all -> 0x00b4 }
        r6 = r31;	 Catch:{ all -> 0x00b4 }
        r12 = com.google.android.mms.util.SqliteWrapper.query(r4, r5, r6, r7, r8, r9, r10);	 Catch:{ all -> 0x00b4 }
        r18 = new com.google.android.mms.pdu.PduHeaders;	 Catch:{ all -> 0x00b4 }
        r18.<init>();	 Catch:{ all -> 0x00b4 }
        r22 = android.content.ContentUris.parseId(r31);	 Catch:{ all -> 0x00b4 }
        if (r12 == 0) goto L_0x0091;
    L_0x008a:
        r4 = r12.getCount();	 Catch:{ all -> 0x00ad }
        r5 = 1;	 Catch:{ all -> 0x00ad }
        if (r4 == r5) goto L_0x00b7;	 Catch:{ all -> 0x00ad }
    L_0x0091:
        r4 = new com.google.android.mms.MmsException;	 Catch:{ all -> 0x00ad }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ad }
        r5.<init>();	 Catch:{ all -> 0x00ad }
        r6 = "Bad uri: ";	 Catch:{ all -> 0x00ad }
        r5 = r5.append(r6);	 Catch:{ all -> 0x00ad }
        r0 = r31;	 Catch:{ all -> 0x00ad }
        r5 = r5.append(r0);	 Catch:{ all -> 0x00ad }
        r5 = r5.toString();	 Catch:{ all -> 0x00ad }
        r4.<init>(r5);	 Catch:{ all -> 0x00ad }
        throw r4;	 Catch:{ all -> 0x00ad }
    L_0x00ad:
        r4 = move-exception;
        if (r12 == 0) goto L_0x00b3;
    L_0x00b0:
        r12.close();	 Catch:{ all -> 0x00b4 }
    L_0x00b3:
        throw r4;	 Catch:{ all -> 0x00b4 }
    L_0x00b4:
        r4 = move-exception;
        r13 = r14;
        goto L_0x004d;
    L_0x00b7:
        r4 = r12.moveToFirst();	 Catch:{ all -> 0x00ad }
        r4 = r4 ^ 1;	 Catch:{ all -> 0x00ad }
        if (r4 != 0) goto L_0x0091;	 Catch:{ all -> 0x00ad }
    L_0x00bf:
        r4 = 1;	 Catch:{ all -> 0x00ad }
        r20 = r12.getInt(r4);	 Catch:{ all -> 0x00ad }
        r4 = 2;	 Catch:{ all -> 0x00ad }
        r28 = r12.getLong(r4);	 Catch:{ all -> 0x00ad }
        r4 = ENCODED_STRING_COLUMN_INDEX_MAP;	 Catch:{ all -> 0x00ad }
        r27 = r4.entrySet();	 Catch:{ all -> 0x00ad }
        r17 = r27.iterator();	 Catch:{ all -> 0x00ad }
    L_0x00d3:
        r4 = r17.hasNext();	 Catch:{ all -> 0x00ad }
        if (r4 == 0) goto L_0x00fb;	 Catch:{ all -> 0x00ad }
    L_0x00d9:
        r16 = r17.next();	 Catch:{ all -> 0x00ad }
        r16 = (java.util.Map.Entry) r16;	 Catch:{ all -> 0x00ad }
        r4 = r16.getValue();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r5 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r4 = r16.getKey();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r4 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r0 = r30;	 Catch:{ all -> 0x00ad }
        r1 = r18;	 Catch:{ all -> 0x00ad }
        r0.setEncodedStringValueToHeaders(r12, r5, r1, r4);	 Catch:{ all -> 0x00ad }
        goto L_0x00d3;	 Catch:{ all -> 0x00ad }
    L_0x00fb:
        r4 = TEXT_STRING_COLUMN_INDEX_MAP;	 Catch:{ all -> 0x00ad }
        r27 = r4.entrySet();	 Catch:{ all -> 0x00ad }
        r17 = r27.iterator();	 Catch:{ all -> 0x00ad }
    L_0x0105:
        r4 = r17.hasNext();	 Catch:{ all -> 0x00ad }
        if (r4 == 0) goto L_0x012d;	 Catch:{ all -> 0x00ad }
    L_0x010b:
        r16 = r17.next();	 Catch:{ all -> 0x00ad }
        r16 = (java.util.Map.Entry) r16;	 Catch:{ all -> 0x00ad }
        r4 = r16.getValue();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r5 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r4 = r16.getKey();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r4 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r0 = r30;	 Catch:{ all -> 0x00ad }
        r1 = r18;	 Catch:{ all -> 0x00ad }
        r0.setTextStringToHeaders(r12, r5, r1, r4);	 Catch:{ all -> 0x00ad }
        goto L_0x0105;	 Catch:{ all -> 0x00ad }
    L_0x012d:
        r4 = OCTET_COLUMN_INDEX_MAP;	 Catch:{ all -> 0x00ad }
        r27 = r4.entrySet();	 Catch:{ all -> 0x00ad }
        r17 = r27.iterator();	 Catch:{ all -> 0x00ad }
    L_0x0137:
        r4 = r17.hasNext();	 Catch:{ all -> 0x00ad }
        if (r4 == 0) goto L_0x015f;	 Catch:{ all -> 0x00ad }
    L_0x013d:
        r16 = r17.next();	 Catch:{ all -> 0x00ad }
        r16 = (java.util.Map.Entry) r16;	 Catch:{ all -> 0x00ad }
        r4 = r16.getValue();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r5 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r4 = r16.getKey();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r4 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r0 = r30;	 Catch:{ all -> 0x00ad }
        r1 = r18;	 Catch:{ all -> 0x00ad }
        r0.setOctetToHeaders(r12, r5, r1, r4);	 Catch:{ all -> 0x00ad }
        goto L_0x0137;	 Catch:{ all -> 0x00ad }
    L_0x015f:
        r4 = LONG_COLUMN_INDEX_MAP;	 Catch:{ all -> 0x00ad }
        r27 = r4.entrySet();	 Catch:{ all -> 0x00ad }
        r17 = r27.iterator();	 Catch:{ all -> 0x00ad }
    L_0x0169:
        r4 = r17.hasNext();	 Catch:{ all -> 0x00ad }
        if (r4 == 0) goto L_0x0191;	 Catch:{ all -> 0x00ad }
    L_0x016f:
        r16 = r17.next();	 Catch:{ all -> 0x00ad }
        r16 = (java.util.Map.Entry) r16;	 Catch:{ all -> 0x00ad }
        r4 = r16.getValue();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r5 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r4 = r16.getKey();	 Catch:{ all -> 0x00ad }
        r4 = (java.lang.Integer) r4;	 Catch:{ all -> 0x00ad }
        r4 = r4.intValue();	 Catch:{ all -> 0x00ad }
        r0 = r30;	 Catch:{ all -> 0x00ad }
        r1 = r18;	 Catch:{ all -> 0x00ad }
        r0.setLongToHeaders(r12, r5, r1, r4);	 Catch:{ all -> 0x00ad }
        goto L_0x0169;
    L_0x0191:
        if (r12 == 0) goto L_0x0196;
    L_0x0193:
        r12.close();	 Catch:{ all -> 0x00b4 }
    L_0x0196:
        r4 = -1;	 Catch:{ all -> 0x00b4 }
        r4 = (r22 > r4 ? 1 : (r22 == r4 ? 0 : -1));	 Catch:{ all -> 0x00b4 }
        if (r4 != 0) goto L_0x01a5;	 Catch:{ all -> 0x00b4 }
    L_0x019c:
        r4 = new com.google.android.mms.MmsException;	 Catch:{ all -> 0x00b4 }
        r5 = "Error! ID of the message: -1.";	 Catch:{ all -> 0x00b4 }
        r4.<init>(r5);	 Catch:{ all -> 0x00b4 }
        throw r4;	 Catch:{ all -> 0x00b4 }
    L_0x01a5:
        r0 = r30;	 Catch:{ all -> 0x00b4 }
        r1 = r22;	 Catch:{ all -> 0x00b4 }
        r3 = r18;	 Catch:{ all -> 0x00b4 }
        r0.loadAddress(r1, r3);	 Catch:{ all -> 0x00b4 }
        r4 = 140; // 0x8c float:1.96E-43 double:6.9E-322;	 Catch:{ all -> 0x00b4 }
        r0 = r18;	 Catch:{ all -> 0x00b4 }
        r21 = r0.getOctet(r4);	 Catch:{ all -> 0x00b4 }
        r11 = new com.google.android.mms.pdu.PduBody;	 Catch:{ all -> 0x00b4 }
        r11.<init>();	 Catch:{ all -> 0x00b4 }
        r4 = 132; // 0x84 float:1.85E-43 double:6.5E-322;	 Catch:{ all -> 0x00b4 }
        r0 = r21;	 Catch:{ all -> 0x00b4 }
        if (r0 == r4) goto L_0x01c7;	 Catch:{ all -> 0x00b4 }
    L_0x01c1:
        r4 = 128; // 0x80 float:1.794E-43 double:6.32E-322;	 Catch:{ all -> 0x00b4 }
        r0 = r21;	 Catch:{ all -> 0x00b4 }
        if (r0 != r4) goto L_0x01e6;	 Catch:{ all -> 0x00b4 }
    L_0x01c7:
        r0 = r30;	 Catch:{ all -> 0x00b4 }
        r1 = r22;	 Catch:{ all -> 0x00b4 }
        r24 = r0.loadParts(r1);	 Catch:{ all -> 0x00b4 }
        if (r24 == 0) goto L_0x01e6;	 Catch:{ all -> 0x00b4 }
    L_0x01d1:
        r0 = r24;	 Catch:{ all -> 0x00b4 }
        r0 = r0.length;	 Catch:{ all -> 0x00b4 }
        r25 = r0;	 Catch:{ all -> 0x00b4 }
        r19 = 0;	 Catch:{ all -> 0x00b4 }
    L_0x01d8:
        r0 = r19;	 Catch:{ all -> 0x00b4 }
        r1 = r25;	 Catch:{ all -> 0x00b4 }
        if (r0 >= r1) goto L_0x01e6;	 Catch:{ all -> 0x00b4 }
    L_0x01de:
        r4 = r24[r19];	 Catch:{ all -> 0x00b4 }
        r11.addPart(r4);	 Catch:{ all -> 0x00b4 }
        r19 = r19 + 1;	 Catch:{ all -> 0x00b4 }
        goto L_0x01d8;	 Catch:{ all -> 0x00b4 }
    L_0x01e6:
        switch(r21) {
            case 128: goto L_0x024b;
            case 129: goto L_0x0273;
            case 130: goto L_0x0207;
            case 131: goto L_0x025f;
            case 132: goto L_0x0241;
            case 133: goto L_0x0255;
            case 134: goto L_0x022d;
            case 135: goto L_0x0269;
            case 136: goto L_0x0237;
            case 137: goto L_0x0273;
            case 138: goto L_0x0273;
            case 139: goto L_0x0273;
            case 140: goto L_0x0273;
            case 141: goto L_0x0273;
            case 142: goto L_0x0273;
            case 143: goto L_0x0273;
            case 144: goto L_0x0273;
            case 145: goto L_0x0273;
            case 146: goto L_0x0273;
            case 147: goto L_0x0273;
            case 148: goto L_0x0273;
            case 149: goto L_0x0273;
            case 150: goto L_0x0273;
            case 151: goto L_0x0273;
            default: goto L_0x01e9;
        };	 Catch:{ all -> 0x00b4 }
    L_0x01e9:
        r4 = new com.google.android.mms.MmsException;	 Catch:{ all -> 0x00b4 }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b4 }
        r5.<init>();	 Catch:{ all -> 0x00b4 }
        r6 = "Unrecognized PDU type: ";	 Catch:{ all -> 0x00b4 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x00b4 }
        r6 = java.lang.Integer.toHexString(r21);	 Catch:{ all -> 0x00b4 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x00b4 }
        r5 = r5.toString();	 Catch:{ all -> 0x00b4 }
        r4.<init>(r5);	 Catch:{ all -> 0x00b4 }
        throw r4;	 Catch:{ all -> 0x00b4 }
    L_0x0207:
        r26 = new com.google.android.mms.pdu.NotificationInd;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1);	 Catch:{ all -> 0x00b4 }
    L_0x0210:
        r5 = PDU_CACHE_INSTANCE;
        monitor-enter(r5);
        if (r26 == 0) goto L_0x02bc;
    L_0x0215:
        r4 = -assertionsDisabled;	 Catch:{ all -> 0x0229 }
        if (r4 != 0) goto L_0x0291;	 Catch:{ all -> 0x0229 }
    L_0x0219:
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x0229 }
        r0 = r31;	 Catch:{ all -> 0x0229 }
        r4 = r4.get(r0);	 Catch:{ all -> 0x0229 }
        if (r4 == 0) goto L_0x0291;	 Catch:{ all -> 0x0229 }
    L_0x0223:
        r4 = new java.lang.AssertionError;	 Catch:{ all -> 0x0229 }
        r4.<init>();	 Catch:{ all -> 0x0229 }
        throw r4;	 Catch:{ all -> 0x0229 }
    L_0x0229:
        r4 = move-exception;
        r13 = r14;
    L_0x022b:
        monitor-exit(r5);
        throw r4;
    L_0x022d:
        r26 = new com.google.android.mms.pdu.DeliveryInd;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x0237:
        r26 = new com.google.android.mms.pdu.ReadOrigInd;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x0241:
        r26 = new com.google.android.mms.pdu.RetrieveConf;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1, r11);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x024b:
        r26 = new com.google.android.mms.pdu.SendReq;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1, r11);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x0255:
        r26 = new com.google.android.mms.pdu.AcknowledgeInd;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x025f:
        r26 = new com.google.android.mms.pdu.NotifyRespInd;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x0269:
        r26 = new com.google.android.mms.pdu.ReadRecInd;	 Catch:{ all -> 0x00b4 }
        r0 = r26;	 Catch:{ all -> 0x00b4 }
        r1 = r18;	 Catch:{ all -> 0x00b4 }
        r0.<init>(r1);	 Catch:{ all -> 0x00b4 }
        goto L_0x0210;	 Catch:{ all -> 0x00b4 }
    L_0x0273:
        r4 = new com.google.android.mms.MmsException;	 Catch:{ all -> 0x00b4 }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b4 }
        r5.<init>();	 Catch:{ all -> 0x00b4 }
        r6 = "Unsupported PDU type: ";	 Catch:{ all -> 0x00b4 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x00b4 }
        r6 = java.lang.Integer.toHexString(r21);	 Catch:{ all -> 0x00b4 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x00b4 }
        r5 = r5.toString();	 Catch:{ all -> 0x00b4 }
        r4.<init>(r5);	 Catch:{ all -> 0x00b4 }
        throw r4;	 Catch:{ all -> 0x00b4 }
    L_0x0291:
        r13 = new com.google.android.mms.util.PduCacheEntry;	 Catch:{ all -> 0x0229 }
        r0 = r26;	 Catch:{ all -> 0x0229 }
        r1 = r20;	 Catch:{ all -> 0x0229 }
        r2 = r28;	 Catch:{ all -> 0x0229 }
        r13.<init>(r0, r1, r2);	 Catch:{ all -> 0x0229 }
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x02b5 }
        r0 = r31;	 Catch:{ all -> 0x02b5 }
        r4.put(r0, r13);	 Catch:{ all -> 0x02b5 }
    L_0x02a3:
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x02b5 }
        r6 = 0;	 Catch:{ all -> 0x02b5 }
        r0 = r31;	 Catch:{ all -> 0x02b5 }
        r4.setUpdating(r0, r6);	 Catch:{ all -> 0x02b5 }
        r4 = PDU_CACHE_INSTANCE;	 Catch:{ all -> 0x02b5 }
        r4.notifyAll();	 Catch:{ all -> 0x02b5 }
        monitor-exit(r5);
        return r26;
    L_0x02b2:
        r4 = move-exception;
        monitor-exit(r5);
        throw r4;
    L_0x02b5:
        r4 = move-exception;
        goto L_0x022b;
    L_0x02b8:
        r4 = move-exception;
        r13 = r14;
        goto L_0x004a;
    L_0x02bc:
        r13 = r14;
        goto L_0x02a3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.mms.pdu.PduPersister.load(android.net.Uri):com.google.android.mms.pdu.GenericPdu");
    }

    private void persistAddress(long msgId, int type, EncodedStringValue[] array) {
        ContentValues values = new ContentValues(3);
        for (EncodedStringValue addr : array) {
            values.clear();
            values.put("address", toIsoString(addr.getTextString()));
            values.put("charset", Integer.valueOf(addr.getCharacterSet()));
            values.put(Calls.TYPE, Integer.valueOf(type));
            SqliteWrapper.insert(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), values);
        }
    }

    private static String getPartContentType(PduPart part) {
        return part.getContentType() == null ? null : toIsoString(part.getContentType());
    }

    public Uri persistPart(PduPart part, long msgId, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        Uri uri = Uri.parse("content://mms/" + msgId + "/part");
        ContentValues values = new ContentValues(8);
        int charset = part.getCharset();
        if (charset != 0) {
            values.put("chset", Integer.valueOf(charset));
        }
        String contentType = getPartContentType(part);
        if (contentType != null) {
            if (ContentType.IMAGE_JPG.equals(contentType)) {
                contentType = ContentType.IMAGE_JPEG;
            }
            values.put("ct", contentType);
            if (ContentType.APP_SMIL.equals(contentType)) {
                values.put("seq", Integer.valueOf(-1));
            }
            if (part.getFilename() != null) {
                values.put("fn", new String(part.getFilename()));
            }
            if (part.getName() != null) {
                values.put(Calls.CACHED_NAME, new String(part.getName()));
            }
            if (part.getContentDisposition() != null) {
                values.put("cd", toIsoString(part.getContentDisposition()));
            }
            if (part.getContentId() != null) {
                values.put("cid", toIsoString(part.getContentId()));
            }
            if (part.getContentLocation() != null) {
                values.put("cl", toIsoString(part.getContentLocation()));
            }
            Uri res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
            if (res == null) {
                throw new MmsException("Failed to persist part, return null.");
            }
            persistData(part, res, contentType, preOpenedFiles);
            part.setDataUri(res);
            return res;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    private EncodedStringValue getEncodedStringValue(int charset, byte[] data) {
        if (getCharsetName(Integer.valueOf(charset)) != null) {
            return new EncodedStringValue(charset, data);
        }
        return new EncodedStringValue(data);
    }

    private void persistData(PduPart part, Uri uri, String contentType, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        OutputStream os = null;
        InputStream is = null;
        DrmConvertSession drmConvertSession = null;
        String str = null;
        File file;
        try {
            byte[] data = part.getData();
            if (ContentType.TEXT_PLAIN.equals(contentType) || ContentType.APP_SMIL.equals(contentType) || ContentType.TEXT_HTML.equals(contentType)) {
                ContentValues cv = new ContentValues();
                if (data == null) {
                    cv.put("text", new EncodedStringValue(new String(SpnOverride.MVNO_TYPE_NONE).getBytes("utf-8")).getString());
                    Log.w(TAG, "Part data is null. contentType: " + contentType);
                } else {
                    int charset = part.getCharset();
                    if (charset == 3 && ContentType.APP_SMIL.equals(contentType)) {
                        charset = 106;
                    }
                    EncodedStringValue ev = getEncodedStringValue(charset, data);
                    cv.put("chset", Integer.valueOf(ev.getCharacterSet()));
                    cv.put("text", ev.getString());
                }
                if (this.mContentResolver.update(uri, cv, null, null) != 1) {
                    throw new MmsException("unable to update " + uri.toString());
                }
            }
            boolean isDrm = DownloadDrmHelper.isDrmConvertNeeded(contentType);
            if (isDrm) {
                if (uri != null) {
                    try {
                        str = convertUriToPath(this.mContext, uri);
                        if (new File(str).length() > 0) {
                            return;
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "Can't get file info for: " + part.getDataUri(), e);
                    }
                }
                drmConvertSession = DrmConvertSession.open(this.mContext, contentType);
                if (drmConvertSession == null) {
                    throw new MmsException("Mimetype " + contentType + " can not be converted.");
                }
            }
            os = this.mContentResolver.openOutputStream(uri);
            Uri dataUri;
            byte[] convertedData;
            if (data == null) {
                dataUri = part.getDataUri();
                if (dataUri != null && !dataUri.equals(uri)) {
                    if (preOpenedFiles != null) {
                        if (preOpenedFiles.containsKey(dataUri)) {
                            is = (InputStream) preOpenedFiles.get(dataUri);
                        }
                    }
                    if (is == null) {
                        is = this.mContentResolver.openInputStream(dataUri);
                    }
                    byte[] buffer = new byte[RadioAccessFamily.EHRPD];
                    while (true) {
                        int len = is.read(buffer);
                        if (len == -1) {
                            break;
                        } else if (isDrm) {
                            convertedData = drmConvertSession.convert(buffer, len);
                            if (convertedData != null) {
                                os.write(convertedData, 0, convertedData.length);
                            } else {
                                throw new MmsException("Error converting drm data.");
                            }
                        } else {
                            os.write(buffer, 0, len);
                        }
                    }
                } else {
                    Log.w(TAG, "Can't find data for this part.");
                    if (os != null) {
                        try {
                            os.close();
                        } catch (Throwable e2) {
                            Log.e(TAG, "IOException while closing: " + os, e2);
                        }
                    }
                    if (drmConvertSession != null) {
                        drmConvertSession.close(str);
                        file = new File(str);
                        SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + file.getName()), new ContentValues(0), null, null);
                    }
                    return;
                }
            } else if (isDrm) {
                dataUri = uri;
                convertedData = drmConvertSession.convert(data, data.length);
                if (convertedData != null) {
                    os.write(convertedData, 0, convertedData.length);
                } else {
                    throw new MmsException("Error converting drm data.");
                }
            } else {
                os.write(data);
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable e22) {
                    Log.e(TAG, "IOException while closing: " + os, e22);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e222) {
                    Log.e(TAG, "IOException while closing: " + is, e222);
                }
            }
            if (drmConvertSession != null) {
                drmConvertSession.close(str);
                file = new File(str);
                SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + file.getName()), new ContentValues(0), null, null);
            }
        } catch (Throwable e3) {
            Log.e(TAG, "Failed to open Input/Output stream.", e3);
            throw new MmsException(e3);
        } catch (Throwable e2222) {
            Log.e(TAG, "Failed to read/write data.", e2222);
            throw new MmsException(e2222);
        } catch (Throwable th) {
            Throwable th2 = th;
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable e22222) {
                    Log.e(TAG, "IOException while closing: " + os, e22222);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e222222) {
                    Log.e(TAG, "IOException while closing: " + is, e222222);
                }
            }
            if (drmConvertSession != null) {
                drmConvertSession.close(str);
                file = new File(str);
                SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/resetFilePerm/" + file.getName()), new ContentValues(0), null, null);
            }
        }
    }

    public static String convertUriToPath(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(SpnOverride.MVNO_TYPE_NONE) || scheme.equals("file")) {
            return uri.getPath();
        }
        if (scheme.equals("http")) {
            return uri.toString();
        }
        if (scheme.equals("content")) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (!(cursor == null || cursor.getCount() == 0)) {
                    if ((cursor.moveToFirst() ^ 1) == 0) {
                        String path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                        if (cursor == null) {
                            return path;
                        }
                        cursor.close();
                        return path;
                    }
                }
                throw new IllegalArgumentException("Given Uri could not be found in media store");
            } catch (SQLiteException e) {
                throw new IllegalArgumentException("Given Uri is not formatted in a way so that it can be found in media store.");
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            throw new IllegalArgumentException("Given Uri scheme is not supported");
        }
    }

    private void updateAddress(long msgId, int type, EncodedStringValue[] array) {
        SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + msgId + "/addr"), "type=" + type, null);
        persistAddress(msgId, type, array);
    }

    public void updateHeaders(Uri uri, SendReq sendReq) {
        synchronized (PDU_CACHE_INSTANCE) {
            if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                try {
                    PDU_CACHE_INSTANCE.wait();
                } catch (Throwable e) {
                    Log.e(TAG, "updateHeaders: ", e);
                }
            }
        }
        PDU_CACHE_INSTANCE.purge(uri);
        ContentValues values = new ContentValues(10);
        byte[] contentType = sendReq.getContentType();
        if (contentType != null) {
            values.put("ct_t", toIsoString(contentType));
        }
        long date = sendReq.getDate();
        if (date != -1) {
            values.put(Calls.DATE, Long.valueOf(date));
        }
        int deliveryReport = sendReq.getDeliveryReport();
        if (deliveryReport != 0) {
            values.put("d_rpt", Integer.valueOf(deliveryReport));
        }
        long expiry = sendReq.getExpiry();
        if (expiry != -1) {
            values.put("exp", Long.valueOf(expiry));
        }
        byte[] msgClass = sendReq.getMessageClass();
        if (msgClass != null) {
            values.put("m_cls", toIsoString(msgClass));
        }
        int priority = sendReq.getPriority();
        if (priority != 0) {
            values.put("pri", Integer.valueOf(priority));
        }
        int readReport = sendReq.getReadReport();
        if (readReport != 0) {
            values.put("rr", Integer.valueOf(readReport));
        }
        byte[] transId = sendReq.getTransactionId();
        if (transId != null) {
            values.put("tr_id", toIsoString(transId));
        }
        EncodedStringValue subject = sendReq.getSubject();
        if (subject != null) {
            values.put("sub", toIsoString(subject.getTextString()));
            values.put("sub_cs", Integer.valueOf(subject.getCharacterSet()));
        } else {
            values.put("sub", SpnOverride.MVNO_TYPE_NONE);
        }
        long messageSize = sendReq.getMessageSize();
        if (messageSize > 0) {
            values.put("m_size", Long.valueOf(messageSize));
        }
        PduHeaders headers = sendReq.getPduHeaders();
        HashSet<String> recipients = new HashSet();
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == 137) {
                if (headers.getEncodedStringValue(addrType) != null) {
                    array = new EncodedStringValue[]{headers.getEncodedStringValue(addrType)};
                }
            } else {
                array = headers.getEncodedStringValues(addrType);
            }
            if (array != null) {
                updateAddress(ContentUris.parseId(uri), addrType, array);
                if (addrType == 151) {
                    for (EncodedStringValue v : array) {
                        if (v != null) {
                            recipients.add(v.getString());
                        }
                    }
                }
            }
        }
        if (!recipients.isEmpty()) {
            values.put("thread_id", Long.valueOf(Threads.getOrCreateThreadId(this.mContext, recipients)));
        }
        SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
        return;
    }

    private void updatePart(Uri uri, PduPart part, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        ContentValues values = new ContentValues(7);
        int charset = part.getCharset();
        if (charset != 0) {
            values.put("chset", Integer.valueOf(charset));
        }
        if (part.getContentType() != null) {
            String contentType = toIsoString(part.getContentType());
            values.put("ct", contentType);
            if (part.getFilename() != null) {
                values.put("fn", new String(part.getFilename()));
            }
            if (part.getName() != null) {
                values.put(Calls.CACHED_NAME, new String(part.getName()));
            }
            if (part.getContentDisposition() != null) {
                values.put("cd", toIsoString(part.getContentDisposition()));
            }
            if (part.getContentId() != null) {
                values.put("cid", toIsoString(part.getContentId()));
            }
            if (part.getContentLocation() != null) {
                values.put("cl", toIsoString(part.getContentLocation()));
            }
            SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
            if (part.getData() != null || (uri.equals(part.getDataUri()) ^ 1) != 0) {
                persistData(part, uri, contentType, preOpenedFiles);
                return;
            }
            return;
        }
        throw new MmsException("MIME type of the part must be set.");
    }

    public void updateParts(Uri uri, PduBody body, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        PduPart pduPart;
        try {
            PduPart part;
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "updateParts: ", e);
                    }
                    PduCacheEntry cacheEntry = (PduCacheEntry) PDU_CACHE_INSTANCE.get(uri);
                    if (cacheEntry != null) {
                        ((MultimediaMessagePdu) cacheEntry.getPdu()).setBody(body);
                    }
                }
                PDU_CACHE_INSTANCE.setUpdating(uri, true);
            }
            ArrayList<PduPart> toBeCreated = new ArrayList();
            HashMap<Uri, PduPart> toBeUpdated = new HashMap();
            int partsNum = body.getPartsNum();
            StringBuilder filter = new StringBuilder().append('(');
            for (int i = 0; i < partsNum; i++) {
                part = body.getPart(i);
                Uri partUri = part.getDataUri();
                if (partUri == null || TextUtils.isEmpty(partUri.getAuthority()) || (partUri.getAuthority().startsWith("mms") ^ 1) != 0) {
                    toBeCreated.add(part);
                } else {
                    toBeUpdated.put(partUri, part);
                    if (filter.length() > 1) {
                        filter.append(" AND ");
                    }
                    filter.append("_id");
                    filter.append("!=");
                    DatabaseUtils.appendEscapedSQLString(filter, partUri.getLastPathSegment());
                }
            }
            filter.append(')');
            long msgId = ContentUris.parseId(uri);
            pduPart = this.mContext;
            SqliteWrapper.delete(pduPart, this.mContentResolver, Uri.parse(Mms.CONTENT_URI + "/" + msgId + "/part"), filter.length() > 2 ? filter.toString() : null, null);
            for (PduPart part2 : toBeCreated) {
                persistPart(part2, msgId, preOpenedFiles);
            }
            for (Entry<Uri, PduPart> e2 : toBeUpdated.entrySet()) {
                pduPart = (PduPart) e2.getValue();
                updatePart((Uri) e2.getKey(), pduPart, preOpenedFiles);
            }
            PDU_CACHE_INSTANCE.setUpdating(uri, false);
            PDU_CACHE_INSTANCE.notifyAll();
            return;
        } finally {
            pduPart = PDU_CACHE_INSTANCE;
            synchronized (pduPart) {
                PDU_CACHE_INSTANCE.setUpdating(uri, false);
                PDU_CACHE_INSTANCE.notifyAll();
            }
        }
    }

    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, boolean groupMmsEnabled, HashMap<Uri, InputStream> preOpenedFiles) throws MmsException {
        if (uri == null) {
            throw new MmsException("Uri may not be null.");
        }
        long msgId = -1;
        try {
            msgId = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
        }
        boolean existingUri = msgId != -1;
        if (existingUri || MESSAGE_BOX_MAP.get(uri) != null) {
            EncodedStringValue[] array;
            Uri res;
            synchronized (PDU_CACHE_INSTANCE) {
                if (PDU_CACHE_INSTANCE.isUpdating(uri)) {
                    try {
                        PDU_CACHE_INSTANCE.wait();
                    } catch (Throwable e2) {
                        Log.e(TAG, "persist1: ", e2);
                    }
                }
            }
            PDU_CACHE_INSTANCE.purge(uri);
            PduHeaders header = pdu.getPduHeaders();
            ContentValues values = new ContentValues();
            for (Entry<Integer, String> e3 : ENCODED_STRING_COLUMN_NAME_MAP.entrySet()) {
                int field = ((Integer) e3.getKey()).intValue();
                EncodedStringValue encodedString = header.getEncodedStringValue(field);
                if (encodedString != null) {
                    String charsetColumn = (String) CHARSET_COLUMN_NAME_MAP.get(Integer.valueOf(field));
                    values.put((String) e3.getValue(), toIsoString(encodedString.getTextString()));
                    values.put(charsetColumn, Integer.valueOf(encodedString.getCharacterSet()));
                }
            }
            for (Entry<Integer, String> e32 : TEXT_STRING_COLUMN_NAME_MAP.entrySet()) {
                byte[] text = header.getTextString(((Integer) e32.getKey()).intValue());
                if (text != null) {
                    values.put((String) e32.getValue(), toIsoString(text));
                }
            }
            for (Entry<Integer, String> e322 : OCTET_COLUMN_NAME_MAP.entrySet()) {
                int b = header.getOctet(((Integer) e322.getKey()).intValue());
                if (b != 0) {
                    values.put((String) e322.getValue(), Integer.valueOf(b));
                }
            }
            for (Entry<Integer, String> e3222 : LONG_COLUMN_NAME_MAP.entrySet()) {
                long l = header.getLongInteger(((Integer) e3222.getKey()).intValue());
                if (l != -1) {
                    values.put((String) e3222.getValue(), Long.valueOf(l));
                }
            }
            HashMap<Integer, EncodedStringValue[]> addressMap = new HashMap(ADDRESS_FIELDS.length);
            for (int addrType : ADDRESS_FIELDS) {
                array = null;
                if (addrType == 137) {
                    if (header.getEncodedStringValue(addrType) != null) {
                        array = new EncodedStringValue[]{header.getEncodedStringValue(addrType)};
                    }
                } else {
                    array = header.getEncodedStringValues(addrType);
                }
                addressMap.put(Integer.valueOf(addrType), array);
            }
            HashSet<String> recipients = new HashSet();
            int msgType = pdu.getMessageType();
            if (msgType == 130 || msgType == 132 || msgType == 128) {
                switch (msgType) {
                    case 128:
                        loadRecipients(151, recipients, addressMap, false);
                        break;
                    case 130:
                    case 132:
                        loadRecipients(137, recipients, addressMap, false);
                        if (groupMmsEnabled) {
                            loadRecipients(151, recipients, addressMap, true);
                            loadRecipients(130, recipients, addressMap, true);
                            break;
                        }
                        break;
                }
                long threadId = 0;
                if (createThreadId && (recipients.isEmpty() ^ 1) != 0) {
                    threadId = Threads.getOrCreateThreadId(this.mContext, recipients);
                }
                values.put("thread_id", Long.valueOf(threadId));
            }
            long dummyId = System.currentTimeMillis();
            boolean textOnly = true;
            int messageSize = 0;
            if (pdu instanceof MultimediaMessagePdu) {
                PduBody body = ((MultimediaMessagePdu) pdu).getBody();
                if (body != null) {
                    int partsNum = body.getPartsNum();
                    if (partsNum > 2) {
                        textOnly = false;
                    }
                    for (int i = 0; i < partsNum; i++) {
                        PduPart part = body.getPart(i);
                        messageSize += part.getDataLength();
                        persistPart(part, dummyId, preOpenedFiles);
                        String contentType = getPartContentType(part);
                        if (!(contentType == null || (ContentType.APP_SMIL.equals(contentType) ^ 1) == 0 || (ContentType.TEXT_PLAIN.equals(contentType) ^ 1) == 0)) {
                            textOnly = false;
                        }
                    }
                }
            }
            values.put("text_only", Integer.valueOf(textOnly ? 1 : 0));
            if (values.getAsInteger("m_size") == null) {
                values.put("m_size", Integer.valueOf(messageSize));
            }
            if (existingUri) {
                res = uri;
                SqliteWrapper.update(this.mContext, this.mContentResolver, uri, values, null, null);
            } else {
                res = SqliteWrapper.insert(this.mContext, this.mContentResolver, uri, values);
                if (res == null) {
                    throw new MmsException("persist() failed: return null.");
                }
                msgId = ContentUris.parseId(res);
            }
            values = new ContentValues(1);
            values.put("mid", Long.valueOf(msgId));
            SqliteWrapper.update(this.mContext, this.mContentResolver, Uri.parse("content://mms/" + dummyId + "/part"), values, null, null);
            if (!existingUri) {
                res = Uri.parse(uri + "/" + msgId);
            }
            for (int addrType2 : ADDRESS_FIELDS) {
                array = (EncodedStringValue[]) addressMap.get(Integer.valueOf(addrType2));
                if (array != null) {
                    persistAddress(msgId, addrType2, array);
                }
            }
            return res;
        }
        throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
    }

    private void loadRecipients(int addressType, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, boolean excludeMyNumber) {
        EncodedStringValue[] array = (EncodedStringValue[]) addressMap.get(Integer.valueOf(addressType));
        if (array != null) {
            if (!excludeMyNumber || array.length != 1) {
                String myNumber;
                SubscriptionManager subscriptionManager = SubscriptionManager.from(this.mContext);
                Set<String> myPhoneNumbers = new HashSet();
                if (excludeMyNumber) {
                    for (int subid : subscriptionManager.getActiveSubscriptionIdList()) {
                        myNumber = this.mTelephonyManager.getLine1Number(subid);
                        if (myNumber != null) {
                            myPhoneNumbers.add(myNumber);
                        }
                    }
                }
                for (EncodedStringValue v : array) {
                    if (v != null) {
                        String number = v.getString();
                        if (excludeMyNumber) {
                            for (String myNumber2 : myPhoneNumbers) {
                                if (!PhoneNumberUtils.compare(number, myNumber2) && (recipients.contains(number) ^ 1) != 0) {
                                    recipients.add(number);
                                    break;
                                }
                            }
                        } else if (!recipients.contains(number)) {
                            recipients.add(number);
                        }
                    }
                }
            }
        }
    }

    public Uri move(Uri from, Uri to) throws MmsException {
        long msgId = ContentUris.parseId(from);
        if (msgId == -1) {
            throw new MmsException("Error! ID of the message: -1.");
        }
        Integer msgBox = (Integer) MESSAGE_BOX_MAP.get(to);
        if (msgBox == null) {
            throw new MmsException("Bad destination, must be one of content://mms/inbox, content://mms/sent, content://mms/drafts, content://mms/outbox, content://mms/temp.");
        }
        ContentValues values = new ContentValues(1);
        values.put("msg_box", msgBox);
        SqliteWrapper.update(this.mContext, this.mContentResolver, from, values, null, null);
        return ContentUris.withAppendedId(to, msgId);
    }

    public static String toIsoString(byte[] bytes) {
        try {
            return new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return SpnOverride.MVNO_TYPE_NONE;
        }
    }

    public static byte[] getBytes(String data) {
        try {
            return data.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "ISO_8859_1 must be supported!", e);
            return new byte[0];
        }
    }

    public void release() {
        SqliteWrapper.delete(this.mContext, this.mContentResolver, Uri.parse(TEMPORARY_DRM_OBJECT_URI), null, null);
    }

    public Cursor getPendingMessages(long dueTime) {
        Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        String[] selectionArgs = new String[]{String.valueOf(10), String.valueOf(dueTime)};
        return SqliteWrapper.query(this.mContext, this.mContentResolver, uriBuilder.build(), null, "err_type < ? AND due_time <= ?", selectionArgs, "due_time");
    }
}
