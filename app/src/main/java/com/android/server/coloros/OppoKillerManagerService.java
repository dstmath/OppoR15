package com.android.server.coloros;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.am.ActivityManagerService;

public class OppoKillerManagerService extends Binder implements IOppoKillerManager {
    private static final String TAG = "OppoKillerManagerService";
    private static OppoKillerManagerService sInstance;
    private final ActivityManagerService mActivityManager;

    public abstract class OKillerContext {
        public static final String OKILLER_SERVICE = "athenaservice";
    }

    public static class SystemServer {
        public static void addService(ActivityManagerService ams) {
            ServiceManager.addService(OKillerContext.OKILLER_SERVICE, OppoKillerManagerService.getInstance(ams));
        }
    }

    private OppoKillerManagerService(ActivityManagerService service) {
        this.mActivityManager = service;
    }

    public static OppoKillerManagerService getInstance(ActivityManagerService service) {
        if (sInstance == null) {
            sInstance = new OppoKillerManagerService(service);
        }
        return sInstance;
    }

    public IBinder asBinder() {
        return this;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        int result;
        switch (code) {
            case 101:
                data.enforceInterface(IOppoKillerManager.DESCRIPTOR);
                result = oppoKill(data.readInt(), data.readInt(), data.readString(), data.readInt(), data.readInt());
                reply.writeNoException();
                reply.writeInt(result);
                return true;
            case 102:
                data.enforceInterface(IOppoKillerManager.DESCRIPTOR);
                result = oppoFreeze(data.readInt(), data.readInt(), data.readString(), data.readInt(), data.readInt());
                reply.writeNoException();
                reply.writeInt(result);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    public int oppoKill(int uid, int pid, String packageName, int level, int flag) {
        if (level == 2) {
            forceStopPackage(packageName, UserHandle.getUserId(uid));
        } else if (level == 1) {
            killPidForce(pid, packageName);
        }
        return 0;
    }

    public int oppoFreeze(int uid, int pid, String packageName, int level, int flag) {
        return 0;
    }

    private void forceStopPackage(String packagename, int userId) {
        Log.d(TAG, "forceStopPackage packagename " + packagename + " userId " + userId);
        this.mActivityManager.forceStopPackage(packagename, userId);
    }

    private void killPidForce(int pid, String packageNameOrProcessName) {
        Log.d(TAG, "killPidForce pid " + pid);
        Process.killProcess(pid);
    }
}
