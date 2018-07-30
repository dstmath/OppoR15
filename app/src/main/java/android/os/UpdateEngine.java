package android.os;

import android.os.IUpdateEngine.Stub;

public class UpdateEngine {
    private static final String TAG = "UpdateEngine";
    private static final String UPDATE_ENGINE_SERVICE = "android.os.UpdateEngineService";
    private IUpdateEngine mUpdateEngine = Stub.asInterface(ServiceManager.getService(UPDATE_ENGINE_SERVICE));
    private IUpdateEngineCallback mUpdateEngineCallback = null;
    private final Object mUpdateEngineCallbackLock = new Object();

    public static final class ErrorCodeConstants {
        public static final int DOWNLOAD_PAYLOAD_VERIFICATION_ERROR = 12;
        public static final int DOWNLOAD_TRANSFER_ERROR = 9;
        public static final int ERROR = 1;
        public static final int FILESYSTEM_COPIER_ERROR = 4;
        public static final int INSTALL_DEVICE_OPEN_ERROR = 7;
        public static final int KERNEL_DEVICE_OPEN_ERROR = 8;
        public static final int PAYLOAD_HASH_MISMATCH_ERROR = 10;
        public static final int PAYLOAD_MISMATCHED_TYPE_ERROR = 6;
        public static final int PAYLOAD_SIZE_MISMATCH_ERROR = 11;
        public static final int POST_INSTALL_RUNNER_ERROR = 5;
        public static final int SUCCESS = 0;
    }

    public static final class UpdateStatusConstants {
        public static final int ATTEMPTING_ROLLBACK = 8;
        public static final int CHECKING_FOR_UPDATE = 1;
        public static final int DISABLED = 9;
        public static final int DOWNLOADING = 3;
        public static final int FINALIZING = 5;
        public static final int IDLE = 0;
        public static final int REPORTING_ERROR_EVENT = 7;
        public static final int UPDATED_NEED_REBOOT = 6;
        public static final int UPDATE_AVAILABLE = 2;
        public static final int VERIFYING = 4;
    }

    public boolean bind(final UpdateEngineCallback callback, final Handler handler) {
        boolean bind;
        synchronized (this.mUpdateEngineCallbackLock) {
            this.mUpdateEngineCallback = new IUpdateEngineCallback.Stub() {
                public void onStatusUpdate(final int status, final float percent) {
                    if (handler != null) {
                        Handler handler = handler;
                        final UpdateEngineCallback updateEngineCallback = callback;
                        handler.post(new Runnable() {
                            public void run() {
                                updateEngineCallback.onStatusUpdate(status, percent);
                            }
                        });
                        return;
                    }
                    callback.onStatusUpdate(status, percent);
                }

                public void onPayloadApplicationComplete(final int errorCode) {
                    if (handler != null) {
                        Handler handler = handler;
                        final UpdateEngineCallback updateEngineCallback = callback;
                        handler.post(new Runnable() {
                            public void run() {
                                updateEngineCallback.onPayloadApplicationComplete(errorCode);
                            }
                        });
                        return;
                    }
                    callback.onPayloadApplicationComplete(errorCode);
                }
            };
            try {
                bind = this.mUpdateEngine.bind(this.mUpdateEngineCallback);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return bind;
    }

    public boolean bind(UpdateEngineCallback callback) {
        return bind(callback, null);
    }

    public void applyPayload(String url, long offset, long size, String[] headerKeyValuePairs) {
        try {
            this.mUpdateEngine.applyPayload(url, offset, size, headerKeyValuePairs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void cancel() {
        try {
            this.mUpdateEngine.cancel();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void suspend() {
        try {
            this.mUpdateEngine.suspend();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resume() {
        try {
            this.mUpdateEngine.resume();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetStatus() {
        try {
            this.mUpdateEngine.resetStatus();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean unbind() {
        synchronized (this.mUpdateEngineCallbackLock) {
            if (this.mUpdateEngineCallback == null) {
                return true;
            }
            try {
                boolean result = this.mUpdateEngine.unbind(this.mUpdateEngineCallback);
                this.mUpdateEngineCallback = null;
                return result;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }
}
