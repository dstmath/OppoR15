package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseConfiguration;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.Pair;
import java.io.File;

public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {
    private static final String TAG = "DefaultDatabaseErrorHandler";

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onCorruption(SQLiteDatabase dbObj) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.getPath());
        if (dbObj.isOpen()) {
            Iterable attachedDbs = null;
            try {
                attachedDbs = dbObj.getAttachedDbs();
            } catch (SQLiteException e) {
            } catch (Throwable th) {
                Throwable th2 = th;
                if (attachedDbs != null) {
                    for (Pair<String, String> p : attachedDbs) {
                        deleteDatabaseFile((String) p.second);
                    }
                } else {
                    deleteDatabaseFile(dbObj.getPath());
                }
            }
            dbObj.close();
            if (attachedDbs != null) {
                for (Pair<String, String> p2 : attachedDbs) {
                    deleteDatabaseFile((String) p2.second);
                }
            } else {
                deleteDatabaseFile(dbObj.getPath());
            }
            return;
        }
        deleteDatabaseFile(dbObj.getPath());
    }

    private void deleteDatabaseFile(String fileName) {
        if (!fileName.equalsIgnoreCase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH) && fileName.trim().length() != 0) {
            Log.e(TAG, "deleting the database file: " + fileName);
            try {
                SQLiteDatabase.deleteDatabase(new File(fileName));
            } catch (Exception e) {
                Log.w(TAG, "delete failed: " + e.getMessage());
            }
        }
    }
}
