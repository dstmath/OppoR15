package com.android.server.pm;

import android.app.IInstantAppResolver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.InstantAppResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IRemoteCallback;
import android.os.IRemoteCallback.Stub;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

final class EphemeralResolverConnection implements DeathRecipient {
    private static final long BIND_SERVICE_TIMEOUT_MS = ((long) (Build.IS_ENG ? 500 : 300));
    private static final long CALL_SERVICE_TIMEOUT_MS = ((long) (Build.IS_ENG ? 200 : 100));
    private static final boolean DEBUG_EPHEMERAL = Build.IS_DEBUGGABLE;
    private static final int STATE_BINDING = 1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PENDING = 2;
    private static final String TAG = "PackageManager";
    @GuardedBy("mLock")
    private int mBindState = 0;
    private final Context mContext;
    private final GetEphemeralResolveInfoCaller mGetEphemeralResolveInfoCaller = new GetEphemeralResolveInfoCaller();
    private final Intent mIntent;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private IInstantAppResolver mRemoteInstance;
    private final ServiceConnection mServiceConnection = new MyServiceConnection();

    public static class ConnectionException extends Exception {
        public static final int FAILURE_BIND = 1;
        public static final int FAILURE_CALL = 2;
        public static final int FAILURE_INTERRUPTED = 3;
        public final int failure;

        public ConnectionException(int _failure) {
            this.failure = _failure;
        }
    }

    private static final class GetEphemeralResolveInfoCaller extends TimedRemoteCaller<List<InstantAppResolveInfo>> {
        private final IRemoteCallback mCallback = new Stub() {
            public void sendResult(Bundle data) throws RemoteException {
                GetEphemeralResolveInfoCaller.this.onRemoteMethodResult(data.getParcelableArrayList("android.app.extra.RESOLVE_INFO"), data.getInt("android.app.extra.SEQUENCE", -1));
            }
        };

        public GetEphemeralResolveInfoCaller() {
            super(EphemeralResolverConnection.CALL_SERVICE_TIMEOUT_MS);
        }

        public List<InstantAppResolveInfo> getEphemeralResolveInfoList(IInstantAppResolver target, int[] hashPrefix, String token) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getInstantAppResolveInfoList(hashPrefix, token, sequence, this.mCallback);
            return (List) getResultTimed(sequence);
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        /* synthetic */ MyServiceConnection(EphemeralResolverConnection this$0, MyServiceConnection -this1) {
            this();
        }

        private MyServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (EphemeralResolverConnection.DEBUG_EPHEMERAL) {
                Slog.d(EphemeralResolverConnection.TAG, "Connected to instant app resolver");
            }
            synchronized (EphemeralResolverConnection.this.mLock) {
                EphemeralResolverConnection.this.mRemoteInstance = IInstantAppResolver.Stub.asInterface(service);
                if (EphemeralResolverConnection.this.mBindState == 2) {
                    EphemeralResolverConnection.this.mBindState = 0;
                }
                try {
                    service.linkToDeath(EphemeralResolverConnection.this, 0);
                } catch (RemoteException e) {
                    EphemeralResolverConnection.this.handleBinderDiedLocked();
                }
                EphemeralResolverConnection.this.mLock.notifyAll();
            }
            return;
        }

