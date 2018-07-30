package android.os;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.backup.FullBackup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.IRecoverySystemProgressListener.Stub;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.logging.MetricsLogger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import libcore.io.Streams;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

public class RecoverySystem {
    private static final String ACTION_EUICC_FACTORY_RESET = "com.android.internal.action.EUICC_FACTORY_RESET";
    public static final File BLOCK_MAP_FILE = new File(RECOVERY_DIR, "block.map");
    private static final long DEFAULT_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 30000;
    private static final File DEFAULT_KEYSTORE = new File("/system/etc/security/otacerts.zip");
    private static final File LAST_INSTALL_FILE = new File(RECOVERY_DIR, "last_install");
    private static final String LAST_PREFIX = "last_";
    private static final File LOG_FILE = new File(RECOVERY_DIR, "log");
    private static final int LOG_FILE_MAX_LENGTH = 65536;
    private static final long MAX_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 60000;
    private static final long MIN_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 5000;
    private static final long PUBLISH_PROGRESS_INTERVAL_MS = 500;
    private static final File RECOVERY_DIR = new File("/cache/recovery");
    private static final String TAG = "RecoverySystem";
    public static final File UNCRYPT_PACKAGE_FILE = new File(RECOVERY_DIR, "uncrypt_file");
    public static final File UNCRYPT_STATUS_FILE = new File(RECOVERY_DIR, "uncrypt_status");
    private static final Object sRequestLock = new Object();
    private final IRecoverySystem mService;

    public interface ProgressListener {
        void onProgress(int i);
    }

