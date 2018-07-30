package com.android.server.wifi;

import android.content.Context;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.Log;
import com.oppo.RomUpdateHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;

public class OppoWifiCfgUpdateHelper extends RomUpdateHelper {
    private static final String DATA_FILE_DIR = "/data/misc/wifi/WCNSS_qcom_cfg_new.ini";
    public static final String FILTER_NAME = "qcom_wifi_cfg";
    private static final String MTK_CFG_FILE = "/data/misc/wifi/wifi_fw.cfg";
    private static final String MTK_CFG_FILE_BACK = "/data/misc/wifi/wifi_fw_back.cfg";
    private static final String RO_BOARD_PLATFORM = "ro.board.platform";
    private static final String SYS_FILE_DIR = "/vendor/etc/wifi/WCNSS_qcom_cfg.ini";
    private static final String TAG = "WifiCfgUpdateHelper";
    private static final String VERSION_PATTERN = "#OppoVersion=";
    private static final String WCNSS_CFG_FILE = "/persist/WCNSS_qcom_cfg.ini";
    private static final String WCNSS_CFG_FILE_BACK = "/persist/WCNSS_qcom_cfg_back.ini";
    private static final String WCNSS_CFG_FILE_NEW = "/data/misc/wifi/WCNSS_qcom_cfg_new.ini";
    private String mNewCfgFile = null;
    private String mOldBackupCfgFile = null;
    private String mOldCfgFile = null;

    private class MyTask extends AsyncTask<Void, Void, Void> {
        /* synthetic */ MyTask(OppoWifiCfgUpdateHelper this$0, MyTask -this1) {
            this();
        }

        private MyTask() {
        }

        protected Void doInBackground(Void... params) {
            try {
                if (!new File("/data/misc/wifi/WCNSS_qcom_cfg_new.ini").exists()) {
                    Log.d(OppoWifiCfgUpdateHelper.TAG, " no new cfg file exsits");
                } else if (OppoWifiCfgUpdateHelper.this.hasNewerVersion()) {
                    OppoWifiCfgUpdateHelper.this.replaceOldcfg();
                }
            } catch (SecurityException se) {
                se.printStackTrace();
            }
            return null;
        }
    }

    public OppoWifiCfgUpdateHelper(Context context) {
        super(context, FILTER_NAME, SYS_FILE_DIR, "/data/misc/wifi/WCNSS_qcom_cfg_new.ini");
        setUpdateInfo(null, null);
        String platform = SystemProperties.get(RO_BOARD_PLATFORM);
        if (platform != null ? platform.startsWith("mt") : false) {
            this.mOldCfgFile = MTK_CFG_FILE;
            this.mOldBackupCfgFile = MTK_CFG_FILE_BACK;
        } else {
            this.mOldCfgFile = WCNSS_CFG_FILE;
            this.mOldBackupCfgFile = WCNSS_CFG_FILE_BACK;
        }
        this.mNewCfgFile = "/data/misc/wifi/WCNSS_qcom_cfg_new.ini";
    }

    public void getUpdateFromProvider() {
        super.getUpdateFromProvider();
        new MyTask().execute(new Void[0]);
    }

    private boolean hasNewerVersion() {
        File oldCfgFile = new File(this.mOldCfgFile);
        File newCfgFile = new File(this.mNewCfgFile);
        int oldVersion = getVersion(oldCfgFile);
        int newVersion = getVersion(newCfgFile);
        Log.d(TAG, "old version: " + oldVersion + "new version :" + newVersion);
        return newVersion > oldVersion;
    }

    private void replaceOldcfg() {
        File oldCfgFile = new File(this.mOldCfgFile);
        File backupCfgFile = new File(this.mOldBackupCfgFile);
        if (oldCfgFile.exists()) {
            if (oldCfgFile.renameTo(backupCfgFile) && copyNewCfg()) {
                File backCfgFile = new File(this.mOldBackupCfgFile);
                File newCfgFile = new File(this.mNewCfgFile);
                backCfgFile.delete();
                newCfgFile.delete();
            }
        } else if (copyNewCfg()) {
            new File(this.mNewCfgFile).delete();
        }
    }

