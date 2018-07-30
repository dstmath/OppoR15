package com.oppo.neuron;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import java.io.File;

public class NsDbManager {
    public static final String ACTIVITY = "activity";
    public static final String APP_EVENT = "app_event";
    public static final int APP_EVENT_TINDEX = 1;
    public static final String APP_FORCE_STOPPED = "app_stopped";
    public static final int APP_FORCE_STOPPED_TINDEX = 2;
    public static final String BATT_EVENT = "batt_event";
    public static final int BATT_EVENT_TINDEX = 5;
    public static final String BATT_LEVEL = "level";
    public static final String BATT_STATUS = "status";
    public static final String BOOT_DATA = "data";
    public static final String BOOT_EVENT = "boot_event";
    public static final int BOOT_EVENT_TINDEX = 4;
    public static final String BOOT_IS_UP = "is_up";
    public static final String BSSID = "bssid";
    public static final String CHARGER = "charger";
    public static final String DATE = "date";
    public static final int HEADSET_A2DP_BT = 3;
    public static final int HEADSET_BLUETOOTH = 2;
    public static final String HEADSET_EVENT = "headset_event";
    public static final int HEADSET_EVENT_TINDEX = 7;
    public static final String HEADSET_STATUS = "status";
    public static final String HEADSET_TYPE = "type";
    public static final int HEADSET_WIRED = 1;
    public static final int NETWORK_EVENT_TINDEX = 8;
    public static final String NET_NAME = "netwok_name";
    public static final String NET_STATUS = "network_status";
    public static final String NET_SUB_NAME = "network_sub_name";
    public static final String NET_TYPE = "netwok_type";
    public static final String OTHER_AP = "other_ap";
    public static final String PKGNAME = "pkgname";
    public static final String REASON = "reason";
    public static final String SCREEN_EVENT = "screen_event";
    public static final int SCREEN_EVENT_TINDEX = 3;
    public static final String SCREEN_ON = "screen_on";
    public static final String SSID = "ssid";
    public static final String[] TABLE_LIST = new String[]{APP_EVENT, APP_FORCE_STOPPED, SCREEN_EVENT, BOOT_EVENT, BATT_EVENT, WIFI_CONNECTION_EVENT, HEADSET_EVENT};
    public static final String TAG = "NeuronSystem";
    public static final String UID = "uid";
    public static final String WIFI_CONNECTION_EVENT = "wifi_connection_event";
    public static final int WIFI_CONNECTION_EVENT_TINDEX = 6;
    public static final String WIFI_STATUS = "status";

    public static SQLiteDatabase getDatabase(String path) {
        File dir = new File(path).getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SQLiteDatabase sqLiteDatabase = null;
        try {
            return SQLiteDatabase.openOrCreateDatabase(path, null);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return sqLiteDatabase;
        }
    }

    public static SQLiteDatabase openDatabase(String path) {
        SQLiteDatabase sqLiteDatabase = null;
        try {
            return SQLiteDatabase.openDatabase(path, null, 1);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return sqLiteDatabase;
        }
    }

    public static SQLiteDatabase getDatabase(File file) {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SQLiteDatabase sqLiteDatabase = null;
        try {
            return SQLiteDatabase.openOrCreateDatabase(file, null);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return sqLiteDatabase;
        }
    }

    public static boolean initTables(SQLiteDatabase db) {
        if (db != null && createAppEventTable(db) && createAppForceStoppedTable(db) && createScreenEventTable(db) && createBootEventTable(db) && createBattEventTable(db) && createWifiConnectiontEventTable(db) && createHeadSetEventTable(db)) {
            return true;
        }
        return false;
    }

