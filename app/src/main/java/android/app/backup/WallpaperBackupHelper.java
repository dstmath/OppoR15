package android.app.backup;

import android.app.WallpaperManager;
import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WallpaperBackupHelper extends FileBackupHelperBase implements BackupHelper {
    private static final boolean DEBUG = false;
    private static final String STAGE_FILE = new File(Environment.getUserSystemDirectory(0), "wallpaper-tmp").getAbsolutePath();
    private static final String TAG = "WallpaperBackupHelper";
    public static final String WALLPAPER_IMAGE_KEY = "/data/data/com.android.settings/files/wallpaper";
    public static final String WALLPAPER_INFO_KEY = "/data/system/wallpaper_info.xml";
    private final String[] mKeys;
    private final WallpaperManager mWpm;

    public /* bridge */ /* synthetic */ void writeNewStateDescription(ParcelFileDescriptor parcelFileDescriptor) {
        super.writeNewStateDescription(parcelFileDescriptor);
    }

    public WallpaperBackupHelper(Context context, String[] keys) {
        super(context);
        this.mContext = context;
        this.mKeys = keys;
        this.mWpm = (WallpaperManager) context.getSystemService(Context.WALLPAPER_SERVICE);
    }

    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
    }

    public void restoreEntity(BackupDataInputStream data) {
        File stage;
        IOException e;
        Throwable th;
        Throwable th2 = null;
        String key = data.getKey();
        if (isKeyInList(key, this.mKeys) && key.equals(WALLPAPER_IMAGE_KEY)) {
            stage = new File(STAGE_FILE);
            try {
                if (writeFile(stage, data)) {
                    FileInputStream in = null;
                    try {
                        FileInputStream in2 = new FileInputStream(stage);
                        try {
                            this.mWpm.setStream(in2);
                            if (in2 != null) {
                                try {
                                    in2.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            if (th2 != null) {
                                try {
                                    throw th2;
                                } catch (IOException e2) {
                                    e = e2;
                                    in = in2;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            in = in2;
                            if (in != null) {
                                try {
                                    in.close();
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
                        if (in != null) {
                            try {
                                in.close();
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
                } else {
                    Slog.e(TAG, "Unable to save restored wallpaper");
                }
                stage.delete();
            } catch (Throwable th7) {
                stage.delete();
            }
        } else {
            return;
        }
        Slog.e(TAG, "Unable to set restored wallpaper: " + e.getMessage());
        stage.delete();
    }
}
