package android.media;

import android.app.ActivityThread;
import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.app.backup.FullBackup;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes.Builder;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;
import android.media.MediaTimeProvider.OnMediaTimeListener;
import android.media.SubtitleController.Anchor;
import android.media.SubtitleController.Listener;
import android.media.SubtitleTrack.RenderingWidget;
import android.media.VolumeShaper.Configuration;
import android.media.VolumeShaper.Operation;
import android.media.VolumeShaper.State;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.util.Preconditions;
import com.oppo.media.OppoMediaPlayer;
import com.oppo.media.OppoMultimediaManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import libcore.io.IoBridge;
import libcore.io.Libcore;
import libcore.io.Streams;

public class MediaPlayer extends PlayerBase implements Listener, VolumeAutomation {
    public static final boolean APPLY_METADATA_FILTER = true;
    public static final boolean BYPASS_METADATA_FILTER = false;
    private static final String CTS_TEST_PACKAGE = ".cts";
    public static final int ERROR_CANNOT_CONNECT_TO_SERVER = 261;
    private static final String GOOGLE_MUSIC_PACKAGE = "com.google.android.music";
    private static final String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE = 2;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE_FD = 3;
    private static final int INVOKE_ID_DESELECT_TRACK = 5;
    private static final int INVOKE_ID_GET_SELECTED_TRACK = 7;
    private static final int INVOKE_ID_GET_TRACK_INFO = 1;
    private static final int INVOKE_ID_SELECT_TRACK = 4;
    private static final int INVOKE_ID_SET_VIDEO_SCALE_MODE = 6;
    private static final int KEY_PARAMETER_AUDIO_ATTRIBUTES = 1400;
    private static final int KEY_PARAMETER_INTERCEPT = 10011;
    private static final int KEY_PARAMETER_STREAMTYPE_CHANGE = 10111;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_DRM_INFO = 210;
    private static final int MEDIA_ERROR = 100;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_SYSTEM = Integer.MIN_VALUE;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    private static final int MEDIA_INFO = 200;
    public static final int MEDIA_INFO_AUDIO_NOT_PLAYING = 804;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;
    public static final int MEDIA_INFO_HAS_UNSUPPORT_VIDEO = 860;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_VIDEO_NOT_PLAYING = 805;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    private static final int MEDIA_META_DATA = 202;
    public static final String MEDIA_MIMETYPE_TEXT_CEA_608 = "text/cea-608";
    public static final String MEDIA_MIMETYPE_TEXT_CEA_708 = "text/cea-708";
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
    public static final String MEDIA_MIMETYPE_TEXT_VTT = "text/vtt";
    private static final int MEDIA_NOP = 0;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    public static final int MEDIA_PLAYER_IDLE = 1;
    public static final int MEDIA_PLAYER_INITIALIZED = 2;
    public static final int MEDIA_PLAYER_PAUSED = 32;
    public static final int MEDIA_PLAYER_PLAYBACK_COMPLETE = 128;
    public static final int MEDIA_PLAYER_PREPARED = 8;
    public static final int MEDIA_PLAYER_PREPARING = 4;
    public static final int MEDIA_PLAYER_STARTED = 16;
    public static final int MEDIA_PLAYER_STOPPED = 64;
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_TIMED_TEXT = 99;
    public static final boolean METADATA_ALL = false;
    public static final boolean METADATA_UPDATE_ONLY = true;
    public static final int MUTE_FLAG = 1;
    public static final int PLAYBACK_RATE_AUDIO_MODE_DEFAULT = 0;
    public static final int PLAYBACK_RATE_AUDIO_MODE_RESAMPLE = 2;
    public static final int PLAYBACK_RATE_AUDIO_MODE_STRETCH = 1;
    public static final int PREPARE_DRM_STATUS_PREPARATION_ERROR = 3;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR = 1;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR = 2;
    public static final int PREPARE_DRM_STATUS_SUCCESS = 0;
    public static final int SEEK_CLOSEST = 3;
    public static final int SEEK_CLOSEST_SYNC = 2;
    public static final int SEEK_NEXT_SYNC = 1;
    public static final int SEEK_PREVIOUS_SYNC = 0;
    public static final int STREAMTYPE_CHANGE_FLAG = 1;
    public static final int STREAMTYPE_UNCHANGE_FLAG = 0;
    private static final String SYSTEM_NOTIFICATION_AUDIO_PATH = "/system/media/audio/notifications/";
    private static final String TAG = "MediaPlayer";
    public static final int UNMUTE_FLAG = 0;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    private boolean isCreateOppoMediaPlayer = false;
    private boolean isOppoCreate = false;
    private boolean mActiveDrmScheme;
    private int mAudioSessionId = -1;
    private int mAudioStreamType = -1;
    private boolean mBypassInterruptionPolicy;
    private Context mContext = null;
    private int mCurrentState;
    private boolean mDebugLog = true;
    private boolean mDefaultPlayerStarted = false;
    private boolean mDisableOppoMedia = false;
    private boolean mDrmConfigAllowed;
    private DrmInfo mDrmInfo;
    private boolean mDrmInfoResolved;
    private final Object mDrmLock = new Object();
    private MediaDrm mDrmObj;
    private boolean mDrmProvisioningInProgress;
    private ProvisioningThread mDrmProvisioningThread;
    private byte[] mDrmSessionId;
    private UUID mDrmUUID;
    private EventHandler mEventHandler;
    private FileDescriptor mFd = null;
    private Map<String, String> mHeaders = null;
    private BitSet mInbandTrackIndices = new BitSet();
    private Vector<Pair<Integer, SubtitleTrack>> mIndexTrackPairs = new Vector();
    private boolean mInterceptFlag = false;
    private long mLength = -1;
    private int mListenerContext;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private boolean mNeedMute = false;
    private boolean mNeedSeeking = false;
    private boolean mNotUsingOppoMedia = false;
    private final INotificationManager mNotificationManager;
    private long mOffset = -1;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private final OnCompletionListener mOnCompletionInternalListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            MediaPlayer.this.baseStop();
        }
    };
    private OnCompletionListener mOnCompletionListener;
    private OnDrmConfigHelper mOnDrmConfigHelper;
    private OnDrmInfoHandlerDelegate mOnDrmInfoHandlerDelegate;
    private OnDrmPreparedHandlerDelegate mOnDrmPreparedHandlerDelegate;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnPreparedListener mOnPreparedListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnSubtitleDataListener mOnSubtitleDataListener;
    private OnTimedMetaDataAvailableListener mOnTimedMetaDataAvailableListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private Vector<InputStream> mOpenSubtitleSources;
    private OppoMediaPlayer mOppoMediaPlayer = null;
    private String mPath = null;
    private boolean mPrepareAsync = true;
    private boolean mPrepareDrmInProgress;
    private boolean mRecoverFlag = false;
    private boolean mScreenOn = false;
    private boolean mScreenOnWhilePlaying;
    private int mSeekMs = -1;
    private int mSelectedSubtitleTrackIndex = -1;
    private boolean mStayAwake;
    private int mStreamType = Integer.MIN_VALUE;
    private SubtitleController mSubtitleController;
    private OnSubtitleDataListener mSubtitleDataListener = new OnSubtitleDataListener() {
        public void onSubtitleData(MediaPlayer mp, SubtitleData data) {
            int index = data.getTrackIndex();
            synchronized (MediaPlayer.this.mIndexTrackPairs) {
                for (Pair<Integer, SubtitleTrack> p : MediaPlayer.this.mIndexTrackPairs) {
                    if (!(p.first == null || ((Integer) p.first).intValue() != index || p.second == null)) {
                        p.second.onData(data);
                    }
                }
            }
        }
    };
    private Surface mSurface = null;
    private SurfaceHolder mSurfaceHolder;
    private TimeProvider mTimeProvider;
    private Uri mUri = null;
    private int mUsage = -1;
    private WakeLock mWakeLock = null;

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mediaPlayer);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mediaPlayer);
    }

    /* renamed from: android.media.MediaPlayer$14 */
    static class AnonymousClass14 implements Runnable {
        final /* synthetic */ MediaPlayer val$mp;

        AnonymousClass14(MediaPlayer mediaPlayer) {
            this.val$mp = mediaPlayer;
        }

        public void run() {
            this.val$mp.start();
        }
    }

    public interface OnSubtitleDataListener {
        void onSubtitleData(MediaPlayer mediaPlayer, SubtitleData subtitleData);
    }

    public static final class DrmInfo {
        private Map<UUID, byte[]> mapPssh;
        private UUID[] supportedSchemes;

        /* synthetic */ DrmInfo(Parcel parcel, DrmInfo -this1) {
            this(parcel);
        }

        public Map<UUID, byte[]> getPssh() {
            return this.mapPssh;
        }

        public UUID[] getSupportedSchemes() {
            return this.supportedSchemes;
        }

        private DrmInfo(Map<UUID, byte[]> Pssh, UUID[] SupportedSchemes) {
            this.mapPssh = Pssh;
            this.supportedSchemes = SupportedSchemes;
        }

        private DrmInfo(Parcel parcel) {
            Log.v(MediaPlayer.TAG, "DrmInfo(" + parcel + ") size " + parcel.dataSize());
            int psshsize = parcel.readInt();
            byte[] pssh = new byte[psshsize];
            parcel.readByteArray(pssh);
            Log.v(MediaPlayer.TAG, "DrmInfo() PSSH: " + arrToHex(pssh));
            this.mapPssh = parsePSSH(pssh, psshsize);
            Log.v(MediaPlayer.TAG, "DrmInfo() PSSH: " + this.mapPssh);
            int supportedDRMsCount = parcel.readInt();
            this.supportedSchemes = new UUID[supportedDRMsCount];
            for (int i = 0; i < supportedDRMsCount; i++) {
                byte[] uuid = new byte[16];
                parcel.readByteArray(uuid);
                this.supportedSchemes[i] = bytesToUUID(uuid);
                Log.v(MediaPlayer.TAG, "DrmInfo() supportedScheme[" + i + "]: " + this.supportedSchemes[i]);
            }
            Log.v(MediaPlayer.TAG, "DrmInfo() Parcel psshsize: " + psshsize + " supportedDRMsCount: " + supportedDRMsCount);
        }

        private DrmInfo makeCopy() {
            return new DrmInfo(this.mapPssh, this.supportedSchemes);
        }

        private String arrToHex(byte[] bytes) {
            String out = "0x";
            for (int i = 0; i < bytes.length; i++) {
                out = out + String.format("%02x", new Object[]{Byte.valueOf(bytes[i])});
            }
            return out;
        }

        private UUID bytesToUUID(byte[] uuid) {
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb |= (((long) uuid[i]) & 255) << ((7 - i) * 8);
                lsb |= (((long) uuid[i + 8]) & 255) << ((7 - i) * 8);
            }
            return new UUID(msb, lsb);
        }

        private Map<UUID, byte[]> parsePSSH(byte[] pssh, int psshsize) {
            Map<UUID, byte[]> result = new HashMap();
            int len = psshsize;
            int numentries = 0;
            int i = 0;
            while (len > 0) {
                if (len < 16) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse UUID: (%d < 16) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(psshsize)}));
                    return null;
                }
                UUID uuid = bytesToUUID(Arrays.copyOfRange(pssh, i, i + 16));
                i += 16;
                len -= 16;
                if (len < 4) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse datalen: (%d < 4) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(psshsize)}));
                    return null;
                }
                int datalen;
                byte[] subset = Arrays.copyOfRange(pssh, i, i + 4);
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    datalen = ((((subset[3] & 255) << 24) | ((subset[2] & 255) << 16)) | ((subset[1] & 255) << 8)) | (subset[0] & 255);
                } else {
                    datalen = ((((subset[0] & 255) << 24) | ((subset[1] & 255) << 16)) | ((subset[2] & 255) << 8)) | (subset[3] & 255);
                }
                i += 4;
                len -= 4;
                if (len < datalen) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse data: (%d < %d) pssh: %d", new Object[]{Integer.valueOf(len), Integer.valueOf(datalen), Integer.valueOf(psshsize)}));
                    return null;
                }
                byte[] data = Arrays.copyOfRange(pssh, i, i + datalen);
                i += datalen;
                len -= datalen;
                Log.v(MediaPlayer.TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d", new Object[]{Integer.valueOf(numentries), uuid, arrToHex(data), Integer.valueOf(psshsize)}));
                numentries++;
                result.put(uuid, data);
            }
            return result;
        }
    }

    private class EventHandler extends Handler {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            this.mMediaPlayer = mp;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer.TAG, "mediaplayer went away with unhandled events");
                return;
            }
            OnCompletionListener onCompletionListener;
            TimeProvider timeProvider;
            Parcel parcel;
            switch (msg.what) {
                case 0:
                    break;
                case 1:
                    try {
                        MediaPlayer.this.scanInternalSubtitleTracks();
                    } catch (RuntimeException e) {
                        sendMessage(obtainMessage(100, 1, MediaPlayer.MEDIA_ERROR_UNSUPPORTED, null));
                    }
                    OnPreparedListener onPreparedListener = MediaPlayer.this.mOnPreparedListener;
                    if (onPreparedListener != null) {
                        onPreparedListener.onPrepared(this.mMediaPlayer);
                    }
                    return;
                case 2:
                    MediaPlayer.this.mOnCompletionInternalListener.onCompletion(this.mMediaPlayer);
                    onCompletionListener = MediaPlayer.this.mOnCompletionListener;
                    if (onCompletionListener != null) {
                        onCompletionListener.onCompletion(this.mMediaPlayer);
                    }
                    MediaPlayer.this.stayAwake(false);
                    return;
                case 3:
                    OnBufferingUpdateListener onBufferingUpdateListener = MediaPlayer.this.mOnBufferingUpdateListener;
                    if (onBufferingUpdateListener != null) {
                        onBufferingUpdateListener.onBufferingUpdate(this.mMediaPlayer, msg.arg1);
                    }
                    return;
                case 4:
                    OnSeekCompleteListener onSeekCompleteListener = MediaPlayer.this.mOnSeekCompleteListener;
                    if (onSeekCompleteListener != null) {
                        onSeekCompleteListener.onSeekComplete(this.mMediaPlayer);
                        break;
                    }
                    break;
                case 5:
                    OnVideoSizeChangedListener onVideoSizeChangedListener = MediaPlayer.this.mOnVideoSizeChangedListener;
                    if (onVideoSizeChangedListener != null) {
                        onVideoSizeChangedListener.onVideoSizeChanged(this.mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    return;
                case 6:
                case 7:
                    timeProvider = MediaPlayer.this.mTimeProvider;
                    if (timeProvider != null) {
                        timeProvider.onPaused(msg.what == 7);
                        break;
                    }
                    break;
                case 8:
                    timeProvider = MediaPlayer.this.mTimeProvider;
                    if (timeProvider != null) {
                        timeProvider.onStopped();
                        break;
                    }
                    break;
                case 9:
                    break;
                case 99:
                    OnTimedTextListener onTimedTextListener = MediaPlayer.this.mOnTimedTextListener;
                    if (onTimedTextListener != null) {
                        if (msg.obj == null) {
                            onTimedTextListener.onTimedText(this.mMediaPlayer, null);
                        } else if (msg.obj instanceof Parcel) {
                            parcel = msg.obj;
                            TimedText timedText = new TimedText(parcel);
                            parcel.recycle();
                            onTimedTextListener.onTimedText(this.mMediaPlayer, timedText);
                        }
                        return;
                    }
                    return;
                case 100:
                    Log.e(MediaPlayer.TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                    OnErrorListener onErrorListener;
                    if (msg.arg1 == 100 || msg.arg1 == -38 || msg.arg2 == -19 || msg.arg2 == -38 || MediaPlayer.this.mCurrentState == 64 || MediaPlayer.this.mCurrentState == 261 || MediaPlayer.this.mDisableOppoMedia || MediaPlayer.this.mNotUsingOppoMedia) {
                        int error_was_handled = 0;
                        onErrorListener = MediaPlayer.this.mOnErrorListener;
                        if (onErrorListener != null) {
                            error_was_handled = onErrorListener.onError(this.mMediaPlayer, msg.arg1, msg.arg2);
                        }
                        onCompletionListener = MediaPlayer.this.mOnCompletionListener;
                        if (!(onCompletionListener == null || (error_was_handled ^ 1) == 0)) {
                            onCompletionListener.onCompletion(this.mMediaPlayer);
                        }
                        MediaPlayer.this.stayAwake(false);
                    } else if (!(MediaPlayer.this.isOppoCreate || MediaPlayer.this.handleMediaPlayerError())) {
                        int was_handled = 0;
                        onErrorListener = MediaPlayer.this.mOnErrorListener;
                        if (onErrorListener != null) {
                            was_handled = onErrorListener.onError(this.mMediaPlayer, msg.arg1, msg.arg2);
                        }
                        onCompletionListener = MediaPlayer.this.mOnCompletionListener;
                        if (!(onCompletionListener == null || (was_handled ^ 1) == 0)) {
                            onCompletionListener.onCompletion(this.mMediaPlayer);
                        }
                        MediaPlayer.this.stayAwake(false);
                        return;
                    }
                    return;
                case 200:
                    switch (msg.arg1) {
                        case 700:
                            Log.i(MediaPlayer.TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START /*701*/:
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END /*702*/:
                            timeProvider = MediaPlayer.this.mTimeProvider;
                            if (timeProvider != null) {
                                timeProvider.onBuffering(msg.arg1 == 701);
                                break;
                            }
                            break;
                        case 802:
                            try {
                                MediaPlayer.this.scanInternalSubtitleTracks();
                                break;
                            } catch (RuntimeException e2) {
                                sendMessage(obtainMessage(100, 1, MediaPlayer.MEDIA_ERROR_UNSUPPORTED, null));
                                break;
                            }
                        case 803:
                            break;
                    }
                    msg.arg1 = 802;
                    if (MediaPlayer.this.mSubtitleController != null) {
                        MediaPlayer.this.mSubtitleController.selectDefaultTrack();
                    }
                    OnInfoListener onInfoListener = MediaPlayer.this.mOnInfoListener;
                    if (onInfoListener != null) {
                        onInfoListener.onInfo(this.mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    return;
                case 201:
                    OnSubtitleDataListener onSubtitleDataListener = MediaPlayer.this.mOnSubtitleDataListener;
                    if (onSubtitleDataListener != null && (msg.obj instanceof Parcel)) {
                        parcel = (Parcel) msg.obj;
                        SubtitleData data = new SubtitleData(parcel);
                        parcel.recycle();
                        onSubtitleDataListener.onSubtitleData(this.mMediaPlayer, data);
                    }
                    return;
                case 202:
                    OnTimedMetaDataAvailableListener onTimedMetaDataAvailableListener = MediaPlayer.this.mOnTimedMetaDataAvailableListener;
                    if (onTimedMetaDataAvailableListener != null && (msg.obj instanceof Parcel)) {
                        parcel = (Parcel) msg.obj;
                        TimedMetaData data2 = TimedMetaData.createTimedMetaDataFromParcel(parcel);
                        parcel.recycle();
                        onTimedMetaDataAvailableListener.onTimedMetaDataAvailable(this.mMediaPlayer, data2);
                    }
                    return;
                case 210:
                    Log.v(MediaPlayer.TAG, "MEDIA_DRM_INFO " + MediaPlayer.this.mOnDrmInfoHandlerDelegate);
                    if (msg.obj == null) {
                        Log.w(MediaPlayer.TAG, "MEDIA_DRM_INFO msg.obj=NULL");
                    } else if (msg.obj instanceof Parcel) {
                        OnDrmInfoHandlerDelegate onDrmInfoHandlerDelegate;
                        DrmInfo drmInfo = null;
                        synchronized (MediaPlayer.this.mDrmLock) {
                            if (!(MediaPlayer.this.mOnDrmInfoHandlerDelegate == null || MediaPlayer.this.mDrmInfo == null)) {
                                drmInfo = MediaPlayer.this.mDrmInfo.makeCopy();
                            }
                            onDrmInfoHandlerDelegate = MediaPlayer.this.mOnDrmInfoHandlerDelegate;
                        }
                        if (onDrmInfoHandlerDelegate != null) {
                            onDrmInfoHandlerDelegate.notifyClient(drmInfo);
                        }
                    } else {
                        Log.w(MediaPlayer.TAG, "MEDIA_DRM_INFO msg.obj of unexpected type " + msg.obj);
                    }
                    return;
                default:
                    Log.e(MediaPlayer.TAG, "Unknown message type " + msg.what);
                    return;
            }
        }
    }

    public static final class MetricsConstants {
        public static final String CODEC_AUDIO = "android.media.mediaplayer.audio.codec";
        public static final String CODEC_VIDEO = "android.media.mediaplayer.video.codec";
        public static final String DURATION = "android.media.mediaplayer.durationMs";
        public static final String ERRORS = "android.media.mediaplayer.err";
        public static final String ERROR_CODE = "android.media.mediaplayer.errcode";
        public static final String FRAMES = "android.media.mediaplayer.frames";
        public static final String FRAMES_DROPPED = "android.media.mediaplayer.dropped";
        public static final String HEIGHT = "android.media.mediaplayer.height";
        public static final String MIME_TYPE_AUDIO = "android.media.mediaplayer.audio.mime";
        public static final String MIME_TYPE_VIDEO = "android.media.mediaplayer.video.mime";
        public static final String PLAYING = "android.media.mediaplayer.playingMs";
        public static final String WIDTH = "android.media.mediaplayer.width";

        private MetricsConstants() {
        }
    }

    public static final class NoDrmSchemeException extends MediaDrmException {
        public NoDrmSchemeException(String detailMessage) {
            super(detailMessage);
        }
    }

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(MediaPlayer mediaPlayer, int i);
    }

    public interface OnDrmConfigHelper {
        void onDrmConfig(MediaPlayer mediaPlayer);
    }

    private class OnDrmInfoHandlerDelegate {
        private Handler mHandler;
        private MediaPlayer mMediaPlayer;
        private OnDrmInfoListener mOnDrmInfoListener;

        OnDrmInfoHandlerDelegate(MediaPlayer mp, OnDrmInfoListener listener, Handler handler) {
            this.mMediaPlayer = mp;
            this.mOnDrmInfoListener = listener;
            if (handler != null) {
                this.mHandler = handler;
            }
        }

        void notifyClient(final DrmInfo drmInfo) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        OnDrmInfoHandlerDelegate.this.mOnDrmInfoListener.onDrmInfo(OnDrmInfoHandlerDelegate.this.mMediaPlayer, drmInfo);
                    }
                });
            } else {
                this.mOnDrmInfoListener.onDrmInfo(this.mMediaPlayer, drmInfo);
            }
        }
    }

    public interface OnDrmInfoListener {
        void onDrmInfo(MediaPlayer mediaPlayer, DrmInfo drmInfo);
    }

    private class OnDrmPreparedHandlerDelegate {
        private Handler mHandler;
        private MediaPlayer mMediaPlayer;
        private OnDrmPreparedListener mOnDrmPreparedListener;

        OnDrmPreparedHandlerDelegate(MediaPlayer mp, OnDrmPreparedListener listener, Handler handler) {
            this.mMediaPlayer = mp;
            this.mOnDrmPreparedListener = listener;
            if (handler != null) {
                this.mHandler = handler;
            } else if (MediaPlayer.this.mEventHandler != null) {
                this.mHandler = MediaPlayer.this.mEventHandler;
            } else {
                Log.e(MediaPlayer.TAG, "OnDrmPreparedHandlerDelegate: Unexpected null mEventHandler");
            }
        }

        void notifyClient(final int status) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        OnDrmPreparedHandlerDelegate.this.mOnDrmPreparedListener.onDrmPrepared(OnDrmPreparedHandlerDelegate.this.mMediaPlayer, status);
                    }
                });
            } else {
                Log.e(MediaPlayer.TAG, "OnDrmPreparedHandlerDelegate:notifyClient: Unexpected null mHandler");
            }
        }
    }

    public interface OnDrmPreparedListener {
        void onDrmPrepared(MediaPlayer mediaPlayer, int i);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnInfoListener {
        boolean onInfo(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mediaPlayer);
    }

    public interface OnTimedMetaDataAvailableListener {
        void onTimedMetaDataAvailable(MediaPlayer mediaPlayer, TimedMetaData timedMetaData);
    }

    public interface OnTimedTextListener {
        void onTimedText(MediaPlayer mediaPlayer, TimedText timedText);
    }

    public static final class ProvisioningNetworkErrorException extends MediaDrmException {
        public ProvisioningNetworkErrorException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningServerErrorException extends MediaDrmException {
        public ProvisioningServerErrorException(String detailMessage) {
            super(detailMessage);
        }
    }

    private class ProvisioningThread extends Thread {
        public static final int TIMEOUT_MS = 60000;
        private Object drmLock;
        private boolean finished;
        private MediaPlayer mediaPlayer;
        private OnDrmPreparedHandlerDelegate onDrmPreparedHandlerDelegate;
        private int status;
        private String urlStr;
        private UUID uuid;

        /* synthetic */ ProvisioningThread(MediaPlayer this$0, ProvisioningThread -this1) {
            this();
        }

        private ProvisioningThread() {
        }

        public int status() {
            return this.status;
        }

        public ProvisioningThread initialize(ProvisionRequest request, UUID uuid, MediaPlayer mediaPlayer) {
            this.drmLock = mediaPlayer.mDrmLock;
            this.onDrmPreparedHandlerDelegate = mediaPlayer.mOnDrmPreparedHandlerDelegate;
            this.mediaPlayer = mediaPlayer;
            this.urlStr = request.getDefaultUrl() + "&signedRequest=" + new String(request.getData());
            this.uuid = uuid;
            this.status = 3;
            Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread is initialised url: " + this.urlStr);
            return this;
        }

        public void run() {
            int i = 0;
            byte[] bArr = null;
            boolean provisioningSucceeded = false;
            try {
                URL url = new URL(this.urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    connection.setConnectTimeout(TIMEOUT_MS);
                    connection.setReadTimeout(TIMEOUT_MS);
                    connection.connect();
                    bArr = Streams.readFully(connection.getInputStream());
                    Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread run: response " + bArr.length + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + bArr);
                } catch (Exception e) {
                    this.status = 1;
                    Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: connect " + e + " url: " + url);
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                this.status = 1;
                Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: openConnection " + e2);
            }
            if (bArr != null) {
                try {
                    MediaPlayer.this.mDrmObj.provideProvisionResponse(bArr);
                    Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse SUCCEEDED!");
                    provisioningSucceeded = true;
                } catch (Exception e22) {
                    this.status = 2;
                    Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse " + e22);
                }
            }
            boolean succeeded = false;
            if (this.onDrmPreparedHandlerDelegate != null) {
                synchronized (this.drmLock) {
                    if (provisioningSucceeded) {
                        succeeded = this.mediaPlayer.resumePrepareDrm(this.uuid);
                        if (!succeeded) {
                            i = 3;
                        }
                        this.status = i;
                    }
                    this.mediaPlayer.mDrmProvisioningInProgress = false;
                    this.mediaPlayer.mPrepareDrmInProgress = false;
                    if (!succeeded) {
                        MediaPlayer.this.cleanDrmObj();
                    }
                }
                this.onDrmPreparedHandlerDelegate.notifyClient(this.status);
            } else {
                if (provisioningSucceeded) {
                    int i2;
                    succeeded = this.mediaPlayer.resumePrepareDrm(this.uuid);
                    if (succeeded) {
                        i2 = 0;
                    } else {
                        i2 = 3;
                    }
                    this.status = i2;
                }
                this.mediaPlayer.mDrmProvisioningInProgress = false;
                this.mediaPlayer.mPrepareDrmInProgress = false;
                if (!succeeded) {
                    MediaPlayer.this.cleanDrmObj();
                }
            }
            this.finished = true;
        }
    }

    static class TimeProvider implements OnSeekCompleteListener, MediaTimeProvider {
        private static final long MAX_EARLY_CALLBACK_US = 1000;
        private static final long MAX_NS_WITHOUT_POSITION_CHECK = 5000000000L;
        private static final int NOTIFY = 1;
        private static final int NOTIFY_SEEK = 3;
        private static final int NOTIFY_STOP = 2;
        private static final int NOTIFY_TIME = 0;
        private static final int NOTIFY_TRACK_DATA = 4;
        private static final int REFRESH_AND_NOTIFY_TIME = 1;
        private static final String TAG = "MTP";
        private static final long TIME_ADJUSTMENT_RATE = 2;
        public boolean DEBUG = false;
        private boolean mBuffering;
        private Handler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastNanoTime;
        private long mLastReportedTime;
        private long mLastTimeUs = 0;
        private OnMediaTimeListener[] mListeners;
        private boolean mPaused = true;
        private boolean mPausing = false;
        private MediaPlayer mPlayer;
        private boolean mRefresh = false;
        private boolean mSeeking = false;
        private boolean mStopped = true;
        private long mTimeAdjustment;
        private long[] mTimes;

        private class EventHandler extends Handler {
            public EventHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    switch (msg.arg1) {
                        case 0:
                            TimeProvider.this.notifyTimedEvent(false);
                            return;
                        case 1:
                            TimeProvider.this.notifyTimedEvent(true);
                            return;
                        case 2:
                            TimeProvider.this.notifyStop();
                            return;
                        case 3:
                            TimeProvider.this.notifySeek();
                            return;
                        case 4:
                            TimeProvider.this.notifyTrackData((Pair) msg.obj);
                            return;
                        default:
                            return;
                    }
                }
            }
        }

        public TimeProvider(MediaPlayer mp) {
            this.mPlayer = mp;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
            }
            Looper looper = Looper.myLooper();
            if (looper == null) {
                looper = Looper.getMainLooper();
                if (looper == null) {
                    this.mHandlerThread = new HandlerThread("MediaPlayerMTPEventThread", -2);
                    this.mHandlerThread.start();
                    looper = this.mHandlerThread.getLooper();
                }
            }
            this.mEventHandler = new EventHandler(looper);
            this.mListeners = new OnMediaTimeListener[0];
            this.mTimes = new long[0];
            this.mLastTimeUs = 0;
            this.mTimeAdjustment = 0;
        }

        private void scheduleNotification(int type, long delayUs) {
            if (!this.mSeeking || (type != 0 && type != 1)) {
                if (this.DEBUG) {
                    Log.v(TAG, "scheduleNotification " + type + " in " + delayUs);
                }
                this.mEventHandler.removeMessages(1);
                this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(1, type, 0), (long) ((int) (delayUs / MAX_EARLY_CALLBACK_US)));
            }
        }

        public void close() {
            this.mEventHandler.removeMessages(1);
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
                this.mHandlerThread = null;
            }
        }

        protected void finalize() {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
            }
        }

        public void onPaused(boolean paused) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onPaused: " + paused);
                }
                if (this.mStopped) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    scheduleNotification(3, 0);
                } else {
                    this.mPausing = paused;
                    this.mSeeking = false;
                    scheduleNotification(1, 0);
                }
            }
        }

        public void onBuffering(boolean buffering) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onBuffering: " + buffering);
                }
                this.mBuffering = buffering;
                scheduleNotification(1, 0);
            }
        }

        public void onStopped() {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onStopped");
                }
                this.mPaused = true;
                this.mStopped = true;
                this.mSeeking = false;
                this.mBuffering = false;
                scheduleNotification(2, 0);
            }
        }

        public void onSeekComplete(MediaPlayer mp) {
            synchronized (this) {
                this.mStopped = false;
                this.mSeeking = true;
                scheduleNotification(3, 0);
            }
        }

        public void onNewPlayer() {
            if (this.mRefresh) {
                synchronized (this) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    this.mBuffering = false;
                    scheduleNotification(3, 0);
                }
            }
        }

        private synchronized void notifySeek() {
            synchronized (this) {
                this.mSeeking = false;
                try {
                    long timeUs = getCurrentTimeUs(true, false);
                    if (this.DEBUG) {
                        Log.d(TAG, "onSeekComplete at " + timeUs);
                    }
                    for (OnMediaTimeListener listener : this.mListeners) {
                        if (listener == null) {
                            break;
                        }
                        listener.onSeek(timeUs);
                    }
                } catch (IllegalStateException e) {
                    if (this.DEBUG) {
                        Log.d(TAG, "onSeekComplete but no player");
                    }
                    this.mPausing = true;
                    notifyTimedEvent(false);
                }
            }
        }

        private synchronized void notifyTrackData(Pair<SubtitleTrack, byte[]> trackData) {
            trackData.first.onData(trackData.second, true, -1);
        }

        private synchronized void notifyStop() {
            for (OnMediaTimeListener listener : this.mListeners) {
                if (listener == null) {
                    break;
                }
                listener.onStop();
            }
        }

        private int registerListener(OnMediaTimeListener listener) {
            int i = 0;
            while (i < this.mListeners.length && this.mListeners[i] != listener && this.mListeners[i] != null) {
                i++;
            }
            if (i >= this.mListeners.length) {
                OnMediaTimeListener[] newListeners = new OnMediaTimeListener[(i + 1)];
                long[] newTimes = new long[(i + 1)];
                System.arraycopy(this.mListeners, 0, newListeners, 0, this.mListeners.length);
                System.arraycopy(this.mTimes, 0, newTimes, 0, this.mTimes.length);
                this.mListeners = newListeners;
                this.mTimes = newTimes;
            }
            if (this.mListeners[i] == null) {
                this.mListeners[i] = listener;
                this.mTimes[i] = -1;
            }
            return i;
        }

        public void notifyAt(long timeUs, OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "notifyAt " + timeUs);
                }
                this.mTimes[registerListener(listener)] = timeUs;
                scheduleNotification(0, 0);
            }
        }

        public void scheduleUpdate(OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduleUpdate");
                }
                int i = registerListener(listener);
                if (!this.mStopped) {
                    this.mTimes[i] = 0;
                    scheduleNotification(0, 0);
                }
            }
        }

        public void cancelNotifications(OnMediaTimeListener listener) {
            synchronized (this) {
                int i = 0;
                while (i < this.mListeners.length) {
                    if (this.mListeners[i] != listener) {
                        if (this.mListeners[i] == null) {
                            break;
                        }
                        i++;
                    } else {
                        System.arraycopy(this.mListeners, i + 1, this.mListeners, i, (this.mListeners.length - i) - 1);
                        System.arraycopy(this.mTimes, i + 1, this.mTimes, i, (this.mTimes.length - i) - 1);
                        this.mListeners[this.mListeners.length - 1] = null;
                        this.mTimes[this.mTimes.length - 1] = -1;
                        break;
                    }
                }
                scheduleNotification(0, 0);
            }
        }

        private synchronized void notifyTimedEvent(boolean refreshTime) {
            long nowUs;
            try {
                nowUs = getCurrentTimeUs(refreshTime, true);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
                this.mPausing = true;
                nowUs = getCurrentTimeUs(refreshTime, true);
            }
            long nextTimeUs = nowUs;
            if (!this.mSeeking) {
                if (this.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("notifyTimedEvent(").append(this.mLastTimeUs).append(" -> ").append(nowUs).append(") from {");
                    boolean first = true;
                    for (long time : this.mTimes) {
                        if (time != -1) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(time);
                            first = false;
                        }
                    }
                    sb.append("}");
                    Log.d(TAG, sb.toString());
                }
                Vector<OnMediaTimeListener> activatedListeners = new Vector();
                int ix = 0;
                while (ix < this.mTimes.length && this.mListeners[ix] != null) {
                    if (this.mTimes[ix] > -1) {
                        if (this.mTimes[ix] <= MAX_EARLY_CALLBACK_US + nowUs) {
                            activatedListeners.add(this.mListeners[ix]);
                            if (this.DEBUG) {
                                Log.d(TAG, Environment.MEDIA_REMOVED);
                            }
                            this.mTimes[ix] = -1;
                        } else if (nextTimeUs == nowUs || this.mTimes[ix] < nextTimeUs) {
                            nextTimeUs = this.mTimes[ix];
                        }
                    }
                    ix++;
                }
                if (nextTimeUs <= nowUs || (this.mPaused ^ 1) == 0) {
                    this.mEventHandler.removeMessages(1);
                } else {
                    if (this.DEBUG) {
                        Log.d(TAG, "scheduling for " + nextTimeUs + " and " + nowUs);
                    }
                    scheduleNotification(0, nextTimeUs - nowUs);
                }
                for (OnMediaTimeListener listener : activatedListeners) {
                    listener.onTimedEvent(nowUs);
                }
            }
        }

        private long getEstimatedTime(long nanoTime, boolean monotonic) {
            if (this.mPaused) {
                this.mLastReportedTime = this.mLastTimeUs + this.mTimeAdjustment;
            } else {
                long timeSinceRead = (nanoTime - this.mLastNanoTime) / MAX_EARLY_CALLBACK_US;
                this.mLastReportedTime = this.mLastTimeUs + timeSinceRead;
                if (this.mTimeAdjustment > 0) {
                    long adjustment = this.mTimeAdjustment - (timeSinceRead / 2);
                    if (adjustment <= 0) {
                        this.mTimeAdjustment = 0;
                    } else {
                        this.mLastReportedTime += adjustment;
                    }
                }
            }
            return this.mLastReportedTime;
        }

        public long getCurrentTimeUs(boolean refreshTime, boolean monotonic) throws IllegalStateException {
            boolean z = true;
            synchronized (this) {
                long estimatedTime;
                if (!this.mPaused || (refreshTime ^ 1) == 0) {
                    long nanoTime = System.nanoTime();
                    if (refreshTime || nanoTime >= this.mLastNanoTime + MAX_NS_WITHOUT_POSITION_CHECK) {
                        try {
                            this.mLastTimeUs = ((long) this.mPlayer.getCurrentPosition()) * MAX_EARLY_CALLBACK_US;
                            if (this.mPlayer.isPlaying()) {
                                z = this.mBuffering;
                            }
                            this.mPaused = z;
                            if (this.DEBUG) {
                                String str;
                                String str2 = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                if (this.mPaused) {
                                    str = "paused";
                                } else {
                                    str = "playing";
                                }
                                Log.v(str2, stringBuilder.append(str).append(" at ").append(this.mLastTimeUs).toString());
                            }
                            this.mLastNanoTime = nanoTime;
                            if (!monotonic || this.mLastTimeUs >= this.mLastReportedTime) {
                                this.mTimeAdjustment = 0;
                            } else {
                                this.mTimeAdjustment = this.mLastReportedTime - this.mLastTimeUs;
                                if (this.mTimeAdjustment > 1000000) {
                                    this.mStopped = false;
                                    this.mSeeking = true;
                                    scheduleNotification(3, 0);
                                }
                            }
                        } catch (IllegalStateException e) {
                            if (this.mPausing) {
                                this.mPausing = false;
                                getEstimatedTime(nanoTime, monotonic);
                                this.mPaused = true;
                                if (this.DEBUG) {
                                    Log.d(TAG, "illegal state, but pausing: estimating at " + this.mLastReportedTime);
                                }
                                return this.mLastReportedTime;
                            }
                            throw e;
                        }
                    }
                    estimatedTime = getEstimatedTime(nanoTime, monotonic);
                    return estimatedTime;
                }
                estimatedTime = this.mLastReportedTime;
                return estimatedTime;
            }
        }
    }

    public static class TrackInfo implements Parcelable {
        static final Creator<TrackInfo> CREATOR = new Creator<TrackInfo>() {
            public TrackInfo createFromParcel(Parcel in) {
                return new TrackInfo(in);
            }

            public TrackInfo[] newArray(int size) {
                return new TrackInfo[size];
            }
        };
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_METADATA = 5;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        final MediaFormat mFormat;
        final int mTrackType;

        public int getTrackType() {
            return this.mTrackType;
        }

        public String getLanguage() {
            String language = this.mFormat.getString(MediaFormat.KEY_LANGUAGE);
            return language == null ? "und" : language;
        }

        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfo(Parcel in) {
            this.mTrackType = in.readInt();
            this.mFormat = MediaFormat.createSubtitleFormat(in.readString(), in.readString());
            if (this.mTrackType == 4) {
                this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, in.readInt());
            }
        }

        TrackInfo(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mTrackType);
            dest.writeString(getLanguage());
            if (this.mTrackType == 4) {
                dest.writeString(this.mFormat.getString(MediaFormat.KEY_MIME));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
            }
        }

        public String toString() {
            StringBuilder out = new StringBuilder(128);
            out.append(getClass().getName());
            out.append('{');
            switch (this.mTrackType) {
                case 1:
                    out.append("VIDEO");
                    break;
                case 2:
                    out.append("AUDIO");
                    break;
                case 3:
                    out.append("TIMEDTEXT");
                    break;
                case 4:
                    out.append("SUBTITLE");
                    break;
                default:
                    out.append("UNKNOWN");
                    break;
            }
            out.append(", ").append(this.mFormat.toString());
            out.append("}");
            return out.toString();
        }
    }

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _pause() throws IllegalStateException;

    private native void _prepare() throws IOException, IllegalStateException;

    private native void _prepareDrm(byte[] bArr, byte[] bArr2);

    private native void _release();

    private native void _releaseDrm();

    private native void _reset();

    private final native void _seekTo(long j, int i);

    private native void _setAudioStreamType(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setDataSource(MediaDataSource mediaDataSource) throws IllegalArgumentException, IllegalStateException;

    private native void _setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IOException, IllegalArgumentException, IllegalStateException;

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native void nativeSetDataSource(IBinder iBinder, String str, String[] strArr, String[] strArr2) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    private native int native_applyVolumeShaper(Configuration configuration, Operation operation);

    private final native void native_finalize();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private native PersistableBundle native_getMetrics();

    private native State native_getVolumeShaperState(int i);

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    public static native int native_pullBatteryData(Parcel parcel);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native int native_setRetransmitEndpoint(String str, int i);

    private final native void native_setup(Object obj);

    private native boolean setParameter(int i, Parcel parcel);

    public native void _attachAuxEffect(int i);

    public native int _getAudioSessionId();

    public native int _getCurrentPosition();

    public native int _getDuration();

    public native int _getVideoHeight();

    public native int _getVideoWidth();

    public native boolean _isLooping();

    public native boolean _isPlaying();

    public native void _prepareAsync() throws IllegalStateException;

    public native void _setAudioSessionId(int i) throws IllegalArgumentException, IllegalStateException;

    public native void _setLooping(boolean z);

    public native BufferingParams getBufferingParams();

    public native BufferingParams getDefaultBufferingParams();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    public native void setBufferingParams(BufferingParams bufferingParams);

    public native void setNextMediaPlayer(MediaPlayer mediaPlayer);

    public native void setPlaybackParams(PlaybackParams playbackParams);

    public native void setSyncParams(SyncParams syncParams);

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public MediaPlayer() {
        super(new Builder().build(), 2);
        Log.d(TAG, "new MediaPlayer()");
        Looper looper = Looper.myLooper();
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            looper = Looper.getMainLooper();
            if (looper != null) {
                this.mEventHandler = new EventHandler(this, looper);
            } else {
                this.mEventHandler = null;
            }
        }
        this.mCurrentState = 1;
        String packageName = ActivityThread.currentPackageName();
        if (packageName != null && packageName.length() > 0) {
            Log.i(TAG, "new mediaplayer ,packageName = " + packageName);
            if (packageName.contains(CTS_TEST_PACKAGE)) {
                Log.i(TAG, "Cts test start ");
            }
            if (packageName.equals(GOOGLE_MUSIC_PACKAGE)) {
                this.mDisableOppoMedia = true;
                Log.i(TAG, "Google Music don't use oppo player");
            }
        }
        if (SystemProperties.getBoolean("remove.oppomedia.support", false)) {
            this.mDisableOppoMedia = true;
            Log.i(TAG, "remove.oppomedia.support is true!");
        }
        this.mTimeProvider = new TimeProvider(this);
        this.mOpenSubtitleSources = new Vector();
        this.mNotificationManager = Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        native_setup(new WeakReference(this));
        baseRegisterPlayer();
    }

    private void closeFd() {
        try {
            Log.d(TAG, "closeFd()");
            if (this.mFd != null && this.mFd.valid()) {
                Log.d(TAG, "mFd is valid, close it.");
                Libcore.os.close(this.mFd);
                this.mFd = null;
                this.mOffset = -1;
                this.mLength = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception = " + e);
            e.printStackTrace();
        }
    }

    private boolean handleMediaPlayerError() {
        if (this.mDebugLog) {
            Log.i(TAG, "handleMediaPlayerError() mCurrentState=" + this.mCurrentState);
        }
        int mChangeState = this.mCurrentState;
        this.mCurrentState = 1;
        if (this.mNotUsingOppoMedia) {
            Log.i(TAG, "handleMediaPlayerError() mNotUsingOppoMedia is true");
            return false;
        }
        createOppoMediaPlayer();
        if (mChangeState >= 2) {
            try {
                if (this.mPath != null) {
                    Log.d(TAG, "handleMediaPlayerError() mOppoMediaPlayer setDataSource(String)");
                    this.mOppoMediaPlayer.setDataSource(this.mPath);
                } else if (this.mFd == null || !this.mFd.valid()) {
                    if (this.mUri != null) {
                        Log.d(TAG, "handleMediaPlayerError() mOppoMediaPlayer setDataSource(Context, Uri, Map)");
                        this.mOppoMediaPlayer.setDataSource(this.mContext, this.mUri, this.mHeaders);
                    } else {
                        Log.e(TAG, "handleMediaPlayerError() mOppoMediaPlayer setDataSource null");
                        closeFd();
                        return false;
                    }
                } else if (this.mOffset > 0) {
                    Log.d(TAG, "handleMediaPlayerError() mOppoMediaPlayer setDataSource(fd, offset,length) mOffset = " + this.mOffset + " mLength = " + this.mLength);
                    this.mOppoMediaPlayer.setDataSource(this.mFd, this.mOffset, this.mLength);
                } else {
                    Log.d(TAG, "handleMediaPlayerError() mOppoMediaPlayer setDataSource(fd)");
                    this.mOppoMediaPlayer.setDataSource(this.mFd);
                }
                closeFd();
                if (this.mSurfaceHolder != null) {
                    setDisplay(this.mSurfaceHolder);
                }
                if (this.mSurface != null) {
                    this.mOppoMediaPlayer.setSurface(this.mSurface);
                }
            } catch (Exception ex) {
                Log.w(TAG, "mOppoMediaPlayer setDataSource error mPath=" + this.mPath + " mUri=" + this.mUri);
                closeFd();
                ex.printStackTrace();
                return false;
            }
        }
        if (mChangeState >= 4) {
            Log.e(TAG, "handleMediaPlayerError() mOppoMediaPlayer prepare mPrepareAsync=" + this.mPrepareAsync);
            if (this.mPrepareAsync) {
                try {
                    prepareAsync();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "handleMediaPlayerError() prepareAsync error!");
                    e.printStackTrace();
                    return false;
                }
            }
            try {
                this.mOppoMediaPlayer.prepare();
                this.mCurrentState = 8;
            } catch (IOException e2) {
                Log.e(TAG, "handleMediaPlayerError() prepare error!");
                e2.printStackTrace();
                return false;
            }
        }
        if (this.mSeekMs >= 0) {
            this.mNeedSeeking = true;
        }
        if (mChangeState >= 16 && mChangeState != 32) {
            Log.v(TAG, "handleMediaPlayerError() mOppoMediaPlayer start mPrepareAsync=" + this.mPrepareAsync);
            if (!this.mPrepareAsync) {
                start();
            }
        }
        return true;
    }

    private void createOppoMediaPlayer() {
        if (!this.mNotUsingOppoMedia) {
            int tempSeekMs = this.mSeekMs;
            boolean tempScreenOn = this.mScreenOn;
            this.isCreateOppoMediaPlayer = true;
            reset();
            this.mSeekMs = tempSeekMs;
            this.mScreenOn = tempScreenOn;
            if (this.mOppoMediaPlayer != null) {
                this.mOppoMediaPlayer.reset();
                this.isOppoCreate = true;
                Log.i(TAG, "mOppoMediaPlayer has exist, return.");
                return;
            }
            this.mOppoMediaPlayer = new OppoMediaPlayer();
            this.isOppoCreate = true;
            if (this.mAudioSessionId >= 0) {
                this.mOppoMediaPlayer.setAudioSessionId(this.mAudioSessionId);
            }
            if (this.mAudioStreamType >= 0) {
                this.mOppoMediaPlayer.setAudioStreamType(this.mAudioStreamType);
            }
            if (this.mScreenOn) {
                this.mOppoMediaPlayer.setScreenOnWhilePlaying(true);
            }
            this.mOppoMediaPlayer.setOnPreparedListener(new com.oppo.media.OppoMediaPlayer.OnPreparedListener() {
                public void onPrepared(OppoMediaPlayer mp) {
                    if (MediaPlayer.this.mOnPreparedListener != null) {
                        MediaPlayer.this.mCurrentState = 8;
                        MediaPlayer.this.mOnPreparedListener.onPrepared(MediaPlayer.this);
                        if (MediaPlayer.this.mDefaultPlayerStarted) {
                            Log.d(MediaPlayer.TAG, "mDefaultPlayerStarted is true ,start !");
                            MediaPlayer.this.start();
                            MediaPlayer.this.mDefaultPlayerStarted = false;
                        }
                    }
                }
            });
            this.mOppoMediaPlayer.setOnBufferingUpdateListener(new com.oppo.media.OppoMediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(OppoMediaPlayer mp, int percent) {
                    if (MediaPlayer.this.mOnBufferingUpdateListener != null) {
                        MediaPlayer.this.mOnBufferingUpdateListener.onBufferingUpdate(MediaPlayer.this, percent);
                    }
                }
            });
            this.mOppoMediaPlayer.setOnCompletionListener(new com.oppo.media.OppoMediaPlayer.OnCompletionListener() {
                public void onCompletion(OppoMediaPlayer mp) {
                    if (MediaPlayer.this.mOnCompletionListener != null) {
                        MediaPlayer.this.mOnCompletionListener.onCompletion(MediaPlayer.this);
                    }
                }
            });
            this.mOppoMediaPlayer.setOnErrorListener(new com.oppo.media.OppoMediaPlayer.OnErrorListener() {
                public boolean onError(OppoMediaPlayer mp, int what, int extra) {
                    Log.e(MediaPlayer.TAG, "mOppoMediaPlayer OnError is running");
                    if (MediaPlayer.this.mOnErrorListener != null) {
                        MediaPlayer.this.mOnErrorListener.onError(MediaPlayer.this, what, extra);
                    }
                    return true;
                }
            });
            this.mOppoMediaPlayer.setOnSeekCompleteListener(new com.oppo.media.OppoMediaPlayer.OnSeekCompleteListener() {
                public void onSeekComplete(OppoMediaPlayer mp) {
                    if (MediaPlayer.this.mOnSeekCompleteListener != null) {
                        MediaPlayer.this.mOnSeekCompleteListener.onSeekComplete(MediaPlayer.this);
                    }
                }
            });
            this.mOppoMediaPlayer.setOnVideoSizeChangedListener(new com.oppo.media.OppoMediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(OppoMediaPlayer mp, int width, int height) {
                    if (MediaPlayer.this.mOnVideoSizeChangedListener != null) {
                        MediaPlayer.this.mOnVideoSizeChangedListener.onVideoSizeChanged(MediaPlayer.this, width, height);
                    }
                }
            });
            this.mOppoMediaPlayer.setOnInfoListener(new com.oppo.media.OppoMediaPlayer.OnInfoListener() {
                public boolean onInfo(OppoMediaPlayer mp, int what, int extra) {
                    if (MediaPlayer.this.mOnInfoListener != null) {
                        MediaPlayer.this.mOnInfoListener.onInfo(MediaPlayer.this, what, extra);
                    }
                    return true;
                }
            });
        }
    }

    public Parcel newRequest() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInterfaceToken(IMEDIA_PLAYER);
        return parcel;
    }

    public void invoke(Parcel request, Parcel reply) {
        int retcode = native_invoke(request, reply);
        reply.setDataPosition(0);
        if (retcode != 0) {
            throw new RuntimeException("failure code: " + retcode);
        }
    }

    public void setDisplay(SurfaceHolder sh) {
        if (this.mDebugLog) {
            Log.i(TAG, "setDisplay() isOppoCreate=" + this.isOppoCreate);
        }
        this.mSurfaceHolder = sh;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setDisplay(sh);
            return;
        }
        Surface surface;
        this.mSurfaceHolder = sh;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setSurface(Surface surface) {
        if (this.mDebugLog) {
            Log.i(TAG, "setDisplay() isOppoCreate=" + this.isOppoCreate);
        }
        this.mSurface = surface;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setSurface(surface);
            return;
        }
        if (this.mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        this.mSurfaceHolder = null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setVideoScalingMode(int mode) {
        if (this.mDebugLog) {
            Log.i(TAG, "setDisplay() isOppoCreate=" + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setVideoScalingMode(mode);
        } else if (isVideoScalingModeSupported(mode)) {
            Parcel request = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                request.writeInterfaceToken(IMEDIA_PLAYER);
                request.writeInt(6);
                request.writeInt(mode);
                invoke(request, reply);
            } finally {
                request.recycle();
                reply.recycle();
            }
        } else {
            throw new IllegalArgumentException("Scaling mode " + mode + " is not supported");
        }
    }

    public static MediaPlayer create(Context context, Uri uri) {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {
        int s = AudioSystem.newAudioSessionId();
        if (s <= 0) {
            s = 0;
        }
        return create(context, uri, holder, null, s);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            AudioAttributes aa;
            MediaPlayer mp = new MediaPlayer();
            if (audioAttributes != null) {
                aa = audioAttributes;
            } else {
                aa = new Builder().build();
            }
            mp.setAudioAttributes(aa);
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(context, uri);
            if (holder != null) {
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public static MediaPlayer create(Context context, int resid) {
        int s = AudioSystem.newAudioSessionId();
        if (s <= 0) {
            s = 0;
        }
        return create(context, resid, null, s);
    }

    public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) {
                return null;
            }
            AudioAttributes aa;
            MediaPlayer mp = new MediaPlayer();
            if (audioAttributes != null) {
                aa = audioAttributes;
            } else {
                aa = new Builder().build();
            }
            mp.setAudioAttributes(aa);
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "setDataSource(Context, Uri) isOppoCreate=" + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = false;
        this.mDefaultPlayerStarted = false;
        this.mContext = context;
        this.mUri = uri;
        this.mCurrentState = 2;
        setDataSource(context, uri, null, null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers, List<HttpCookie> cookies) throws IOException {
        if (context == null) {
            throw new NullPointerException("context param can not be null.");
        } else if (uri == null) {
            throw new NullPointerException("uri param can not be null.");
        } else {
            if (cookies != null) {
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (!(cookieHandler == null || ((cookieHandler instanceof CookieManager) ^ 1) == 0)) {
                    throw new IllegalArgumentException("The cookie handler has to be of CookieManager type when cookies are provided.");
                }
            }
            if (this.mDebugLog) {
                Log.i(TAG, "setDataSource(Context, Uri, headers) isOppoCreate=" + this.isOppoCreate);
            }
            this.mNotUsingOppoMedia = false;
            this.mDefaultPlayerStarted = false;
            this.mContext = context;
            this.mUri = uri;
            this.mHeaders = headers;
            this.mCurrentState = 2;
            ContentResolver resolver = context.getContentResolver();
            String scheme = uri.getScheme();
            String authority = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
            if (ContentResolver.SCHEME_FILE.equals(scheme) || scheme == null) {
                setDataSource(uri.getPath());
                return;
            }
            if ("content".equals(scheme) && "settings".equals(authority)) {
                int type = RingtoneManager.getDefaultType(uri);
                Uri cacheUri = RingtoneManager.getCacheForType(type, context.getUserId());
                Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
                if (!attemptDataSource(resolver, cacheUri) && !attemptDataSource(resolver, actualUri)) {
                    setDataSource(uri.toString(), (Map) headers, (List) cookies);
                }
            } else if (!attemptDataSource(resolver, uri)) {
                setDataSource(uri.toString(), (Map) headers, (List) cookies);
            }
        }
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, (Map) headers, null);
    }

    private boolean attemptDataSource(ContentResolver resolver, Uri uri) {
        Throwable th;
        Throwable th2 = null;
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = resolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            setDataSource(assetFileDescriptor);
            if (assetFileDescriptor != null) {
                try {
                    assetFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 == null) {
                return true;
            }
            try {
                throw th2;
            } catch (Exception ex) {
                Log.w(TAG, "Couldn't open " + uri + ": " + ex);
                return false;
            }
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (assetFileDescriptor != null) {
            try {
                assetFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "setDataSource(String) isOppoCreate=" + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = false;
        this.mDefaultPlayerStarted = false;
        this.mPath = path;
        this.mCurrentState = 2;
        setDataSource(path, null, null);
    }

    public void setDataSource(String path, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(path, (Map) headers, null);
    }

    private void setDataSource(String path, Map<String, String> headers, List<HttpCookie> cookies) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "setDataSource(path, headers) isOppoCreate=" + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = false;
        this.mDefaultPlayerStarted = false;
        this.mPath = path;
        this.mHeaders = headers;
        this.mCurrentState = 2;
        String[] keys = null;
        String[] values = null;
        if (headers != null) {
            keys = new String[headers.size()];
            values = new String[headers.size()];
            int i = 0;
            for (Entry<String, String> entry : headers.entrySet()) {
                keys[i] = (String) entry.getKey();
                values[i] = (String) entry.getValue();
                i++;
            }
        }
        setDataSource(path, keys, values, (List) cookies);
    }

    private void setDataSource(String path, String[] keys, String[] values, List<HttpCookie> cookies) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("widevine://") || path.startsWith("rtsp://") || path.endsWith(".sdp") || path.endsWith(".sdp?")) {
                this.mNotUsingOppoMedia = true;
            } else if (path.startsWith("file://") && path.endsWith(".m3u8")) {
                this.mNotUsingOppoMedia = true;
            } else {
                this.mNotUsingOppoMedia = false;
            }
            nativeSetDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(path, cookies), path, keys, values);
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            setDataSource(is.getFD());
            is.close();
            return;
        }
        throw new IOException("setDataSource failed.");
    }

    public void setDataSource(AssetFileDescriptor afd) throws IOException, IllegalArgumentException, IllegalStateException {
        Preconditions.checkNotNull(afd);
        if (afd.getDeclaredLength() < 0) {
            setDataSource(afd.getFileDescriptor());
        } else {
            setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
        }
    }

    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "setDataSource(FileDescriptor fd) isOppoCreate=" + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = false;
        this.mDefaultPlayerStarted = false;
        this.mCurrentState = 2;
        setDataSource(fd, 0, 576460752303423487L);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "setDataSource(FileDescriptor, long, long) isOppoCreate=" + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = false;
        this.mDefaultPlayerStarted = false;
        try {
            closeFd();
            this.mFd = Libcore.os.dup(fd);
            this.mOffset = offset;
            this.mLength = length;
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            ex.printStackTrace();
        }
        this.mCurrentState = 2;
        _setDataSource(fd, offset, length);
        String packageName = ActivityThread.currentPackageName();
        if (packageName != null && packageName.length() > 0) {
            String result = OppoMultimediaManager.getInstance(null).getParameters("check_daemon_listinfo_byname=zenmode=" + packageName);
            if (result != null && result.equals("true")) {
                Log.i(TAG, "The package name is " + packageName + "qq & wecha zenmode control !");
                int uid = Process.myUid();
                if (this.mNotificationManager != null) {
                    try {
                        this.mInterceptFlag = this.mNotificationManager.shouldInterceptSound(packageName, uid);
                    } catch (RemoteException e) {
                        Log.e(TAG, " start RemoteException");
                    }
                }
                Parcel pMute = Parcel.obtain();
                pMute.writeInt(this.mInterceptFlag ? 1 : 0);
                setParameter(10011, pMute);
                pMute.recycle();
            }
        }
    }

    public void setDataSource(MediaDataSource dataSource) throws IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "setDataSource(MediaDataSource) isOppoCreate = " + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = true;
        this.mDefaultPlayerStarted = false;
        if (this.isOppoCreate) {
            Log.e(TAG, "setDataSource(MediaDataSource) oppomedia do not support! return directly");
        } else {
            _setDataSource(dataSource);
        }
    }

    public void prepare() throws IOException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "prepare() isOppoCreate=" + this.isOppoCreate);
        }
        this.mPrepareAsync = false;
        this.mCurrentState = 4;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.prepare();
            return;
        }
        try {
            _prepare();
            scanInternalSubtitleTracks();
            synchronized (this.mDrmLock) {
                this.mDrmInfoResolved = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (!handleMediaPlayerError()) {
                Log.i(TAG, "prepare failed ,throw IOException to app");
                throw e;
            }
        }
    }

    public void prepareAsync() throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "prepareAsync() isOppoCreate=" + this.isOppoCreate);
        }
        this.mPrepareAsync = true;
        this.mCurrentState = 4;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.prepareAsync();
        } else {
            _prepareAsync();
        }
    }

    public void start() throws IllegalStateException {
        Log.d(TAG, "start()");
        final int delay = getStartDelayMs();
        if (delay == 0) {
            startImpl();
        } else {
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep((long) delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MediaPlayer.this.baseSetStartDelayMs(0);
                    try {
                        MediaPlayer.this.startImpl();
                    } catch (IllegalStateException e2) {
                    }
                }
            }.start();
        }
    }

    private void startImpl() {
        if (this.mDebugLog) {
            Log.i(TAG, "start() isOppoCreate = " + this.isOppoCreate + " mCurrentState = " + this.mCurrentState);
        }
        if (!this.isOppoCreate) {
            baseStart();
            stayAwake(true);
            _start();
            this.mDefaultPlayerStarted = true;
        } else if (this.mCurrentState >= 8 || !this.mPrepareAsync) {
            if (this.mNeedSeeking) {
                Log.v(TAG, "mOppoMediaPlayer start seekTo: " + this.mSeekMs);
                seekTo(this.mSeekMs);
                this.mNeedSeeking = false;
                this.mSeekMs = -1;
            }
            this.mOppoMediaPlayer.start();
        } else {
            return;
        }
        this.mCurrentState = 16;
    }

    private int getAudioStreamType() {
        if (this.mStreamType == Integer.MIN_VALUE) {
            this.mStreamType = _getAudioStreamType();
        }
        return this.mStreamType;
    }

    public void stop() throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "stop() isOppoCreate = " + this.isOppoCreate);
        }
        this.mCurrentState = 64;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.stop();
        } else {
            stayAwake(false);
            _stop();
            baseStop();
        }
        this.mScreenOn = false;
    }

    public void pause() throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "pause() isOppoCreate=" + this.isOppoCreate);
        }
        this.mCurrentState = 32;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.pause();
            return;
        }
        stayAwake(false);
        _pause();
        basePause();
        this.mDefaultPlayerStarted = false;
    }

    void playerStart() {
        start();
    }

    void playerPause() {
        pause();
    }

    void playerStop() {
        stop();
    }

    int playerApplyVolumeShaper(Configuration configuration, Operation operation) {
        return native_applyVolumeShaper(configuration, operation);
    }

    State playerGetVolumeShaperState(int id) {
        return native_getVolumeShaperState(id);
    }

    public VolumeShaper createVolumeShaper(Configuration configuration) {
        return new VolumeShaper(configuration, this);
    }

    public void setWakeMode(Context context, int mode) {
        if (this.mDebugLog) {
            Log.i(TAG, "setWakeMode() isOppoCreate=" + this.isOppoCreate + " mode=" + mode);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setWakeMode(context, mode);
        } else {
            boolean washeld = false;
            if (SystemProperties.getBoolean("audio.offload.ignore_setawake", false)) {
                Log.w(TAG, "IGNORING setWakeMode " + mode);
                return;
            }
            if (this.mWakeLock != null) {
                if (this.mWakeLock.isHeld()) {
                    washeld = true;
                    this.mWakeLock.release();
                }
                this.mWakeLock = null;
            }
            this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(536870912 | mode, MediaPlayer.class.getName());
            this.mWakeLock.setReferenceCounted(false);
            if (washeld) {
                this.mWakeLock.acquire();
            }
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (this.mDebugLog) {
            Log.i(TAG, "setScreenOnWhilePlaying() isOppoCreate=" + this.isOppoCreate + " screenOn=" + screenOn);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setScreenOnWhilePlaying(screenOn);
        } else if (this.mScreenOnWhilePlaying != screenOn) {
            if (screenOn && this.mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            this.mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
        if (screenOn) {
            this.mScreenOn = true;
        }
    }

    private void stayAwake(boolean awake) {
        if (this.mWakeLock != null) {
            if (awake && (this.mWakeLock.isHeld() ^ 1) != 0) {
                this.mWakeLock.acquire();
            } else if (!awake && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
        this.mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (this.mSurfaceHolder != null) {
            this.mSurfaceHolder.setKeepScreenOn(this.mScreenOnWhilePlaying ? this.mStayAwake : false);
        }
    }

    public int getVideoWidth() {
        if (this.mDebugLog) {
            Log.i(TAG, "getVideoWidth() isOppoCreate=" + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            return this.mOppoMediaPlayer.getVideoWidth();
        }
        return _getVideoWidth();
    }

    public int getVideoHeight() {
        if (this.mDebugLog) {
            Log.i(TAG, "getVideoHeight() isOppoCreate=" + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            return this.mOppoMediaPlayer.getVideoHeight();
        }
        return _getVideoHeight();
    }

    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    public boolean isPlaying() {
        if (this.mDebugLog) {
            Log.i(TAG, "isPlaying() isOppoCreate=" + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            return this.mOppoMediaPlayer.isPlaying();
        }
        return _isPlaying();
    }

    public PlaybackParams easyPlaybackParams(float rate, int audioMode) {
        PlaybackParams params = new PlaybackParams();
        params.allowDefaults();
        switch (audioMode) {
            case 0:
                params.setSpeed(rate).setPitch(1.0f);
                break;
            case 1:
                params.setSpeed(rate).setPitch(1.0f).setAudioFallbackMode(2);
                break;
            case 2:
                params.setSpeed(rate).setPitch(rate);
                break;
            default:
                throw new IllegalArgumentException("Audio playback mode " + audioMode + " is not supported");
        }
        return params;
    }

    public void seekTo(long msec, int mode) {
        Log.d(TAG, "seekTo() msec = " + msec + ", seekMode = " + mode + " isOppoCreate = " + this.isOppoCreate);
        if (mode < 0 || mode > 3) {
            throw new IllegalArgumentException("Illegal seek mode: " + mode);
        }
        if (msec > 2147483647L) {
            Log.w(TAG, "seekTo offset " + msec + " is too large, cap to " + Integer.MAX_VALUE);
            msec = 2147483647L;
        } else if (msec < -2147483648L) {
            Log.w(TAG, "seekTo offset " + msec + " is too small, cap to " + Integer.MIN_VALUE);
            msec = -2147483648L;
        }
        this.mSeekMs = (int) msec;
        if (!this.isOppoCreate) {
            _seekTo(msec, mode);
        } else if (this.mCurrentState >= 8 || !this.mPrepareAsync) {
            this.mOppoMediaPlayer.seekTo((int) msec);
        }
    }

    public void seekTo(int msec) throws IllegalStateException {
        seekTo((long) msec, 0);
    }

    public MediaTimestamp getTimestamp() {
        try {
            return new MediaTimestamp(((long) getCurrentPosition()) * 1000, System.nanoTime(), isPlaying() ? getPlaybackParams().getSpeed() : 0.0f);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public int getCurrentPosition() {
        boolean z = this.mDebugLog;
        if (this.mCurrentState < 2) {
            return 0;
        }
        if (!this.isOppoCreate) {
            return _getCurrentPosition();
        }
        if (this.mCurrentState < 8) {
            return 0;
        }
        return this.mOppoMediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if (this.mCurrentState < 2) {
            return -1;
        }
        if (!this.isOppoCreate) {
            return _getDuration();
        }
        if (this.mCurrentState < 8) {
            return 0;
        }
        return this.mOppoMediaPlayer.getDuration();
    }

    public Metadata getMetadata(boolean update_only, boolean apply_filter) {
        Parcel reply = Parcel.obtain();
        Metadata data = new Metadata();
        if (!native_getMetadata(update_only, apply_filter, reply)) {
            reply.recycle();
            return null;
        } else if (data.parse(reply)) {
            return data;
        } else {
            reply.recycle();
            return null;
        }
    }

    public int setMetadataFilter(Set<Integer> allow, Set<Integer> block) {
        Parcel request = newRequest();
        int capacity = request.dataSize() + ((((allow.size() + 1) + 1) + block.size()) * 4);
        if (request.dataCapacity() < capacity) {
            request.setDataCapacity(capacity);
        }
        request.writeInt(allow.size());
        for (Integer t : allow) {
            request.writeInt(t.intValue());
        }
        request.writeInt(block.size());
        for (Integer t2 : block) {
            request.writeInt(t2.intValue());
        }
        return native_setMetadataFilter(request);
    }

    public void release() {
        if (this.mDebugLog) {
            Log.i(TAG, "release() isOppoCreate =" + this.isOppoCreate);
        }
        this.mNotUsingOppoMedia = true;
        this.mCurrentState = 1;
        closeFd();
        if (this.mOppoMediaPlayer != null) {
            this.mOppoMediaPlayer.release();
            this.mOppoMediaPlayer = null;
        }
        baseRelease();
        stayAwake(false);
        updateSurfaceScreenOn();
        this.mOnPreparedListener = null;
        this.mOnBufferingUpdateListener = null;
        this.mOnCompletionListener = null;
        this.mOnSeekCompleteListener = null;
        this.mOnErrorListener = null;
        this.mOnInfoListener = null;
        this.mOnVideoSizeChangedListener = null;
        this.mOnTimedTextListener = null;
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        this.mOnSubtitleDataListener = null;
        this.mOnDrmConfigHelper = null;
        this.mOnDrmInfoHandlerDelegate = null;
        this.mOnDrmPreparedHandlerDelegate = null;
        resetDrmState();
        _release();
        this.isOppoCreate = false;
        this.mAudioSessionId = -1;
        this.mAudioStreamType = -1;
        this.mScreenOn = false;
    }

    public void reset() {
        if (this.mDebugLog) {
            Log.i(TAG, "reset() isOppoCreate =" + this.isOppoCreate);
        }
        this.mSeekMs = -1;
        this.mCurrentState = 1;
        if (this.isCreateOppoMediaPlayer) {
            this.isCreateOppoMediaPlayer = false;
        } else {
            closeFd();
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.reset();
            this.mDefaultPlayerStarted = false;
        } else {
            this.mSelectedSubtitleTrackIndex = -1;
            synchronized (this.mOpenSubtitleSources) {
                for (InputStream is : this.mOpenSubtitleSources) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                this.mOpenSubtitleSources.clear();
            }
            if (this.mSubtitleController != null) {
                this.mSubtitleController.reset();
            }
            if (this.mTimeProvider != null) {
                this.mTimeProvider.close();
                this.mTimeProvider = null;
            }
            stayAwake(false);
            _reset();
            if (this.mEventHandler != null) {
                this.mEventHandler.removeCallbacksAndMessages(null);
            }
            synchronized (this.mIndexTrackPairs) {
                this.mIndexTrackPairs.clear();
                this.mInbandTrackIndices.clear();
            }
        }
        this.mScreenOn = false;
        this.isOppoCreate = false;
        this.mInterceptFlag = false;
        resetDrmState();
    }

    public void setAudioStreamType(int streamtype) {
        if (streamtype == 0) {
            String callerName = ActivityThread.currentPackageName();
            if (callerName != null && callerName.length() > 0) {
                String value = OppoMultimediaManager.getInstance(null).getParameters("check_daemon_listinfo_byname=change-streamtype=" + callerName);
                if (!(this.mContext == null || value == null || !value.equals("true"))) {
                    Log.i(TAG, "setAudioStreamType, caller = " + callerName + ", for change wechat message notification!");
                    try {
                        AudioManager localAudioManager = (AudioManager) this.mContext.getSystemService("audio");
                        boolean bChangeStreamType = false;
                        int currentMode = localAudioManager.getMode();
                        if (localAudioManager.isMusicActive() && localAudioManager.isWiredHeadsetOn() && currentMode != 2 && currentMode != 3) {
                            Log.i(TAG, "Playing music with headphone, should change wechat message notification");
                            bChangeStreamType = true;
                        }
                        Parcel parcel = Parcel.obtain();
                        parcel.writeInt(bChangeStreamType ? 1 : 0);
                        setParameter(KEY_PARAMETER_STREAMTYPE_CHANGE, parcel);
                        parcel.recycle();
                    } catch (Exception e) {
                        Log.e(TAG, "start Exception for change wechat message notification!");
                    }
                }
            } else {
                return;
            }
        }
        this.mAudioStreamType = streamtype;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setAudioStreamType(streamtype);
        } else {
            PlayerBase.deprecateStreamTypeForPlayback(streamtype, TAG, "setAudioStreamType()");
            baseUpdateAudioAttributes(new Builder().setInternalLegacyStreamType(streamtype).build());
            _setAudioStreamType(streamtype);
            this.mStreamType = streamtype;
        }
        String packageName = ActivityThread.currentPackageName();
        if (packageName != null && packageName.length() > 0) {
            String result = OppoMultimediaManager.getInstance(null).getParameters("get_daemon_listinfo_byname=zenmode=" + packageName);
            if (result != null && result.equals("wechat_mute")) {
                Log.i(TAG, "The package name is " + packageName + "wecha zenmode control !");
                if (this.mStreamType == 5 || this.mStreamType == 2) {
                    Parcel pMute = Parcel.obtain();
                    pMute.writeInt(this.mInterceptFlag ? 1 : 0);
                    setParameter(10011, pMute);
                    pMute.recycle();
                }
            }
        }
    }

    private int convertContentTypeToLegacyStreamType(int contenType) {
        switch (contenType) {
            case 1:
                return 0;
            case 2:
                return 3;
            case 4:
                return 5;
            default:
                return 3;
        }
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        boolean z = false;
        if (this.mDebugLog) {
            Log.i(TAG, "setAudioAttributes() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "setAudioAttributes() oppomedia do not support! return directly");
        } else if (attributes == null) {
            String msg = "Cannot set AudioAttributes to null";
            throw new IllegalArgumentException("Cannot set AudioAttributes to null");
        } else {
            baseUpdateAudioAttributes(attributes);
            this.mUsage = attributes.getUsage();
            if ((attributes.getAllFlags() & 64) != 0) {
                z = true;
            }
            this.mBypassInterruptionPolicy = z;
            Parcel pattributes = Parcel.obtain();
            attributes.writeToParcel(pattributes, 1);
            setParameter(KEY_PARAMETER_AUDIO_ATTRIBUTES, pattributes);
            pattributes.recycle();
            if (this.mAudioStreamType == -1) {
                this.mAudioStreamType = convertContentTypeToLegacyStreamType(attributes.getContentType());
            }
        }
    }

    public void setLooping(boolean looping) {
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setLooping(looping);
        } else {
            _setLooping(looping);
        }
    }

    public boolean isLooping() {
        if (this.isOppoCreate) {
            return this.mOppoMediaPlayer.isLooping();
        }
        return _isLooping();
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setVolume(leftVolume, rightVolume);
        } else {
            baseSetVolume(leftVolume, rightVolume);
        }
    }

    void playerSetVolume(boolean muting, float leftVolume, float rightVolume) {
        if (muting) {
            leftVolume = 0.0f;
        }
        if (muting) {
            rightVolume = 0.0f;
        }
        _setVolume(leftVolume, rightVolume);
    }

    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    public void setAudioSessionId(int sessionId) throws IllegalArgumentException, IllegalStateException {
        this.mAudioSessionId = sessionId;
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setAudioSessionId(sessionId);
        } else {
            _setAudioSessionId(sessionId);
        }
    }

    public int getAudioSessionId() {
        if (this.isOppoCreate) {
            this.mAudioSessionId = this.mOppoMediaPlayer.getAudioSessionId();
        } else {
            this.mAudioSessionId = _getAudioSessionId();
        }
        return this.mAudioSessionId;
    }

    public void attachAuxEffect(int effectId) {
        if (this.mDebugLog) {
            Log.i(TAG, "attachAuxEffect(), isOppoCreate =" + this.isOppoCreate + ", effectId = " + effectId);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.attachAuxEffect(effectId);
        } else {
            _attachAuxEffect(effectId);
        }
    }

    public void setAuxEffectSendLevel(float level) {
        if (this.mDebugLog) {
            Log.i(TAG, "setAuxEffectSendLevel(), isOppoCreate =" + this.isOppoCreate + ", level = " + level);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.setAuxEffectSendLevel(level);
        } else {
            baseSetAuxEffectSendLevel(level);
        }
    }

    int playerSetAuxEffectSendLevel(boolean muting, float level) {
        if (muting) {
            level = 0.0f;
        }
        _setAuxEffectSendLevel(level);
        return 0;
    }

    public TrackInfo[] getTrackInfo() throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "getTrackInfo() isOppoCreate=" + this.isOppoCreate);
        }
        int i;
        if (this.isOppoCreate) {
            Parcel mParcel = Parcel.obtain();
            com.oppo.media.OppoMediaPlayer.TrackInfo[] mtrackInfo = this.mOppoMediaPlayer.getTrackInfo();
            TrackInfo[] trackInfo1 = new TrackInfo[mtrackInfo.length];
            for (i = 0; i < mtrackInfo.length; i++) {
                mParcel.setDataPosition(0);
                mParcel.writeInt(mtrackInfo[i].getTrackType());
                mParcel.writeString(mtrackInfo[i].getLanguage());
                mParcel.setDataPosition(0);
                trackInfo1[i] = new TrackInfo(mParcel);
            }
            return trackInfo1;
        }
        TrackInfo[] allTrackInfo;
        TrackInfo[] trackInfo = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            allTrackInfo = new TrackInfo[this.mIndexTrackPairs.size()];
            for (i = 0; i < allTrackInfo.length; i++) {
                Pair<Integer, SubtitleTrack> p = (Pair) this.mIndexTrackPairs.get(i);
                if (p.first != null) {
                    allTrackInfo[i] = trackInfo[((Integer) p.first).intValue()];
                } else {
                    SubtitleTrack track = p.second;
                    allTrackInfo[i] = new TrackInfo(track.getTrackType(), track.getFormat());
                }
            }
        }
        return allTrackInfo;
    }

    private TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(1);
            invoke(request, reply);
            TrackInfo[] trackInfo = (TrackInfo[]) reply.createTypedArray(TrackInfo.CREATOR);
            return trackInfo;
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    private static boolean availableMimeTypeForExternalSource(String mimeType) {
        if (MEDIA_MIMETYPE_TEXT_SUBRIP.equals(mimeType)) {
            return true;
        }
        return false;
    }

    public void setSubtitleAnchor(SubtitleController controller, Anchor anchor) {
        if (this.mDebugLog) {
            Log.i(TAG, "setSubtitleAnchor() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "setSubtitleAnchor() oppomedia do not support! return directly");
            return;
        }
        this.mSubtitleController = controller;
        this.mSubtitleController.setAnchor(anchor);
    }

    private synchronized void setSubtitleAnchor() {
        if (this.mSubtitleController == null && ActivityThread.currentApplication() != null) {
            final HandlerThread thread = new HandlerThread("SetSubtitleAnchorThread");
            thread.start();
            new Handler(thread.getLooper()).post(new Runnable() {
                public void run() {
                    MediaPlayer.this.mSubtitleController = new SubtitleController(ActivityThread.currentApplication(), MediaPlayer.this.mTimeProvider, MediaPlayer.this);
                    MediaPlayer.this.mSubtitleController.setAnchor(new Anchor() {
                        public void setSubtitleWidget(RenderingWidget subtitleWidget) {
                        }

                        public Looper getSubtitleLooper() {
                            return Looper.getMainLooper();
                        }
                    });
                    thread.getLooper().quitSafely();
                }
            });
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "failed to join SetSubtitleAnchorThread");
            }
        }
        return;
    }

    public void onSubtitleTrackSelected(SubtitleTrack track) {
        if (this.mDebugLog) {
            Log.i(TAG, "onSubtitleTrackSelected() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "onSubtitleTrackSelected() oppomedia do not support! return directly");
            return;
        }
        if (this.mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, false);
            } catch (IllegalStateException e) {
            }
            this.mSelectedSubtitleTrackIndex = -1;
        }
        setOnSubtitleDataListener(null);
        if (track != null) {
            synchronized (this.mIndexTrackPairs) {
                for (Pair<Integer, SubtitleTrack> p : this.mIndexTrackPairs) {
                    if (p.first != null && p.second == track) {
                        this.mSelectedSubtitleTrackIndex = ((Integer) p.first).intValue();
                        break;
                    }
                }
            }
            if (this.mSelectedSubtitleTrackIndex >= 0) {
                try {
                    selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, true);
                } catch (IllegalStateException e2) {
                }
                setOnSubtitleDataListener(this.mSubtitleDataListener);
            }
        }
    }

    public void addSubtitleSource(final InputStream is, final MediaFormat format) throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "addSubtitleSource() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "addSubtitleSource() oppomedia do not support! return directly");
            return;
        }
        InputStream fIs = is;
        MediaFormat fFormat = format;
        if (is != null) {
            synchronized (this.mOpenSubtitleSources) {
                this.mOpenSubtitleSources.add(is);
            }
        } else {
            Log.w(TAG, "addSubtitleSource called with null InputStream");
        }
        getMediaTimeProvider();
        final HandlerThread thread = new HandlerThread("SubtitleReadThread", 9);
        thread.start();
        new Handler(thread.getLooper()).post(new Runnable() {
            private int addTrack() {
                if (is == null || MediaPlayer.this.mSubtitleController == null) {
                    return MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE;
                }
                SubtitleTrack track = MediaPlayer.this.mSubtitleController.addTrack(format);
                if (track == null) {
                    return MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE;
                }
                Scanner scanner = new Scanner(is, "UTF-8");
                String contents = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer.this.mOpenSubtitleSources) {
                    MediaPlayer.this.mOpenSubtitleSources.remove(is);
                }
                scanner.close();
                synchronized (MediaPlayer.this.mIndexTrackPairs) {
                    MediaPlayer.this.mIndexTrackPairs.add(Pair.create(null, track));
                }
                Handler h = MediaPlayer.this.mTimeProvider.mEventHandler;
                h.sendMessage(h.obtainMessage(1, 4, 0, Pair.create(track, contents.getBytes())));
                return 803;
            }

            public void run() {
                int res = addTrack();
                if (MediaPlayer.this.mEventHandler != null) {
                    MediaPlayer.this.mEventHandler.sendMessage(MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null));
                }
                thread.getLooper().quitSafely();
            }
        });
    }

    private void scanInternalSubtitleTracks() {
        setSubtitleAnchor();
        populateInbandTracks();
        if (this.mSubtitleController != null) {
            this.mSubtitleController.selectDefaultTrack();
        }
    }

    private void populateInbandTracks() {
        TrackInfo[] tracks = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            for (int i = 0; i < tracks.length; i++) {
                if (!this.mInbandTrackIndices.get(i)) {
                    this.mInbandTrackIndices.set(i);
                    if (tracks[i].getTrackType() == 4) {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), this.mSubtitleController.addTrack(tracks[i].getFormat())));
                    } else {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), null));
                    }
                }
            }
        }
    }

    public void addTimedTextSource(String path, String mimeType) throws IOException, IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "addTimedTextSource() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "addTimedTextSource(String, String ) oppomedia do not support! return directly");
        } else if (availableMimeTypeForExternalSource(mimeType)) {
            File file = new File(path);
            if (file.exists()) {
                FileInputStream is = new FileInputStream(file);
                addTimedTextSource(is.getFD(), mimeType);
                is.close();
                return;
            }
            throw new IOException(path);
        } else {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + mimeType);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addTimedTextSource(Context context, Uri uri, String mimeType) throws IOException, IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "addTimedTextSource() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "addTimedTextSource(Context , Uri , String) oppomedia do not support! return directly");
            return;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
            addTimedTextSource(uri.getPath(), mimeType);
            return;
        }
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            if (assetFileDescriptor == null) {
                if (assetFileDescriptor != null) {
                    assetFileDescriptor.close();
                }
                return;
            }
            addTimedTextSource(assetFileDescriptor.getFileDescriptor(), mimeType);
            if (assetFileDescriptor != null) {
                assetFileDescriptor.close();
            }
        } catch (SecurityException e) {
            if (assetFileDescriptor != null) {
                assetFileDescriptor.close();
            }
        } catch (IOException e2) {
            if (assetFileDescriptor != null) {
                assetFileDescriptor.close();
            }
        } catch (Throwable th) {
            if (assetFileDescriptor != null) {
                assetFileDescriptor.close();
            }
        }
    }

    public void addTimedTextSource(FileDescriptor fd, String mimeType) throws IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "addTimedTextSource(FileDescriptor, String) isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "addTimedTextSource(FileDescriptor, String) oppomedia do not support! return directly");
        } else {
            addTimedTextSource(fd, 0, 576460752303423487L, mimeType);
        }
    }

    public void addTimedTextSource(FileDescriptor fd, long offset, long length, String mime) throws IllegalArgumentException, IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "addTimedTextSource(FileDescriptor, long, long, String) isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "addTimedTextSource(FileDescriptor, long, long, String) oppomedia do not support! return directly");
        } else if (availableMimeTypeForExternalSource(mime)) {
            try {
                final FileDescriptor dupedFd = Libcore.os.dup(fd);
                MediaFormat fFormat = new MediaFormat();
                fFormat.setString(MediaFormat.KEY_MIME, mime);
                fFormat.setInteger(MediaFormat.KEY_IS_TIMED_TEXT, 1);
                if (this.mSubtitleController == null) {
                    setSubtitleAnchor();
                }
                if (!this.mSubtitleController.hasRendererFor(fFormat)) {
                    this.mSubtitleController.registerRenderer(new SRTRenderer(ActivityThread.currentApplication(), this.mEventHandler));
                }
                final SubtitleTrack track = this.mSubtitleController.addTrack(fFormat);
                synchronized (this.mIndexTrackPairs) {
                    this.mIndexTrackPairs.add(Pair.create(null, track));
                }
                getMediaTimeProvider();
                long offset2 = offset;
                long length2 = length;
                final HandlerThread thread = new HandlerThread("TimedTextReadThread", 9);
                thread.start();
                final long j = offset;
                final long j2 = length;
                new Handler(thread.getLooper()).post(new Runnable() {
                    private int addTrack() {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try {
                            Libcore.os.lseek(dupedFd, j, OsConstants.SEEK_SET);
                            byte[] buffer = new byte[4096];
                            long total = 0;
                            while (total < j2) {
                                int bytes = IoBridge.read(dupedFd, buffer, 0, (int) Math.min((long) buffer.length, j2 - total));
                                if (bytes < 0) {
                                    break;
                                }
                                bos.write(buffer, 0, bytes);
                                total += (long) bytes;
                            }
                            Handler h = MediaPlayer.this.mTimeProvider.mEventHandler;
                            h.sendMessage(h.obtainMessage(1, 4, 0, Pair.create(track, bos.toByteArray())));
                            try {
                                Libcore.os.close(dupedFd);
                            } catch (ErrnoException e) {
                                Log.e(MediaPlayer.TAG, e.getMessage(), e);
                            }
                            return 803;
                        } catch (Exception e2) {
                            Log.e(MediaPlayer.TAG, e2.getMessage(), e2);
                            try {
                                Libcore.os.close(dupedFd);
                            } catch (ErrnoException e3) {
                                Log.e(MediaPlayer.TAG, e3.getMessage(), e3);
                            }
                            return MediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR;
                        } catch (Throwable th) {
                            try {
                                Libcore.os.close(dupedFd);
                            } catch (ErrnoException e32) {
                                Log.e(MediaPlayer.TAG, e32.getMessage(), e32);
                            }
                            throw th;
                        }
                    }

                    public void run() {
                        int res = addTrack();
                        if (MediaPlayer.this.mEventHandler != null) {
                            MediaPlayer.this.mEventHandler.sendMessage(MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null));
                        }
                        thread.getLooper().quitSafely();
                    }
                });
            } catch (ErrnoException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        } else {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + mime);
        }
    }

    public int getSelectedTrack(int trackType) throws IllegalStateException {
        int i;
        if (this.mDebugLog) {
            Log.i(TAG, "getSelectedTrack() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "getSelectedTrack() oppomedia do not support! return directly");
            return -1;
        }
        if (this.mSubtitleController != null && (trackType == 4 || trackType == 3)) {
            SubtitleTrack subtitleTrack = this.mSubtitleController.getSelectedTrack();
            if (subtitleTrack != null) {
                synchronized (this.mIndexTrackPairs) {
                    i = 0;
                    while (i < this.mIndexTrackPairs.size()) {
                        if (((Pair) this.mIndexTrackPairs.get(i)).second == subtitleTrack && subtitleTrack.getTrackType() == trackType) {
                            return i;
                        }
                        i++;
                    }
                }
            }
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(7);
            request.writeInt(trackType);
            invoke(request, reply);
            int inbandTrackIndex = reply.readInt();
            synchronized (this.mIndexTrackPairs) {
                i = 0;
                while (i < this.mIndexTrackPairs.size()) {
                    Pair<Integer, SubtitleTrack> p = (Pair) this.mIndexTrackPairs.get(i);
                    if (p.first == null || ((Integer) p.first).intValue() != inbandTrackIndex) {
                        i++;
                    }
                }
                request.recycle();
                reply.recycle();
                return -1;
            }
        } finally {
            request.recycle();
            reply.recycle();
        }
        return i;
    }

    public void selectTrack(int index) throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "selectTrack() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.selectTrack(index);
        } else {
            selectOrDeselectTrack(index, true);
        }
    }

    public void deselectTrack(int index) throws IllegalStateException {
        if (this.mDebugLog) {
            Log.i(TAG, "deselectTrack() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            this.mOppoMediaPlayer.deselectTrack(index);
        } else {
            selectOrDeselectTrack(index, false);
        }
    }

    private void selectOrDeselectTrack(int index, boolean select) throws IllegalStateException {
        populateInbandTracks();
        try {
            Pair<Integer, SubtitleTrack> p = (Pair) this.mIndexTrackPairs.get(index);
            SubtitleTrack track = p.second;
            if (track == null) {
                selectOrDeselectInbandTrack(((Integer) p.first).intValue(), select);
            } else if (this.mSubtitleController != null) {
                if (select) {
                    if (track.getTrackType() == 3) {
                        int ttIndex = getSelectedTrack(3);
                        synchronized (this.mIndexTrackPairs) {
                            if (ttIndex >= 0) {
                                if (ttIndex < this.mIndexTrackPairs.size()) {
                                    Pair<Integer, SubtitleTrack> p2 = (Pair) this.mIndexTrackPairs.get(ttIndex);
                                    if (p2.first != null && p2.second == null) {
                                        selectOrDeselectInbandTrack(((Integer) p2.first).intValue(), false);
                                    }
                                }
                            }
                        }
                    }
                    this.mSubtitleController.selectTrack(track);
                    return;
                }
                if (this.mSubtitleController.getSelectedTrack() == track) {
                    this.mSubtitleController.selectTrack(null);
                } else {
                    Log.w(TAG, "trying to deselect track that was not selected");
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private void selectOrDeselectInbandTrack(int index, boolean select) throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(select ? 4 : 5);
            request.writeInt(index);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public void setRetransmitEndpoint(InetSocketAddress endpoint) throws IllegalStateException, IllegalArgumentException {
        if (this.mDebugLog) {
            Log.i(TAG, "setRetransmitEndpoint() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "setRetransmitEndpoint() oppomedia do not support! return directly");
            return;
        }
        String addrString = null;
        int port = 0;
        if (endpoint != null) {
            addrString = endpoint.getAddress().getHostAddress();
            port = endpoint.getPort();
        }
        int ret = native_setRetransmitEndpoint(addrString, port);
        if (ret != 0) {
            throw new IllegalArgumentException("Illegal re-transmit endpoint; native ret " + ret);
        }
    }

    protected void finalize() {
        baseRelease();
        native_finalize();
    }

    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mDebugLog) {
            Log.i(TAG, "getMediaTimeProvider() isOppoCreate = " + this.isOppoCreate);
        }
        if (this.isOppoCreate) {
            Log.e(TAG, "getMediaTimeProvider() oppomedia do not support! return directly");
            return null;
        }
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void postEventFromNative(Object mediaplayer_ref, int what, int arg1, int arg2, Object obj) {
        MediaPlayer mp = (MediaPlayer) ((WeakReference) mediaplayer_ref).get();
        if (mp != null) {
            Object obj2;
            switch (what) {
                case 1:
                    obj2 = mp.mDrmLock;
                    synchronized (obj2) {
                        mp.mDrmInfoResolved = true;
                    }
                case 200:
                    if (arg1 == 2) {
                        new Thread(new AnonymousClass14(mp)).start();
                        Thread.yield();
                        break;
                    }
                    break;
                case 210:
                    Log.v(TAG, "postEventFromNative MEDIA_DRM_INFO");
                    if (!(obj instanceof Parcel)) {
                        Log.w(TAG, "MEDIA_DRM_INFO msg.obj of unexpected type " + obj);
                        break;
                    }
                    DrmInfo drmInfo = new DrmInfo((Parcel) obj, null);
                    obj2 = mp.mDrmLock;
                    synchronized (obj2) {
                        mp.mDrmInfo = drmInfo;
                    }
            }
            if (mp.mEventHandler != null) {
                mp.mEventHandler.sendMessage(mp.mEventHandler.obtainMessage(what, arg1, arg2, obj));
            }
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        this.mOnBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        this.mOnSeekCompleteListener = listener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        this.mOnVideoSizeChangedListener = listener;
    }

    public void setOnTimedTextListener(OnTimedTextListener listener) {
        this.mOnTimedTextListener = listener;
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener listener) {
        this.mOnSubtitleDataListener = listener;
    }

    public void setOnTimedMetaDataAvailableListener(OnTimedMetaDataAvailableListener listener) {
        this.mOnTimedMetaDataAvailableListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        this.mOnInfoListener = listener;
    }

    public void setOnDrmConfigHelper(OnDrmConfigHelper listener) {
        synchronized (this.mDrmLock) {
            this.mOnDrmConfigHelper = listener;
        }
    }

    public void setOnDrmInfoListener(OnDrmInfoListener listener) {
        setOnDrmInfoListener(listener, null);
    }

    public void setOnDrmInfoListener(OnDrmInfoListener listener, Handler handler) {
        synchronized (this.mDrmLock) {
            if (listener != null) {
                this.mOnDrmInfoHandlerDelegate = new OnDrmInfoHandlerDelegate(this, listener, handler);
            } else {
                this.mOnDrmInfoHandlerDelegate = null;
            }
        }
    }

    public void setOnDrmPreparedListener(OnDrmPreparedListener listener) {
        setOnDrmPreparedListener(listener, null);
    }

    public void setOnDrmPreparedListener(OnDrmPreparedListener listener, Handler handler) {
        synchronized (this.mDrmLock) {
            if (listener != null) {
                this.mOnDrmPreparedHandlerDelegate = new OnDrmPreparedHandlerDelegate(this, listener, handler);
            } else {
                this.mOnDrmPreparedHandlerDelegate = null;
            }
        }
    }

    public DrmInfo getDrmInfo() {
        DrmInfo drmInfo = null;
        synchronized (this.mDrmLock) {
            if (this.mDrmInfoResolved || this.mDrmInfo != null) {
                if (this.mDrmInfo != null) {
                    drmInfo = this.mDrmInfo.makeCopy();
                }
            } else {
                String msg = "The Player has not been prepared yet";
                Log.v(TAG, "The Player has not been prepared yet");
                throw new IllegalStateException("The Player has not been prepared yet");
            }
        }
        return drmInfo;
    }

    public void prepareDrm(UUID uuid) throws UnsupportedSchemeException, ResourceBusyException, ProvisioningNetworkErrorException, ProvisioningServerErrorException {
        String msg;
        OnDrmPreparedHandlerDelegate onDrmPreparedHandlerDelegate;
        Log.v(TAG, "prepareDrm: uuid: " + uuid + " mOnDrmConfigHelper: " + this.mOnDrmConfigHelper);
        boolean allDoneWithoutProvisioning = false;
        synchronized (this.mDrmLock) {
            if (this.mDrmInfo == null) {
                msg = "prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.";
                Log.e(TAG, "prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.");
                throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.");
            } else if (this.mActiveDrmScheme) {
                msg = "prepareDrm(): Wrong usage: There is already an active DRM scheme with " + this.mDrmUUID;
                Log.e(TAG, msg);
                throw new IllegalStateException(msg);
            } else if (this.mPrepareDrmInProgress) {
                msg = "prepareDrm(): Wrong usage: There is already a pending prepareDrm call.";
                Log.e(TAG, "prepareDrm(): Wrong usage: There is already a pending prepareDrm call.");
                throw new IllegalStateException("prepareDrm(): Wrong usage: There is already a pending prepareDrm call.");
            } else if (this.mDrmProvisioningInProgress) {
                msg = "prepareDrm(): Unexpectd: Provisioning is already in progress.";
                Log.e(TAG, "prepareDrm(): Unexpectd: Provisioning is already in progress.");
                throw new IllegalStateException("prepareDrm(): Unexpectd: Provisioning is already in progress.");
            } else {
                cleanDrmObj();
                this.mPrepareDrmInProgress = true;
                onDrmPreparedHandlerDelegate = this.mOnDrmPreparedHandlerDelegate;
                try {
                    prepareDrm_createDrmStep(uuid);
                    this.mDrmConfigAllowed = true;
                } catch (Exception e) {
                    Log.w(TAG, "prepareDrm(): Exception ", e);
                    this.mPrepareDrmInProgress = false;
                    throw e;
                }
            }
        }
        if (this.mOnDrmConfigHelper != null) {
            this.mOnDrmConfigHelper.onDrmConfig(this);
        }
        synchronized (this.mDrmLock) {
            this.mDrmConfigAllowed = false;
            try {
                prepareDrm_openSessionStep(uuid);
                this.mDrmUUID = uuid;
                this.mActiveDrmScheme = true;
                allDoneWithoutProvisioning = true;
                if (!this.mDrmProvisioningInProgress) {
                    this.mPrepareDrmInProgress = false;
                }
                if (null != null) {
                    cleanDrmObj();
                }
            } catch (IllegalStateException e2) {
                msg = "prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().";
                Log.e(TAG, "prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
                throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
            } catch (NotProvisionedException e3) {
                Log.w(TAG, "prepareDrm: NotProvisionedException");
                int result = HandleProvisioninig(uuid);
                if (result != 0) {
                    switch (result) {
                        case 1:
                            msg = "prepareDrm: Provisioning was required but failed due to a network error.";
                            Log.e(TAG, msg);
                            throw new ProvisioningNetworkErrorException(msg);
                        case 2:
                            msg = "prepareDrm: Provisioning was required but the request was denied by the server.";
                            Log.e(TAG, msg);
                            throw new ProvisioningServerErrorException(msg);
                        default:
                            msg = "prepareDrm: Post-provisioning preparation failed.";
                            Log.e(TAG, msg);
                            throw new IllegalStateException(msg);
                    }
                }
                if (!this.mDrmProvisioningInProgress) {
                    this.mPrepareDrmInProgress = false;
                }
                if (null != null) {
                    cleanDrmObj();
                }
            } catch (Exception e4) {
                Log.e(TAG, "prepareDrm: Exception " + e4);
                throw e4;
            } catch (Throwable th) {
                if (!this.mDrmProvisioningInProgress) {
                    this.mPrepareDrmInProgress = false;
                }
                if (null != null) {
                    cleanDrmObj();
                }
            }
        }
        if (allDoneWithoutProvisioning && onDrmPreparedHandlerDelegate != null) {
            onDrmPreparedHandlerDelegate.notifyClient(0);
        }
    }

    public void releaseDrm() throws NoDrmSchemeException {
        Log.v(TAG, "releaseDrm:");
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                try {
                    _releaseDrm();
                    cleanDrmObj();
                    this.mActiveDrmScheme = false;
                } catch (IllegalStateException e) {
                    Log.w(TAG, "releaseDrm: Exception ", e);
                    throw new IllegalStateException("releaseDrm: The player is not in a valid state.");
                } catch (Exception e2) {
                    Log.e(TAG, "releaseDrm: Exception ", e2);
                }
            } else {
                Log.e(TAG, "releaseDrm(): No active DRM scheme to release.");
                throw new NoDrmSchemeException("releaseDrm: No active DRM scheme to release.");
            }
        }
    }

    public KeyRequest getKeyRequest(byte[] keySetId, byte[] initData, String mimeType, int keyType, Map<String, String> optionalParameters) throws NoDrmSchemeException {
        KeyRequest request;
        Log.v(TAG, "getKeyRequest:  keySetId: " + keySetId + " initData:" + initData + " mimeType: " + mimeType + " keyType: " + keyType + " optionalParameters: " + optionalParameters);
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                byte[] scope;
                HashMap hmapOptionalParameters;
                if (keyType != 3) {
                    try {
                        scope = this.mDrmSessionId;
                    } catch (NotProvisionedException e) {
                        Log.w(TAG, "getKeyRequest NotProvisionedException: Unexpected. Shouldn't have reached here.");
                        throw new IllegalStateException("getKeyRequest: Unexpected provisioning error.");
                    } catch (Exception e2) {
                        Log.w(TAG, "getKeyRequest Exception " + e2);
                        throw e2;
                    }
                }
                scope = keySetId;
                if (optionalParameters != null) {
                    hmapOptionalParameters = new HashMap(optionalParameters);
                } else {
                    hmapOptionalParameters = null;
                }
                request = this.mDrmObj.getKeyRequest(scope, initData, mimeType, keyType, hmapOptionalParameters);
                Log.v(TAG, "getKeyRequest:   --> request: " + request);
            } else {
                Log.e(TAG, "getKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeException("getKeyRequest: Has to set a DRM scheme first.");
            }
        }
        return request;
    }

    public byte[] provideKeyResponse(byte[] keySetId, byte[] response) throws NoDrmSchemeException, DeniedByServerException {
        byte[] keySetResult;
        Log.v(TAG, "provideKeyResponse: keySetId: " + keySetId + " response: " + response);
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                byte[] scope;
                if (keySetId == null) {
                    try {
                        scope = this.mDrmSessionId;
                    } catch (NotProvisionedException e) {
                        Log.w(TAG, "provideKeyResponse NotProvisionedException: Unexpected. Shouldn't have reached here.");
                        throw new IllegalStateException("provideKeyResponse: Unexpected provisioning error.");
                    } catch (Exception e2) {
                        Log.w(TAG, "provideKeyResponse Exception " + e2);
                        throw e2;
                    }
                }
                scope = keySetId;
                keySetResult = this.mDrmObj.provideKeyResponse(scope, response);
                Log.v(TAG, "provideKeyResponse: keySetId: " + keySetId + " response: " + response + " --> " + keySetResult);
            } else {
                Log.e(TAG, "getKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeException("getKeyRequest: Has to set a DRM scheme first.");
            }
        }
        return keySetResult;
    }

    public void restoreKeys(byte[] keySetId) throws NoDrmSchemeException {
        Log.v(TAG, "restoreKeys: keySetId: " + keySetId);
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme) {
                try {
                    this.mDrmObj.restoreKeys(this.mDrmSessionId, keySetId);
                } catch (Exception e) {
                    Log.w(TAG, "restoreKeys Exception " + e);
                    throw e;
                }
            }
            Log.w(TAG, "restoreKeys NoDrmSchemeException");
            throw new NoDrmSchemeException("restoreKeys: Has to set a DRM scheme first.");
        }
    }

    public String getDrmPropertyString(String propertyName) throws NoDrmSchemeException {
        String value;
        Log.v(TAG, "getDrmPropertyString: propertyName: " + propertyName);
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme || (this.mDrmConfigAllowed ^ 1) == 0) {
                try {
                    value = this.mDrmObj.getPropertyString(propertyName);
                } catch (Exception e) {
                    Log.w(TAG, "getDrmPropertyString Exception " + e);
                    throw e;
                }
            }
            Log.w(TAG, "getDrmPropertyString NoDrmSchemeException");
            throw new NoDrmSchemeException("getDrmPropertyString: Has to prepareDrm() first.");
        }
        Log.v(TAG, "getDrmPropertyString: propertyName: " + propertyName + " --> value: " + value);
        return value;
    }

    public void setDrmPropertyString(String propertyName, String value) throws NoDrmSchemeException {
        Log.v(TAG, "setDrmPropertyString: propertyName: " + propertyName + " value: " + value);
        synchronized (this.mDrmLock) {
            if (this.mActiveDrmScheme || (this.mDrmConfigAllowed ^ 1) == 0) {
                try {
                    this.mDrmObj.setPropertyString(propertyName, value);
                } catch (Exception e) {
                    Log.w(TAG, "setDrmPropertyString Exception " + e);
                    throw e;
                }
            }
            Log.w(TAG, "setDrmPropertyString NoDrmSchemeException");
            throw new NoDrmSchemeException("setDrmPropertyString: Has to prepareDrm() first.");
        }
    }

    private void prepareDrm_createDrmStep(UUID uuid) throws UnsupportedSchemeException {
        Log.v(TAG, "prepareDrm_createDrmStep: UUID: " + uuid);
        try {
            this.mDrmObj = new MediaDrm(uuid);
            Log.v(TAG, "prepareDrm_createDrmStep: Created mDrmObj=" + this.mDrmObj);
        } catch (Exception e) {
            Log.e(TAG, "prepareDrm_createDrmStep: MediaDrm failed with " + e);
            throw e;
        }
    }

    private void prepareDrm_openSessionStep(UUID uuid) throws NotProvisionedException, ResourceBusyException {
        Log.v(TAG, "prepareDrm_openSessionStep: uuid: " + uuid);
        try {
            this.mDrmSessionId = this.mDrmObj.openSession();
            Log.v(TAG, "prepareDrm_openSessionStep: mDrmSessionId=" + this.mDrmSessionId);
            _prepareDrm(getByteArrayFromUUID(uuid), this.mDrmSessionId);
            Log.v(TAG, "prepareDrm_openSessionStep: _prepareDrm/Crypto succeeded");
        } catch (Exception e) {
            Log.e(TAG, "prepareDrm_openSessionStep: open/crypto failed with " + e);
            throw e;
        }
    }

    private int HandleProvisioninig(UUID uuid) {
        if (this.mDrmProvisioningInProgress) {
            Log.e(TAG, "HandleProvisioninig: Unexpected mDrmProvisioningInProgress");
            return 3;
        }
        ProvisionRequest provReq = this.mDrmObj.getProvisionRequest();
        if (provReq == null) {
            Log.e(TAG, "HandleProvisioninig: getProvisionRequest returned null.");
            return 3;
        }
        int result;
        Log.v(TAG, "HandleProvisioninig provReq  data: " + provReq.getData() + " url: " + provReq.getDefaultUrl());
        this.mDrmProvisioningInProgress = true;
        this.mDrmProvisioningThread = new ProvisioningThread().initialize(provReq, uuid, this);
        this.mDrmProvisioningThread.start();
        if (this.mOnDrmPreparedHandlerDelegate != null) {
            result = 0;
        } else {
            try {
                this.mDrmProvisioningThread.join();
            } catch (Exception e) {
                Log.w(TAG, "HandleProvisioninig: Thread.join Exception " + e);
            }
            result = this.mDrmProvisioningThread.status();
            this.mDrmProvisioningThread = null;
        }
        return result;
    }

    private boolean resumePrepareDrm(UUID uuid) {
        Log.v(TAG, "resumePrepareDrm: uuid: " + uuid);
        try {
            prepareDrm_openSessionStep(uuid);
            this.mDrmUUID = uuid;
            this.mActiveDrmScheme = true;
            return true;
        } catch (Exception e) {
            Log.w(TAG, "HandleProvisioninig: Thread run _prepareDrm resume failed with " + e);
            return false;
        }
    }

    private void resetDrmState() {
        synchronized (this.mDrmLock) {
            Log.v(TAG, "resetDrmState:  mDrmInfo=" + this.mDrmInfo + " mDrmProvisioningThread=" + this.mDrmProvisioningThread + " mPrepareDrmInProgress=" + this.mPrepareDrmInProgress + " mActiveDrmScheme=" + this.mActiveDrmScheme);
            this.mDrmInfoResolved = false;
            this.mDrmInfo = null;
            if (this.mDrmProvisioningThread != null) {
                try {
                    this.mDrmProvisioningThread.join();
                } catch (InterruptedException e) {
                    Log.w(TAG, "resetDrmState: ProvThread.join Exception " + e);
                }
                this.mDrmProvisioningThread = null;
            }
            this.mPrepareDrmInProgress = false;
            this.mActiveDrmScheme = false;
            cleanDrmObj();
        }
        return;
    }

    private void cleanDrmObj() {
        Log.v(TAG, "cleanDrmObj: mDrmObj=" + this.mDrmObj + " mDrmSessionId=" + this.mDrmSessionId);
        if (this.mDrmSessionId != null) {
            this.mDrmObj.closeSession(this.mDrmSessionId);
            this.mDrmSessionId = null;
        }
        if (this.mDrmObj != null) {
            this.mDrmObj.release();
            this.mDrmObj = null;
        }
    }

    private static final byte[] getByteArrayFromUUID(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] uuidBytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            uuidBytes[i] = (byte) ((int) (msb >>> ((7 - i) * 8)));
            uuidBytes[i + 8] = (byte) ((int) (lsb >>> ((7 - i) * 8)));
        }
        return uuidBytes;
    }

    private boolean isVideoScalingModeSupported(int mode) {
        if (mode == 1 || mode == 2) {
            return true;
        }
        return false;
    }
}
