package com.android.commands.monkey;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Random;

public class MonkeySourceScript implements MonkeyEventSource {
    private static final String EVENT_KEYWORD_ACTIVITY = "LaunchActivity";
    private static final String EVENT_KEYWORD_DEVICE_WAKEUP = "DeviceWakeUp";
    private static final String EVENT_KEYWORD_DRAG = "Drag";
    private static final String EVENT_KEYWORD_END_APP_FRAMERATE_CAPTURE = "EndCaptureAppFramerate";
    private static final String EVENT_KEYWORD_END_FRAMERATE_CAPTURE = "EndCaptureFramerate";
    private static final String EVENT_KEYWORD_FLIP = "DispatchFlip";
    private static final String EVENT_KEYWORD_INPUT_STRING = "DispatchString";
    private static final String EVENT_KEYWORD_INSTRUMENTATION = "LaunchInstrumentation";
    private static final String EVENT_KEYWORD_KEY = "DispatchKey";
    private static final String EVENT_KEYWORD_KEYPRESS = "DispatchPress";
    private static final String EVENT_KEYWORD_LONGPRESS = "LongPress";
    private static final String EVENT_KEYWORD_PINCH_ZOOM = "PinchZoom";
    private static final String EVENT_KEYWORD_POINTER = "DispatchPointer";
    private static final String EVENT_KEYWORD_POWERLOG = "PowerLog";
    private static final String EVENT_KEYWORD_PRESSANDHOLD = "PressAndHold";
    private static final String EVENT_KEYWORD_PROFILE_WAIT = "ProfileWait";
    private static final String EVENT_KEYWORD_ROTATION = "RotateScreen";
    private static final String EVENT_KEYWORD_RUNCMD = "RunCmd";
    private static final String EVENT_KEYWORD_START_APP_FRAMERATE_CAPTURE = "StartCaptureAppFramerate";
    private static final String EVENT_KEYWORD_START_FRAMERATE_CAPTURE = "StartCaptureFramerate";
    private static final String EVENT_KEYWORD_TAP = "Tap";
    private static final String EVENT_KEYWORD_TRACKBALL = "DispatchTrackball";
    private static final String EVENT_KEYWORD_WAIT = "UserWait";
    private static final String EVENT_KEYWORD_WRITEPOWERLOG = "WriteLog";
    private static final String HEADER_COUNT = "count=";
    private static final String HEADER_LINE_BY_LINE = "linebyline";
    private static final String HEADER_SPEED = "speed=";
    private static int LONGPRESS_WAIT_TIME = 2000;
    private static final int MAX_ONE_TIME_READS = 100;
    private static final long SLEEP_COMPENSATE_DIFF = 16;
    private static final String STARTING_DATA_LINE = "start data >>";
    private static final boolean THIS_DEBUG = false;
    BufferedReader mBufferedReader;
    private long mDeviceSleepTime = 30000;
    private int mEventCountInScript = 0;
    FileInputStream mFStream;
    private boolean mFileOpened = false;
    DataInputStream mInputStream;
    private long mLastExportDownTimeKey = 0;
    private long mLastExportDownTimeMotion = 0;
    private long mLastExportEventTime = -1;
    private long mLastRecordedDownTimeKey = 0;
    private long mLastRecordedDownTimeMotion = 0;
    private long mLastRecordedEventTime = -1;
    private float[] mLastX = new float[2];
    private float[] mLastY = new float[2];
    private long mMonkeyStartTime = -1;
    private long mProfileWaitTime = 5000;
    private MonkeyEventQueue mQ;
    private boolean mReadScriptLineByLine = false;
    private String mScriptFileName;
    private long mScriptStartTime = -1;
    private double mSpeed = 1.0d;
    private int mVerbose = 0;

    public MonkeySourceScript(Random random, String filename, long throttle, boolean randomizeThrottle, long profileWaitTime, long deviceSleepTime) {
        this.mScriptFileName = filename;
        this.mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
        this.mProfileWaitTime = profileWaitTime;
        this.mDeviceSleepTime = deviceSleepTime;
    }

    private void resetValue() {
        this.mLastRecordedDownTimeKey = 0;
        this.mLastRecordedDownTimeMotion = 0;
        this.mLastRecordedEventTime = -1;
        this.mLastExportDownTimeKey = 0;
        this.mLastExportDownTimeMotion = 0;
        this.mLastExportEventTime = -1;
    }

    private boolean readHeader() throws IOException {
        this.mFileOpened = true;
        this.mFStream = new FileInputStream(this.mScriptFileName);
        this.mInputStream = new DataInputStream(this.mFStream);
        this.mBufferedReader = new BufferedReader(new InputStreamReader(this.mInputStream));
        while (true) {
            String line = this.mBufferedReader.readLine();
            if (line == null) {
                return false;
            }
            line = line.trim();
            if (line.indexOf(HEADER_COUNT) >= 0) {
                try {
                    this.mEventCountInScript = Integer.parseInt(line.substring(HEADER_COUNT.length() + 1).trim());
                } catch (NumberFormatException e) {
                    Logger.err.println("" + e);
                    return false;
                }
            } else if (line.indexOf(HEADER_SPEED) >= 0) {
                try {
                    this.mSpeed = Double.parseDouble(line.substring(HEADER_COUNT.length() + 1).trim());
                } catch (NumberFormatException e2) {
                    Logger.err.println("" + e2);
                    return false;
                }
            } else if (line.indexOf(HEADER_LINE_BY_LINE) >= 0) {
                this.mReadScriptLineByLine = true;
            } else if (line.indexOf(STARTING_DATA_LINE) >= 0) {
                return true;
            }
        }
    }

    private int readLines() throws IOException {
        for (int i = 0; i < MAX_ONE_TIME_READS; i++) {
            String line = this.mBufferedReader.readLine();
            if (line == null) {
                return i;
            }
            line.trim();
            processLine(line);
        }
        return MAX_ONE_TIME_READS;
    }

    private int readOneLine() throws IOException {
        String line = this.mBufferedReader.readLine();
        if (line == null) {
            return 0;
        }
        line.trim();
        processLine(line);
        return 1;
    }