    private static HashSet<X509Certificate> getTrustedCerts(File keystore) throws IOException, GeneralSecurityException {
        HashSet<X509Certificate> trusted = new HashSet();
        if (keystore == null) {
            keystore = DEFAULT_KEYSTORE;
        }
        ZipFile zip = new ZipFile(keystore);
        InputStream is;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                is = zip.getInputStream((ZipEntry) entries.nextElement());
                trusted.add((X509Certificate) cf.generateCertificate(is));
                is.close();
            }
            zip.close();
            return trusted;
        } catch (Throwable th) {
            zip.close();
        }
    }

    public static void verifyPackage(File packageFile, ProgressListener listener, File deviceCertsZipFile) throws IOException, GeneralSecurityException {
        final long fileLen = packageFile.length();
        final RandomAccessFile raf = new RandomAccessFile(packageFile, FullBackup.ROOT_TREE_TOKEN);
        try {
            final long startTimeMillis = System.currentTimeMillis();
            if (listener != null) {
                listener.onProgress(0);
            }
            raf.seek(fileLen - 6);
            byte[] footer = new byte[6];
            raf.readFully(footer);
            if (footer[2] == (byte) -1 && footer[3] == (byte) -1) {
                final int commentSize = (footer[4] & 255) | ((footer[5] & 255) << 8);
                int signatureStart = (footer[0] & 255) | ((footer[1] & 255) << 8);
                byte[] eocd = new byte[(commentSize + 22)];
                raf.seek(fileLen - ((long) (commentSize + 22)));
                raf.readFully(eocd);
                if (eocd[0] == (byte) 80 && eocd[1] == (byte) 75 && eocd[2] == (byte) 5 && eocd[3] == (byte) 6) {
                    int i = 4;
                    while (i < eocd.length - 3) {
                        if (eocd[i] == (byte) 80 && eocd[i + 1] == (byte) 75 && eocd[i + 2] == (byte) 5 && eocd[i + 3] == (byte) 6) {
                            throw new SignatureException("EOCD marker found after start of EOCD");
                        }
                        i++;
                    }
                    PKCS7 block = new PKCS7(new ByteArrayInputStream(eocd, (commentSize + 22) - signatureStart, signatureStart));
                    X509Certificate[] certificates = block.getCertificates();
                    if (certificates == null || certificates.length == 0) {
                        throw new SignatureException("signature contains no certificates");
                    }
                    PublicKey signatureKey = certificates[0].getPublicKey();
                    SignerInfo[] signerInfos = block.getSignerInfos();
                    if (signerInfos == null || signerInfos.length == 0) {
                        throw new SignatureException("signature contains no signedData");
                    }
                    SignerInfo signerInfo = signerInfos[0];
                    boolean verified = false;
                    if (deviceCertsZipFile == null) {
                        deviceCertsZipFile = DEFAULT_KEYSTORE;
                    }
                    for (X509Certificate c : getTrustedCerts(deviceCertsZipFile)) {
                        if (c.getPublicKey().equals(signatureKey)) {
                            verified = true;
                            break;
                        }
                    }
                    if (verified) {
                        raf.seek(0);
                        ProgressListener listenerForInner = listener;
                        final ProgressListener progressListener = listener;
                        SignerInfo verifyResult = block.verify(signerInfo, new InputStream() {
                            int lastPercent = 0;
                            long lastPublishTime = startTimeMillis;
                            long soFar = 0;
                            long toRead = ((fileLen - ((long) commentSize)) - 2);

                            public int read() throws IOException {
                                throw new UnsupportedOperationException();
                            }

                            public int read(byte[] b, int off, int len) throws IOException {
                                if (this.soFar >= this.toRead || Thread.currentThread().isInterrupted()) {
                                    return -1;
                                }
                                int size = len;
                                if (this.soFar + ((long) len) > this.toRead) {
                                    size = (int) (this.toRead - this.soFar);
                                }
                                int read = raf.read(b, off, size);
                                this.soFar += (long) read;
                                if (progressListener != null) {
                                    long now = System.currentTimeMillis();
                                    int p = (int) ((this.soFar * 100) / this.toRead);
                                    if (p > this.lastPercent && now - this.lastPublishTime > RecoverySystem.PUBLISH_PROGRESS_INTERVAL_MS) {
                                        this.lastPercent = p;
                                        this.lastPublishTime = now;
                                        progressListener.onProgress(this.lastPercent);
                                    }
                                }
                                return read;
                            }
                        });
                        boolean interrupted = Thread.interrupted();
                        if (listener != null) {
                            listener.onProgress(100);
                        }
                        if (interrupted) {
                            throw new SignatureException("verification was interrupted");
                        } else if (verifyResult == null) {
                            throw new SignatureException("signature digest verification failed");
                        } else if (!readAndVerifyPackageCompatibilityEntry(packageFile)) {
                            throw new SignatureException("package compatibility verification failed");
                        } else {
                            return;
                        }
                    }
                    throw new SignatureException("signature doesn't match any trusted key");
                }
                throw new SignatureException("no signature in file (bad footer)");
            }
            throw new SignatureException("no signature in file (no footer)");
        } finally {
            raf.close();
        }
    }

    private static boolean verifyPackageCompatibility(InputStream inputStream) throws IOException {
        long entrySize;
        ArrayList<String> list = new ArrayList();
        ZipInputStream zis = new ZipInputStream(inputStream);
        while (true) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                entrySize = entry.getSize();
                if (entrySize <= 2147483647L && entrySize >= 0) {
                    byte[] bytes = new byte[((int) entrySize)];
                    Streams.readFully(zis, bytes);
                    list.add(new String(bytes, StandardCharsets.UTF_8));
                }
            } else if (!list.isEmpty()) {
                return VintfObject.verify((String[]) list.toArray(new String[list.size()])) == 0;
            } else {
                throw new IOException("no entries found in the compatibility file");
            }
        }
        throw new IOException("invalid entry size (" + entrySize + ") in the compatibility file");
    }

    private static boolean readAndVerifyPackageCompatibilityEntry(File packageFile) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ZipFile zip = null;
        try {
            ZipFile zip2 = new ZipFile(packageFile);
            try {
                ZipEntry entry = zip2.getEntry("compatibility.zip");
                if (entry == null) {
                    if (zip2 != null) {
                        try {
                            zip2.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 == null) {
                        return true;
                    }
                    throw th2;
                }
                boolean verifyPackageCompatibility = verifyPackageCompatibility(zip2.getInputStream(entry));
                if (zip2 != null) {
                    try {
                        zip2.close();
                    } catch (Throwable th4) {
                        th2 = th4;
                    }
                }
                if (th2 == null) {
                    return verifyPackageCompatibility;
                }
                throw th2;
            } catch (Throwable th5) {
                th = th5;
                zip = zip2;
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (Throwable th6) {
                        if (th2 == null) {
                            th2 = th6;
                        } else if (th2 != th6) {
                            th2.addSuppressed(th6);
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        } catch (Throwable th7) {
            th = th7;
            if (zip != null) {
                try {
                    zip.close();
                } catch (Throwable th62) {
                    if (th2 == null) {
                        th2 = th62;
                    } else if (th2 != th62) {
                        th2.addSuppressed(th62);
                    }
                }
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }

    @SuppressLint({"Doclava125"})
    public static boolean verifyPackageCompatibility(File compatibilityFile) throws IOException {
        Throwable th;
        Throwable th2 = null;
        InputStream inputStream = null;
        try {
            InputStream inputStream2 = new FileInputStream(compatibilityFile);
            try {
                boolean verifyPackageCompatibility = verifyPackageCompatibility(inputStream2);
                if (inputStream2 != null) {
                    try {
                        inputStream2.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return verifyPackageCompatibility;
                }
                throw th2;
            } catch (Throwable th4) {
                th = th4;
                inputStream = inputStream2;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (inputStream != null) {
                try {
                    inputStream.close();
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
            }
            throw th2;
        }
    }

    public static void processPackage(Context context, File packageFile, final ProgressListener listener, Handler handler) throws IOException {
        String filename = packageFile.getCanonicalPath();
        if (filename.startsWith("/data/")) {
            RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");
            IRecoverySystemProgressListener iRecoverySystemProgressListener = null;
            if (listener != null) {
                Handler progressHandler;
                if (handler != null) {
                    progressHandler = handler;
                } else {
                    progressHandler = new Handler(context.getMainLooper());
                }
                iRecoverySystemProgressListener = new Stub() {
                    int lastProgress = 0;
                    long lastPublishTime = System.currentTimeMillis();

                    public void onProgress(int progress) {
                        final long now = System.currentTimeMillis();
                        Handler handler = progressHandler;
                        final ProgressListener progressListener = listener;
                        final int i = progress;
                        handler.post(new Runnable() {
                            public void run() {
                                if (i > AnonymousClass2.this.lastProgress && now - AnonymousClass2.this.lastPublishTime > RecoverySystem.PUBLISH_PROGRESS_INTERVAL_MS) {
                                    AnonymousClass2.this.lastProgress = i;
                                    AnonymousClass2.this.lastPublishTime = now;
                                    progressListener.onProgress(i);
                                }
                            }
                        });
                    }
                };
            }
            if (!rs.uncrypt(filename, iRecoverySystemProgressListener)) {
                throw new IOException("process package failed");
            }
        }
    }

    public static void processPackage(Context context, File packageFile, ProgressListener listener) throws IOException {
        processPackage(context, packageFile, listener, null);
    }

    public static void installPackage(Context context, File packageFile) throws IOException {
        installPackage(context, packageFile, false);
    }

    public static void installPackage(Context context, File packageFile, boolean processed) throws IOException {
        synchronized (sRequestLock) {
            LOG_FILE.delete();
            UNCRYPT_PACKAGE_FILE.delete();
            String filename = packageFile.getCanonicalPath();
            Log.w(TAG, "!!! REBOOTING TO INSTALL " + filename + " !!!");
            boolean securityUpdate = filename.endsWith("_s.zip");
            if (filename.startsWith("/data/")) {
                if (!processed) {
                    FileWriter uncryptFile = new FileWriter(UNCRYPT_PACKAGE_FILE);
                    try {
                        uncryptFile.write(filename + "\n");
                        if (!(UNCRYPT_PACKAGE_FILE.setReadable(true, false) && (UNCRYPT_PACKAGE_FILE.setWritable(true, false) ^ 1) == 0)) {
                            Log.e(TAG, "Error setting permission for " + UNCRYPT_PACKAGE_FILE);
                        }
                        BLOCK_MAP_FILE.delete();
                    } finally {
                        uncryptFile.close();
                    }
                } else if (!BLOCK_MAP_FILE.exists()) {
                    Log.e(TAG, "Package claimed to have been processed but failed to find the block map file.");
                    throw new IOException("Failed to find block map file");
                }
                filename = "@/cache/recovery/block.map";
            }
            String filenameArg = "--update_package=" + filename + "\n";
            String securityArg = "--security\n";
            String command = filenameArg + ("--locale=" + Locale.getDefault().toLanguageTag() + "\n");
            if (securityUpdate) {
                command = command + "--security\n";
            }
            if (((RecoverySystem) context.getSystemService("recovery")).setupBcb(command)) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                String reason = PowerManager.REBOOT_RECOVERY_UPDATE;
                if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK) && ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getState() != 2) {
                    reason = reason + ",quiescent";
                }
                pm.reboot(reason);
                throw new IOException("Reboot failed (no permissions?)");
            }
            throw new IOException("Setup BCB failed");
        }
    }

    public static void scheduleUpdateOnBoot(Context context, File packageFile) throws IOException {
        String filename = packageFile.getCanonicalPath();
        boolean securityUpdate = filename.endsWith("_s.zip");
        if (filename.startsWith("/data/")) {
            filename = "@/cache/recovery/block.map";
        }
        String filenameArg = "--update_package=" + filename + "\n";
        String securityArg = "--security\n";
        String command = filenameArg + ("--locale=" + Locale.getDefault().toLanguageTag() + "\n");
        if (securityUpdate) {
            command = command + "--security\n";
        }
        if (!((RecoverySystem) context.getSystemService("recovery")).setupBcb(command)) {
            throw new IOException("schedule update on boot failed");
        }
    }

    public static void cancelScheduledUpdate(Context context) throws IOException {
        if (!((RecoverySystem) context.getSystemService("recovery")).clearBcb()) {
            throw new IOException("cancel scheduled update failed");
        }
    }

    public static void rebootWipeUserData(Context context) throws IOException {
        rebootWipeUserData(context, false, context.getPackageName(), false, false);
    }

    public static void rebootWipeUserData(Context context, String reason) throws IOException {
        rebootWipeUserData(context, false, reason, false, false);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown) throws IOException {
        rebootWipeUserData(context, shutdown, context.getPackageName(), false, false);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown, String reason, boolean force) throws IOException {
        rebootWipeUserData(context, shutdown, reason, force, false);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown, String reason, boolean force, boolean wipeEuicc) throws IOException {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (!force) {
            if (um.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)) {
                throw new SecurityException("Wiping data is not allowed for this user.");
            }
        }
        final ConditionVariable condition = new ConditionVariable();
        Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR_NOTIFICATION);
        intent.addFlags(285212672);
        context.sendOrderedBroadcastAsUser(intent, UserHandle.SYSTEM, permission.MASTER_CLEAR, new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                condition.open();
            }
        }, null, 0, null, null);
        condition.block();
        wipeEuiccData(context, wipeEuicc);
        String shutdownArg = null;
        if (shutdown) {
            shutdownArg = "--shutdown_after";
        }
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            reasonArg = "--reason=" + sanitizeArg(reason);
        }
        try {
            IStorageManager mountService = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
            mountService.setField(StorageManager.SYSTEM_LOCALE_KEY, "");
            mountService.setField(StorageManager.PATTERN_VISIBLE_KEY, "");
            mountService.setField(StorageManager.PASSWORD_VISIBLE_KEY, "");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        String localeArg = "--locale=" + Locale.getDefault().toLanguageTag();
        bootCommand(context, shutdownArg, "--wipe_data", reasonArg, localeArg);
    }

    public static void rebootFormatUserData(Context context, boolean shutdown, String reason, boolean force, boolean wipeEuicc) throws IOException {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (force || !um.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)) {
            final ConditionVariable condition = new ConditionVariable();
            Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR_NOTIFICATION);
            intent.addFlags(285212672);
            context.sendOrderedBroadcastAsUser(intent, UserHandle.SYSTEM, permission.MASTER_CLEAR, new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    condition.open();
                }
            }, null, 0, null, null);
            condition.block();
            wipeEuiccData(context, wipeEuicc);
            String shutdownArg = null;
            if (shutdown) {
                shutdownArg = "--shutdown_after";
            }
            String reasonArg = null;
            if (!TextUtils.isEmpty(reason)) {
                reasonArg = "--reason=" + sanitizeArg(reason);
            }
            String localeArg = "--locale=" + Locale.getDefault().toLanguageTag();
            bootCommand(context, shutdownArg, "--format_data", reasonArg, localeArg);
            return;
        }
        throw new SecurityException("Wiping data is not allowed for this user.");
    }

    private static void wipeEuiccData(Context context, boolean isWipeEuicc) {
        if (Global.getInt(context.getContentResolver(), "euicc_provisioned", 0) == 0) {
            Log.d(TAG, "Skipping eUICC wipe/retain as it is not provisioned");
            return;
        }
        EuiccManager euiccManager = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
        if (euiccManager != null && euiccManager.isEnabled()) {
            final CountDownLatch euiccFactoryResetLatch = new CountDownLatch(1);
            final boolean z = isWipeEuicc;
            BroadcastReceiver euiccWipeFinishReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (RecoverySystem.ACTION_EUICC_FACTORY_RESET.equals(intent.getAction())) {
                        if (getResultCode() != 0) {
                            int detailedCode = intent.getIntExtra("android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE", 0);
                            if (z) {
                                Log.e(RecoverySystem.TAG, "Error wiping euicc data, Detailed code = " + detailedCode);
                            } else {
                                Log.e(RecoverySystem.TAG, "Error retaining euicc data, Detailed code = " + detailedCode);
                            }
                        } else if (z) {
                            Log.d(RecoverySystem.TAG, "Successfully wiped euicc data.");
                        } else {
                            Log.d(RecoverySystem.TAG, "Successfully retained euicc data.");
                        }
                        euiccFactoryResetLatch.countDown();
                    }
                }
            };
            Intent intent = new Intent(ACTION_EUICC_FACTORY_RESET);
            intent.setPackage("android");
            PendingIntent callbackIntent = PendingIntent.getBroadcastAsUser(context, 0, intent, 134217728, UserHandle.SYSTEM);
            IntentFilter filterConsent = new IntentFilter();
            filterConsent.addAction(ACTION_EUICC_FACTORY_RESET);
            HandlerThread euiccHandlerThread = new HandlerThread("euiccWipeFinishReceiverThread");
            euiccHandlerThread.start();
            context.getApplicationContext().registerReceiver(euiccWipeFinishReceiver, filterConsent, null, new Handler(euiccHandlerThread.getLooper()));
            if (isWipeEuicc) {
                euiccManager.eraseSubscriptions(callbackIntent);
            } else {
                euiccManager.retainSubscriptionsForFactoryReset(callbackIntent);
            }
            try {
                long waitingTimeMillis = Global.getLong(context.getContentResolver(), "euicc_factory_reset_timeout_millis", 30000);
                if (waitingTimeMillis < MIN_EUICC_FACTORY_RESET_TIMEOUT_MILLIS) {
                    waitingTimeMillis = MIN_EUICC_FACTORY_RESET_TIMEOUT_MILLIS;
                } else if (waitingTimeMillis > MAX_EUICC_FACTORY_RESET_TIMEOUT_MILLIS) {
                    waitingTimeMillis = MAX_EUICC_FACTORY_RESET_TIMEOUT_MILLIS;
                }
                if (!euiccFactoryResetLatch.await(waitingTimeMillis, TimeUnit.MILLISECONDS)) {
                    if (isWipeEuicc) {
                        Log.e(TAG, "Timeout wiping eUICC data.");
                    } else {
                        Log.e(TAG, "Timeout retaining eUICC data.");
                    }
                }
                context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (isWipeEuicc) {
                    Log.e(TAG, "Wiping eUICC data interrupted", e);
                } else {
                    Log.e(TAG, "Retaining eUICC data interrupted", e);
                }
            }
        }
    }

    public static void rebootPromptAndWipeUserData(Context context, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            reasonArg = "--reason=" + sanitizeArg(reason);
        }
        String localeArg = "--locale=" + Locale.getDefault().toString();
        bootCommand(context, null, "--prompt_and_wipe_data", reasonArg, localeArg);
    }

    public static void rebootWipeCache(Context context) throws IOException {
        rebootWipeCache(context, context.getPackageName());
    }

    public static void rebootWipeCache(Context context, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            reasonArg = "--reason=" + sanitizeArg(reason);
        }
        String localeArg = "--locale=" + Locale.getDefault().toLanguageTag();
        bootCommand(context, "--wipe_cache", reasonArg, localeArg);
    }

    public static void rebootWipeAb(Context context, File packageFile, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            reasonArg = "--reason=" + sanitizeArg(reason);
        }
        String filenameArg = "--wipe_package=" + packageFile.getCanonicalPath();
        String localeArg = "--locale=" + Locale.getDefault().toLanguageTag();
        bootCommand(context, "--wipe_ab", filenameArg, reasonArg, localeArg);
    }

    private static void bootCommand(Context context, String... args) throws IOException {
        LOG_FILE.delete();
        StringBuilder command = new StringBuilder();
        for (String arg : args) {
            if (!TextUtils.isEmpty(arg)) {
                command.append(arg);
                command.append("\n");
            }
        }
        ((RecoverySystem) context.getSystemService("recovery")).rebootRecoveryWithCommand(command.toString());
        throw new IOException("Reboot failed (no permissions?)");
    }

    private static void parseLastInstallLog(Context context) {
        Throwable th;
        Throwable th2 = null;
        BufferedReader in = null;
        try {
            IOException e;
            BufferedReader in2 = new BufferedReader(new FileReader(LAST_INSTALL_FILE));
            int bytesWrittenInMiB = -1;
            int bytesStashedInMiB = -1;
            int timeTotal = -1;
            int uncryptTime = -1;
            int sourceVersion = -1;
            int temperatureStart = -1;
            int temperatureEnd = -1;
            int temperatureMax = -1;
            int errorCode = -1;
            int causeCode = -1;
            while (true) {
                try {
                    String line = in2.readLine();
                    if (line == null) {
                        break;
                    }
                    int numIndex = line.indexOf(58);
                    if (numIndex != -1 && numIndex + 1 < line.length()) {
                        try {
                            long parsedNum = Long.parseLong(line.substring(numIndex + 1).trim());
                            try {
                                int scaled;
                                if (line.startsWith("bytes")) {
                                    scaled = Math.toIntExact(parsedNum / 1048576);
                                } else {
                                    scaled = Math.toIntExact(parsedNum);
                                }
                                if (line.startsWith(DropBoxManager.EXTRA_TIME)) {
                                    timeTotal = scaled;
                                } else if (line.startsWith("uncrypt_time")) {
                                    uncryptTime = scaled;
                                } else if (line.startsWith("source_build")) {
                                    sourceVersion = scaled;
                                } else if (line.startsWith("bytes_written")) {
                                    if (bytesWrittenInMiB == -1) {
                                        bytesWrittenInMiB = scaled;
                                    } else {
                                        bytesWrittenInMiB += scaled;
                                    }
                                } else if (line.startsWith("bytes_stashed")) {
                                    if (bytesStashedInMiB == -1) {
                                        bytesStashedInMiB = scaled;
                                    } else {
                                        bytesStashedInMiB += scaled;
                                    }
                                } else if (line.startsWith("temperature_start")) {
                                    temperatureStart = scaled;
                                } else if (line.startsWith("temperature_end")) {
                                    temperatureEnd = scaled;
                                } else if (line.startsWith("temperature_max")) {
                                    temperatureMax = scaled;
                                } else if (line.startsWith("error")) {
                                    errorCode = scaled;
                                } else if (line.startsWith("cause")) {
                                    causeCode = scaled;
                                }
                            } catch (ArithmeticException e2) {
                                Log.e(TAG, "Number overflows in " + line);
                            }
                        } catch (NumberFormatException e3) {
                            Log.e(TAG, "Failed to parse numbers in " + line);
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    in = in2;
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th4) {
                            if (th2 == null) {
                                th2 = th4;
                            } else if (th2 != th4) {
                                th2.addSuppressed(th4);
                            }
                        }
                    }
                    if (th2 == null) {
                        throw th;
                    }
                    try {
                        throw th2;
                    } catch (IOException e4) {
                        e = e4;
                    }
                }
            }
            if (timeTotal != -1) {
                MetricsLogger.histogram(context, "ota_time_total", timeTotal);
            }
            if (uncryptTime != -1) {
                MetricsLogger.histogram(context, "ota_uncrypt_time", uncryptTime);
            }
            if (sourceVersion != -1) {
                MetricsLogger.histogram(context, "ota_source_version", sourceVersion);
            }
            if (bytesWrittenInMiB != -1) {
                MetricsLogger.histogram(context, "ota_written_in_MiBs", bytesWrittenInMiB);
            }
            if (bytesStashedInMiB != -1) {
                MetricsLogger.histogram(context, "ota_stashed_in_MiBs", bytesStashedInMiB);
            }
            if (temperatureStart != -1) {
                MetricsLogger.histogram(context, "ota_temperature_start", temperatureStart);
            }
            if (temperatureEnd != -1) {
                MetricsLogger.histogram(context, "ota_temperature_end", temperatureEnd);
            }
            if (temperatureMax != -1) {
                MetricsLogger.histogram(context, "ota_temperature_max", temperatureMax);
            }
            if (errorCode != -1) {
                MetricsLogger.histogram(context, "ota_non_ab_error_code", errorCode);
            }
            if (causeCode != -1) {
                MetricsLogger.histogram(context, "ota_non_ab_cause_code", causeCode);
            }
            if (in2 != null) {
                try {
                    in2.close();
                } catch (Throwable th5) {
                    th2 = th5;
                }
            }
            if (th2 != null) {
                try {
                    throw th2;
                } catch (IOException e5) {
                    e = e5;
                    in = in2;
                }
            } else {
                return;
            }
            Log.e(TAG, "Failed to read lines in last_install", e);
        } catch (Throwable th6) {
            th = th6;
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable th42) {
                    if (th2 == null) {
                        th2 = th42;
                    } else if (th2 != th42) {
                        th2.addSuppressed(th42);
                    }
                }
            }
            if (th2 == null) {
                try {
                    throw th2;
                } catch (IOException e42) {
                    e = e42;
                }
            } else {
                throw th;
            }
        }
    }

    public static String handleAftermath(Context context) {
        String log = null;
        try {
            log = FileUtils.readTextFile(LOG_FILE, Color.RED, "...\n");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No recovery log file");
        } catch (IOException e2) {
            Log.e(TAG, "Error reading recovery log", e2);
        }
        if (log != null) {
            parseLastInstallLog(context);
        }
        boolean reservePackage = BLOCK_MAP_FILE.exists();
        if (!reservePackage && UNCRYPT_PACKAGE_FILE.exists()) {
            String filename = null;
            try {
                filename = FileUtils.readTextFile(UNCRYPT_PACKAGE_FILE, 0, null);
            } catch (IOException e22) {
                Log.e(TAG, "Error reading uncrypt file", e22);
            }
            if (filename != null && filename.startsWith("/data")) {
                if (UNCRYPT_PACKAGE_FILE.delete()) {
                    Log.i(TAG, "Deleted: " + filename);
                } else {
                    Log.e(TAG, "Can't delete: " + filename);
                }
            }
        }
        String[] names = RECOVERY_DIR.list();
        int i = 0;
        while (names != null && i < names.length) {
            if (!(names[i].startsWith(LAST_PREFIX) || ((reservePackage && names[i].equals(BLOCK_MAP_FILE.getName())) || (reservePackage && names[i].equals(UNCRYPT_PACKAGE_FILE.getName()))))) {
                recursiveDelete(new File(RECOVERY_DIR, names[i]));
            }
            i++;
        }
        return log;
    }

    private static void recursiveDelete(File name) {
        if (name.isDirectory()) {
            String[] files = name.list();
            int i = 0;
            while (files != null && i < files.length) {
                recursiveDelete(new File(name, files[i]));
                i++;
            }
        }
        if (name.delete()) {
            Log.i(TAG, "Deleted: " + name);
        } else {
            Log.e(TAG, "Can't delete: " + name);
        }
    }

    private boolean uncrypt(String packageFile, IRecoverySystemProgressListener listener) {
        try {
            return this.mService.uncrypt(packageFile, listener);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean setupBcb(String command) {
        try {
            return this.mService.setupBcb(command);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean clearBcb() {
        try {
            return this.mService.clearBcb();
        } catch (RemoteException e) {
            return false;
        }
    }

    private void rebootRecoveryWithCommand(String command) {
        try {
            this.mService.rebootRecoveryWithCommand(command);
        } catch (RemoteException e) {
        }
    }

    private static String sanitizeArg(String arg) {
        return arg.replace(0, '?').replace(10, '?');
    }

    public RecoverySystem() {
        this.mService = null;
    }

    public RecoverySystem(IRecoverySystem service) {
        this.mService = service;
    }
}
