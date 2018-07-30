package com.android.internal.telephony;

import android.content.Context;
import android.os.Message;
import android.telephony.SmsCbMessage;

public class CellBroadcastHandler extends WakeLockStateMachine {
    private CellBroadcastHandler(Context context, Phone phone) {
        this("CellBroadcastHandler", context, phone);
    }

    protected CellBroadcastHandler(String debugTag, Context context, Phone phone) {
        super(debugTag, context, phone);
    }

    public static CellBroadcastHandler makeCellBroadcastHandler(Context context, Phone phone) {
        CellBroadcastHandler handler = new CellBroadcastHandler(context, phone);
        handler.start();
        return handler;
    }

    protected boolean handleSmsMessage(Message message) {
        if (message.obj instanceof SmsCbMessage) {
            handleBroadcastSms((SmsCbMessage) message.obj);
            return true;
        }
        loge("handleMessage got object of type: " + message.obj.getClass().getName());
        return false;
    }

    protected void handleBroadcastSms(android.telephony.SmsCbMessage r20) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_0 'intent' android.content.Intent) in PHI: PHI: (r18_1 'intent' android.content.Intent) = (r18_0 'intent' android.content.Intent), (r18_2 'intent' android.content.Intent) binds: {(r18_0 'intent' android.content.Intent)=B:2:0x0006, (r18_2 'intent' android.content.Intent)=B:10:0x00a7}
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
        r19 = this;
        r2 = r20.isEmergencyMessage();
        if (r2 == 0) goto L_0x00a7;
    L_0x0006:
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r4 = "Dispatching emergency SMS CB, SmsCbMessage is: ";
        r2 = r2.append(r4);
        r0 = r20;
        r2 = r2.append(r0);
        r2 = r2.toString();
        r0 = r19;
        r0.log(r2);
        r18 = new android.content.Intent;
        r2 = "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";
        r0 = r18;
        r0.<init>(r2);
        r0 = r19;
        r2 = r0.mContext;
        r2 = r2.getResources();
        r4 = 17039661; // 0x104012d float:2.4245415E-38 double:8.418711E-317;
        r2 = r2.getString(r4);
        r0 = r18;
        r0.setPackage(r2);
        r5 = "android.permission.RECEIVE_EMERGENCY_BROADCAST";
        r6 = 17;
    L_0x0044:
        r2 = "message";
        r0 = r18;
        r1 = r20;
        r0.putExtra(r2, r1);
        r0 = r19;
        r2 = r0.mPhone;
        r2 = r2.getPhoneId();
        r0 = r18;
        android.telephony.SubscriptionManager.putPhoneIdAndSubIdExtra(r0, r2);
        r2 = android.os.Build.IS_DEBUGGABLE;
        if (r2 == 0) goto L_0x008d;
    L_0x005f:
        r0 = r19;
        r2 = r0.mContext;
        r2 = r2.getContentResolver();
        r4 = "cmas_additional_broadcast_pkg";
        r17 = android.provider.Settings.Secure.getString(r2, r4);
        if (r17 == 0) goto L_0x008d;
    L_0x0070:
        r3 = new android.content.Intent;
        r0 = r18;
        r3.<init>(r0);
        r0 = r17;
        r3.setPackage(r0);
        r0 = r19;
        r2 = r0.mContext;
        r4 = android.os.UserHandle.ALL;
        r8 = r19.getHandler();
        r7 = 0;
        r9 = -1;
        r10 = 0;
        r11 = 0;
        r2.sendOrderedBroadcastAsUser(r3, r4, r5, r6, r7, r8, r9, r10, r11);
    L_0x008d:
        r0 = r19;
        r7 = r0.mContext;
        r9 = android.os.UserHandle.ALL;
        r0 = r19;
        r12 = r0.mReceiver;
        r13 = r19.getHandler();
        r14 = -1;
        r15 = 0;
        r16 = 0;
        r8 = r18;
        r10 = r5;
        r11 = r6;
        r7.sendOrderedBroadcastAsUser(r8, r9, r10, r11, r12, r13, r14, r15, r16);
        return;
    L_0x00a7:
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r4 = "Dispatching SMS CB, SmsCbMessage is: ";
        r2 = r2.append(r4);
        r0 = r20;
        r2 = r2.append(r0);
        r2 = r2.toString();
        r0 = r19;
        r0.log(r2);
        r18 = new android.content.Intent;
        r2 = "android.provider.Telephony.SMS_CB_RECEIVED";
        r0 = r18;
        r0.<init>(r2);
        r2 = 16777216; // 0x1000000 float:2.3509887E-38 double:8.289046E-317;
        r0 = r18;
        r0.addFlags(r2);
        r5 = "android.permission.RECEIVE_SMS";
        r6 = 16;
        goto L_0x0044;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.CellBroadcastHandler.handleBroadcastSms(android.telephony.SmsCbMessage):void");
    }
}