    private void handleEvent(java.lang.String r109, java.lang.String[] r110) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r54_0 'e' com.android.commands.monkey.MonkeyEvent) in PHI: PHI: (r54_1 'e' com.android.commands.monkey.MonkeyEvent) = (r54_0 'e' com.android.commands.monkey.MonkeyEvent), (r54_2 'e' com.android.commands.monkey.MonkeyEvent) binds: {(r54_0 'e' com.android.commands.monkey.MonkeyEvent)=B:17:0x0139, (r54_2 'e' com.android.commands.monkey.MonkeyEvent)=B:20:0x017d}
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
        r108 = this;
        r16 = "DispatchKey";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x00a5;
    L_0x000d:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 8;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x00a5;
    L_0x001a:
        r16 = com.android.commands.monkey.Logger.out;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = " old key\n";	 Catch:{ NumberFormatException -> 0x0d1d }
        r16.println(r17);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 0;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r6 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 1;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r8 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 2;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r10 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 3;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r11 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 4;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r12 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 5;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r13 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 6;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r14 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = 7;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1d }
        r15 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1d }
        r5 = new com.android.commands.monkey.MonkeyKeyEvent;	 Catch:{ NumberFormatException -> 0x0d1d }
        r5.<init>(r6, r8, r10, r11, r12, r13, r14, r15);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = com.android.commands.monkey.Logger.out;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = new java.lang.StringBuilder;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17.<init>();	 Catch:{ NumberFormatException -> 0x0d1d }
        r22 = " Key code ";	 Catch:{ NumberFormatException -> 0x0d1d }
        r0 = r17;	 Catch:{ NumberFormatException -> 0x0d1d }
        r1 = r22;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = r0.append(r1);	 Catch:{ NumberFormatException -> 0x0d1d }
        r0 = r17;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = r0.append(r11);	 Catch:{ NumberFormatException -> 0x0d1d }
        r22 = "\n";	 Catch:{ NumberFormatException -> 0x0d1d }
        r0 = r17;	 Catch:{ NumberFormatException -> 0x0d1d }
        r1 = r22;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = r0.append(r1);	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = r17.toString();	 Catch:{ NumberFormatException -> 0x0d1d }
        r16.println(r17);	 Catch:{ NumberFormatException -> 0x0d1d }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0d1d }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x0d1d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1d }
        r0.addLast(r5);	 Catch:{ NumberFormatException -> 0x0d1d }
        r16 = com.android.commands.monkey.Logger.out;	 Catch:{ NumberFormatException -> 0x0d1d }
        r17 = "Added key up \n";	 Catch:{ NumberFormatException -> 0x0d1d }
        r16.println(r17);	 Catch:{ NumberFormatException -> 0x0d1d }
    L_0x00a4:
        return;
    L_0x00a5:
        r16 = "DispatchPointer";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 >= 0) goto L_0x00bf;
    L_0x00b2:
        r16 = "DispatchTrackball";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0185;
    L_0x00bf:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 12;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0185;
    L_0x00cc:
        r16 = 0;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r6 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 1;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r8 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 2;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r10 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 3;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r18 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 4;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r19 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 5;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r20 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 6;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r21 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 7;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r13 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 8;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r103 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 9;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r106 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 10;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r14 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = 11;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d1a }
        r62 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = "Pointer";	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r109;	 Catch:{ NumberFormatException -> 0x0d1a }
        r1 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.indexOf(r1);	 Catch:{ NumberFormatException -> 0x0d1a }
        if (r16 <= 0) goto L_0x017d;	 Catch:{ NumberFormatException -> 0x0d1a }
    L_0x0139:
        r54 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r54;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0.<init>(r10);	 Catch:{ NumberFormatException -> 0x0d1a }
    L_0x0140:
        r0 = r54;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.setDownTime(r6);	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.setEventTime(r8);	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.setMetaState(r13);	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r1 = r103;	 Catch:{ NumberFormatException -> 0x0d1a }
        r2 = r106;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.setPrecision(r1, r2);	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.setDeviceId(r14);	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r1 = r62;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0.setEdgeFlags(r1);	 Catch:{ NumberFormatException -> 0x0d1a }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16.addPointer(r17, r18, r19, r20, r21);	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0d1a }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d1a }
        r1 = r54;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x0d1a }
    L_0x017c:
        return;	 Catch:{ NumberFormatException -> 0x0d1a }
    L_0x017d:
        r54 = new com.android.commands.monkey.MonkeyTrackballEvent;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0 = r54;	 Catch:{ NumberFormatException -> 0x0d1a }
        r0.<init>(r10);	 Catch:{ NumberFormatException -> 0x0d1a }
        goto L_0x0140;
    L_0x0185:
        r16 = "DispatchPointer";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 >= 0) goto L_0x019f;
    L_0x0192:
        r16 = "DispatchTrackball";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x037e;
    L_0x019f:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 13;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x037e;
    L_0x01ac:
        r16 = 0;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r6 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r8 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 2;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r10 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 3;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r18 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 4;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r19 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 5;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r20 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 6;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r21 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 7;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r13 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 8;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r103 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 9;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r106 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 10;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r14 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 11;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r62 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 12;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x037b }
        r72 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = "Pointer";	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r109;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.indexOf(r1);	 Catch:{ NumberFormatException -> 0x037b }
        if (r16 <= 0) goto L_0x0304;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0221:
        r16 = 5;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        if (r10 != r0) goto L_0x02fb;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0227:
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = r72 << 8;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = r17 | 5;	 Catch:{ NumberFormatException -> 0x037b }
        r16.<init>(r17);	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r54 = r16.setIntermediateNote(r17);	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0236:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mScriptStartTime;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r22 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = (r16 > r22 ? 1 : (r16 == r22 ? 0 : -1));	 Catch:{ NumberFormatException -> 0x037b }
        if (r16 >= 0) goto L_0x0250;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0242:
        r16 = android.os.SystemClock.uptimeMillis();	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r2 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r2.mMonkeyStartTime = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0.mScriptStartTime = r8;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0250:
        r16 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r72;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        if (r0 != r1) goto L_0x030d;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0258:
        r0 = r54;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setDownTime(r6);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setEventTime(r8);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setMetaState(r13);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r103;	 Catch:{ NumberFormatException -> 0x037b }
        r2 = r106;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setPrecision(r1, r2);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setDeviceId(r14);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r62;	 Catch:{ NumberFormatException -> 0x037b }
        r22 = r0.setEdgeFlags(r1);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastX;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r24 = r16[r17];	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastY;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r25 = r16[r17];	 Catch:{ NumberFormatException -> 0x037b }
        r23 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r26 = r20;	 Catch:{ NumberFormatException -> 0x037b }
        r27 = r21;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r22.addPointer(r23, r24, r25, r26, r27);	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r16.addPointer(r17, r18, r19, r20, r21);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastX;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r16[r17] = r18;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastY;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r16[r17] = r19;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x02b9:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mReadScriptLineByLine;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        if (r16 == 0) goto L_0x02ed;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x02c1:
        r44 = android.os.SystemClock.uptimeMillis();	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mMonkeyStartTime;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r86 = r44 - r16;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mScriptStartTime;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r90 = r8 - r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = (r86 > r90 ? 1 : (r86 == r90 ? 0 : -1));	 Catch:{ NumberFormatException -> 0x037b }
        if (r16 >= 0) goto L_0x02ed;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x02d9:
        r100 = r90 - r86;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = new com.android.commands.monkey.MonkeyWaitEvent;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r17;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r100;	 Catch:{ NumberFormatException -> 0x037b }
        r0.<init>(r1);	 Catch:{ NumberFormatException -> 0x037b }
        r16.addLast(r17);	 Catch:{ NumberFormatException -> 0x037b }
    L_0x02ed:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r54;	 Catch:{ NumberFormatException -> 0x037b }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x037b }
    L_0x02fa:
        return;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x02fb:
        r54 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r54;	 Catch:{ NumberFormatException -> 0x037b }
        r0.<init>(r10);	 Catch:{ NumberFormatException -> 0x037b }
        goto L_0x0236;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0304:
        r54 = new com.android.commands.monkey.MonkeyTrackballEvent;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r54;	 Catch:{ NumberFormatException -> 0x037b }
        r0.<init>(r10);	 Catch:{ NumberFormatException -> 0x037b }
        goto L_0x0250;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x030d:
        if (r72 != 0) goto L_0x02b9;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x030f:
        r0 = r54;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setDownTime(r6);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setEventTime(r8);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setMetaState(r13);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r103;	 Catch:{ NumberFormatException -> 0x037b }
        r2 = r106;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setPrecision(r1, r2);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setDeviceId(r14);	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r62;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0.setEdgeFlags(r1);	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r16.addPointer(r17, r18, r19, r20, r21);	 Catch:{ NumberFormatException -> 0x037b }
        r16 = 6;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        if (r10 != r0) goto L_0x0365;	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0344:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastX;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r16[r17];	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastY;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r22 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = r17[r22];	 Catch:{ NumberFormatException -> 0x037b }
        r22 = 1;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r54;	 Catch:{ NumberFormatException -> 0x037b }
        r1 = r22;	 Catch:{ NumberFormatException -> 0x037b }
        r2 = r16;	 Catch:{ NumberFormatException -> 0x037b }
        r3 = r17;	 Catch:{ NumberFormatException -> 0x037b }
        r0.addPointer(r1, r2, r3);	 Catch:{ NumberFormatException -> 0x037b }
    L_0x0365:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastX;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r16[r17] = r18;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x037b }
        r0 = r0.mLastY;	 Catch:{ NumberFormatException -> 0x037b }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x037b }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x037b }
        r16[r17] = r19;	 Catch:{ NumberFormatException -> 0x037b }
        goto L_0x02b9;
    L_0x037b:
        r57 = move-exception;
        goto L_0x02fa;
    L_0x037e:
        r16 = "RotateScreen";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x03e3;
    L_0x038b:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x03e3;
    L_0x0398:
        r16 = 0;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d17 }
        r88 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d17 }
        r16 = 1;	 Catch:{ NumberFormatException -> 0x0d17 }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d17 }
        r70 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d17 }
        if (r88 == 0) goto L_0x03b2;	 Catch:{ NumberFormatException -> 0x0d17 }
    L_0x03aa:
        r16 = 1;	 Catch:{ NumberFormatException -> 0x0d17 }
        r0 = r88;	 Catch:{ NumberFormatException -> 0x0d17 }
        r1 = r16;	 Catch:{ NumberFormatException -> 0x0d17 }
        if (r0 != r1) goto L_0x03cf;	 Catch:{ NumberFormatException -> 0x0d17 }
    L_0x03b2:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0d17 }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0d17 }
        r17 = r0;	 Catch:{ NumberFormatException -> 0x0d17 }
        r22 = new com.android.commands.monkey.MonkeyRotationEvent;	 Catch:{ NumberFormatException -> 0x0d17 }
        if (r70 == 0) goto L_0x03e0;	 Catch:{ NumberFormatException -> 0x0d17 }
    L_0x03bc:
        r16 = 1;	 Catch:{ NumberFormatException -> 0x0d17 }
    L_0x03be:
        r0 = r22;	 Catch:{ NumberFormatException -> 0x0d17 }
        r1 = r88;	 Catch:{ NumberFormatException -> 0x0d17 }
        r2 = r16;	 Catch:{ NumberFormatException -> 0x0d17 }
        r0.<init>(r1, r2);	 Catch:{ NumberFormatException -> 0x0d17 }
        r0 = r17;	 Catch:{ NumberFormatException -> 0x0d17 }
        r1 = r22;	 Catch:{ NumberFormatException -> 0x0d17 }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x0d17 }
    L_0x03ce:
        return;
    L_0x03cf:
        r16 = 2;
        r0 = r88;
        r1 = r16;
        if (r0 == r1) goto L_0x03b2;
    L_0x03d7:
        r16 = 3;
        r0 = r88;
        r1 = r16;
        if (r0 != r1) goto L_0x03ce;
    L_0x03df:
        goto L_0x03b2;
    L_0x03e0:
        r16 = 0;
        goto L_0x03be;
    L_0x03e3:
        r16 = "Tap";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x04c4;
    L_0x03f0:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 < r1) goto L_0x04c4;
    L_0x03fd:
        r16 = 0;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x049d }
        r18 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x049d }
        r16 = 1;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x049d }
        r19 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x049d }
        r96 = 0;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r110;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r0.length;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x049d }
        r17 = 3;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r1 = r17;	 Catch:{ NumberFormatException -> 0x049d }
        if (r0 != r1) goto L_0x0424;	 Catch:{ NumberFormatException -> 0x049d }
    L_0x041c:
        r16 = 2;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x049d }
        r96 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x049d }
    L_0x0424:
        r6 = android.os.SystemClock.uptimeMillis();	 Catch:{ NumberFormatException -> 0x049d }
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x049d }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x049d }
        r16.<init>(r17);	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r0.setDownTime(r6);	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r22 = r0.setEventTime(r6);	 Catch:{ NumberFormatException -> 0x049d }
        r26 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;	 Catch:{ NumberFormatException -> 0x049d }
        r27 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;	 Catch:{ NumberFormatException -> 0x049d }
        r23 = 0;	 Catch:{ NumberFormatException -> 0x049d }
        r24 = r18;	 Catch:{ NumberFormatException -> 0x049d }
        r25 = r19;	 Catch:{ NumberFormatException -> 0x049d }
        r58 = r22.addPointer(r23, r24, r25, r26, r27);	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r1 = r58;	 Catch:{ NumberFormatException -> 0x049d }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x049d }
        r16 = 0;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = (r96 > r16 ? 1 : (r96 == r16 ? 0 : -1));	 Catch:{ NumberFormatException -> 0x049d }
        if (r16 <= 0) goto L_0x046e;	 Catch:{ NumberFormatException -> 0x049d }
    L_0x045c:
        r0 = r108;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x049d }
        r17 = new com.android.commands.monkey.MonkeyWaitEvent;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r17;	 Catch:{ NumberFormatException -> 0x049d }
        r1 = r96;	 Catch:{ NumberFormatException -> 0x049d }
        r0.<init>(r1);	 Catch:{ NumberFormatException -> 0x049d }
        r16.addLast(r17);	 Catch:{ NumberFormatException -> 0x049d }
    L_0x046e:
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x049d }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x049d }
        r16.<init>(r17);	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r0.setDownTime(r6);	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r22 = r0.setEventTime(r6);	 Catch:{ NumberFormatException -> 0x049d }
        r26 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;	 Catch:{ NumberFormatException -> 0x049d }
        r27 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;	 Catch:{ NumberFormatException -> 0x049d }
        r23 = 0;	 Catch:{ NumberFormatException -> 0x049d }
        r24 = r18;	 Catch:{ NumberFormatException -> 0x049d }
        r25 = r19;	 Catch:{ NumberFormatException -> 0x049d }
        r59 = r22.addPointer(r23, r24, r25, r26, r27);	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x049d }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x049d }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x049d }
        r1 = r59;	 Catch:{ NumberFormatException -> 0x049d }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x049d }
    L_0x049c:
        return;
    L_0x049d:
        r57 = move-exception;
        r16 = com.android.commands.monkey.Logger.err;
        r17 = new java.lang.StringBuilder;
        r17.<init>();
        r22 = "// ";
        r0 = r17;
        r1 = r22;
        r17 = r0.append(r1);
        r22 = r57.toString();
        r0 = r17;
        r1 = r22;
        r17 = r0.append(r1);
        r17 = r17.toString();
        r16.println(r17);
        goto L_0x049c;
    L_0x04c4:
        r16 = "PressAndHold";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x059c;
    L_0x04d1:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 3;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x059c;
    L_0x04de:
        r16 = 0;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0575 }
        r18 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = 1;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0575 }
        r19 = java.lang.Float.parseFloat(r16);	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = 2;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0575 }
        r74 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x0575 }
        r6 = android.os.SystemClock.uptimeMillis();	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x0575 }
        r17 = 0;	 Catch:{ NumberFormatException -> 0x0575 }
        r16.<init>(r17);	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r0.setDownTime(r6);	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r22 = r0.setEventTime(r6);	 Catch:{ NumberFormatException -> 0x0575 }
        r26 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;	 Catch:{ NumberFormatException -> 0x0575 }
        r27 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;	 Catch:{ NumberFormatException -> 0x0575 }
        r23 = 0;	 Catch:{ NumberFormatException -> 0x0575 }
        r24 = r18;	 Catch:{ NumberFormatException -> 0x0575 }
        r25 = r19;	 Catch:{ NumberFormatException -> 0x0575 }
        r58 = r22.addPointer(r23, r24, r25, r26, r27);	 Catch:{ NumberFormatException -> 0x0575 }
        r60 = new com.android.commands.monkey.MonkeyWaitEvent;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r60;	 Catch:{ NumberFormatException -> 0x0575 }
        r1 = r74;	 Catch:{ NumberFormatException -> 0x0575 }
        r0.<init>(r1);	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;	 Catch:{ NumberFormatException -> 0x0575 }
        r17 = 1;	 Catch:{ NumberFormatException -> 0x0575 }
        r16.<init>(r17);	 Catch:{ NumberFormatException -> 0x0575 }
        r22 = r6 + r74;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r1 = r22;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r0.setDownTime(r1);	 Catch:{ NumberFormatException -> 0x0575 }
        r22 = r6 + r74;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r1 = r22;	 Catch:{ NumberFormatException -> 0x0575 }
        r22 = r0.setEventTime(r1);	 Catch:{ NumberFormatException -> 0x0575 }
        r26 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;	 Catch:{ NumberFormatException -> 0x0575 }
        r27 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;	 Catch:{ NumberFormatException -> 0x0575 }
        r23 = 0;	 Catch:{ NumberFormatException -> 0x0575 }
        r24 = r18;	 Catch:{ NumberFormatException -> 0x0575 }
        r25 = r19;	 Catch:{ NumberFormatException -> 0x0575 }
        r61 = r22.addPointer(r23, r24, r25, r26, r27);	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r1 = r58;	 Catch:{ NumberFormatException -> 0x0575 }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r1 = r60;	 Catch:{ NumberFormatException -> 0x0575 }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0575 }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x0575 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0575 }
        r1 = r60;	 Catch:{ NumberFormatException -> 0x0575 }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x0575 }
    L_0x0574:
        return;
    L_0x0575:
        r57 = move-exception;
        r16 = com.android.commands.monkey.Logger.err;
        r17 = new java.lang.StringBuilder;
        r17.<init>();
        r22 = "// ";
        r0 = r17;
        r1 = r22;
        r17 = r0.append(r1);
        r22 = r57.toString();
        r0 = r17;
        r1 = r22;
        r17 = r0.append(r1);
        r17 = r17.toString();
        r16.println(r17);
        goto L_0x0574;
    L_0x059c:
        r16 = "Drag";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x069b;
    L_0x05a9:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 5;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x069b;
    L_0x05b6:
        r16 = 0;
        r16 = r110[r16];
        r24 = java.lang.Float.parseFloat(r16);
        r16 = 1;
        r16 = r110[r16];
        r25 = java.lang.Float.parseFloat(r16);
        r16 = 2;
        r16 = r110[r16];
        r102 = java.lang.Float.parseFloat(r16);
        r16 = 3;
        r16 = r110[r16];
        r105 = java.lang.Float.parseFloat(r16);
        r16 = 4;
        r16 = r110[r16];
        r94 = java.lang.Integer.parseInt(r16);
        r18 = r24;
        r19 = r25;
        r6 = android.os.SystemClock.uptimeMillis();
        r8 = android.os.SystemClock.uptimeMillis();
        if (r94 <= 0) goto L_0x069b;
    L_0x05ec:
        r16 = r102 - r24;
        r0 = r94;
        r0 = (float) r0;
        r17 = r0;
        r104 = r16 / r17;
        r16 = r105 - r25;
        r0 = r94;
        r0 = (float) r0;
        r17 = r0;
        r107 = r16 / r17;
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;
        r17 = 0;
        r16.<init>(r17);
        r0 = r16;
        r16 = r0.setDownTime(r6);
        r0 = r16;
        r22 = r0.setEventTime(r8);
        r26 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r27 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;
        r23 = 0;
        r54 = r22.addPointer(r23, r24, r25, r26, r27);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r54;
        r0.addLast(r1);
        r63 = 0;
    L_0x062a:
        r0 = r63;
        r1 = r94;
        if (r0 >= r1) goto L_0x0669;
    L_0x0630:
        r18 = r18 + r104;
        r19 = r19 + r107;
        r8 = android.os.SystemClock.uptimeMillis();
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;
        r17 = 2;
        r16.<init>(r17);
        r0 = r16;
        r16 = r0.setDownTime(r6);
        r0 = r16;
        r26 = r0.setEventTime(r8);
        r30 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r31 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;
        r27 = 0;
        r28 = r18;
        r29 = r19;
        r54 = r26.addPointer(r27, r28, r29, r30, r31);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r54;
        r0.addLast(r1);
        r63 = r63 + 1;
        goto L_0x062a;
    L_0x0669:
        r8 = android.os.SystemClock.uptimeMillis();
        r16 = new com.android.commands.monkey.MonkeyTouchEvent;
        r17 = 1;
        r16.<init>(r17);
        r0 = r16;
        r16 = r0.setDownTime(r6);
        r0 = r16;
        r26 = r0.setEventTime(r8);
        r30 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r31 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;
        r27 = 0;
        r28 = r18;
        r29 = r19;
        r54 = r26.addPointer(r27, r28, r29, r30, r31);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r54;
        r0.addLast(r1);
    L_0x069b:
        r16 = "PinchZoom";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0828;
    L_0x06a8:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 9;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0828;
    L_0x06b5:
        r16 = 0;
        r16 = r110[r16];
        r28 = java.lang.Float.parseFloat(r16);
        r16 = 1;
        r16 = r110[r16];
        r29 = java.lang.Float.parseFloat(r16);
        r16 = 2;
        r16 = r110[r16];
        r76 = java.lang.Float.parseFloat(r16);
        r16 = 3;
        r16 = r110[r16];
        r78 = java.lang.Float.parseFloat(r16);
        r16 = 4;
        r16 = r110[r16];
        r81 = java.lang.Float.parseFloat(r16);
        r16 = 5;
        r16 = r110[r16];
        r84 = java.lang.Float.parseFloat(r16);
        r16 = 6;
        r16 = r110[r16];
        r80 = java.lang.Float.parseFloat(r16);
        r16 = 7;
        r16 = r110[r16];
        r83 = java.lang.Float.parseFloat(r16);
        r16 = 8;
        r16 = r110[r16];
        r94 = java.lang.Integer.parseInt(r16);
        r32 = r28;
        r33 = r29;
        r36 = r81;
        r37 = r84;
        r6 = android.os.SystemClock.uptimeMillis();
        r8 = android.os.SystemClock.uptimeMillis();
        if (r94 <= 0) goto L_0x0828;
    L_0x070f:
        r16 = r76 - r28;
        r0 = r94;
        r0 = (float) r0;
        r17 = r0;
        r77 = r16 / r17;
        r16 = r78 - r29;
        r0 = r94;
        r0 = (float) r0;
        r17 = r0;
        r79 = r16 / r17;
        r16 = r80 - r81;
        r0 = r94;
        r0 = (float) r0;
        r17 = r0;
        r82 = r16 / r17;
        r16 = r83 - r84;
        r0 = r94;
        r0 = (float) r0;
        r17 = r0;
        r85 = r16 / r17;
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyTouchEvent;
        r22 = 0;
        r0 = r17;
        r1 = r22;
        r0.<init>(r1);
        r0 = r17;
        r17 = r0.setDownTime(r6);
        r0 = r17;
        r26 = r0.setEventTime(r8);
        r30 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r31 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;
        r27 = 0;
        r17 = r26.addPointer(r27, r28, r29, r30, r31);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyTouchEvent;
        r22 = 261; // 0x105 float:3.66E-43 double:1.29E-321;
        r0 = r17;
        r1 = r22;
        r0.<init>(r1);
        r0 = r17;
        r17 = r0.setDownTime(r6);
        r22 = 0;
        r0 = r17;
        r1 = r22;
        r2 = r28;
        r3 = r29;
        r17 = r0.addPointer(r1, r2, r3);
        r22 = 1;
        r0 = r17;
        r1 = r22;
        r2 = r81;
        r3 = r84;
        r17 = r0.addPointer(r1, r2, r3);
        r22 = 1;
        r0 = r17;
        r1 = r22;
        r17 = r0.setIntermediateNote(r1);
        r16.addLast(r17);
        r63 = 0;
    L_0x079f:
        r0 = r63;
        r1 = r94;
        if (r0 >= r1) goto L_0x07e8;
    L_0x07a5:
        r32 = r32 + r77;
        r33 = r33 + r79;
        r36 = r36 + r82;
        r37 = r37 + r85;
        r8 = android.os.SystemClock.uptimeMillis();
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyTouchEvent;
        r22 = 2;
        r0 = r17;
        r1 = r22;
        r0.<init>(r1);
        r0 = r17;
        r17 = r0.setDownTime(r6);
        r0 = r17;
        r30 = r0.setEventTime(r8);
        r34 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r35 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;
        r31 = 0;
        r34 = r30.addPointer(r31, r32, r33, r34, r35);
        r38 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r39 = 1084227584; // 0x40a00000 float:5.0 double:5.356796015E-315;
        r35 = 1;
        r17 = r34.addPointer(r35, r36, r37, r38, r39);
        r16.addLast(r17);
        r63 = r63 + 1;
        goto L_0x079f;
    L_0x07e8:
        r8 = android.os.SystemClock.uptimeMillis();
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyTouchEvent;
        r22 = 6;
        r0 = r17;
        r1 = r22;
        r0.<init>(r1);
        r0 = r17;
        r17 = r0.setDownTime(r6);
        r0 = r17;
        r17 = r0.setEventTime(r8);
        r22 = 0;
        r0 = r17;
        r1 = r22;
        r2 = r32;
        r3 = r33;
        r17 = r0.addPointer(r1, r2, r3);
        r22 = 1;
        r0 = r17;
        r1 = r22;
        r2 = r36;
        r3 = r37;
        r17 = r0.addPointer(r1, r2, r3);
        r16.addLast(r17);
    L_0x0828:
        r16 = "DispatchFlip";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0860;
    L_0x0835:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0860;
    L_0x0842:
        r16 = 0;
        r16 = r110[r16];
        r67 = java.lang.Boolean.parseBoolean(r16);
        r50 = new com.android.commands.monkey.MonkeyFlipEvent;
        r0 = r50;
        r1 = r67;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r50;
        r0.addLast(r1);
    L_0x0860:
        r16 = "LaunchActivity";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0908;
    L_0x086d:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 < r1) goto L_0x0908;
    L_0x087a:
        r16 = 0;
        r71 = r110[r16];
        r16 = 1;
        r42 = r110[r16];
        r40 = 0;
        r69 = new android.content.ComponentName;
        r0 = r69;
        r1 = r71;
        r2 = r42;
        r0.<init>(r1, r2);
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 <= r1) goto L_0x08a4;
    L_0x089c:
        r16 = 2;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x08c8 }
        r40 = java.lang.Long.parseLong(r16);	 Catch:{ NumberFormatException -> 0x08c8 }
    L_0x08a4:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x08ef;
    L_0x08b1:
        r48 = new com.android.commands.monkey.MonkeyActivityEvent;
        r0 = r48;
        r1 = r69;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r48;
        r0.addLast(r1);
    L_0x08c7:
        return;
    L_0x08c8:
        r57 = move-exception;
        r16 = com.android.commands.monkey.Logger.err;
        r17 = new java.lang.StringBuilder;
        r17.<init>();
        r22 = "// ";
        r0 = r17;
        r1 = r22;
        r17 = r0.append(r1);
        r22 = r57.toString();
        r0 = r17;
        r1 = r22;
        r17 = r0.append(r1);
        r17 = r17.toString();
        r16.println(r17);
        return;
    L_0x08ef:
        r48 = new com.android.commands.monkey.MonkeyActivityEvent;
        r0 = r48;
        r1 = r69;
        r2 = r40;
        r0.<init>(r1, r2);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r48;
        r0.addLast(r1);
        goto L_0x08c7;
    L_0x0908:
        r16 = "DeviceWakeUp";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x09e7;
    L_0x0915:
        r71 = "com.google.android.powerutil";
        r42 = "com.google.android.powerutil.WakeUpScreen";
        r0 = r108;
        r0 = r0.mDeviceSleepTime;
        r46 = r0;
        r69 = new android.content.ComponentName;
        r0 = r69;
        r1 = r71;
        r2 = r42;
        r0.<init>(r1, r2);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyActivityEvent;
        r0 = r17;
        r1 = r69;
        r2 = r46;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyKeyEvent;
        r22 = 0;
        r23 = 7;
        r0 = r17;
        r1 = r22;
        r2 = r23;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyKeyEvent;
        r22 = 1;
        r23 = 7;
        r0 = r17;
        r1 = r22;
        r2 = r23;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyWaitEvent;
        r22 = 3000; // 0xbb8 float:4.204E-42 double:1.482E-320;
        r22 = r22 + r46;
        r0 = r17;
        r1 = r22;
        r0.<init>(r1);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyKeyEvent;
        r22 = 0;
        r23 = 82;
        r0 = r17;
        r1 = r22;
        r2 = r23;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyKeyEvent;
        r22 = 1;
        r23 = 82;
        r0 = r17;
        r1 = r22;
        r2 = r23;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyKeyEvent;
        r22 = 0;
        r23 = 4;
        r0 = r17;
        r1 = r22;
        r2 = r23;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r17 = new com.android.commands.monkey.MonkeyKeyEvent;
        r22 = 1;
        r23 = 4;
        r0 = r17;
        r1 = r22;
        r2 = r23;
        r0.<init>(r1, r2);
        r16.addLast(r17);
        return;
    L_0x09e7:
        r16 = "LaunchInstrumentation";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0a22;
    L_0x09f4:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0a22;
    L_0x0a01:
        r16 = 0;
        r98 = r110[r16];
        r16 = 1;
        r89 = r110[r16];
        r53 = new com.android.commands.monkey.MonkeyInstrumentationEvent;
        r0 = r53;
        r1 = r98;
        r2 = r89;
        r0.<init>(r1, r2);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r53;
        r0.addLast(r1);
        return;
    L_0x0a22:
        r16 = "UserWait";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0a60;
    L_0x0a2f:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0a60;
    L_0x0a3c:
        r16 = 0;
        r16 = r110[r16];	 Catch:{ NumberFormatException -> 0x0d14 }
        r16 = java.lang.Integer.parseInt(r16);	 Catch:{ NumberFormatException -> 0x0d14 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d14 }
        r0 = (long) r0;	 Catch:{ NumberFormatException -> 0x0d14 }
        r92 = r0;	 Catch:{ NumberFormatException -> 0x0d14 }
        r56 = new com.android.commands.monkey.MonkeyWaitEvent;	 Catch:{ NumberFormatException -> 0x0d14 }
        r0 = r56;	 Catch:{ NumberFormatException -> 0x0d14 }
        r1 = r92;	 Catch:{ NumberFormatException -> 0x0d14 }
        r0.<init>(r1);	 Catch:{ NumberFormatException -> 0x0d14 }
        r0 = r108;	 Catch:{ NumberFormatException -> 0x0d14 }
        r0 = r0.mQ;	 Catch:{ NumberFormatException -> 0x0d14 }
        r16 = r0;	 Catch:{ NumberFormatException -> 0x0d14 }
        r0 = r16;	 Catch:{ NumberFormatException -> 0x0d14 }
        r1 = r56;	 Catch:{ NumberFormatException -> 0x0d14 }
        r0.addLast(r1);	 Catch:{ NumberFormatException -> 0x0d14 }
    L_0x0a5f:
        return;
    L_0x0a60:
        r16 = "ProfileWait";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0a8a;
    L_0x0a6d:
        r56 = new com.android.commands.monkey.MonkeyWaitEvent;
        r0 = r108;
        r0 = r0.mProfileWaitTime;
        r16 = r0;
        r0 = r56;
        r1 = r16;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r56;
        r0.addLast(r1);
        return;
    L_0x0a8a:
        r16 = "DispatchPress";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0adc;
    L_0x0a97:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0adc;
    L_0x0aa4:
        r16 = 0;
        r66 = r110[r16];
        r65 = com.android.commands.monkey.MonkeySourceRandom.getKeyCode(r66);
        if (r65 != 0) goto L_0x0aaf;
    L_0x0aae:
        return;
    L_0x0aaf:
        r5 = new com.android.commands.monkey.MonkeyKeyEvent;
        r16 = 0;
        r0 = r16;
        r1 = r65;
        r5.<init>(r0, r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r0.addLast(r5);
        r5 = new com.android.commands.monkey.MonkeyKeyEvent;
        r16 = 1;
        r0 = r16;
        r1 = r65;
        r5.<init>(r0, r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r0.addLast(r5);
        return;
    L_0x0adc:
        r16 = "LongPress";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0b36;
    L_0x0ae9:
        r5 = new com.android.commands.monkey.MonkeyKeyEvent;
        r16 = 0;
        r17 = 23;
        r0 = r16;
        r1 = r17;
        r5.<init>(r0, r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r0.addLast(r5);
        r99 = new com.android.commands.monkey.MonkeyWaitEvent;
        r16 = LONGPRESS_WAIT_TIME;
        r0 = r16;
        r0 = (long) r0;
        r16 = r0;
        r0 = r99;
        r1 = r16;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r99;
        r0.addLast(r1);
        r5 = new com.android.commands.monkey.MonkeyKeyEvent;
        r16 = 1;
        r17 = 23;
        r0 = r16;
        r1 = r17;
        r5.<init>(r0, r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r0.addLast(r5);
    L_0x0b36:
        r16 = "PowerLog";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0b71;
    L_0x0b43:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        if (r16 <= 0) goto L_0x0b71;
    L_0x0b4a:
        r16 = 0;
        r73 = r110[r16];
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0c11;
    L_0x0b5b:
        r55 = new com.android.commands.monkey.MonkeyPowerEvent;
        r0 = r55;
        r1 = r73;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r55;
        r0.addLast(r1);
    L_0x0b71:
        r16 = "WriteLog";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0b90;
    L_0x0b7e:
        r55 = new com.android.commands.monkey.MonkeyPowerEvent;
        r55.<init>();
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r55;
        r0.addLast(r1);
    L_0x0b90:
        r16 = "RunCmd";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0bc4;
    L_0x0b9d:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0bc4;
    L_0x0baa:
        r16 = 0;
        r43 = r110[r16];
        r49 = new com.android.commands.monkey.MonkeyCommandEvent;
        r0 = r49;
        r1 = r43;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r49;
        r0.addLast(r1);
    L_0x0bc4:
        r16 = "DispatchString";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0c3c;
    L_0x0bd1:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0c3c;
    L_0x0bde:
        r16 = 0;
        r64 = r110[r16];
        r16 = new java.lang.StringBuilder;
        r16.<init>();
        r17 = "input text ";
        r16 = r16.append(r17);
        r0 = r16;
        r1 = r64;
        r16 = r0.append(r1);
        r43 = r16.toString();
        r49 = new com.android.commands.monkey.MonkeyCommandEvent;
        r0 = r49;
        r1 = r43;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r49;
        r0.addLast(r1);
        return;
    L_0x0c11:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0b71;
    L_0x0c1e:
        r16 = 1;
        r95 = r110[r16];
        r55 = new com.android.commands.monkey.MonkeyPowerEvent;
        r0 = r55;
        r1 = r73;
        r2 = r95;
        r0.<init>(r1, r2);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r55;
        r0.addLast(r1);
        goto L_0x0b71;
    L_0x0c3c:
        r16 = "StartCaptureFramerate";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0c63;
    L_0x0c49:
        r52 = new com.android.commands.monkey.MonkeyGetFrameRateEvent;
        r16 = "start";
        r0 = r52;
        r1 = r16;
        r0.<init>(r1);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r52;
        r0.addLast(r1);
        return;
    L_0x0c63:
        r16 = "EndCaptureFramerate";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0c9d;
    L_0x0c70:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0c9d;
    L_0x0c7d:
        r16 = 0;
        r64 = r110[r16];
        r52 = new com.android.commands.monkey.MonkeyGetFrameRateEvent;
        r16 = "end";
        r0 = r52;
        r1 = r16;
        r2 = r64;
        r0.<init>(r1, r2);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r52;
        r0.addLast(r1);
        return;
    L_0x0c9d:
        r16 = "StartCaptureAppFramerate";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0cd5;
    L_0x0caa:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 1;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0cd5;
    L_0x0cb7:
        r16 = 0;
        r4 = r110[r16];
        r51 = new com.android.commands.monkey.MonkeyGetAppFrameRateEvent;
        r16 = "start";
        r0 = r51;
        r1 = r16;
        r0.<init>(r1, r4);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r51;
        r0.addLast(r1);
        return;
    L_0x0cd5:
        r16 = "EndCaptureAppFramerate";
        r0 = r109;
        r1 = r16;
        r16 = r0.indexOf(r1);
        if (r16 < 0) goto L_0x0d13;
    L_0x0ce2:
        r0 = r110;
        r0 = r0.length;
        r16 = r0;
        r17 = 2;
        r0 = r16;
        r1 = r17;
        if (r0 != r1) goto L_0x0d13;
    L_0x0cef:
        r16 = 0;
        r4 = r110[r16];
        r16 = 1;
        r68 = r110[r16];
        r51 = new com.android.commands.monkey.MonkeyGetAppFrameRateEvent;
        r16 = "end";
        r0 = r51;
        r1 = r16;
        r2 = r68;
        r0.<init>(r1, r4, r2);
        r0 = r108;
        r0 = r0.mQ;
        r16 = r0;
        r0 = r16;
        r1 = r51;
        r0.addLast(r1);
        return;
    L_0x0d13:
        return;
    L_0x0d14:
        r57 = move-exception;
        goto L_0x0a5f;
    L_0x0d17:
        r57 = move-exception;
        goto L_0x03ce;
    L_0x0d1a:
        r57 = move-exception;
        goto L_0x017c;
    L_0x0d1d:
        r57 = move-exception;
        goto L_0x00a4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.commands.monkey.MonkeySourceScript.handleEvent(java.lang.String, java.lang.String[]):void");
    }

    private void processLine(String line) {
        int index1 = line.indexOf(40);
        int index2 = line.indexOf(41);
        if (index1 >= 0 && index2 >= 0) {
            String[] args = line.substring(index1 + 1, index2).split(",");
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            handleEvent(line, args);
        }
    }

    private void closeFile() throws IOException {
        this.mFileOpened = false;
        try {
            this.mFStream.close();
            this.mInputStream.close();
        } catch (NullPointerException e) {
        }
    }

    private void readNextBatch() throws IOException {
        int linesRead;
        if (!this.mFileOpened) {
            resetValue();
            readHeader();
        }
        if (this.mReadScriptLineByLine) {
            linesRead = readOneLine();
        } else {
            linesRead = readLines();
        }
        if (linesRead == 0) {
            closeFile();
        }
    }

    private void needSleep(long time) {
        if (time >= 1) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }
        }
    }

    public boolean validate() {
        try {
            boolean validHeader = readHeader();
            closeFile();
            if (this.mVerbose > 0) {
                Logger.out.println("Replaying " + this.mEventCountInScript + " events with speed " + this.mSpeed);
            }
            return validHeader;
        } catch (IOException e) {
            return false;
        }
    }

    public void setVerbose(int verbose) {
        this.mVerbose = verbose;
    }

    private void adjustKeyEventTime(MonkeyKeyEvent e) {
        if (e.getEventTime() >= 0) {
            long thisDownTime;
            long thisEventTime;
            if (this.mLastRecordedEventTime <= 0) {
                thisDownTime = SystemClock.uptimeMillis();
                thisEventTime = thisDownTime;
            } else {
                if (e.getDownTime() != this.mLastRecordedDownTimeKey) {
                    thisDownTime = e.getDownTime();
                } else {
                    thisDownTime = this.mLastExportDownTimeKey;
                }
                long expectedDelay = (long) (((double) (e.getEventTime() - this.mLastRecordedEventTime)) * this.mSpeed);
                thisEventTime = this.mLastExportEventTime + expectedDelay;
                needSleep(expectedDelay - SLEEP_COMPENSATE_DIFF);
            }
            this.mLastRecordedDownTimeKey = e.getDownTime();
            this.mLastRecordedEventTime = e.getEventTime();
            e.setDownTime(thisDownTime);
            e.setEventTime(thisEventTime);
            this.mLastExportDownTimeKey = thisDownTime;
            this.mLastExportEventTime = thisEventTime;
        }
    }

    private void adjustMotionEventTime(MonkeyMotionEvent e) {
        long thisEventTime = SystemClock.uptimeMillis();
        long thisDownTime = e.getDownTime();
        if (thisDownTime == this.mLastRecordedDownTimeMotion) {
            e.setDownTime(this.mLastExportDownTimeMotion);
        } else {
            this.mLastRecordedDownTimeMotion = thisDownTime;
            e.setDownTime(thisEventTime);
            this.mLastExportDownTimeMotion = thisEventTime;
        }
        e.setEventTime(thisEventTime);
    }

    public MonkeyEvent getNextEvent() {
        if (this.mQ.isEmpty()) {
            try {
                readNextBatch();
            } catch (IOException e) {
                return null;
            }
        }
        try {
            MonkeyEvent ev = (MonkeyEvent) this.mQ.getFirst();
            this.mQ.removeFirst();
            if (ev.getEventType() == 0) {
                adjustKeyEventTime((MonkeyKeyEvent) ev);
            } else if (ev.getEventType() == 1 || ev.getEventType() == 2) {
                adjustMotionEventTime((MonkeyMotionEvent) ev);
            }
            return ev;
        } catch (NoSuchElementException e2) {
            return null;
        }
    }
}
