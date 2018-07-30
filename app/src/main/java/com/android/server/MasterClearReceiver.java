package com.android.server;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RecoverySystem;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import java.io.IOException;

public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";
    private boolean mWipeEsims;
    private boolean mWipeExternalStorage;

    private class WipeDataTask extends AsyncTask<Void, Void, Void> {
        private final Thread mChainedTask;
        private final Context mContext;
        private final ProgressDialog mProgressDialog;

        public WipeDataTask(Context context, Thread chainedTask) {
            this.mContext = context;
            this.mChainedTask = chainedTask;
            this.mProgressDialog = new ProgressDialog(context);
        }

        protected void onPreExecute() {
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.getWindow().setType(2003);
            this.mProgressDialog.setMessage(this.mContext.getText(17040729));
            this.mProgressDialog.show();
        }

        protected Void doInBackground(Void... params) {
            Slog.w(MasterClearReceiver.TAG, "Wiping adoptable disks");
            if (MasterClearReceiver.this.mWipeExternalStorage) {
                ((StorageManager) this.mContext.getSystemService("storage")).wipeAdoptableDisks();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            this.mProgressDialog.dismiss();
            this.mChainedTask.start();
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE") || "google.com".equals(intent.getStringExtra("from"))) {
            boolean forceWipe;
            if ("android.intent.action.MASTER_CLEAR".equals(intent.getAction())) {
                Slog.w(TAG, "The request uses the deprecated Intent#ACTION_MASTER_CLEAR, Intent#ACTION_FACTORY_RESET should be used instead.");
            }
            if (intent.hasExtra("android.intent.extra.FORCE_MASTER_CLEAR")) {
                Slog.w(TAG, "The request uses the deprecated Intent#EXTRA_FORCE_MASTER_CLEAR, Intent#EXTRA_FORCE_FACTORY_RESET should be used instead.");
            }
            final boolean shutdown = intent.getBooleanExtra("shutdown", false);
            final String reason = intent.getStringExtra("android.intent.extra.REASON");
            this.mWipeExternalStorage = intent.getBooleanExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false);
            this.mWipeEsims = intent.getBooleanExtra("com.android.internal.intent.extra.WIPE_ESIMS", false);
            if (intent.getBooleanExtra("android.intent.extra.FORCE_MASTER_CLEAR", false)) {
                forceWipe = true;
            } else {
                forceWipe = intent.getBooleanExtra("android.intent.extra.FORCE_FACTORY_RESET", false);
            }
            final boolean bFormat = intent.getBooleanExtra("formatdata", false);
            Slog.w(TAG, "Get format command from extra, data will be formated!!!");
            Slog.w(TAG, "!!! FACTORY RESET !!!");
            final Context context2 = context;
            Thread thr = new Thread("Reboot") {
                public void run() {
                    try {
                        if (bFormat) {
                            RecoverySystem.rebootFormatUserData(context2, shutdown, reason, forceWipe, MasterClearReceiver.this.mWipeEsims);
                        } else {
                            RecoverySystem.rebootWipeUserData(context2, shutdown, reason, forceWipe, MasterClearReceiver.this.mWipeEsims);
                        }
                        Log.wtf(MasterClearReceiver.TAG, "Still running after master clear?!");
                    } catch (IOException e) {
                        Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e);
                    } catch (SecurityException e2) {
                        Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e2);
                    }
                }
            };
            if (this.mWipeExternalStorage || this.mWipeEsims) {
                new WipeDataTask(context, thr).execute(new Void[0]);
            } else {
                thr.start();
            }
            return;
        }
        Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
    }
}
