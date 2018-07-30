package android.speech.tts;

import android.os.SystemClock;

abstract class AbstractEventLogger {
    protected final int mCallerPid;
    protected final int mCallerUid;
    private volatile long mEngineCompleteTime = -1;
    private volatile long mEngineStartTime = -1;
    private boolean mLogWritten = false;
    protected long mPlaybackStartTime = -1;
    protected final long mReceivedTime;
    private volatile long mRequestProcessingStartTime = -1;
    protected final String mServiceApp;

    protected abstract void logFailure(int i);

    protected abstract void logSuccess(long j, long j2, long j3);

    AbstractEventLogger(int callerUid, int callerPid, String serviceApp) {
        this.mCallerUid = callerUid;
        this.mCallerPid = callerPid;
        this.mServiceApp = serviceApp;
        this.mReceivedTime = SystemClock.elapsedRealtime();
    }

    public void onRequestProcessingStart() {
        this.mRequestProcessingStartTime = SystemClock.elapsedRealtime();
    }

    public void onEngineDataReceived() {
        if (this.mEngineStartTime == -1) {
            this.mEngineStartTime = SystemClock.elapsedRealtime();
        }
    }

    public void onEngineComplete() {
        this.mEngineCompleteTime = SystemClock.elapsedRealtime();
    }

    public void onAudioDataWritten() {
        if (this.mPlaybackStartTime == -1) {
            this.mPlaybackStartTime = SystemClock.elapsedRealtime();
        }
    }

    public void onCompleted(int statusCode) {
        if (!this.mLogWritten) {
            this.mLogWritten = true;
            long completionTime = SystemClock.elapsedRealtime();
            if (statusCode != 0 || this.mPlaybackStartTime == -1 || this.mEngineCompleteTime == -1) {
                logFailure(statusCode);
            } else {
                logSuccess(this.mPlaybackStartTime - this.mReceivedTime, this.mEngineStartTime - this.mRequestProcessingStartTime, this.mEngineCompleteTime - this.mRequestProcessingStartTime);
            }
        }
    }
}