    private boolean copyNewCfg() {
        FileNotFoundException notFound;
        IOException ioException;
        Throwable th;
        File newCfgFile = new File(this.mNewCfgFile);
        File oldCfgFile = new File(this.mOldCfgFile);
        FileInputStream src = null;
        try {
            FileInputStream src2 = new FileInputStream(newCfgFile);
            try {
                Files.copy(src2, oldCfgFile.toPath(), new CopyOption[0]);
                FileUtils.setPermissions(this.mOldCfgFile, 432, 1000, 1010);
                if (src2 != null) {
                    try {
                        src2.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            } catch (FileNotFoundException e2) {
                notFound = e2;
                src = src2;
                notFound.printStackTrace();
                if (src != null) {
                    try {
                        src.close();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
                return false;
            } catch (IOException e4) {
                ioException = e4;
                src = src2;
                try {
                    ioException.printStackTrace();
                    if (src != null) {
                        try {
                            src.close();
                        } catch (Exception e32) {
                            e32.printStackTrace();
                        }
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    if (src != null) {
                        try {
                            src.close();
                        } catch (Exception e322) {
                            e322.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                src = src2;
                if (src != null) {
                    try {
                        src.close();
                    } catch (Exception e3222) {
                        e3222.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e5) {
            notFound = e5;
            notFound.printStackTrace();
            if (src != null) {
                try {
                    src.close();
                } catch (Exception e32222) {
                    e32222.printStackTrace();
                }
            }
            return false;
        } catch (IOException e6) {
            ioException = e6;
            ioException.printStackTrace();
            if (src != null) {
                try {
                    src.close();
                } catch (Exception e322222) {
                    e322222.printStackTrace();
                }
            }
            return false;
        }
    }

    private int getVersion(File file) {
        FileNotFoundException notFound;
        IOException ioexception;
        NumberFormatException foramtException;
        Throwable th;
        int version = 0;
        FileInputStream inputStream = null;
        BufferedReader reader = null;
        try {
            FileInputStream inputStream2 = new FileInputStream(file);
            try {
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(inputStream2));
                try {
                    String versionString = reader2.readLine();
                    if (versionString.startsWith(VERSION_PATTERN)) {
                        String trimedVersion = versionString.substring(VERSION_PATTERN.length()).trim();
                        Log.d(TAG, "getVersion trimedVersion " + trimedVersion + " trimedVersion length " + trimedVersion.length());
                        version = Integer.parseInt(trimedVersion);
                    }
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (Exception e) {
                        }
                    }
                    if (inputStream2 != null) {
                        try {
                            inputStream2.close();
                        } catch (Exception e2) {
                        }
                    }
                    inputStream = inputStream2;
                } catch (FileNotFoundException e3) {
                    notFound = e3;
                    reader = reader2;
                    inputStream = inputStream2;
                    notFound.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e4) {
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e5) {
                        }
                    }
                    return version;
                } catch (IOException e6) {
                    ioexception = e6;
                    reader = reader2;
                    inputStream = inputStream2;
                    ioexception.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e7) {
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e8) {
                        }
                    }
                    return version;
                } catch (NumberFormatException e9) {
                    foramtException = e9;
                    reader = reader2;
                    inputStream = inputStream2;
                    try {
                        foramtException.printStackTrace();
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e10) {
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Exception e11) {
                            }
                        }
                        return version;
                    } catch (Throwable th2) {
                        th = th2;
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e12) {
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Exception e13) {
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    reader = reader2;
                    inputStream = inputStream2;
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e122) {
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e132) {
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e14) {
                notFound = e14;
                inputStream = inputStream2;
                notFound.printStackTrace();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e42) {
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e52) {
                    }
                }
                return version;
            } catch (IOException e15) {
                ioexception = e15;
                inputStream = inputStream2;
                ioexception.printStackTrace();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e72) {
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e82) {
                    }
                }
                return version;
            } catch (NumberFormatException e16) {
                foramtException = e16;
                inputStream = inputStream2;
                foramtException.printStackTrace();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e102) {
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e112) {
                    }
                }
                return version;
            } catch (Throwable th4) {
                th = th4;
                inputStream = inputStream2;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e1222) {
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e1322) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e17) {
            notFound = e17;
            notFound.printStackTrace();
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e422) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e522) {
                }
            }
            return version;
        } catch (IOException e18) {
            ioexception = e18;
            ioexception.printStackTrace();
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e722) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e822) {
                }
            }
            return version;
        } catch (NumberFormatException e19) {
            foramtException = e19;
            foramtException.printStackTrace();
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e1022) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e1122) {
                }
            }
            return version;
        }
        return version;
    }
}
