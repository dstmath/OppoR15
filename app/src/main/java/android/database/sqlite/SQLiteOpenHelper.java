package android.database.sqlite;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabase.OpenParams;
import android.database.sqlite.SQLiteDatabase.OpenParams.Builder;
import android.os.FileUtils;
import android.util.Log;
import java.io.File;

public abstract class SQLiteOpenHelper {
    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();
    private final Context mContext;
    private SQLiteDatabase mDatabase;
    private boolean mIsInitializing;
    private final int mMinimumSupportedVersion;
    private final String mName;
    private final int mNewVersion;
    private final Builder mOpenParamsBuilder;

    public abstract void onCreate(SQLiteDatabase sQLiteDatabase);

    public abstract void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2);

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
        this(context, name, factory, version, null);
    }

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        this(context, name, factory, version, 0, errorHandler);
    }

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version, int minimumSupportedVersion, DatabaseErrorHandler errorHandler) {
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1, was " + version);
        }
        this.mContext = context;
        this.mName = name;
        this.mNewVersion = version;
        this.mMinimumSupportedVersion = Math.max(0, minimumSupportedVersion);
        this.mOpenParamsBuilder = new Builder();
        this.mOpenParamsBuilder.setCursorFactory(factory);
        this.mOpenParamsBuilder.setErrorHandler(errorHandler);
        this.mOpenParamsBuilder.addOpenFlags(268435456);
    }

    public String getDatabaseName() {
        return this.mName;
    }

    public void setWriteAheadLoggingEnabled(boolean enabled) {
        synchronized (this) {
            if (this.mOpenParamsBuilder.isWriteAheadLoggingEnabled() != enabled) {
                if (!(this.mDatabase == null || !this.mDatabase.isOpen() || (this.mDatabase.isReadOnly() ^ 1) == 0)) {
                    if (enabled) {
                        this.mDatabase.enableWriteAheadLogging();
                    } else {
                        this.mDatabase.disableWriteAheadLogging();
                    }
                }
                this.mOpenParamsBuilder.setWriteAheadLoggingEnabled(enabled);
            }
        }
    }

    public void setLookasideConfig(int slotSize, int slotCount) {
        synchronized (this) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                this.mOpenParamsBuilder.setLookasideConfig(slotSize, slotCount);
            } else {
                throw new IllegalStateException("Lookaside memory config cannot be changed after opening the database");
            }
        }
    }

    public void setIdleConnectionTimeout(long idleConnectionTimeoutMs) {
        synchronized (this) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                this.mOpenParamsBuilder.setIdleConnectionTimeout(idleConnectionTimeoutMs);
            } else {
                throw new IllegalStateException("Connection timeout setting cannot be changed after opening the database");
            }
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase databaseLocked;
        synchronized (this) {
            databaseLocked = getDatabaseLocked(true);
        }
        return databaseLocked;
    }

    public SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase databaseLocked;
        synchronized (this) {
            databaseLocked = getDatabaseLocked(false);
        }
        return databaseLocked;
    }

    private SQLiteDatabase getDatabaseLocked(boolean writable) {
        if (this.mDatabase != null) {
            if (!this.mDatabase.isOpen()) {
                this.mDatabase = null;
            } else if (!(writable && (this.mDatabase.isReadOnly() ^ 1) == 0)) {
                return this.mDatabase;
            }
        }
        if (this.mIsInitializing) {
            throw new IllegalStateException("getDatabase called recursively");
        }
        SQLiteDatabase db = this.mDatabase;
        File filePath;
        OpenParams params;
        try {
            this.mIsInitializing = true;
            if (db != null) {
                if (writable && db.isReadOnly()) {
                    db.reopenReadWrite();
                }
            } else if (this.mName == null) {
                db = SQLiteDatabase.createInMemory(this.mOpenParamsBuilder.build());
            } else {
                filePath = this.mContext.getDatabasePath(this.mName);
                params = this.mOpenParamsBuilder.build();
                db = SQLiteDatabase.openDatabase(filePath, params);
                setFilePermissionsForDb(filePath.getPath());
            }
        } catch (SQLException ex) {
            if (writable) {
                throw ex;
            }
            Log.e(TAG, "Couldn't open " + this.mName + " for writing (will try read-only):", ex);
            db = SQLiteDatabase.openDatabase(filePath, params.toBuilder().addOpenFlags(1).build());
        } catch (Throwable th) {
            this.mIsInitializing = false;
            if (!(db == null || db == this.mDatabase)) {
                db.close();
            }
        }
        onConfigure(db);
        int version = db.getVersion();
        if (version != this.mNewVersion) {
            if (db.isReadOnly()) {
                throw new SQLiteException("Can't upgrade read-only database from version " + db.getVersion() + " to " + this.mNewVersion + ": " + this.mName);
            } else if (version <= 0 || version >= this.mMinimumSupportedVersion) {
                db.beginTransaction();
                if (version == 0) {
                    onCreate(db);
                } else if (version > this.mNewVersion) {
                    onDowngrade(db, version, this.mNewVersion);
                } else {
                    onUpgrade(db, version, this.mNewVersion);
                }
                db.setVersion(this.mNewVersion);
                db.setTransactionSuccessful();
                db.endTransaction();
            } else {
                File databaseFile = new File(db.getPath());
                onBeforeDelete(db);
                db.close();
                if (SQLiteDatabase.deleteDatabase(databaseFile)) {
                    this.mIsInitializing = false;
                    SQLiteDatabase databaseLocked = getDatabaseLocked(writable);
                    this.mIsInitializing = false;
                    if (!(db == null || db == this.mDatabase)) {
                        db.close();
                    }
                    return databaseLocked;
                }
                throw new IllegalStateException("Unable to delete obsolete database " + this.mName + " with version " + version);
            }
        }
        onOpen(db);
        if (db.isReadOnly()) {
            Log.w(TAG, "Opened " + this.mName + " in read-only mode");
        }
        this.mDatabase = db;
        this.mIsInitializing = false;
        if (!(db == null || db == this.mDatabase)) {
            db.close();
        }
        return db;
    }

    private static void setFilePermissionsForDb(String dbPath) {
        FileUtils.setPermissions(dbPath, 432, -1, -1);
    }

    public synchronized void close() {
        if (this.mIsInitializing) {
            throw new IllegalStateException("Closed during initialization");
        } else if (this.mDatabase != null && this.mDatabase.isOpen()) {
            this.mDatabase.close();
            this.mDatabase = null;
        }
    }

    public void onConfigure(SQLiteDatabase db) {
    }

    public void onBeforeDelete(SQLiteDatabase db) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Can't downgrade database from version " + oldVersion + " to " + newVersion);
    }

    public void onOpen(SQLiteDatabase db) {
    }
}
