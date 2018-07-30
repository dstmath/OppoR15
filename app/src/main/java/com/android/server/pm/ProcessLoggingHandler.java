package com.android.server.pm;

import android.app.admin.SecurityLog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public final class ProcessLoggingHandler extends Handler {
    static final int INVALIDATE_BASE_APK_HASH_MSG = 2;
    static final int LOG_APP_PROCESS_START_MSG = 1;
    private static final String TAG = "ProcessLoggingHandler";
    private final HashMap<String, String> mProcessLoggingBaseApkHashes = new HashMap();

    ProcessLoggingHandler() {
        super(BackgroundThread.getHandler().getLooper());
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                Bundle bundle = msg.getData();
                String processName = bundle.getString("processName");
                int uid = bundle.getInt("uid");
                String seinfo = bundle.getString("seinfo");
                String apkFile = bundle.getString("apkFile");
                int pid = bundle.getInt("pid");
                long startTimestamp = bundle.getLong("startTimestamp");
                String apkHash = computeStringHashOfApk(apkFile);
                SecurityLog.writeEvent(210005, new Object[]{processName, Long.valueOf(startTimestamp), Integer.valueOf(uid), Integer.valueOf(pid), seinfo, apkHash});
                return;
            case 2:
                this.mProcessLoggingBaseApkHashes.remove(msg.getData().getString("apkFile"));
                return;
            default:
                return;
        }
    }

    void invalidateProcessLoggingBaseApkHash(String apkPath) {
        Bundle data = new Bundle();
        data.putString("apkFile", apkPath);
        Message msg = obtainMessage(2);
        msg.setData(data);
        sendMessage(msg);
    }

    private String computeStringHashOfApk(String apkFile) {
        if (apkFile == null) {
            return "No APK";
        }
        String apkHash = (String) this.mProcessLoggingBaseApkHashes.get(apkFile);
        if (apkHash == null) {
            try {
                byte[] hash = computeHashOfApkFile(apkFile);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hash.length; i++) {
                    sb.append(String.format("%02x", new Object[]{Byte.valueOf(hash[i])}));
                }
                apkHash = sb.toString();
                this.mProcessLoggingBaseApkHashes.put(apkFile, apkHash);
            } catch (Exception e) {
                Slog.w(TAG, "computeStringHashOfApk() failed", e);
            }
        }
        if (apkHash == null) {
            apkHash = "Failed to count APK hash";
        }
        return apkHash;
    }

    private byte[] computeHashOfApkFile(String packageArchiveLocation) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream input = new FileInputStream(new File(packageArchiveLocation));
        byte[] buffer = new byte[65536];
        while (true) {
            int size = input.read(buffer);
            if (size > 0) {
                md.update(buffer, 0, size);
            } else {
                input.close();
                return md.digest();
            }
        }
    }
}
