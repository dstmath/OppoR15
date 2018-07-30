package android.security;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.gatekeeper.IGateKeeperService;
import android.service.gatekeeper.IGateKeeperService.Stub;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public abstract class GateKeeper {
    public static final long INVALID_SECURE_USER_ID = 0;

    private GateKeeper() {
    }

    public static IGateKeeperService getService() {
        IGateKeeperService service = Stub.asInterface(ServiceManager.getService("android.service.gatekeeper.IGateKeeperService"));
        if (service != null) {
            return service;
        }
        throw new IllegalStateException("Gatekeeper service not available");
    }

    public static long getSecureUserId() throws IllegalStateException {
        try {
            int userId = UserHandle.myUserId();
            if (userId == MetricsEvent.ASSIST_GESTURE_TRIGGERED) {
                userId = 0;
            }
            return getService().getSecureUserId(userId);
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to obtain secure user ID from gatekeeper", e);
        }
    }
}
