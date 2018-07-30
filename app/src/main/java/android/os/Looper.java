package android.os;

import android.util.Printer;
import android.util.proto.ProtoOutputStream;

public final class Looper {
    private static final String DEBUG_TAG = "ANR_LOG";
    private static final long DISPATCH_TIMEOUT = 1500;
    private static final String TAG = "Looper";
    private static Looper sMainLooper;
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal();
    private Printer mLogging;
    final MessageQueue mQueue;
    private long mSlowDispatchThresholdMs;
    final Thread mThread = Thread.currentThread();
    private long mTraceTag;

    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }

    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    public static Looper getMainLooper() {
        Looper looper;
        synchronized (Looper.class) {
            looper = sMainLooper;
        }
        return looper;
    }

    public static void loop() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r22_0 'pw' java.io.PrintWriter) in PHI: PHI: (r22_1 'pw' java.io.PrintWriter) = (r22_0 'pw' java.io.PrintWriter), (r22_3 'pw' java.io.PrintWriter) binds: {(r22_0 'pw' java.io.PrintWriter)=B:4:0x0013, (r22_3 'pw' java.io.PrintWriter)=B:53:0x0309}
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
        r17 = myLooper();
        if (r17 != 0) goto L_0x0013;
    L_0x0006:
        r37 = new java.lang.RuntimeException;
        r40 = "No Looper; Looper.prepare() wasn't called on this thread.";
        r0 = r37;
        r1 = r40;
        r0.<init>(r1);
        throw r37;
    L_0x0013:
        r0 = r17;
        r0 = r0.mQueue;
        r24 = r0;
        r37 = java.lang.Thread.currentThread();
        r8 = r37.getName();
        r37 = "main";
        r0 = r37;
        r16 = r0.equals(r8);
        r25 = new java.io.StringWriter;
        r25.<init>();
        r22 = new com.android.internal.util.FastPrintWriter;
        r37 = 0;
        r40 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
        r0 = r22;
        r1 = r25;
        r2 = r37;
        r3 = r40;
        r0.<init>(r1, r2, r3);
        android.os.Binder.clearCallingIdentity();
        r14 = android.os.Binder.clearCallingIdentity();
    L_0x0047:
        r18 = r24.next();
        if (r18 != 0) goto L_0x004e;
    L_0x004d:
        return;
    L_0x004e:
        r0 = r17;
        r11 = r0.mLogging;
        if (r11 == 0) goto L_0x00ad;
    L_0x0054:
        r37 = new java.lang.StringBuilder;
        r37.<init>();
        r40 = ">>>>> Dispatching to ";
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r0 = r18;
        r0 = r0.target;
        r40 = r0;
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r40 = " ";
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r0 = r18;
        r0 = r0.callback;
        r40 = r0;
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r40 = ": ";
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r0 = r18;
        r0 = r0.what;
        r40 = r0;
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r37 = r37.toString();
        r0 = r37;
        r11.println(r0);
    L_0x00ad:
        r0 = r17;
        r0 = r0.mSlowDispatchThresholdMs;
        r26 = r0;
        r0 = r17;
        r0 = r0.mTraceTag;
        r38 = r0;
        r40 = 0;
        r37 = (r38 > r40 ? 1 : (r38 == r40 ? 0 : -1));
        if (r37 == 0) goto L_0x00da;
    L_0x00bf:
        r37 = android.os.Trace.isTagEnabled(r38);
        if (r37 == 0) goto L_0x00da;
    L_0x00c5:
        r0 = r18;
        r0 = r0.target;
        r37 = r0;
        r0 = r37;
        r1 = r18;
        r37 = r0.getTraceName(r1);
        r0 = r38;
        r2 = r37;
        android.os.Trace.traceBegin(r0, r2);
    L_0x00da:
        r34 = android.os.SystemClock.uptimeMillis();
        r40 = 0;
        r37 = (r26 > r40 ? 1 : (r26 == r40 ? 0 : -1));
        if (r37 != 0) goto L_0x030e;
    L_0x00e4:
        r28 = 0;
    L_0x00e6:
        r0 = r18;	 Catch:{ all -> 0x031a }
        r0 = r0.target;	 Catch:{ all -> 0x031a }
        r37 = r0;	 Catch:{ all -> 0x031a }
        r0 = r37;	 Catch:{ all -> 0x031a }
        r1 = r18;	 Catch:{ all -> 0x031a }
        r0.dispatchMessage(r1);	 Catch:{ all -> 0x031a }
        r40 = 0;
        r37 = (r26 > r40 ? 1 : (r26 == r40 ? 0 : -1));
        if (r37 != 0) goto L_0x0314;
    L_0x00f9:
        r12 = 0;
    L_0x00fb:
        r40 = 0;
        r37 = (r38 > r40 ? 1 : (r38 == r40 ? 0 : -1));
        if (r37 == 0) goto L_0x0104;
    L_0x0101:
        android.os.Trace.traceEnd(r38);
    L_0x0104:
        r40 = 0;
        r37 = (r26 > r40 ? 1 : (r26 == r40 ? 0 : -1));
        if (r37 <= 0) goto L_0x0178;
    L_0x010a:
        r32 = r12 - r28;
        r37 = (r32 > r26 ? 1 : (r32 == r26 ? 0 : -1));
        if (r37 <= 0) goto L_0x0178;
    L_0x0110:
        r37 = "Looper";
        r40 = new java.lang.StringBuilder;
        r40.<init>();
        r41 = "Dispatch took ";
        r40 = r40.append(r41);
        r0 = r40;
        r1 = r32;
        r40 = r0.append(r1);
        r41 = "ms on ";
        r40 = r40.append(r41);
        r41 = java.lang.Thread.currentThread();
        r41 = r41.getName();
        r40 = r40.append(r41);
        r41 = ", h=";
        r40 = r40.append(r41);
        r0 = r18;
        r0 = r0.target;
        r41 = r0;
        r40 = r40.append(r41);
        r41 = " cb=";
        r40 = r40.append(r41);
        r0 = r18;
        r0 = r0.callback;
        r41 = r0;
        r40 = r40.append(r41);
        r41 = " msg=";
        r40 = r40.append(r41);
        r0 = r18;
        r0 = r0.what;
        r41 = r0;
        r40 = r40.append(r41);
        r40 = r40.toString();
        r0 = r37;
        r1 = r40;
        android.util.Slog.w(r0, r1);
    L_0x0178:
        if (r16 == 0) goto L_0x0253;
    L_0x017a:
        r40 = android.os.SystemClock.uptimeMillis();
        r6 = r40 - r34;
        r40 = 1500; // 0x5dc float:2.102E-42 double:7.41E-321;
        r37 = (r6 > r40 ? 1 : (r6 == r40 ? 0 : -1));
        if (r37 < 0) goto L_0x0253;
    L_0x0186:
        r37 = new java.lang.StringBuilder;	 Catch:{ NullPointerException -> 0x0397 }
        r37.<init>();	 Catch:{ NullPointerException -> 0x0397 }
        r40 = "Blocked msg = ";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r40 = r6 + r34;	 Catch:{ NullPointerException -> 0x0397 }
        r42 = 1;	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r18;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r3 = r42;	 Catch:{ NullPointerException -> 0x0397 }
        r40 = r0.toStringLite(r1, r3);	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r40 = " , cost  = ";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r6);	 Catch:{ NullPointerException -> 0x0397 }
        r40 = " ms";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r31 = r37.toString();	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r22;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r31;	 Catch:{ NullPointerException -> 0x0397 }
        r0.println(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r37 = "ANR_LOG";	 Catch:{ NullPointerException -> 0x0397 }
        r40 = ">>> msg's executing time is too long";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        android.util.Log.e(r0, r1);	 Catch:{ NullPointerException -> 0x0397 }
        r37 = "ANR_LOG";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r31;	 Catch:{ NullPointerException -> 0x0397 }
        android.util.Log.e(r0, r1);	 Catch:{ NullPointerException -> 0x0397 }
        r19 = 0;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = "ANR_LOG";	 Catch:{ NullPointerException -> 0x0397 }
        r40 = ">>>Current msg List is:";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        android.util.Log.e(r0, r1);	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r24;	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r0.mMessages;	 Catch:{ NullPointerException -> 0x0397 }
        r36 = r0;	 Catch:{ NullPointerException -> 0x0397 }
    L_0x01ff:
        if (r36 == 0) goto L_0x020b;	 Catch:{ NullPointerException -> 0x0397 }
    L_0x0201:
        r19 = r19 + 1;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = 10;	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r19;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        if (r0 <= r1) goto L_0x0325;	 Catch:{ NullPointerException -> 0x0397 }
    L_0x020b:
        r37 = "ANR_LOG";	 Catch:{ NullPointerException -> 0x0397 }
        r40 = ">>>CURRENT MSG DUMP OVER<<<";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        android.util.Log.e(r0, r1);	 Catch:{ NullPointerException -> 0x0397 }
        r22.flush();	 Catch:{ NullPointerException -> 0x0397 }
        r37 = android.app.ActivityManagerNative.getDefault();	 Catch:{ RemoteException -> 0x0386 }
        r40 = "Looper";	 Catch:{ RemoteException -> 0x0386 }
        r41 = android.app.ActivityThread.currentPackageName();	 Catch:{ RemoteException -> 0x0386 }
        r42 = r25.toString();	 Catch:{ RemoteException -> 0x0386 }
        r43 = 0;	 Catch:{ RemoteException -> 0x0386 }
        r0 = r37;	 Catch:{ RemoteException -> 0x0386 }
        r1 = r40;	 Catch:{ RemoteException -> 0x0386 }
        r2 = r41;	 Catch:{ RemoteException -> 0x0386 }
        r3 = r42;	 Catch:{ RemoteException -> 0x0386 }
        r4 = r43;	 Catch:{ RemoteException -> 0x0386 }
        r0.reportJunkFromApp(r1, r2, r3, r4);	 Catch:{ RemoteException -> 0x0386 }
    L_0x0239:
        r30 = new java.io.StringWriter;	 Catch:{ NullPointerException -> 0x0397 }
        r30.<init>();	 Catch:{ NullPointerException -> 0x0397 }
        r23 = new com.android.internal.util.FastPrintWriter;	 Catch:{ NullPointerException -> 0x03ba }
        r37 = 0;	 Catch:{ NullPointerException -> 0x03ba }
        r40 = 128; // 0x80 float:1.794E-43 double:6.32E-322;	 Catch:{ NullPointerException -> 0x03ba }
        r0 = r23;	 Catch:{ NullPointerException -> 0x03ba }
        r1 = r30;	 Catch:{ NullPointerException -> 0x03ba }
        r2 = r37;	 Catch:{ NullPointerException -> 0x03ba }
        r3 = r40;	 Catch:{ NullPointerException -> 0x03ba }
        r0.<init>(r1, r2, r3);	 Catch:{ NullPointerException -> 0x03ba }
        r22 = r23;
        r25 = r30;
    L_0x0253:
        if (r11 == 0) goto L_0x0295;
    L_0x0255:
        r37 = new java.lang.StringBuilder;
        r37.<init>();
        r40 = "<<<<< Finished to ";
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r0 = r18;
        r0 = r0.target;
        r40 = r0;
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r40 = " ";
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r0 = r18;
        r0 = r0.callback;
        r40 = r0;
        r0 = r37;
        r1 = r40;
        r37 = r0.append(r1);
        r37 = r37.toString();
        r0 = r37;
        r11.println(r0);
    L_0x0295:
        r20 = android.os.Binder.clearCallingIdentity();
        r37 = (r14 > r20 ? 1 : (r14 == r20 ? 0 : -1));
        if (r37 == 0) goto L_0x0309;
    L_0x029d:
        r37 = "Looper";
        r40 = new java.lang.StringBuilder;
        r40.<init>();
        r41 = "Thread identity changed from 0x";
        r40 = r40.append(r41);
        r41 = java.lang.Long.toHexString(r14);
        r40 = r40.append(r41);
        r41 = " to 0x";
        r40 = r40.append(r41);
        r41 = java.lang.Long.toHexString(r20);
        r40 = r40.append(r41);
        r41 = " while dispatching to ";
        r40 = r40.append(r41);
        r0 = r18;
        r0 = r0.target;
        r41 = r0;
        r41 = r41.getClass();
        r41 = r41.getName();
        r40 = r40.append(r41);
        r41 = " ";
        r40 = r40.append(r41);
        r0 = r18;
        r0 = r0.callback;
        r41 = r0;
        r40 = r40.append(r41);
        r41 = " what=";
        r40 = r40.append(r41);
        r0 = r18;
        r0 = r0.what;
        r41 = r0;
        r40 = r40.append(r41);
        r40 = r40.toString();
        r0 = r37;
        r1 = r40;
        android.util.Log.wtf(r0, r1);
    L_0x0309:
        r18.recycleUnchecked();
        goto L_0x0047;
    L_0x030e:
        r28 = android.os.SystemClock.uptimeMillis();
        goto L_0x00e6;
    L_0x0314:
        r12 = android.os.SystemClock.uptimeMillis();	 Catch:{ all -> 0x031a }
        goto L_0x00fb;
    L_0x031a:
        r37 = move-exception;
        r40 = 0;
        r40 = (r38 > r40 ? 1 : (r38 == r40 ? 0 : -1));
        if (r40 == 0) goto L_0x0324;
    L_0x0321:
        android.os.Trace.traceEnd(r38);
    L_0x0324:
        throw r37;
    L_0x0325:
        r37 = new java.lang.StringBuilder;	 Catch:{ NullPointerException -> 0x0397 }
        r37.<init>();	 Catch:{ NullPointerException -> 0x0397 }
        r40 = "Current msg <";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r19;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r40 = "> ";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r40 = " = ";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r40 = r6 + r34;	 Catch:{ NullPointerException -> 0x0397 }
        r42 = 1;	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r36;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r3 = r42;	 Catch:{ NullPointerException -> 0x0397 }
        r40 = r0.toStringLite(r1, r3);	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = r0.append(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r31 = r37.toString();	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r22;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r31;	 Catch:{ NullPointerException -> 0x0397 }
        r0.println(r1);	 Catch:{ NullPointerException -> 0x0397 }
        r37 = "ANR_LOG";	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r31;	 Catch:{ NullPointerException -> 0x0397 }
        android.util.Log.e(r0, r1);	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r36;	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r0.next;	 Catch:{ NullPointerException -> 0x0397 }
        r36 = r0;	 Catch:{ NullPointerException -> 0x0397 }
        goto L_0x01ff;	 Catch:{ NullPointerException -> 0x0397 }
    L_0x0386:
        r9 = move-exception;	 Catch:{ NullPointerException -> 0x0397 }
        r37 = "ANR_LOG";	 Catch:{ NullPointerException -> 0x0397 }
        r40 = r9.toString();	 Catch:{ NullPointerException -> 0x0397 }
        r0 = r37;	 Catch:{ NullPointerException -> 0x0397 }
        r1 = r40;	 Catch:{ NullPointerException -> 0x0397 }
        android.util.Log.e(r0, r1);	 Catch:{ NullPointerException -> 0x0397 }
        goto L_0x0239;
    L_0x0397:
        r10 = move-exception;
    L_0x0398:
        r37 = "Looper";
        r40 = new java.lang.StringBuilder;
        r40.<init>();
        r41 = "Failure log ANR msg.";
        r40 = r40.append(r41);
        r0 = r40;
        r40 = r0.append(r10);
        r40 = r40.toString();
        r0 = r37;
        r1 = r40;
        android.util.Log.e(r0, r1);
        goto L_0x0253;
    L_0x03ba:
        r10 = move-exception;
        r25 = r30;
        goto L_0x0398;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.os.Looper.loop():void");
    }

    public static Looper myLooper() {
        return (Looper) sThreadLocal.get();
    }

    public static MessageQueue myQueue() {
        return myLooper().mQueue;
    }

    private Looper(boolean quitAllowed) {
        this.mQueue = new MessageQueue(quitAllowed);
    }

    public boolean isCurrentThread() {
        return Thread.currentThread() == this.mThread;
    }

    public void setMessageLogging(Printer printer) {
        this.mLogging = printer;
    }

    public void setTraceTag(long traceTag) {
        this.mTraceTag = traceTag;
    }

    public void setSlowDispatchThresholdMs(long slowDispatchThresholdMs) {
        this.mSlowDispatchThresholdMs = slowDispatchThresholdMs;
    }

    public void quit() {
        this.mQueue.quit(false);
    }

    public void quitSafely() {
        this.mQueue.quit(true);
    }

    public Thread getThread() {
        return this.mThread;
    }

    public MessageQueue getQueue() {
        return this.mQueue;
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + toString());
        this.mQueue.dump(pw, prefix + "  ", null);
    }

    public void dump(Printer pw, String prefix, Handler handler) {
        pw.println(prefix + toString());
        this.mQueue.dump(pw, prefix + "  ", handler);
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long looperToken = proto.start(fieldId);
        proto.write(1159641169921L, this.mThread.getName());
        proto.write(LooperProto.THREAD_ID, this.mThread.getId());
        proto.write(1112396529667L, System.identityHashCode(this));
        this.mQueue.writeToProto(proto, LooperProto.QUEUE);
        proto.end(looperToken);
    }

    public String toString() {
        return "Looper (" + this.mThread.getName() + ", tid " + this.mThread.getId() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}
