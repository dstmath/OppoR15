package com.android.server.devicepolicy;

import android.app.admin.SecurityLog.SecurityEvent;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$yPMQJaI1L2rJhTx00Ubn7ktEjSE implements Comparator {
    public static final /* synthetic */ -$Lambda$yPMQJaI1L2rJhTx00Ubn7ktEjSE $INST$0 = new -$Lambda$yPMQJaI1L2rJhTx00Ubn7ktEjSE();

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return Long.signum(((SecurityEvent) arg0).getTimeNanos() - ((SecurityEvent) arg1).getTimeNanos());
    }

    private /* synthetic */ -$Lambda$yPMQJaI1L2rJhTx00Ubn7ktEjSE() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