        public void onServiceDisconnected(ComponentName name) {
            if (EphemeralResolverConnection.DEBUG_EPHEMERAL) {
                Slog.d(EphemeralResolverConnection.TAG, "Disconnected from instant app resolver");
            }
            synchronized (EphemeralResolverConnection.this.mLock) {
                EphemeralResolverConnection.this.handleBinderDiedLocked();
            }
        }
    }

    public static abstract class PhaseTwoCallback {
        abstract void onPhaseTwoResolved(List<InstantAppResolveInfo> list, long j);
    }

    public EphemeralResolverConnection(Context context, ComponentName componentName, String action) {
        this.mContext = context;
        this.mIntent = new Intent(action).setComponent(componentName);
    }

    public final List<InstantAppResolveInfo> getInstantAppResolveInfoList(int[] hashPrefix, String token) throws ConnectionException {
        throwIfCalledOnMainThread();
        try {
            List<InstantAppResolveInfo> ephemeralResolveInfoList = this.mGetEphemeralResolveInfoCaller.getEphemeralResolveInfoList(getRemoteInstanceLazy(token), hashPrefix, token);
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
            return ephemeralResolveInfoList;
        } catch (TimeoutException e) {
            throw new ConnectionException(2);
        } catch (RemoteException e2) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
                return null;
            }
        } catch (TimeoutException e3) {
            throw new ConnectionException(1);
        } catch (InterruptedException e4) {
            throw new ConnectionException(3);
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
        }
    }

    public final void getInstantAppIntentFilterList(int[] hashPrefix, String token, String hostName, PhaseTwoCallback callback, Handler callbackHandler, long startTime) throws ConnectionException {
        final Handler handler = callbackHandler;
        final PhaseTwoCallback phaseTwoCallback = callback;
        final long j = startTime;
        try {
            getRemoteInstanceLazy(token).getInstantAppIntentFilterList(hashPrefix, token, hostName, new Stub() {
                public void sendResult(Bundle data) throws RemoteException {
                    final ArrayList<InstantAppResolveInfo> resolveList = data.getParcelableArrayList("android.app.extra.RESOLVE_INFO");
                    Handler handler = handler;
                    final PhaseTwoCallback phaseTwoCallback = phaseTwoCallback;
                    final long j = j;
                    handler.post(new Runnable() {
                        public void run() {
                            phaseTwoCallback.onPhaseTwoResolved(resolveList, j);
                        }
                    });
                }
            });
        } catch (TimeoutException e) {
            throw new ConnectionException(1);
        } catch (InterruptedException e2) {
            throw new ConnectionException(3);
        } catch (RemoteException e3) {
        }
    }

    private IInstantAppResolver getRemoteInstanceLazy(String token) throws ConnectionException, TimeoutException, InterruptedException {
        long binderToken = Binder.clearCallingIdentity();
        try {
            IInstantAppResolver bind = bind(token);
            return bind;
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    private void waitForBindLocked(String token) throws TimeoutException, InterruptedException {
        long startMillis = SystemClock.uptimeMillis();
        while (this.mBindState != 0 && this.mRemoteInstance == null) {
            long remainingMillis = BIND_SERVICE_TIMEOUT_MS - (SystemClock.uptimeMillis() - startMillis);
            if (remainingMillis <= 0) {
                throw new TimeoutException("[" + token + "] Didn't bind to resolver in time!");
            }
            this.mLock.wait(remainingMillis);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private IInstantAppResolver bind(String token) throws ConnectionException, TimeoutException, InterruptedException {
        boolean doUnbind = false;
        synchronized (this.mLock) {
            IInstantAppResolver iInstantAppResolver;
            if (this.mRemoteInstance != null) {
                iInstantAppResolver = this.mRemoteInstance;
                return iInstantAppResolver;
            }
            if (this.mBindState == 2) {
                if (DEBUG_EPHEMERAL) {
                    Slog.i(TAG, "[" + token + "] Previous bind timed out; waiting for connection");
                }
                try {
                    waitForBindLocked(token);
                    if (this.mRemoteInstance != null) {
                        iInstantAppResolver = this.mRemoteInstance;
                        return iInstantAppResolver;
                    }
                } catch (TimeoutException e) {
                    doUnbind = true;
                }
            }
            if (this.mBindState == 1) {
                if (DEBUG_EPHEMERAL) {
                    Slog.i(TAG, "[" + token + "] Another thread is binding; waiting for connection");
                }
                waitForBindLocked(token);
                if (this.mRemoteInstance != null) {
                    iInstantAppResolver = this.mRemoteInstance;
                    return iInstantAppResolver;
                }
                throw new ConnectionException(1);
            }
            this.mBindState = 1;
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() == this.mContext.getMainLooper().getThread()) {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    public void binderDied() {
        if (DEBUG_EPHEMERAL) {
            Slog.d(TAG, "Binder to instant app resolver died");
        }
        synchronized (this.mLock) {
            handleBinderDiedLocked();
        }
    }

    private void handleBinderDiedLocked() {
        if (this.mRemoteInstance != null) {
            try {
                this.mRemoteInstance.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mRemoteInstance = null;
    }
}
