package com.android.server.am;

import android.content.IIntentReceiver;
import android.content.IIntentSender.Stub;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.util.ArrayMap;
import android.util.TimeUtils;
import com.android.internal.os.IResultReceiver;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class PendingIntentRecord extends Stub {
    private static final String TAG = "ActivityManager";
    boolean canceled = false;
    final Key key;
    String lastTag;
    String lastTagPrefix;
    private RemoteCallbackList<IResultReceiver> mCancelCallbacks;
    final ActivityManagerService owner;
    final WeakReference<PendingIntentRecord> ref;
    boolean sent = false;
    String stringName;
    final int uid;
    private ArrayMap<IBinder, Long> whitelistDuration;

    static final class Key {
        private static final int ODD_PRIME_NUMBER = 37;
        final ActivityRecord activity;
        Intent[] allIntents;
        String[] allResolvedTypes;
        final int flags;
        final int hashCode;
        final Bundle options;
        final String packageName;
        final int requestCode;
        final Intent requestIntent;
        final String requestResolvedType;
        final int type;
        final int userId;
        final String who;

        Key(int _t, String _p, ActivityRecord _a, String _w, int _r, Intent[] _i, String[] _it, int _f, Bundle _o, int _userId) {
            Intent intent;
            String str = null;
            this.type = _t;
            this.packageName = _p;
            this.activity = _a;
            this.who = _w;
            this.requestCode = _r;
            if (_i != null) {
                intent = _i[_i.length - 1];
            } else {
                intent = null;
            }
            this.requestIntent = intent;
            if (_it != null) {
                str = _it[_it.length - 1];
            }
            this.requestResolvedType = str;
            this.allIntents = _i;
            this.allResolvedTypes = _it;
            this.flags = _f;
            this.options = _o;
            this.userId = _userId;
            int hash = ((((_f + 851) * 37) + _r) * 37) + _userId;
            if (_w != null) {
                hash = (hash * 37) + _w.hashCode();
            }
            if (_a != null) {
                hash = (hash * 37) + _a.hashCode();
            }
            if (this.requestIntent != null) {
                hash = (hash * 37) + this.requestIntent.filterHashCode();
            }
            if (this.requestResolvedType != null) {
                hash = (hash * 37) + this.requestResolvedType.hashCode();
            }
            this.hashCode = (((hash * 37) + (_p != null ? _p.hashCode() : 0)) * 37) + _t;
        }

        public boolean equals(Object otherObj) {
            if (otherObj == null) {
                return false;
            }
            try {
                Key other = (Key) otherObj;
                if (this.type != other.type || this.userId != other.userId || !Objects.equals(this.packageName, other.packageName) || this.activity != other.activity || !Objects.equals(this.who, other.who) || this.requestCode != other.requestCode) {
                    return false;
                }
                if (this.requestIntent != other.requestIntent) {
                    if (this.requestIntent != null) {
                        if (!this.requestIntent.filterEquals(other.requestIntent)) {
                            return false;
                        }
                    } else if (other.requestIntent != null) {
                        return false;
                    }
                }
                if (Objects.equals(this.requestResolvedType, other.requestResolvedType) && this.flags == other.flags) {
                    return true;
                }
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return this.hashCode;
        }

        public String toString() {
            return "Key{" + typeName() + " pkg=" + this.packageName + " intent=" + (this.requestIntent != null ? this.requestIntent.toShortString(false, true, false, false) : "<null>") + " flags=0x" + Integer.toHexString(this.flags) + " u=" + this.userId + "}";
        }

        String typeName() {
            switch (this.type) {
                case 1:
                    return "broadcastIntent";
                case 2:
                    return "startActivity";
                case 3:
                    return "activityResult";
                case 4:
                    return "startService";
                case 5:
                    return "startForegroundService";
                default:
                    return Integer.toString(this.type);
            }
        }
    }

    PendingIntentRecord(ActivityManagerService _owner, Key _k, int _u) {
        this.owner = _owner;
        this.key = _k;
        this.uid = _u;
        this.ref = new WeakReference(this);
    }

    void setWhitelistDurationLocked(IBinder whitelistToken, long duration) {
        if (duration > 0) {
            if (this.whitelistDuration == null) {
                this.whitelistDuration = new ArrayMap();
            }
            this.whitelistDuration.put(whitelistToken, Long.valueOf(duration));
        } else if (this.whitelistDuration != null) {
            this.whitelistDuration.remove(whitelistToken);
            if (this.whitelistDuration.size() <= 0) {
                this.whitelistDuration = null;
            }
        }
        this.stringName = null;
    }

    public void registerCancelListenerLocked(IResultReceiver receiver) {
        if (this.mCancelCallbacks == null) {
            this.mCancelCallbacks = new RemoteCallbackList();
        }
        this.mCancelCallbacks.register(receiver);
    }

    public void unregisterCancelListenerLocked(IResultReceiver receiver) {
        this.mCancelCallbacks.unregister(receiver);
        if (this.mCancelCallbacks.getRegisteredCallbackCount() <= 0) {
            this.mCancelCallbacks = null;
        }
    }

    public RemoteCallbackList<IResultReceiver> detachCancelListenersLocked() {
        RemoteCallbackList<IResultReceiver> listeners = this.mCancelCallbacks;
        this.mCancelCallbacks = null;
        return listeners;
    }

    public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        sendInner(code, intent, resolvedType, whitelistToken, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options);
    }

    public int sendWithResult(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        return sendInner(code, intent, resolvedType, whitelistToken, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options);
    }

    int sendInner(int r48, android.content.Intent r49, java.lang.String r50, android.os.IBinder r51, android.content.IIntentReceiver r52, java.lang.String r53, android.os.IBinder r54, java.lang.String r55, int r56, int r57, int r58, android.os.Bundle r59) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r16_0 'finalIntent' android.content.Intent) in PHI: PHI: (r16_1 'finalIntent' android.content.Intent) = (r16_0 'finalIntent' android.content.Intent), (r16_2 'finalIntent' android.content.Intent) binds: {(r16_0 'finalIntent' android.content.Intent)=B:14:0x0042, (r16_2 'finalIntent' android.content.Intent)=B:52:?}
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
        r47 = this;
        if (r49 == 0) goto L_0x0008;
    L_0x0002:
        r2 = 1;
        r0 = r49;
        r0.setDefusable(r2);
    L_0x0008:
        if (r59 == 0) goto L_0x0010;
    L_0x000a:
        r2 = 1;
        r0 = r59;
        r0.setDefusable(r2);
    L_0x0010:
        r0 = r47;
        r0 = r0.owner;
        r46 = r0;
        monitor-enter(r46);
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.canceled;	 Catch:{ all -> 0x0158 }
        if (r2 != 0) goto L_0x0348;	 Catch:{ all -> 0x0158 }
    L_0x0020:
        r2 = 1;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r0.sent = r2;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.flags;	 Catch:{ all -> 0x0158 }
        r5 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;	 Catch:{ all -> 0x0158 }
        r2 = r2 & r5;	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x003a;	 Catch:{ all -> 0x0158 }
    L_0x0030:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.owner;	 Catch:{ all -> 0x0158 }
        r5 = 1;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2.cancelIntentSenderLocked(r0, r5);	 Catch:{ all -> 0x0158 }
    L_0x003a:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.requestIntent;	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x0151;	 Catch:{ all -> 0x0158 }
    L_0x0042:
        r16 = new android.content.Intent;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.requestIntent;	 Catch:{ all -> 0x0158 }
        r0 = r16;	 Catch:{ all -> 0x0158 }
        r0.<init>(r2);	 Catch:{ all -> 0x0158 }
    L_0x004f:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.flags;	 Catch:{ all -> 0x0158 }
        r5 = 67108864; // 0x4000000 float:1.5046328E-36 double:3.31561842E-316;	 Catch:{ all -> 0x0158 }
        r2 = r2 & r5;	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x015e;	 Catch:{ all -> 0x0158 }
    L_0x005a:
        r37 = 1;	 Catch:{ all -> 0x0158 }
    L_0x005c:
        if (r37 != 0) goto L_0x016c;	 Catch:{ all -> 0x0158 }
    L_0x005e:
        if (r49 == 0) goto L_0x0162;	 Catch:{ all -> 0x0158 }
    L_0x0060:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.flags;	 Catch:{ all -> 0x0158 }
        r0 = r16;	 Catch:{ all -> 0x0158 }
        r1 = r49;	 Catch:{ all -> 0x0158 }
        r32 = r0.fillIn(r1, r2);	 Catch:{ all -> 0x0158 }
        r2 = r32 & 2;	 Catch:{ all -> 0x0158 }
        if (r2 != 0) goto L_0x007a;	 Catch:{ all -> 0x0158 }
    L_0x0072:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.requestResolvedType;	 Catch:{ all -> 0x0158 }
        r50 = r0;	 Catch:{ all -> 0x0158 }
    L_0x007a:
        r0 = r57;	 Catch:{ all -> 0x0158 }
        r0 = r0 & -196;	 Catch:{ all -> 0x0158 }
        r57 = r0;	 Catch:{ all -> 0x0158 }
        r58 = r58 & r57;	 Catch:{ all -> 0x0158 }
        r2 = r16.getFlags();	 Catch:{ all -> 0x0158 }
        r0 = r57;	 Catch:{ all -> 0x0158 }
        r5 = ~r0;	 Catch:{ all -> 0x0158 }
        r2 = r2 & r5;	 Catch:{ all -> 0x0158 }
        r2 = r2 | r58;	 Catch:{ all -> 0x0158 }
        r0 = r16;	 Catch:{ all -> 0x0158 }
        r0.setFlags(r2);	 Catch:{ all -> 0x0158 }
    L_0x0091:
        r4 = android.os.Binder.getCallingUid();	 Catch:{ all -> 0x0158 }
        r3 = android.os.Binder.getCallingPid();	 Catch:{ all -> 0x0158 }
        r40 = android.os.Binder.clearCallingIdentity();	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.whitelistDuration;	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x00ff;	 Catch:{ all -> 0x0158 }
    L_0x00a3:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.whitelistDuration;	 Catch:{ all -> 0x0158 }
        r0 = r51;	 Catch:{ all -> 0x0158 }
        r33 = r2.get(r0);	 Catch:{ all -> 0x0158 }
        r33 = (java.lang.Long) r33;	 Catch:{ all -> 0x0158 }
        if (r33 == 0) goto L_0x00ff;	 Catch:{ all -> 0x0158 }
    L_0x00b1:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.owner;	 Catch:{ all -> 0x0158 }
        r39 = r2.getUidState(r4);	 Catch:{ all -> 0x0158 }
        r2 = android.app.ActivityManager.isProcStateBackground(r39);	 Catch:{ all -> 0x0158 }
        if (r2 != 0) goto L_0x0198;	 Catch:{ all -> 0x0158 }
    L_0x00bf:
        r45 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0158 }
        r2 = 64;	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        r0.<init>(r2);	 Catch:{ all -> 0x0158 }
        r2 = "pendingintent:";	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        r0.append(r2);	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        android.os.UserHandle.formatUid(r0, r4);	 Catch:{ all -> 0x0158 }
        r2 = ":";	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        r0.append(r2);	 Catch:{ all -> 0x0158 }
        r2 = r16.getAction();	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x0176;	 Catch:{ all -> 0x0158 }
    L_0x00e3:
        r2 = r16.getAction();	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        r0.append(r2);	 Catch:{ all -> 0x0158 }
    L_0x00ec:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.owner;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r5 = r0.uid;	 Catch:{ all -> 0x0158 }
        r6 = r33.longValue();	 Catch:{ all -> 0x0158 }
        r8 = r45.toString();	 Catch:{ all -> 0x0158 }
        r2.tempWhitelistForPendingIntentLocked(r3, r4, r5, r6, r8);	 Catch:{ all -> 0x0158 }
    L_0x00ff:
        if (r52 == 0) goto L_0x01c2;	 Catch:{ all -> 0x0158 }
    L_0x0101:
        r43 = 1;	 Catch:{ all -> 0x0158 }
    L_0x0103:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r12 = r2.userId;	 Catch:{ all -> 0x0158 }
        r2 = -2;	 Catch:{ all -> 0x0158 }
        if (r12 != r2) goto L_0x0116;	 Catch:{ all -> 0x0158 }
    L_0x010c:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.owner;	 Catch:{ all -> 0x0158 }
        r2 = r2.mUserController;	 Catch:{ all -> 0x0158 }
        r12 = r2.getCurrentOrTargetUserIdLocked();	 Catch:{ all -> 0x0158 }
    L_0x0116:
        r42 = 0;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.type;	 Catch:{ all -> 0x0158 }
        switch(r2) {
            case 1: goto L_0x02c3;
            case 2: goto L_0x01c6;
            case 3: goto L_0x0294;
            case 4: goto L_0x0309;
            case 5: goto L_0x0309;
            default: goto L_0x0121;
        };
    L_0x0121:
        if (r43 == 0) goto L_0x0149;
    L_0x0123:
        r2 = -96;
        r0 = r42;
        if (r0 == r2) goto L_0x0149;
    L_0x0129:
        r19 = new android.content.Intent;	 Catch:{ RemoteException -> 0x034f }
        r0 = r19;	 Catch:{ RemoteException -> 0x034f }
        r1 = r16;	 Catch:{ RemoteException -> 0x034f }
        r0.<init>(r1);	 Catch:{ RemoteException -> 0x034f }
        r0 = r47;	 Catch:{ RemoteException -> 0x034f }
        r2 = r0.key;	 Catch:{ RemoteException -> 0x034f }
        r0 = r2.userId;	 Catch:{ RemoteException -> 0x034f }
        r25 = r0;	 Catch:{ RemoteException -> 0x034f }
        r20 = 0;	 Catch:{ RemoteException -> 0x034f }
        r21 = 0;	 Catch:{ RemoteException -> 0x034f }
        r22 = 0;	 Catch:{ RemoteException -> 0x034f }
        r23 = 0;	 Catch:{ RemoteException -> 0x034f }
        r24 = 0;	 Catch:{ RemoteException -> 0x034f }
        r18 = r52;	 Catch:{ RemoteException -> 0x034f }
        r18.performReceive(r19, r20, r21, r22, r23, r24, r25);	 Catch:{ RemoteException -> 0x034f }
    L_0x0149:
        android.os.Binder.restoreCallingIdentity(r40);	 Catch:{ all -> 0x0158 }
        monitor-exit(r46);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return r42;
    L_0x0151:
        r16 = new android.content.Intent;	 Catch:{ all -> 0x0158 }
        r16.<init>();	 Catch:{ all -> 0x0158 }
        goto L_0x004f;
    L_0x0158:
        r2 = move-exception;
        monitor-exit(r46);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        throw r2;
    L_0x015e:
        r37 = 0;
        goto L_0x005c;
    L_0x0162:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.requestResolvedType;	 Catch:{ all -> 0x0158 }
        r50 = r0;	 Catch:{ all -> 0x0158 }
        goto L_0x007a;	 Catch:{ all -> 0x0158 }
    L_0x016c:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.requestResolvedType;	 Catch:{ all -> 0x0158 }
        r50 = r0;	 Catch:{ all -> 0x0158 }
        goto L_0x0091;	 Catch:{ all -> 0x0158 }
    L_0x0176:
        r2 = r16.getComponent();	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x0187;	 Catch:{ all -> 0x0158 }
    L_0x017c:
        r2 = r16.getComponent();	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        r2.appendShortString(r0);	 Catch:{ all -> 0x0158 }
        goto L_0x00ec;	 Catch:{ all -> 0x0158 }
    L_0x0187:
        r2 = r16.getData();	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x00ec;	 Catch:{ all -> 0x0158 }
    L_0x018d:
        r2 = r16.getData();	 Catch:{ all -> 0x0158 }
        r0 = r45;	 Catch:{ all -> 0x0158 }
        r0.append(r2);	 Catch:{ all -> 0x0158 }
        goto L_0x00ec;	 Catch:{ all -> 0x0158 }
    L_0x0198:
        r2 = TAG;	 Catch:{ all -> 0x0158 }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0158 }
        r5.<init>();	 Catch:{ all -> 0x0158 }
        r6 = "Not doing whitelist ";	 Catch:{ all -> 0x0158 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r5 = r5.append(r0);	 Catch:{ all -> 0x0158 }
        r6 = ": caller state=";	 Catch:{ all -> 0x0158 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x0158 }
        r0 = r39;	 Catch:{ all -> 0x0158 }
        r5 = r5.append(r0);	 Catch:{ all -> 0x0158 }
        r5 = r5.toString();	 Catch:{ all -> 0x0158 }
        android.util.Slog.w(r2, r5);	 Catch:{ all -> 0x0158 }
        goto L_0x00ff;	 Catch:{ all -> 0x0158 }
    L_0x01c2:
        r43 = 0;	 Catch:{ all -> 0x0158 }
        goto L_0x0103;	 Catch:{ all -> 0x0158 }
    L_0x01c6:
        if (r59 != 0) goto L_0x024e;	 Catch:{ all -> 0x0158 }
    L_0x01c8:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.options;	 Catch:{ all -> 0x0158 }
        r59 = r0;	 Catch:{ all -> 0x0158 }
    L_0x01d0:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allIntents;	 Catch:{ RuntimeException -> 0x0241 }
        if (r2 == 0) goto L_0x026e;	 Catch:{ RuntimeException -> 0x0241 }
    L_0x01d8:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allIntents;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.length;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = 1;	 Catch:{ RuntimeException -> 0x0241 }
        if (r2 <= r5) goto L_0x026e;	 Catch:{ RuntimeException -> 0x0241 }
    L_0x01e2:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allIntents;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.length;	 Catch:{ RuntimeException -> 0x0241 }
        r8 = new android.content.Intent[r2];	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allIntents;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.length;	 Catch:{ RuntimeException -> 0x0241 }
        r9 = new java.lang.String[r2];	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allIntents;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r5.allIntents;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r5.length;	 Catch:{ RuntimeException -> 0x0241 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0241 }
        r7 = 0;	 Catch:{ RuntimeException -> 0x0241 }
        java.lang.System.arraycopy(r2, r6, r8, r7, r5);	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allResolvedTypes;	 Catch:{ RuntimeException -> 0x0241 }
        if (r2 == 0) goto L_0x0220;	 Catch:{ RuntimeException -> 0x0241 }
    L_0x020e:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2.allResolvedTypes;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r5.allResolvedTypes;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r5.length;	 Catch:{ RuntimeException -> 0x0241 }
        r6 = 0;	 Catch:{ RuntimeException -> 0x0241 }
        r7 = 0;	 Catch:{ RuntimeException -> 0x0241 }
        java.lang.System.arraycopy(r2, r6, r9, r7, r5);	 Catch:{ RuntimeException -> 0x0241 }
    L_0x0220:
        r2 = r8.length;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2 + -1;	 Catch:{ RuntimeException -> 0x0241 }
        r8[r2] = r16;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r9.length;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r2 + -1;	 Catch:{ RuntimeException -> 0x0241 }
        r9[r2] = r50;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r5 = r0.owner;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r6 = r0.uid;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r7 = r2.packageName;	 Catch:{ RuntimeException -> 0x0241 }
        r10 = r54;	 Catch:{ RuntimeException -> 0x0241 }
        r11 = r59;	 Catch:{ RuntimeException -> 0x0241 }
        r5.startActivitiesInPackage(r6, r7, r8, r9, r10, r11, r12);	 Catch:{ RuntimeException -> 0x0241 }
        goto L_0x0121;
    L_0x0241:
        r36 = move-exception;
        r2 = TAG;	 Catch:{ all -> 0x0158 }
        r5 = "Unable to send startActivity intent";	 Catch:{ all -> 0x0158 }
        r0 = r36;	 Catch:{ all -> 0x0158 }
        android.util.Slog.w(r2, r5, r0);	 Catch:{ all -> 0x0158 }
        goto L_0x0121;	 Catch:{ all -> 0x0158 }
    L_0x024e:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.options;	 Catch:{ all -> 0x0158 }
        if (r2 == 0) goto L_0x01d0;	 Catch:{ all -> 0x0158 }
    L_0x0256:
        r38 = new android.os.Bundle;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.options;	 Catch:{ all -> 0x0158 }
        r0 = r38;	 Catch:{ all -> 0x0158 }
        r0.<init>(r2);	 Catch:{ all -> 0x0158 }
        r0 = r38;	 Catch:{ all -> 0x0158 }
        r1 = r59;	 Catch:{ all -> 0x0158 }
        r0.putAll(r1);	 Catch:{ all -> 0x0158 }
        r59 = r38;
        goto L_0x01d0;
    L_0x026e:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r13 = r0.owner;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r14 = r0.uid;	 Catch:{ RuntimeException -> 0x0241 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0241 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0241 }
        r15 = r2.packageName;	 Catch:{ RuntimeException -> 0x0241 }
        r25 = "PendingIntentRecord";	 Catch:{ RuntimeException -> 0x0241 }
        r21 = 0;	 Catch:{ RuntimeException -> 0x0241 }
        r24 = 0;	 Catch:{ RuntimeException -> 0x0241 }
        r17 = r50;	 Catch:{ RuntimeException -> 0x0241 }
        r18 = r54;	 Catch:{ RuntimeException -> 0x0241 }
        r19 = r55;	 Catch:{ RuntimeException -> 0x0241 }
        r20 = r56;	 Catch:{ RuntimeException -> 0x0241 }
        r22 = r59;	 Catch:{ RuntimeException -> 0x0241 }
        r23 = r12;	 Catch:{ RuntimeException -> 0x0241 }
        r13.startActivityInPackage(r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24, r25);	 Catch:{ RuntimeException -> 0x0241 }
        goto L_0x0121;
    L_0x0294:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r2 = r2.activity;	 Catch:{ all -> 0x0158 }
        r17 = r2.getStack();	 Catch:{ all -> 0x0158 }
        if (r17 == 0) goto L_0x0121;	 Catch:{ all -> 0x0158 }
    L_0x02a0:
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.activity;	 Catch:{ all -> 0x0158 }
        r19 = r0;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.who;	 Catch:{ all -> 0x0158 }
        r20 = r0;	 Catch:{ all -> 0x0158 }
        r0 = r47;	 Catch:{ all -> 0x0158 }
        r2 = r0.key;	 Catch:{ all -> 0x0158 }
        r0 = r2.requestCode;	 Catch:{ all -> 0x0158 }
        r21 = r0;	 Catch:{ all -> 0x0158 }
        r18 = -1;	 Catch:{ all -> 0x0158 }
        r22 = r48;	 Catch:{ all -> 0x0158 }
        r23 = r16;	 Catch:{ all -> 0x0158 }
        r17.sendActivityResultLocked(r18, r19, r20, r21, r22, r23);	 Catch:{ all -> 0x0158 }
        goto L_0x0121;
    L_0x02c3:
        r0 = r47;	 Catch:{ RuntimeException -> 0x02fc }
        r0 = r0.owner;	 Catch:{ RuntimeException -> 0x02fc }
        r18 = r0;	 Catch:{ RuntimeException -> 0x02fc }
        r0 = r47;	 Catch:{ RuntimeException -> 0x02fc }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x02fc }
        r0 = r2.packageName;	 Catch:{ RuntimeException -> 0x02fc }
        r19 = r0;	 Catch:{ RuntimeException -> 0x02fc }
        r0 = r47;	 Catch:{ RuntimeException -> 0x02fc }
        r0 = r0.uid;	 Catch:{ RuntimeException -> 0x02fc }
        r20 = r0;	 Catch:{ RuntimeException -> 0x02fc }
        if (r52 == 0) goto L_0x02f9;	 Catch:{ RuntimeException -> 0x02fc }
    L_0x02d9:
        r29 = 1;	 Catch:{ RuntimeException -> 0x02fc }
    L_0x02db:
        r25 = 0;	 Catch:{ RuntimeException -> 0x02fc }
        r26 = 0;	 Catch:{ RuntimeException -> 0x02fc }
        r30 = 0;	 Catch:{ RuntimeException -> 0x02fc }
        r21 = r16;	 Catch:{ RuntimeException -> 0x02fc }
        r22 = r50;	 Catch:{ RuntimeException -> 0x02fc }
        r23 = r52;	 Catch:{ RuntimeException -> 0x02fc }
        r24 = r48;	 Catch:{ RuntimeException -> 0x02fc }
        r27 = r53;	 Catch:{ RuntimeException -> 0x02fc }
        r28 = r59;	 Catch:{ RuntimeException -> 0x02fc }
        r31 = r12;	 Catch:{ RuntimeException -> 0x02fc }
        r44 = r18.broadcastIntentInPackage(r19, r20, r21, r22, r23, r24, r25, r26, r27, r28, r29, r30, r31);	 Catch:{ RuntimeException -> 0x02fc }
        if (r44 != 0) goto L_0x0121;
    L_0x02f5:
        r43 = 0;
        goto L_0x0121;
    L_0x02f9:
        r29 = 0;
        goto L_0x02db;
    L_0x02fc:
        r36 = move-exception;
        r2 = TAG;	 Catch:{ all -> 0x0158 }
        r5 = "Unable to send startActivity intent";	 Catch:{ all -> 0x0158 }
        r0 = r36;	 Catch:{ all -> 0x0158 }
        android.util.Slog.w(r2, r5, r0);	 Catch:{ all -> 0x0158 }
        goto L_0x0121;
    L_0x0309:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r0 = r0.owner;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r18 = r0;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r0 = r0.uid;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r19 = r0;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r0 = r47;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r2 = r2.type;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r5 = 5;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        if (r2 != r5) goto L_0x0340;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
    L_0x031e:
        r22 = 1;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
    L_0x0320:
        r0 = r47;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r2 = r0.key;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r0 = r2.packageName;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r23 = r0;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r20 = r16;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r21 = r50;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r24 = r12;	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        r18.startServiceInPackage(r19, r20, r21, r22, r23, r24);	 Catch:{ RuntimeException -> 0x0333, TransactionTooLargeException -> 0x0343 }
        goto L_0x0121;
    L_0x0333:
        r36 = move-exception;
        r2 = TAG;	 Catch:{ all -> 0x0158 }
        r5 = "Unable to send startService intent";	 Catch:{ all -> 0x0158 }
        r0 = r36;	 Catch:{ all -> 0x0158 }
        android.util.Slog.w(r2, r5, r0);	 Catch:{ all -> 0x0158 }
        goto L_0x0121;
    L_0x0340:
        r22 = 0;
        goto L_0x0320;
    L_0x0343:
        r35 = move-exception;
        r42 = -96;
        goto L_0x0121;
    L_0x0348:
        monitor-exit(r46);
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        r2 = -96;
        return r2;
    L_0x034f:
        r34 = move-exception;
        goto L_0x0149;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.PendingIntentRecord.sendInner(int, android.content.Intent, java.lang.String, android.os.IBinder, android.content.IIntentReceiver, java.lang.String, android.os.IBinder, java.lang.String, int, int, int, android.os.Bundle):int");
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.canceled) {
                this.owner.mHandler.sendMessage(this.owner.mHandler.obtainMessage(23, this));
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public void completeFinalize() {
        synchronized (this.owner) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (((WeakReference) this.owner.mIntentSenderRecords.get(this.key)) == this.ref) {
                    this.owner.mIntentSenderRecords.remove(this.key);
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    void dump(PrintWriter pw, String prefix) {
        int i;
        pw.print(prefix);
        pw.print("uid=");
        pw.print(this.uid);
        pw.print(" packageName=");
        pw.print(this.key.packageName);
        pw.print(" type=");
        pw.print(this.key.typeName());
        pw.print(" flags=0x");
        pw.println(Integer.toHexString(this.key.flags));
        if (!(this.key.activity == null && this.key.who == null)) {
            pw.print(prefix);
            pw.print("activity=");
            pw.print(this.key.activity);
            pw.print(" who=");
            pw.println(this.key.who);
        }
        if (!(this.key.requestCode == 0 && this.key.requestResolvedType == null)) {
            pw.print(prefix);
            pw.print("requestCode=");
            pw.print(this.key.requestCode);
            pw.print(" requestResolvedType=");
            pw.println(this.key.requestResolvedType);
        }
        if (this.key.requestIntent != null) {
            pw.print(prefix);
            pw.print("requestIntent=");
            pw.println(this.key.requestIntent.toShortString(false, true, true, true));
        }
        if (this.sent || this.canceled) {
            pw.print(prefix);
            pw.print("sent=");
            pw.print(this.sent);
            pw.print(" canceled=");
            pw.println(this.canceled);
        }
        if (this.whitelistDuration != null) {
            pw.print(prefix);
            pw.print("whitelistDuration=");
            for (i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                pw.print(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                pw.print(":");
                TimeUtils.formatDuration(((Long) this.whitelistDuration.valueAt(i)).longValue(), pw);
            }
            pw.println();
        }
        if (this.mCancelCallbacks != null) {
            pw.print(prefix);
            pw.println("mCancelCallbacks:");
            for (i = 0; i < this.mCancelCallbacks.getRegisteredCallbackCount(); i++) {
                pw.print(prefix);
                pw.print("  #");
                pw.print(i);
                pw.print(": ");
                pw.println(this.mCancelCallbacks.getRegisteredCallbackItem(i));
            }
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("PendingIntentRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.key.packageName);
        sb.append(' ');
        sb.append(this.key.typeName());
        if (this.whitelistDuration != null) {
            sb.append(" (whitelist: ");
            for (int i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                sb.append(":");
                TimeUtils.formatDuration(((Long) this.whitelistDuration.valueAt(i)).longValue(), sb);
            }
            sb.append(")");
        }
        sb.append('}');
        String stringBuilder = sb.toString();
        this.stringName = stringBuilder;
        return stringBuilder;
    }
}
