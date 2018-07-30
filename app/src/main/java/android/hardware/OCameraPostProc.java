package android.hardware;

import android.app.ActivityThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.lang.ref.WeakReference;

public class OCameraPostProc {
    private static final int OPPO_CAMERA_MSG_DATA = 4;
    private static final int OPPO_CAMERA_MSG_NOTIFY = 1;
    private static final int OPPO_CAMERA_MSG_POST = 2;
    private static final String TAG = "OCameraPostProc";
    private DataCallback mDataCallback = null;
    private EventHandler mEventHandler = null;

    public interface DataCallback {
        void onDataCallback(byte[] bArr);

        void onNotifyCallback(int i, int i2);

        void onPostCallback(byte[] bArr);
    }

    private class EventHandler extends Handler {
        public EventHandler(OCameraPostProc camera, Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Log.v(OCameraPostProc.TAG, "handleMessage, message type: " + msg.what);
            switch (msg.what) {
                case OCameraPostProc.OPPO_CAMERA_MSG_NOTIFY /*1*/:
                    if (OCameraPostProc.this.mDataCallback != null) {
                        OCameraPostProc.this.mDataCallback.onNotifyCallback(msg.arg1, msg.arg2);
                    }
                    return;
                case OCameraPostProc.OPPO_CAMERA_MSG_POST /*2*/:
                    if (OCameraPostProc.this.mDataCallback != null) {
                        OCameraPostProc.this.mDataCallback.onPostCallback((byte[]) msg.obj);
                    }
                    return;
                case OCameraPostProc.OPPO_CAMERA_MSG_DATA /*4*/:
                    if (OCameraPostProc.this.mDataCallback != null) {
                        OCameraPostProc.this.mDataCallback.onDataCallback((byte[]) msg.obj);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private final native int connect(Object obj, String str);

    private final native void disconnect();

    static {
        System.loadLibrary("oppocamera_jni");
    }

    private static void postEventFromNative(Object cameraRef, int what, int arg1, int arg2, Object obj) {
        OCameraPostProc camera = (OCameraPostProc) ((WeakReference) cameraRef).get();
        if (camera == null) {
            Log.e(TAG, "postEventFromNative, camera object is dead");
            return;
        }
        if (camera.mEventHandler != null) {
            camera.mEventHandler.sendMessage(camera.mEventHandler.obtainMessage(what, arg1, arg2, obj));
        }
    }

    public int connect(DataCallback dataCallback) {
        this.mDataCallback = dataCallback;
        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            this.mEventHandler = null;
        }
        return connect(new WeakReference(this), ActivityThread.currentOpPackageName());
    }
}
