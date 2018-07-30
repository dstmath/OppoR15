package com.android.server.backup.fullbackup;

import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.utils.PasswordUtils;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DeflaterOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class PerformAdbBackupTask extends FullBackupTask implements BackupRestoreTask {
    private RefactoredBackupManagerService backupManagerService;
    boolean mAllApps;
    FullBackupEngine mBackupEngine;
    boolean mCompress;
    private final int mCurrentOpToken;
    String mCurrentPassword;
    PackageInfo mCurrentTarget;
    DeflaterOutputStream mDeflater;
    boolean mDoWidgets;
    String mEncryptPassword;
    boolean mIncludeApks;
    boolean mIncludeObbs;
    boolean mIncludeShared;
    boolean mIncludeSystem;
    boolean mKeyValue;
    final AtomicBoolean mLatch;
    ParcelFileDescriptor mOutputFile;
    ArrayList<String> mPackages;

    public PerformAdbBackupTask(RefactoredBackupManagerService backupManagerService, ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, String curPassword, String encryptPassword, boolean doAllApps, boolean doSystem, boolean doCompress, boolean doKeyValue, String[] packages, AtomicBoolean latch) {
        ArrayList arrayList;
        super(observer);
        this.backupManagerService = backupManagerService;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mLatch = latch;
        this.mOutputFile = fd;
        this.mIncludeApks = includeApks;
        this.mIncludeObbs = includeObbs;
        this.mIncludeShared = includeShared;
        this.mDoWidgets = doWidgets;
        this.mAllApps = doAllApps;
        this.mIncludeSystem = doSystem;
        if (packages == null) {
            arrayList = new ArrayList();
        } else {
            arrayList = new ArrayList(Arrays.asList(packages));
        }
        this.mPackages = arrayList;
        this.mCurrentPassword = curPassword;
        if (encryptPassword == null || "".equals(encryptPassword)) {
            this.mEncryptPassword = curPassword;
        } else {
            this.mEncryptPassword = encryptPassword;
        }
        this.mCompress = doCompress;
        this.mKeyValue = doKeyValue;
    }

    void addPackagesToSet(TreeMap<String, PackageInfo> set, List<String> pkgNames) {
        for (String pkgName : pkgNames) {
            if (!set.containsKey(pkgName)) {
                try {
                    set.put(pkgName, this.backupManagerService.getPackageManager().getPackageInfo(pkgName, 64));
                } catch (NameNotFoundException e) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Unknown package " + pkgName + ", skipping");
                }
            }
        }
    }

    private OutputStream emitAesBackupHeader(StringBuilder headerbuf, OutputStream ofstream) throws Exception {
        byte[] newUserSalt = this.backupManagerService.randomBytes(512);
        SecretKey userKey = PasswordUtils.buildPasswordKey(BackupPasswordManager.PBKDF_CURRENT, this.mEncryptPassword, newUserSalt, 10000);
        byte[] masterPw = new byte[32];
        this.backupManagerService.getRng().nextBytes(masterPw);
        byte[] checksumSalt = this.backupManagerService.randomBytes(512);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec masterKeySpec = new SecretKeySpec(masterPw, "AES");
        c.init(1, masterKeySpec);
        OutputStream finalOutput = new CipherOutputStream(ofstream, c);
        headerbuf.append(PasswordUtils.ENCRYPTION_ALGORITHM_NAME);
        headerbuf.append(10);
        headerbuf.append(PasswordUtils.byteArrayToHex(newUserSalt));
        headerbuf.append(10);
        headerbuf.append(PasswordUtils.byteArrayToHex(checksumSalt));
        headerbuf.append(10);
        headerbuf.append(10000);
        headerbuf.append(10);
        Cipher mkC = Cipher.getInstance("AES/CBC/PKCS5Padding");
        mkC.init(1, userKey);
        headerbuf.append(PasswordUtils.byteArrayToHex(mkC.getIV()));
        headerbuf.append(10);
        byte[] IV = c.getIV();
        byte[] mk = masterKeySpec.getEncoded();
        byte[] checksum = PasswordUtils.makeKeyChecksum(BackupPasswordManager.PBKDF_CURRENT, masterKeySpec.getEncoded(), checksumSalt, 10000);
        ByteArrayOutputStream blob = new ByteArrayOutputStream(((IV.length + mk.length) + checksum.length) + 3);
        DataOutputStream mkOut = new DataOutputStream(blob);
        mkOut.writeByte(IV.length);
        mkOut.write(IV);
        mkOut.writeByte(mk.length);
        mkOut.write(mk);
        mkOut.writeByte(checksum.length);
        mkOut.write(checksum);
        mkOut.flush();
        headerbuf.append(PasswordUtils.byteArrayToHex(mkC.doFinal(blob.toByteArray())));
        headerbuf.append(10);
        return finalOutput;
    }

    private void finalizeBackup(OutputStream out) {
        try {
            out.write(new byte[1024]);
        } catch (IOException e) {
            Slog.w(RefactoredBackupManagerService.TAG, "Error attempting to finalize backup stream");
        }
    }

    public void run() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r25_2 'finalOutput' java.io.OutputStream) in PHI: PHI: (r25_3 'finalOutput' java.io.OutputStream) = (r25_2 'finalOutput' java.io.OutputStream), (r25_4 'finalOutput' java.io.OutputStream) binds: {(r25_2 'finalOutput' java.io.OutputStream)=B:92:0x0295, (r25_4 'finalOutput' java.io.OutputStream)=B:220:0x0602}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r40 = this;
        r0 = r40;
        r5 = r0.mKeyValue;
        if (r5 == 0) goto L_0x0082;
    L_0x0006:
        r30 = ", including key-value backups";
    L_0x0009:
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "--- Performing adb backup";
        r6 = r6.append(r8);
        r0 = r30;
        r6 = r6.append(r0);
        r8 = " ---";
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.i(r5, r6);
        r38 = new java.util.TreeMap;
        r38.<init>();
        r35 = new com.android.server.backup.fullbackup.FullBackupObbConnection;
        r0 = r40;
        r5 = r0.backupManagerService;
        r0 = r35;
        r0.<init>(r5);
        r35.establish();
        r40.sendStartBackup();
        r0 = r40;
        r5 = r0.mAllApps;
        if (r5 == 0) goto L_0x0086;
    L_0x0048:
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getPackageManager();
        r6 = 64;
        r17 = r5.getInstalledPackages(r6);
        r29 = 0;
    L_0x0058:
        r5 = r17.size();
        r0 = r29;
        if (r0 >= r5) goto L_0x0086;
    L_0x0060:
        r0 = r17;
        r1 = r29;
        r9 = r0.get(r1);
        r9 = (android.content.pm.PackageInfo) r9;
        r0 = r40;
        r5 = r0.mIncludeSystem;
        if (r5 != 0) goto L_0x0078;
    L_0x0070:
        r5 = r9.applicationInfo;
        r5 = r5.flags;
        r5 = r5 & 1;
        if (r5 != 0) goto L_0x007f;
    L_0x0078:
        r5 = r9.packageName;
        r0 = r38;
        r0.put(r5, r9);
    L_0x007f:
        r29 = r29 + 1;
        goto L_0x0058;
    L_0x0082:
        r30 = "";
        goto L_0x0009;
    L_0x0086:
        r0 = r40;
        r5 = r0.mDoWidgets;
        if (r5 == 0) goto L_0x009c;
    L_0x008c:
        r5 = 0;
        r39 = com.android.server.AppWidgetBackupBridge.getWidgetParticipants(r5);
        if (r39 == 0) goto L_0x009c;
    L_0x0093:
        r0 = r40;
        r1 = r38;
        r2 = r39;
        r0.addPackagesToSet(r1, r2);
    L_0x009c:
        r0 = r40;
        r5 = r0.mPackages;
        if (r5 == 0) goto L_0x00ad;
    L_0x00a2:
        r0 = r40;
        r5 = r0.mPackages;
        r0 = r40;
        r1 = r38;
        r0.addPackagesToSet(r1, r5);
    L_0x00ad:
        r33 = new java.util.ArrayList;
        r33.<init>();
        r5 = r38.entrySet();
        r32 = r5.iterator();
    L_0x00ba:
        r5 = r32.hasNext();
        if (r5 == 0) goto L_0x0135;
    L_0x00c0:
        r5 = r32.next();
        r5 = (java.util.Map.Entry) r5;
        r9 = r5.getValue();
        r9 = (android.content.pm.PackageInfo) r9;
        r5 = r9.applicationInfo;
        r5 = com.android.server.backup.utils.AppBackupUtils.appIsEligibleForBackup(r5);
        if (r5 == 0) goto L_0x00dc;
    L_0x00d4:
        r5 = r9.applicationInfo;
        r5 = com.android.server.backup.utils.AppBackupUtils.appIsStopped(r5);
        if (r5 == 0) goto L_0x0103;
    L_0x00dc:
        r32.remove();
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "Package ";
        r6 = r6.append(r8);
        r8 = r9.packageName;
        r6 = r6.append(r8);
        r8 = " is not eligible for backup, removing.";
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.i(r5, r6);
        goto L_0x00ba;
    L_0x0103:
        r5 = com.android.server.backup.utils.AppBackupUtils.appIsKeyValueOnly(r9);
        if (r5 == 0) goto L_0x00ba;
    L_0x0109:
        r32.remove();
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "Package ";
        r6 = r6.append(r8);
        r8 = r9.packageName;
        r6 = r6.append(r8);
        r8 = " is key-value.";
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.i(r5, r6);
        r0 = r33;
        r0.add(r9);
        goto L_0x00ba;
    L_0x0135:
        r18 = new java.util.ArrayList;
        r5 = r38.values();
        r0 = r18;
        r0.<init>(r5);
        r37 = new java.io.FileOutputStream;
        r0 = r40;
        r5 = r0.mOutputFile;
        r5 = r5.getFileDescriptor();
        r0 = r37;
        r0.<init>(r5);
        r7 = 0;
        r9 = 0;
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mEncryptPassword;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x01b1;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0157:
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mEncryptPassword;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r5.length();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 <= 0) goto L_0x01b1;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0161:
        r24 = 1;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0163:
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r5.deviceIsEncrypted();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x01d7;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x016d:
        r5 = r24 ^ 1;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x01d7;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0171:
        r5 = "BackupManagerService";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = "Unencrypted backup of encrypted device; aborting";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        android.util.Slog.e(r5, r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ IOException -> 0x01b4 }
        r5 = r0.mOutputFile;	 Catch:{ IOException -> 0x01b4 }
        r5.close();	 Catch:{ IOException -> 0x01b4 }
    L_0x0181:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x01d4 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x01d4 }
        r8 = 1;	 Catch:{ all -> 0x01d4 }
        r5.set(r8);	 Catch:{ all -> 0x01d4 }
        r0 = r40;	 Catch:{ all -> 0x01d4 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x01d4 }
        r5.notifyAll();	 Catch:{ all -> 0x01d4 }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r5 = "BackupManagerService";
        r6 = "Full backup pass complete.";
        android.util.Slog.d(r5, r6);
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
        return;
    L_0x01b1:
        r24 = 0;
        goto L_0x0163;
    L_0x01b4:
        r22 = move-exception;
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "IO error closing adb backup file: ";
        r6 = r6.append(r8);
        r8 = r22.getMessage();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.e(r5, r6);
        goto L_0x0181;
    L_0x01d4:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x01d7:
        r25 = r37;
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r0.mCurrentPassword;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r5.backupPasswordMatches(r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 != 0) goto L_0x024a;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x01e7:
        r5 = "BackupManagerService";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = "Backup password mismatch; aborting";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        android.util.Slog.w(r5, r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ IOException -> 0x0227 }
        r5 = r0.mOutputFile;	 Catch:{ IOException -> 0x0227 }
        r5.close();	 Catch:{ IOException -> 0x0227 }
    L_0x01f7:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x0247 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x0247 }
        r8 = 1;	 Catch:{ all -> 0x0247 }
        r5.set(r8);	 Catch:{ all -> 0x0247 }
        r0 = r40;	 Catch:{ all -> 0x0247 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x0247 }
        r5.notifyAll();	 Catch:{ all -> 0x0247 }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r5 = "BackupManagerService";
        r6 = "Full backup pass complete.";
        android.util.Slog.d(r5, r6);
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
        return;
    L_0x0227:
        r22 = move-exception;
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "IO error closing adb backup file: ";
        r6 = r6.append(r8);
        r8 = r22.getMessage();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.e(r5, r6);
        goto L_0x01f7;
    L_0x0247:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x024a:
        r28 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r28;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.<init>(r5);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = "ANDROID BACKUP\n";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r28;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.append(r5);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = 5;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r28;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.append(r5);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mCompress;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x03ac;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0267:
        r5 = "\n1\n";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x026a:
        r0 = r28;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.append(r5);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r24 == 0) goto L_0x03b1;
    L_0x0271:
        r0 = r40;	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r1 = r28;	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r2 = r37;	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r25 = r0.emitAesBackupHeader(r1, r2);	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r26 = r25;
    L_0x027d:
        r5 = r28.toString();	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r6 = "UTF-8";	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r27 = r5.getBytes(r6);	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0 = r37;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r1 = r27;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0.write(r1);	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0 = r40;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r5 = r0.mCompress;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        if (r5 == 0) goto L_0x0602;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
    L_0x0295:
        r19 = new java.util.zip.Deflater;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r5 = 9;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0 = r19;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0.<init>(r5);	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r25 = new java.util.zip.DeflaterOutputStream;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r5 = 1;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0 = r25;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r1 = r26;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r2 = r19;	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
        r0.<init>(r1, r2, r5);	 Catch:{ Exception -> 0x05fd, RemoteException -> 0x0363 }
    L_0x02aa:
        r7 = r25;
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mIncludeShared;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x02c7;
    L_0x02b2:
        r0 = r40;	 Catch:{ NameNotFoundException -> 0x0423 }
        r5 = r0.backupManagerService;	 Catch:{ NameNotFoundException -> 0x0423 }
        r5 = r5.getPackageManager();	 Catch:{ NameNotFoundException -> 0x0423 }
        r6 = "com.android.sharedstoragebackup";	 Catch:{ NameNotFoundException -> 0x0423 }
        r8 = 0;	 Catch:{ NameNotFoundException -> 0x0423 }
        r9 = r5.getPackageInfo(r6, r8);	 Catch:{ NameNotFoundException -> 0x0423 }
        r0 = r18;	 Catch:{ NameNotFoundException -> 0x0423 }
        r0.add(r9);	 Catch:{ NameNotFoundException -> 0x0423 }
    L_0x02c7:
        r4 = r18.size();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r29 = 0;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x02cd:
        r0 = r29;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r0 >= r4) goto L_0x0483;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x02d1:
        r0 = r18;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r1 = r29;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.get(r1);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r5;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = (android.content.pm.PackageInfo) r0;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r9 = r0;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = "BackupManagerService";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6.<init>();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = "--- Performing full backup for package ";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = r9.packageName;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = " ---";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.toString();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        android.util.Slog.i(r5, r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r9.packageName;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = "com.android.sharedstoragebackup";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r31 = r5.equals(r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = new com.android.server.backup.fullbackup.FullBackupEngine;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r10 = r0.mIncludeApks;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r14 = r0.mCurrentOpToken;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = 0;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r12 = 9223372036854775807; // 0x7fffffffffffffff float:NaN double:NaN;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r11 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5.<init>(r6, r7, r8, r9, r10, r11, r12, r14);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.mBackupEngine = r5;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r31 == 0) goto L_0x047b;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0328:
        r5 = "Shared storage";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x032b:
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.sendOnBackupPackage(r5);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.mCurrentTarget = r9;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mBackupEngine;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5.backupOnePackage();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mIncludeObbs;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x047f;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0341:
        r0 = r35;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r36 = r0.backupObbs(r9, r7);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r36 != 0) goto L_0x047f;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0349:
        r5 = new java.lang.RuntimeException;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6.<init>();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = "Failure writing OBB stack for ";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r9);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.toString();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5.<init>(r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        throw r5;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0363:
        r21 = move-exception;
        r5 = "BackupManagerService";	 Catch:{ all -> 0x04e9 }
        r6 = "App died during full backup";	 Catch:{ all -> 0x04e9 }
        android.util.Slog.e(r5, r6);	 Catch:{ all -> 0x04e9 }
        if (r7 == 0) goto L_0x0375;
    L_0x036f:
        r7.flush();	 Catch:{ IOException -> 0x05b5 }
        r7.close();	 Catch:{ IOException -> 0x05b5 }
    L_0x0375:
        r0 = r40;	 Catch:{ IOException -> 0x05b5 }
        r5 = r0.mOutputFile;	 Catch:{ IOException -> 0x05b5 }
        r5.close();	 Catch:{ IOException -> 0x05b5 }
    L_0x037c:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x05d6 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x05d6 }
        r8 = 1;	 Catch:{ all -> 0x05d6 }
        r5.set(r8);	 Catch:{ all -> 0x05d6 }
        r0 = r40;	 Catch:{ all -> 0x05d6 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x05d6 }
        r5.notifyAll();	 Catch:{ all -> 0x05d6 }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r5 = "BackupManagerService";
        r6 = "Full backup pass complete.";
        android.util.Slog.d(r5, r6);
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
    L_0x03ab:
        return;
    L_0x03ac:
        r5 = "\n0\n";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        goto L_0x026a;
    L_0x03b1:
        r5 = "none\n";	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r0 = r28;	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r0.append(r5);	 Catch:{ Exception -> 0x03bd, RemoteException -> 0x0363 }
        r26 = r25;
        goto L_0x027d;
    L_0x03bd:
        r23 = move-exception;
    L_0x03be:
        r5 = "BackupManagerService";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = "Unable to emit archive header";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r23;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        android.util.Slog.e(r5, r6, r0);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ IOException -> 0x0400 }
        r5 = r0.mOutputFile;	 Catch:{ IOException -> 0x0400 }
        r5.close();	 Catch:{ IOException -> 0x0400 }
    L_0x03d0:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x0420 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x0420 }
        r8 = 1;	 Catch:{ all -> 0x0420 }
        r5.set(r8);	 Catch:{ all -> 0x0420 }
        r0 = r40;	 Catch:{ all -> 0x0420 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x0420 }
        r5.notifyAll();	 Catch:{ all -> 0x0420 }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r5 = "BackupManagerService";
        r6 = "Full backup pass complete.";
        android.util.Slog.d(r5, r6);
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
        return;
    L_0x0400:
        r22 = move-exception;
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "IO error closing adb backup file: ";
        r6 = r6.append(r8);
        r8 = r22.getMessage();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.e(r5, r6);
        goto L_0x03d0;
    L_0x0420:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x0423:
        r20 = move-exception;
        r5 = "BackupManagerService";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = "Unable to find shared-storage backup handler";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        android.util.Slog.e(r5, r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        goto L_0x02c7;
    L_0x042f:
        r23 = move-exception;
        r5 = "BackupManagerService";	 Catch:{ all -> 0x04e9 }
        r6 = "Internal exception during full backup";	 Catch:{ all -> 0x04e9 }
        r0 = r23;	 Catch:{ all -> 0x04e9 }
        android.util.Slog.e(r5, r6, r0);	 Catch:{ all -> 0x04e9 }
        if (r7 == 0) goto L_0x0443;
    L_0x043d:
        r7.flush();	 Catch:{ IOException -> 0x0591 }
        r7.close();	 Catch:{ IOException -> 0x0591 }
    L_0x0443:
        r0 = r40;	 Catch:{ IOException -> 0x0591 }
        r5 = r0.mOutputFile;	 Catch:{ IOException -> 0x0591 }
        r5.close();	 Catch:{ IOException -> 0x0591 }
    L_0x044a:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x05b2 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x05b2 }
        r8 = 1;	 Catch:{ all -> 0x05b2 }
        r5.set(r8);	 Catch:{ all -> 0x05b2 }
        r0 = r40;	 Catch:{ all -> 0x05b2 }
        r5 = r0.mLatch;	 Catch:{ all -> 0x05b2 }
        r5.notifyAll();	 Catch:{ all -> 0x05b2 }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r5 = "BackupManagerService";
        r6 = "Full backup pass complete.";
        android.util.Slog.d(r5, r6);
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
        goto L_0x03ab;
    L_0x047b:
        r5 = r9.packageName;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        goto L_0x032b;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x047f:
        r29 = r29 + 1;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        goto L_0x02cd;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0483:
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.mKeyValue;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x0529;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0489:
        r34 = r33.iterator();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x048d:
        r5 = r34.hasNext();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r5 == 0) goto L_0x0529;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
    L_0x0493:
        r12 = r34.next();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r12 = (android.content.pm.PackageInfo) r12;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = "BackupManagerService";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6.<init>();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = "--- Performing key-value backup for package ";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = r12.packageName;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r8 = " ---";	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.append(r8);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r6 = r6.toString();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        android.util.Slog.i(r5, r6);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r10 = new com.android.server.backup.KeyValueAdbBackupEngine;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r13 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r14 = r5.getPackageManager();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r15 = r5.getBaseStateDir();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r0.backupManagerService;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r16 = r5.getDataDir();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r11 = r7;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r10.<init>(r11, r12, r13, r14, r15, r16);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r5 = r12.packageName;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.sendOnBackupPackage(r5);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r10.backupOnePackage();	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        goto L_0x048d;
    L_0x04e9:
        r5 = move-exception;
        if (r7 == 0) goto L_0x04f2;
    L_0x04ec:
        r7.flush();	 Catch:{ IOException -> 0x05d9 }
        r7.close();	 Catch:{ IOException -> 0x05d9 }
    L_0x04f2:
        r0 = r40;	 Catch:{ IOException -> 0x05d9 }
        r6 = r0.mOutputFile;	 Catch:{ IOException -> 0x05d9 }
        r6.close();	 Catch:{ IOException -> 0x05d9 }
    L_0x04f9:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x05fa }
        r8 = r0.mLatch;	 Catch:{ all -> 0x05fa }
        r11 = 1;	 Catch:{ all -> 0x05fa }
        r8.set(r11);	 Catch:{ all -> 0x05fa }
        r0 = r40;	 Catch:{ all -> 0x05fa }
        r8 = r0.mLatch;	 Catch:{ all -> 0x05fa }
        r8.notifyAll();	 Catch:{ all -> 0x05fa }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r6 = "BackupManagerService";
        r8 = "Full backup pass complete.";
        android.util.Slog.d(r6, r8);
        r0 = r40;
        r6 = r0.backupManagerService;
        r6 = r6.getWakelock();
        r6.release();
        throw r5;
    L_0x0529:
        r0 = r40;	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        r0.finalizeBackup(r7);	 Catch:{ RemoteException -> 0x0363, Exception -> 0x042f }
        if (r7 == 0) goto L_0x0536;
    L_0x0530:
        r7.flush();	 Catch:{ IOException -> 0x056e }
        r7.close();	 Catch:{ IOException -> 0x056e }
    L_0x0536:
        r0 = r40;	 Catch:{ IOException -> 0x056e }
        r5 = r0.mOutputFile;	 Catch:{ IOException -> 0x056e }
        r5.close();	 Catch:{ IOException -> 0x056e }
    L_0x053d:
        r0 = r40;
        r6 = r0.mLatch;
        monitor-enter(r6);
        r0 = r40;	 Catch:{ all -> 0x058e }
        r5 = r0.mLatch;	 Catch:{ all -> 0x058e }
        r8 = 1;	 Catch:{ all -> 0x058e }
        r5.set(r8);	 Catch:{ all -> 0x058e }
        r0 = r40;	 Catch:{ all -> 0x058e }
        r5 = r0.mLatch;	 Catch:{ all -> 0x058e }
        r5.notifyAll();	 Catch:{ all -> 0x058e }
        monitor-exit(r6);
        r40.sendEndBackup();
        r35.tearDown();
        r5 = "BackupManagerService";
        r6 = "Full backup pass complete.";
        android.util.Slog.d(r5, r6);
        r0 = r40;
        r5 = r0.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
        goto L_0x03ab;
    L_0x056e:
        r22 = move-exception;
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "IO error closing adb backup file: ";
        r6 = r6.append(r8);
        r8 = r22.getMessage();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.e(r5, r6);
        goto L_0x053d;
    L_0x058e:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x0591:
        r22 = move-exception;
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "IO error closing adb backup file: ";
        r6 = r6.append(r8);
        r8 = r22.getMessage();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.e(r5, r6);
        goto L_0x044a;
    L_0x05b2:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x05b5:
        r22 = move-exception;
        r5 = "BackupManagerService";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "IO error closing adb backup file: ";
        r6 = r6.append(r8);
        r8 = r22.getMessage();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.e(r5, r6);
        goto L_0x037c;
    L_0x05d6:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x05d9:
        r22 = move-exception;
        r6 = "BackupManagerService";
        r8 = new java.lang.StringBuilder;
        r8.<init>();
        r11 = "IO error closing adb backup file: ";
        r8 = r8.append(r11);
        r11 = r22.getMessage();
        r8 = r8.append(r11);
        r8 = r8.toString();
        android.util.Slog.e(r6, r8);
        goto L_0x04f9;
    L_0x05fa:
        r5 = move-exception;
        monitor-exit(r6);
        throw r5;
    L_0x05fd:
        r23 = move-exception;
        r25 = r26;
        goto L_0x03be;
    L_0x0602:
        r25 = r26;
        goto L_0x02aa;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.backup.fullbackup.PerformAdbBackupTask.run():void");
    }

    public void execute() {
    }

    public void operationComplete(long result) {
    }

    public void handleCancel(boolean cancelAll) {
        PackageInfo target = this.mCurrentTarget;
        Slog.w(RefactoredBackupManagerService.TAG, "adb backup cancel of " + target);
        if (target != null) {
            this.backupManagerService.tearDownAgentAndKill(this.mCurrentTarget.applicationInfo);
        }
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }
}
