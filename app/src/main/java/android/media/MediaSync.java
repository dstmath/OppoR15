package android.media;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class MediaSync {
    private static final int CB_RETURN_AUDIO_BUFFER = 1;
    private static final int EVENT_CALLBACK = 1;
    private static final int EVENT_SET_CALLBACK = 2;
    public static final int MEDIASYNC_ERROR_AUDIOTRACK_FAIL = 1;
    public static final int MEDIASYNC_ERROR_SURFACE_FAIL = 2;
    private static final String TAG = "MediaSync";
    private List<AudioBuffer> mAudioBuffers = new LinkedList();
    private Handler mAudioHandler = null;
    private final Object mAudioLock = new Object();
    private Looper mAudioLooper = null;
    private Thread mAudioThread = null;
    private AudioTrack mAudioTrack = null;
    private Callback mCallback = null;
    private Handler mCallbackHandler = null;
    private final Object mCallbackLock = new Object();
    private long mNativeContext;
    private OnErrorListener mOnErrorListener = null;
    private Handler mOnErrorListenerHandler = null;
    private final Object mOnErrorListenerLock = new Object();
    private float mPlaybackRate = 0.0f;

    private static class AudioBuffer {
        public int mBufferIndex;
        public ByteBuffer mByteBuffer;
        long mPresentationTimeUs;

        public AudioBuffer(ByteBuffer byteBuffer, int bufferId, long presentationTimeUs) {
            this.mByteBuffer = byteBuffer;
            this.mBufferIndex = bufferId;
            this.mPresentationTimeUs = presentationTimeUs;
        }
    }

    public static abstract class Callback {
        public abstract void onAudioBufferConsumed(MediaSync mediaSync, ByteBuffer byteBuffer, int i);
    }

    public interface OnErrorListener {
        void onError(MediaSync mediaSync, int i, int i2);
    }

    private final native void native_finalize();

    private final native void native_flush();

    private final native long native_getPlayTimeForPendingAudioFrames();

    private final native boolean native_getTimestamp(MediaTimestamp mediaTimestamp);

    private static final native void native_init();

    private final native void native_release();

    private final native void native_setAudioTrack(AudioTrack audioTrack);

    private native float native_setPlaybackParams(PlaybackParams playbackParams);

    private final native void native_setSurface(Surface surface);

    private native float native_setSyncParams(SyncParams syncParams);

    private final native void native_setup();

    private final native void native_updateQueuedAudioData(int i, long j);

    public final native Surface createInputSurface();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    public MediaSync() {
        native_setup();
    }

    protected void finalize() {
        native_finalize();
    }

    public final void release() {
        returnAudioBuffers();
        if (!(this.mAudioThread == null || this.mAudioLooper == null)) {
            this.mAudioLooper.quit();
        }
        setCallback(null, null);
        native_release();
    }

    public void setCallback(Callback cb, Handler handler) {
        synchronized (this.mCallbackLock) {
            if (handler != null) {
                this.mCallbackHandler = handler;
            } else {
                Looper looper = Looper.myLooper();
                if (looper == null) {
                    looper = Looper.getMainLooper();
                }
                if (looper == null) {
                    this.mCallbackHandler = null;
                } else {
                    this.mCallbackHandler = new Handler(looper);
                }
            }
            this.mCallback = cb;
        }
    }

    public void setOnErrorListener(OnErrorListener listener, Handler handler) {
        synchronized (this.mOnErrorListenerLock) {
            if (handler != null) {
                this.mOnErrorListenerHandler = handler;
            } else {
                Looper looper = Looper.myLooper();
                if (looper == null) {
                    looper = Looper.getMainLooper();
                }
                if (looper == null) {
                    this.mOnErrorListenerHandler = null;
                } else {
                    this.mOnErrorListenerHandler = new Handler(looper);
                }
            }
            this.mOnErrorListener = listener;
        }
    }

    public void setSurface(Surface surface) {
        native_setSurface(surface);
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        native_setAudioTrack(audioTrack);
        this.mAudioTrack = audioTrack;
        if (audioTrack != null && this.mAudioThread == null) {
            createAudioThread();
        }
    }

    public void setPlaybackParams(PlaybackParams params) {
        synchronized (this.mAudioLock) {
            this.mPlaybackRate = native_setPlaybackParams(params);
        }
        if (((double) this.mPlaybackRate) != 0.0d && this.mAudioThread != null) {
            postRenderAudio(0);
        }
    }

    public void setSyncParams(SyncParams params) {
        synchronized (this.mAudioLock) {
            this.mPlaybackRate = native_setSyncParams(params);
        }
        if (((double) this.mPlaybackRate) != 0.0d && this.mAudioThread != null) {
            postRenderAudio(0);
        }
    }

    public void flush() {
        synchronized (this.mAudioLock) {
            this.mAudioBuffers.clear();
            this.mCallbackHandler.removeCallbacksAndMessages(null);
        }
        if (this.mAudioTrack != null) {
            this.mAudioTrack.pause();
            this.mAudioTrack.flush();
            this.mAudioTrack.stop();
        }
        native_flush();
    }

    public MediaTimestamp getTimestamp() {
        try {
            MediaTimestamp timestamp = new MediaTimestamp();
            if (native_getTimestamp(timestamp)) {
                return timestamp;
            }
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public void queueAudio(ByteBuffer audioData, int bufferId, long presentationTimeUs) {
        if (this.mAudioTrack == null || this.mAudioThread == null) {
            throw new IllegalStateException("AudioTrack is NOT set or audio thread is not created");
        }
        synchronized (this.mAudioLock) {
            this.mAudioBuffers.add(new AudioBuffer(audioData, bufferId, presentationTimeUs));
        }
        if (((double) this.mPlaybackRate) != 0.0d) {
            postRenderAudio(0);
        }
    }

    private void postRenderAudio(long delayMillis) {
        this.mAudioHandler.postDelayed(new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                synchronized (MediaSync.this.mAudioLock) {
                    if (((double) MediaSync.this.mPlaybackRate) == 0.0d) {
                        return;
                    } else if (MediaSync.this.mAudioBuffers.isEmpty()) {
                        return;
                    } else {
                        AudioBuffer audioBuffer = (AudioBuffer) MediaSync.this.mAudioBuffers.get(0);
                        int size = audioBuffer.mByteBuffer.remaining();
                        if (size > 0 && MediaSync.this.mAudioTrack.getPlayState() != 3) {
                            try {
                                MediaSync.this.mAudioTrack.play();
                            } catch (IllegalStateException e) {
                                Log.w(MediaSync.TAG, "could not start audio track");
                            }
                        }
                        int sizeWritten = MediaSync.this.mAudioTrack.write(audioBuffer.mByteBuffer, size, 1);
                        if (sizeWritten > 0) {
                            if (audioBuffer.mPresentationTimeUs != -1) {
                                MediaSync.this.native_updateQueuedAudioData(size, audioBuffer.mPresentationTimeUs);
                                audioBuffer.mPresentationTimeUs = -1;
                            }
                            if (sizeWritten == size) {
                                MediaSync.this.postReturnByteBuffer(audioBuffer);
                                MediaSync.this.mAudioBuffers.remove(0);
                                if (!MediaSync.this.mAudioBuffers.isEmpty()) {
                                    MediaSync.this.postRenderAudio(0);
                                }
                            }
                        }
                        MediaSync.this.postRenderAudio(TimeUnit.MICROSECONDS.toMillis(MediaSync.this.native_getPlayTimeForPendingAudioFrames()) / 2);
                        return;
                    }
                }
            }
        }, delayMillis);
    }

    private final void postReturnByteBuffer(final AudioBuffer audioBuffer) {
        synchronized (this.mCallbackLock) {
            if (this.mCallbackHandler != null) {
                MediaSync sync = this;
                this.mCallbackHandler.post(new Runnable() {
                    /* JADX WARNING: inconsistent code. */
                    /* Code decompiled incorrectly, please refer to instructions dump. */
                    public void run() {
                        synchronized (MediaSync.this.mCallbackLock) {
                            Callback callback = MediaSync.this.mCallback;
                            if (MediaSync.this.mCallbackHandler == null || MediaSync.this.mCallbackHandler.getLooper().getThread() != Thread.currentThread()) {
                            }
                        }
                    }
                });
            }
        }
    }

    private final void returnAudioBuffers() {
        synchronized (this.mAudioLock) {
            for (AudioBuffer audioBuffer : this.mAudioBuffers) {
                postReturnByteBuffer(audioBuffer);
            }
            this.mAudioBuffers.clear();
        }
    }

    private void createAudioThread() {
        this.mAudioThread = new Thread() {
            public void run() {
                Looper.prepare();
                synchronized (MediaSync.this.mAudioLock) {
                    MediaSync.this.mAudioLooper = Looper.myLooper();
                    MediaSync.this.mAudioHandler = new Handler();
                    MediaSync.this.mAudioLock.notify();
                }
                Looper.loop();
            }
        };
        this.mAudioThread.start();
        synchronized (this.mAudioLock) {
            try {
                this.mAudioLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }
}
