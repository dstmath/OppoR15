package com.android.server.coloros;

import android.os.IInterface;
import android.os.RemoteException;

public interface IOppoKillerManager extends IInterface {
    public static final String DESCRIPTOR = "okillerservice";
    public static final String EMPTY_PACKAGE = "";
    public static final int LEVEL_FORCESTOP = 2;
    public static final int LEVEL_KILL = 1;
    public static final int OPPO_KILLER_BINDER_CODE_OFFSET = 100;
    public static final int TRANSACTION_OPPO_FREEZE = 102;
    public static final int TRANSACTION_OPPO_KILL = 101;

    int oppoFreeze(int i, int i2, String str, int i3, int i4) throws RemoteException;

    int oppoKill(int i, int i2, String str, int i3, int i4) throws RemoteException;
}