    public static boolean createAppEventTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(APP_EVENT);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s TEXT, %s TEXT, %s INTEGER", new Object[]{"pkgname", "activity", "date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean createAppForceStoppedTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(APP_FORCE_STOPPED);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s TEXT,%s TEXT,%s INTEGER", new Object[]{"pkgname", "reason", "date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean createScreenEventTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(SCREEN_EVENT);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s INTEGER,%s INTEGER,", new Object[]{"screen_on", "uid"}));
        cmd.append(String.format("%s TEXT,%s INTEGER", new Object[]{"reason", "date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean createBootEventTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(BOOT_EVENT);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s INTERGER,%s TEXT,", new Object[]{BOOT_IS_UP, BOOT_DATA}));
        cmd.append(String.format("%s INTERGER", new Object[]{"date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean createBattEventTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(BATT_EVENT);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s INTERGER,%s TEXT,%s INTERGER,", new Object[]{"status", "charger", "level"}));
        cmd.append(String.format("%s INTERGER", new Object[]{"date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean createWifiConnectiontEventTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(WIFI_CONNECTION_EVENT);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s INTERGER,%s TEXT,%s TEXT,%s TEXT,", new Object[]{"status", "ssid", "bssid", "other_ap"}));
        cmd.append(String.format("%s INTERGER", new Object[]{"date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean createHeadSetEventTable(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("CREATE TABLE IF NOT EXISTS ");
        cmd.append(HEADSET_EVENT);
        cmd.append(" (");
        cmd.append(String.format("_id INTEGER PRIMARY KEY,", new Object[0]));
        cmd.append(String.format("%s INTERGER, %s INTERGER,", new Object[]{"type", "status"}));
        cmd.append(String.format("%s INTERGER", new Object[]{"date"}));
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean insert(SQLiteDatabase db, String table, ContentValues contentValues) {
        if (db == null || contentValues == null) {
            return false;
        }
        try {
            if (db.insert(table, null, contentValues) < 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean insert(SQLiteDatabase db, int table, ContentValues contentValues) {
        if (db == null || contentValues == null) {
            return false;
        }
        String tableName = null;
        switch (table) {
            case 1:
                tableName = APP_EVENT;
                ContentValues newVal = new ContentValues();
                newVal.put("pkgname", contentValues.getAsString("pkgname"));
                newVal.put("activity", contentValues.getAsString("activity"));
                newVal.put("date", contentValues.getAsLong("date"));
                contentValues = newVal;
                break;
            case 2:
                tableName = APP_FORCE_STOPPED;
                break;
            case 3:
                tableName = SCREEN_EVENT;
                break;
            case 4:
                tableName = BOOT_EVENT;
                break;
            case 5:
                tableName = BATT_EVENT;
                break;
            case 6:
                tableName = WIFI_CONNECTION_EVENT;
                break;
            case 7:
                tableName = HEADSET_EVENT;
                break;
        }
        if (tableName == null) {
            return false;
        }
        try {
            if (db.insert(tableName, null, contentValues) < 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static int update(SQLiteDatabase db, String table, ContentValues contentValues, String whereClause) {
        if (db == null || contentValues == null) {
            return -1;
        }
        return db.update(table, contentValues, whereClause, null);
    }

    public static Cursor getAll(SQLiteDatabase db, String table) {
        Cursor cursor = null;
        try {
            cursor = db.query(table, null, null, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
            }
        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
        }
        return cursor;
    }

    public static Cursor query(SQLiteDatabase db, String table, String selection) {
        Cursor cursor = null;
        try {
            cursor = db.query(false, table, null, selection, null, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
            }
        } catch (SQLiteException e) {
            Log.e(TAG, e.toString());
        }
        return cursor;
    }

    public static Cursor query(SQLiteDatabase db, String table, String[] columns, String selection) {
        Cursor cursor = null;
        try {
            cursor = db.query(false, table, columns, selection, null, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return cursor;
    }

    public static boolean isColumnExist(SQLiteDatabase db, String tableName, String columnName) {
        boolean result = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
            result = (cursor == null || cursor.getColumnIndex(columnName) == -1) ? false : true;
            if (!(cursor == null || (cursor.isClosed() ^ 1) == 0)) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            if (!(cursor == null || (cursor.isClosed() ^ 1) == 0)) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (!(cursor == null || (cursor.isClosed() ^ 1) == 0)) {
                cursor.close();
            }
        }
        return result;
    }

    public static boolean insertColumn(SQLiteDatabase db, String tableName, String columnName, String type) {
        if (!isColumnExist(db, tableName, columnName)) {
            try {
                db.execSQL(String.format("alter table %s add %s %s", new Object[]{tableName, columnName, type}));
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return true;
    }

    public static void vaccum(SQLiteDatabase db) {
        try {
            db.execSQL("vacuum");
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    public static void removeTable(SQLiteDatabase db, String tableName) {
        if (db != null) {
            try {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    public static void removeAllTables(SQLiteDatabase db) {
        if (db != null) {
            for (String tableName : TABLE_LIST) {
                try {
                    db.execSQL("DROP TABLE IF EXISTS " + tableName);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

    public static boolean createIndex(SQLiteDatabase db, String table, String[] keys) {
        if (db == null || keys == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.setLength(0);
        cmd.append(String.format("create index if not exists %s_index on %s", new Object[]{table, table}));
        cmd.append(" (");
        for (int i = 0; i < keys.length; i++) {
            if (i == 0) {
                cmd.append(keys[i]);
            } else {
                cmd.append(",").append(keys[i]);
            }
        }
        cmd.append(")");
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean dropIndex(SQLiteDatabase db, String table) {
        if (db == null) {
            return false;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append(String.format("drop index if exists %s_index", new Object[]{table}));
        try {
            db.execSQL(cmd.toString());
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static void removeAllData(SQLiteDatabase db) {
        if (db != null) {
            for (String tableName : TABLE_LIST) {
                try {
                    db.execSQL("DELETE from " + tableName);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

    public static void removeDataBefore(SQLiteDatabase db, long before) {
        if (db != null) {
            long date = System.currentTimeMillis() - before;
            for (String tableName : TABLE_LIST) {
                try {
                    db.execSQL("DELETE from " + tableName + " where date < " + date);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }
}
