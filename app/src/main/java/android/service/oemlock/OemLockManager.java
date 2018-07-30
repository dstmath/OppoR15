package android.service.oemlock;

import android.os.RemoteException;

public class OemLockManager {
    private IOemLockService mService;

    public OemLockManager(IOemLockService service) {
        this.mService = service;
    }

    public void setOemUnlockAllowedByCarrier(boolean allowed, byte[] signature) {
        try {
            this.mService.setOemUnlockAllowedByCarrier(allowed, signature);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isOemUnlockAllowedByCarrier() {
        try {
            return this.mService.isOemUnlockAllowedByCarrier();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setOemUnlockAllowedByUser(boolean allowed) {
        try {
            this.mService.setOemUnlockAllowedByUser(allowed);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isOemUnlockAllowedByUser() {
        try {
            return this.mService.isOemUnlockAllowedByUser();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isOemUnlockAllowed() {
        try {
            return this.mService.isOemUnlockAllowed();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isDeviceOemUnlocked() {
        try {
            return this.mService.isDeviceOemUnlocked();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
