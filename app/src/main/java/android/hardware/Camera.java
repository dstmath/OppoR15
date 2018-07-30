package android.hardware;

import android.app.ActivityThread;
import android.bluetooth.BluetoothClass.Device;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.IAudioService.Stub;
import android.opengl.GLES10;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Element.DataKind;
import android.renderscript.Element.DataType;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RenderScript;
import android.renderscript.Type.Builder;
import android.system.OsConstants;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.TextUtils.StringSplitter;
import android.util.Log;
import android.util.SeempLog;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.oppo.hypnus.HypnusManager;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import oppo.util.OppoStatistics;

@Deprecated
public class Camera {
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";
    private static final String ACTION_VIDEO_CALL_FLOAT = "com.oppo.videocallfloat.VideoFloatWindowService";
    private static final String CAMERA_APP_PACKAGE = "com.oppo.camera";
    public static final int CAMERA_ERROR_EVICTED = 2;
    public static final int CAMERA_ERROR_SERVER_DIED = 100;
    public static final int CAMERA_ERROR_UNKNOWN = 1;
    private static final int CAMERA_FACE_DETECTION_HW = 0;
    private static final int CAMERA_FACE_DETECTION_SW = 1;
    public static final int CAMERA_HAL_API_VERSION_1_0 = 256;
    private static final int CAMERA_HAL_API_VERSION_NORMAL_CONNECT = -2;
    private static final int CAMERA_HAL_API_VERSION_UNSPECIFIED = -1;
    private static final int CAMERA_MSG_COMPRESSED_IMAGE = 256;
    private static final int CAMERA_MSG_ERROR = 1;
    private static final int CAMERA_MSG_FOCUS = 4;
    private static final int CAMERA_MSG_FOCUS_MOVE = 2048;
    private static final int CAMERA_MSG_META_DATA = 8192;
    private static final int CAMERA_MSG_POSTVIEW_FRAME = 64;
    private static final int CAMERA_MSG_PREVIEW_FRAME = 16;
    private static final int CAMERA_MSG_PREVIEW_METADATA = 1024;
    private static final int CAMERA_MSG_RAW_IMAGE = 128;
    private static final int CAMERA_MSG_RAW_IMAGE_NOTIFY = 512;
    private static final int CAMERA_MSG_SHUTTER = 2;
    private static final int CAMERA_MSG_STATS_DATA = 4096;
    private static final int CAMERA_MSG_VIDEO_FRAME = 32;
    private static final int CAMERA_MSG_ZOOM = 8;
    private static final int CAMERA_TIME_HYPNUS_0 = 0;
    private static final int CAMERA_TIME_HYPNUS_1500 = 1500;
    private static final int CAMERA_TIME_HYPNUS_2500 = 2500;
    private static final int CAMERA_TIME_HYPNUS_4000 = 4000;
    private static final int EACCESS = -13;
    private static final int EBUSY = -16;
    private static final int EINVAL = -22;
    private static final int ENODEV = -19;
    private static final int ENOSYS = -38;
    private static final int EOPNOTSUPP = -95;
    private static final int EUSERS = -87;
    private static final String EXTRA_CAMERA_ID = "cameraId";
    private static final String EXTRA_CAMERA_STATUS = "status";
    private static final String EXTRA_CAMERA_STATUS_OPEN = "open";
    private static final String EXTRA_CAMERA_STATUS_RELEASE = "release";
    private static final String EXTRA_CURRENT_PACKAGE_NAME = "pkgName";
    private static final int NO_ERROR = 0;
    private static final String TAG = "Camera";
    private static final String VIDEO_CALL_FLOAT_CLASSNAME = "com.oppo.videocallfloat.VideoFloatWindowService";
    private static final String VIDEO_CALL_FLOAT_PACKAGE = "com.oppo.videocallfloat";
    private static HypnusManager sHM = null;
    private AutoFocusCallback mAutoFocusCallback;
    private final Object mAutoFocusCallbackLock = new Object();
    private AutoFocusMoveCallback mAutoFocusMoveCallback;
    private CameraDataCallback mCameraDataCallback;
    private int mCameraId;
    private CameraMetaDataCallback mCameraMetaDataCallback;
    private ErrorCallback mErrorCallback;
    private EventHandler mEventHandler;
    private boolean mFaceDetectionRunning = false;
    private FaceDetectionListener mFaceListener;
    private boolean mHypnusCtrl;
    private PictureCallback mJpegCallback;
    private boolean mLongshotEnable;
    private long mNativeContext;
    private boolean mOneShot;
    private PictureCallback mPostviewCallback;
    private PreviewCallback mPreviewCallback;
    private PictureCallback mRawImageCallback;
    private ShutterCallback mShutterCallback;
    private boolean mUsingPreviewAllocation;
    private boolean mWithBuffer;
    private OnZoomChangeListener mZoomListener;

    @Deprecated
    public static class Area {
        public Rect rect;
        public int weight;

        public Area(Rect rect, int weight) {
            this.rect = rect;
            this.weight = weight;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof Area)) {
                return false;
            }
            Area a = (Area) obj;
            if (this.rect == null) {
                if (a.rect != null) {
                    return false;
                }
            } else if (!this.rect.equals(a.rect)) {
                return false;
            }
            if (this.weight == a.weight) {
                z = true;
            }
            return z;
        }
    }

    @Deprecated
    public interface AutoFocusCallback {
        void onAutoFocus(boolean z, Camera camera);
    }

    @Deprecated
    public interface AutoFocusMoveCallback {
        void onAutoFocusMoving(boolean z, Camera camera);
    }

    public interface CameraDataCallback {
        void onCameraData(int[] iArr, Camera camera);
    }

    @Deprecated
    public static class CameraInfo {
        public static final int CAMERA_FACING_BACK = 0;
        public static final int CAMERA_FACING_FRONT = 1;
        public static final int CAMERA_SUPPORT_MODE_NONZSL = 3;
        public static final int CAMERA_SUPPORT_MODE_ZSL = 2;
        public boolean canDisableShutterSound;
        public int facing;
        public int orientation;
    }

    public interface CameraMetaDataCallback {
        void onCameraMetaData(byte[] bArr, Camera camera);
    }

    public class Coordinate {
        public int xCoordinate;
        public int yCoordinate;

        public Coordinate(int x, int y) {
            this.xCoordinate = x;
            this.yCoordinate = y;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof Coordinate)) {
                return false;
            }
            Coordinate c = (Coordinate) obj;
            if (this.xCoordinate == c.xCoordinate && this.yCoordinate == c.yCoordinate) {
                z = true;
            }
            return z;
        }
    }

    @Deprecated
    public interface ErrorCallback {
        void onError(int i, Camera camera);
    }

    private class EventHandler extends Handler {
        private final Camera mCamera;

        public EventHandler(Camera c, Looper looper) {
            super(looper);
            this.mCamera = c;
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            boolean z2 = false;
            switch (msg.what) {
                case 1:
                    Log.e(Camera.TAG, "Error " + msg.arg1);
                    if (Camera.sHM == null) {
                        Camera.sHM = new HypnusManager();
                    }
                    if (Camera.sHM == null || !Camera.this.mHypnusCtrl || (Camera.this.mLongshotEnable ^ 1) == 0 || Device.AUDIO_VIDEO_MICROPHONE != msg.arg1) {
                        if (Camera.this.mErrorCallback != null) {
                            Camera.this.mErrorCallback.onError(msg.arg1, this.mCamera);
                        }
                        return;
                    }
                    Camera.sHM.hypnusSetAction(15, 0);
                    return;
                case 2:
                    if (Camera.this.mShutterCallback != null) {
                        Camera.this.mShutterCallback.onShutter();
                    }
                    return;
                case 4:
                    AutoFocusCallback cb;
                    synchronized (Camera.this.mAutoFocusCallbackLock) {
                        cb = Camera.this.mAutoFocusCallback;
                    }
                    if (cb != null) {
                        cb.onAutoFocus(msg.arg1 != 0, this.mCamera);
                    }
                    return;
                case 8:
                    if (Camera.this.mZoomListener != null) {
                        OnZoomChangeListener -get16 = Camera.this.mZoomListener;
                        int i = msg.arg1;
                        if (msg.arg2 == 0) {
                            z = false;
                        }
                        -get16.onZoomChange(i, z, this.mCamera);
                    }
                    return;
                case 16:
                    PreviewCallback pCb = Camera.this.mPreviewCallback;
                    if (pCb != null) {
                        if (Camera.this.mOneShot) {
                            Camera.this.mPreviewCallback = null;
                        } else if (!Camera.this.mWithBuffer) {
                            Camera.this.setHasPreviewCallback(true, false);
                        }
                        pCb.onPreviewFrame((byte[]) msg.obj, this.mCamera);
                    }
                    return;
                case 64:
                    if (Camera.this.mPostviewCallback != null) {
                        Camera.this.mPostviewCallback.onPictureTaken((byte[]) msg.obj, this.mCamera);
                    }
                    return;
                case 128:
                    if (Camera.this.mRawImageCallback != null) {
                        Camera.this.mRawImageCallback.onPictureTaken((byte[]) msg.obj, this.mCamera);
                    }
                    return;
                case 256:
                    if (Camera.this.mJpegCallback != null) {
                        Camera.this.mJpegCallback.onPictureTaken((byte[]) msg.obj, this.mCamera);
                        if (Camera.sHM == null) {
                            Camera.sHM = new HypnusManager();
                        }
                        if (!(Camera.sHM == null || !Camera.this.mHypnusCtrl || (Camera.this.mLongshotEnable ^ 1) == 0)) {
                            Camera.sHM.hypnusSetAction(15, 0);
                        }
                    }
                    return;
                case 1024:
                    if (Camera.this.mFaceListener != null) {
                        Camera.this.mFaceListener.onFaceDetection((Face[]) msg.obj, this.mCamera);
                    }
                    return;
                case 2048:
                    if (Camera.this.mAutoFocusMoveCallback != null) {
                        AutoFocusMoveCallback -get2 = Camera.this.mAutoFocusMoveCallback;
                        if (msg.arg1 != 0) {
                            z2 = true;
                        }
                        -get2.onAutoFocusMoving(z2, this.mCamera);
                    }
                    return;
                case 4096:
                    int[] statsdata = new int[257];
                    for (int i2 = 0; i2 < 257; i2++) {
                        statsdata[i2] = Camera.byteToInt((byte[]) msg.obj, i2 * 4);
                    }
                    if (Camera.this.mCameraDataCallback != null) {
                        Camera.this.mCameraDataCallback.onCameraData(statsdata, this.mCamera);
                    }
                    return;
                case 8192:
                    if (Camera.this.mCameraMetaDataCallback != null) {
                        Camera.this.mCameraMetaDataCallback.onCameraMetaData((byte[]) msg.obj, this.mCamera);
                    }
                    return;
                default:
                    Log.e(Camera.TAG, "Unknown message type " + msg.what);
                    return;
            }
        }
    }

    @Deprecated
    public static class Face {
        public int blinkDetected = 0;
        public int faceRecognised = 0;
        public int id = -1;
        public Point leftEye = null;
        public Point mouth = null;
        public Rect rect;
        public Point rightEye = null;
        public int score;
        public int smileDegree = 0;
        public int smileScore = 0;
    }

    @Deprecated
    public interface FaceDetectionListener {
        void onFaceDetection(Face[] faceArr, Camera camera);
    }

    @Deprecated
    public interface OnZoomChangeListener {
        void onZoomChange(int i, boolean z, Camera camera);
    }

    @Deprecated
    public class Parameters {
        public static final String AE_BRACKET = "AE-Bracket";
        public static final String AE_BRACKET_HDR = "HDR";
        public static final String AE_BRACKET_HDR_OFF = "Off";
        public static final String ANTIBANDING_50HZ = "50hz";
        public static final String ANTIBANDING_60HZ = "60hz";
        public static final String ANTIBANDING_AUTO = "auto";
        public static final String ANTIBANDING_OFF = "off";
        public static final String AUTO_EXPOSURE_CENTER_WEIGHTED = "center-weighted";
        public static final String AUTO_EXPOSURE_FRAME_AVG = "frame-average";
        public static final String AUTO_EXPOSURE_SPOT_METERING = "spot-metering";
        public static final String CONTINUOUS_AF_OFF = "caf-off";
        public static final String CONTINUOUS_AF_ON = "caf-on";
        public static final String DENOISE_OFF = "denoise-off";
        public static final String DENOISE_ON = "denoise-on";
        private static final String DUAL_CAM_LPM_MODE = "dual-cam-lpm-mode";
        public static final String EFFECT_AQUA = "aqua";
        public static final String EFFECT_BLACKBOARD = "blackboard";
        public static final String EFFECT_MONO = "mono";
        public static final String EFFECT_NEGATIVE = "negative";
        public static final String EFFECT_NONE = "none";
        public static final String EFFECT_POSTERIZE = "posterize";
        public static final String EFFECT_SEPIA = "sepia";
        public static final String EFFECT_SOLARIZE = "solarize";
        public static final String EFFECT_WHITEBOARD = "whiteboard";
        public static final String FACE_DETECTION_OFF = "off";
        public static final String FACE_DETECTION_ON = "on";
        private static final String FALSE = "false";
        public static final String FLASH_MODE_AUTO = "auto";
        public static final String FLASH_MODE_OFF = "off";
        public static final String FLASH_MODE_ON = "on";
        public static final String FLASH_MODE_RED_EYE = "red-eye";
        public static final String FLASH_MODE_TORCH = "torch";
        public static final int FOCUS_DISTANCE_FAR_INDEX = 2;
        public static final int FOCUS_DISTANCE_NEAR_INDEX = 0;
        public static final int FOCUS_DISTANCE_OPTIMAL_INDEX = 1;
        public static final String FOCUS_MODE_AUTO = "auto";
        public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";
        public static final String FOCUS_MODE_CONTINUOUS_VIDEO = "continuous-video";
        public static final String FOCUS_MODE_EDOF = "edof";
        public static final String FOCUS_MODE_FIXED = "fixed";
        public static final String FOCUS_MODE_INFINITY = "infinity";
        public static final String FOCUS_MODE_MACRO = "macro";
        public static final String FOCUS_MODE_MANUAL_POSITION = "manual";
        public static final String FOCUS_MODE_NORMAL = "normal";
        public static final String HISTOGRAM_DISABLE = "disable";
        public static final String HISTOGRAM_ENABLE = "enable";
        public static final String ISO_100 = "ISO100";
        public static final String ISO_1600 = "ISO1600";
        public static final String ISO_200 = "ISO200";
        public static final String ISO_3200 = "ISO3200";
        public static final String ISO_400 = "ISO400";
        public static final String ISO_800 = "ISO800";
        public static final String ISO_AUTO = "auto";
        public static final String ISO_HJR = "ISO_HJR";
        private static final String KEY_ANTIBANDING = "antibanding";
        private static final String KEY_AUTO_EXPOSURE_LOCK = "auto-exposure-lock";
        private static final String KEY_AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK = "auto-whitebalance-lock";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
        private static final String KEY_EFFECT = "effect";
        private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
        private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        private static final String KEY_FLASH_MODE = "flash-mode";
        private static final String KEY_FOCAL_LENGTH = "focal-length";
        private static final String KEY_FOCUS_AREAS = "focus-areas";
        private static final String KEY_FOCUS_DISTANCES = "focus-distances";
        private static final String KEY_FOCUS_MODE = "focus-mode";
        private static final String KEY_GPS_ALTITUDE = "gps-altitude";
        private static final String KEY_GPS_LATITUDE = "gps-latitude";
        private static final String KEY_GPS_LONGITUDE = "gps-longitude";
        private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
        private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        private static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
        private static final String KEY_JPEG_QUALITY = "jpeg-quality";
        private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
        private static final String KEY_MAX_NUM_DETECTED_FACES_HW = "max-num-detected-faces-hw";
        private static final String KEY_MAX_NUM_DETECTED_FACES_SW = "max-num-detected-faces-sw";
        private static final String KEY_MAX_NUM_FOCUS_AREAS = "max-num-focus-areas";
        private static final String KEY_MAX_NUM_METERING_AREAS = "max-num-metering-areas";
        private static final String KEY_MAX_ZOOM = "max-zoom";
        private static final String KEY_METERING_AREAS = "metering-areas";
        private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
        private static final String KEY_PICTURE_FORMAT = "picture-format";
        private static final String KEY_PICTURE_SIZE = "picture-size";
        private static final String KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO = "preferred-preview-size-for-video";
        private static final String KEY_PREVIEW_FORMAT = "preview-format";
        private static final String KEY_PREVIEW_FPS_RANGE = "preview-fps-range";
        private static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
        private static final String KEY_PREVIEW_SIZE = "preview-size";
        public static final String KEY_QC_AE_BRACKET_HDR = "ae-bracket-hdr";
        private static final String KEY_QC_AUTO_EXPOSURE = "auto-exposure";
        private static final String KEY_QC_AUTO_HDR_ENABLE = "auto-hdr-enable";
        private static final String KEY_QC_CAMERA_MODE = "camera-mode";
        private static final String KEY_QC_CONTINUOUS_AF = "continuous-af";
        private static final String KEY_QC_CONTRAST = "contrast";
        private static final String KEY_QC_DENOISE = "denoise";
        private static final String KEY_QC_EXIF_DATETIME = "exif-datetime";
        private static final String KEY_QC_EXPOSURE_TIME = "exposure-time";
        private static final String KEY_QC_FACE_DETECTION = "face-detection";
        private static final String KEY_QC_GPS_ALTITUDE_REF = "gps-altitude-ref";
        private static final String KEY_QC_GPS_LATITUDE_REF = "gps-latitude-ref";
        private static final String KEY_QC_GPS_LONGITUDE_REF = "gps-longitude-ref";
        private static final String KEY_QC_GPS_STATUS = "gps-status";
        private static final String KEY_QC_HFR_SIZE = "hfr-size";
        private static final String KEY_QC_HISTOGRAM = "histogram";
        private static final String KEY_QC_ISO_MODE = "iso";
        private static final String KEY_QC_LENSSHADE = "lensshade";
        private static final String KEY_QC_MANUAL_FOCUS_POSITION = "manual-focus-position";
        private static final String KEY_QC_MANUAL_FOCUS_POS_TYPE = "manual-focus-pos-type";
        private static final String KEY_QC_MAX_CONTRAST = "max-contrast";
        private static final String KEY_QC_MAX_EXPOSURE_TIME = "max-exposure-time";
        private static final String KEY_QC_MAX_SATURATION = "max-saturation";
        private static final String KEY_QC_MAX_SHARPNESS = "max-sharpness";
        private static final String KEY_QC_MAX_WB_CCT = "max-wb-cct";
        private static final String KEY_QC_MEMORY_COLOR_ENHANCEMENT = "mce";
        private static final String KEY_QC_MIN_EXPOSURE_TIME = "min-exposure-time";
        private static final String KEY_QC_MIN_WB_CCT = "min-wb-cct";
        private static final String KEY_QC_POWER_MODE = "power-mode";
        private static final String KEY_QC_POWER_MODE_SUPPORTED = "power-mode-supported";
        private static final String KEY_QC_PREVIEW_FRAME_RATE_AUTO_MODE = "frame-rate-auto";
        private static final String KEY_QC_PREVIEW_FRAME_RATE_FIXED_MODE = "frame-rate-fixed";
        private static final String KEY_QC_PREVIEW_FRAME_RATE_MODE = "preview-frame-rate-mode";
        private static final String KEY_QC_REDEYE_REDUCTION = "redeye-reduction";
        private static final String KEY_QC_SATURATION = "saturation";
        private static final String KEY_QC_SCENE_DETECT = "scene-detect";
        private static final String KEY_QC_SELECTABLE_ZONE_AF = "selectable-zone-af";
        private static final String KEY_QC_SHARPNESS = "sharpness";
        private static final String KEY_QC_SKIN_TONE_ENHANCEMENT = "skinToneEnhancement";
        private static final String KEY_QC_TOUCH_AF_AEC = "touch-af-aec";
        private static final String KEY_QC_TOUCH_INDEX_AEC = "touch-index-aec";
        private static final String KEY_QC_TOUCH_INDEX_AF = "touch-index-af";
        private static final String KEY_QC_VIDEO_HDR = "video-hdr";
        private static final String KEY_QC_VIDEO_HIGH_FRAME_RATE = "video-hfr";
        private static final String KEY_QC_VIDEO_ROTATION = "video-rotation";
        private static final String KEY_QC_WB_MANUAL_CCT = "wb-manual-cct";
        private static final String KEY_QC_ZSL = "zsl";
        private static final String KEY_RECORDING_HINT = "recording-hint";
        private static final String KEY_ROTATION = "rotation";
        private static final String KEY_SCENE_MODE = "scene-mode";
        private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
        private static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
        private static final String KEY_VIDEO_SIZE = "video-size";
        private static final String KEY_VIDEO_SNAPSHOT_SUPPORTED = "video-snapshot-supported";
        private static final String KEY_VIDEO_STABILIZATION = "video-stabilization";
        private static final String KEY_VIDEO_STABILIZATION_SUPPORTED = "video-stabilization-supported";
        private static final String KEY_WHITE_BALANCE = "whitebalance";
        private static final String KEY_ZOOM = "zoom";
        private static final String KEY_ZOOM_RATIOS = "zoom-ratios";
        private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
        public static final String LENSSHADE_DISABLE = "disable";
        public static final String LENSSHADE_ENABLE = "enable";
        public static final String LOW_POWER = "Low_Power";
        private static final int MANUAL_FOCUS_POS_TYPE_DAC = 1;
        private static final int MANUAL_FOCUS_POS_TYPE_INDEX = 0;
        public static final String MCE_DISABLE = "disable";
        public static final String MCE_ENABLE = "enable";
        public static final String NORMAL_POWER = "Normal_Power";
        private static final String PIXEL_FORMAT_BAYER_RGGB = "bayer-rggb";
        private static final String PIXEL_FORMAT_JPEG = "jpeg";
        private static final String PIXEL_FORMAT_NV12 = "nv12";
        private static final String PIXEL_FORMAT_RAW = "raw";
        private static final String PIXEL_FORMAT_RGB565 = "rgb565";
        private static final String PIXEL_FORMAT_YUV420P = "yuv420p";
        private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
        private static final String PIXEL_FORMAT_YUV420SP_ADRENO = "yuv420sp-adreno";
        private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
        private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
        private static final String PIXEL_FORMAT_YV12 = "yv12";
        public static final int PREVIEW_FPS_MAX_INDEX = 1;
        public static final int PREVIEW_FPS_MIN_INDEX = 0;
        public static final String REDEYE_REDUCTION_DISABLE = "disable";
        public static final String REDEYE_REDUCTION_ENABLE = "enable";
        public static final String SCENE_DETECT_OFF = "off";
        public static final String SCENE_DETECT_ON = "on";
        public static final String SCENE_MODE_ACTION = "action";
        public static final String SCENE_MODE_ASD = "asd";
        public static final String SCENE_MODE_AUTO = "auto";
        public static final String SCENE_MODE_BACKLIGHT = "backlight";
        public static final String SCENE_MODE_BARCODE = "barcode";
        public static final String SCENE_MODE_BEACH = "beach";
        public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
        public static final String SCENE_MODE_FIREWORKS = "fireworks";
        public static final String SCENE_MODE_FLOWERS = "flowers";
        public static final String SCENE_MODE_HDR = "hdr";
        public static final String SCENE_MODE_LANDSCAPE = "landscape";
        public static final String SCENE_MODE_NIGHT = "night";
        public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
        public static final String SCENE_MODE_PARTY = "party";
        public static final String SCENE_MODE_PORTRAIT = "portrait";
        public static final String SCENE_MODE_SNOW = "snow";
        public static final String SCENE_MODE_SPORTS = "sports";
        public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
        public static final String SCENE_MODE_SUNSET = "sunset";
        public static final String SCENE_MODE_THEATRE = "theatre";
        public static final String SELECTABLE_ZONE_AF_AUTO = "auto";
        public static final String SELECTABLE_ZONE_AF_CENTER_WEIGHTED = "center-weighted";
        public static final String SELECTABLE_ZONE_AF_FRAME_AVERAGE = "frame-average";
        public static final String SELECTABLE_ZONE_AF_SPOTMETERING = "spot-metering";
        public static final String SKIN_TONE_ENHANCEMENT_DISABLE = "disable";
        public static final String SKIN_TONE_ENHANCEMENT_ENABLE = "enable";
        private static final String SUPPORTED_VALUES_SUFFIX = "-values";
        public static final String TOUCH_AF_AEC_OFF = "touch-off";
        public static final String TOUCH_AF_AEC_ON = "touch-on";
        private static final String TRUE = "true";
        public static final String VIDEO_HFR_2X = "60";
        public static final String VIDEO_HFR_3X = "90";
        public static final String VIDEO_HFR_4X = "120";
        public static final String VIDEO_HFR_OFF = "off";
        public static final String VIDEO_ROTATION_0 = "0";
        public static final String VIDEO_ROTATION_180 = "180";
        public static final String VIDEO_ROTATION_270 = "270";
        public static final String VIDEO_ROTATION_90 = "90";
        public static final String WHITE_BALANCE_AUTO = "auto";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
        public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
        public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
        public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
        public static final String WHITE_BALANCE_MANUAL_CCT = "manual-cct";
        public static final String WHITE_BALANCE_SHADE = "shade";
        public static final String WHITE_BALANCE_TWILIGHT = "twilight";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
        public static final String ZSL_OFF = "off";
        public static final String ZSL_ON = "on";
        private final LinkedHashMap<String, String> mMap;

        /* synthetic */ Parameters(Camera this$0, Parameters -this1) {
            this();
        }

        private Parameters() {
            this.mMap = new LinkedHashMap(64);
        }

        public void copyFrom(Parameters other) {
            if (other == null) {
                throw new NullPointerException("other must not be null");
            }
            this.mMap.putAll(other.mMap);
        }

        private Camera getOuter() {
            return Camera.this;
        }

        public boolean same(Parameters other) {
            if (this == other) {
                return true;
            }
            return other != null ? this.mMap.equals(other.mMap) : false;
        }

        @Deprecated
        public void dump() {
            Log.e(Camera.TAG, "dump: size=" + this.mMap.size());
            for (String k : this.mMap.keySet()) {
                Log.e(Camera.TAG, "dump: " + k + "=" + ((String) this.mMap.get(k)));
            }
        }

        public String flatten() {
            StringBuilder flattened = new StringBuilder(128);
            for (String k : this.mMap.keySet()) {
                flattened.append(k);
                flattened.append("=");
                flattened.append((String) this.mMap.get(k));
                flattened.append(";");
            }
            flattened.deleteCharAt(flattened.length() - 1);
            return flattened.toString();
        }

        public void unflatten(String flattened) {
            this.mMap.clear();
            StringSplitter<String> splitter = new SimpleStringSplitter(';');
            splitter.setString(flattened);
            for (String kv : splitter) {
                int pos = kv.indexOf(61);
                if (pos != -1) {
                    this.mMap.put(kv.substring(0, pos), kv.substring(pos + 1));
                }
            }
        }

        public void remove(String key) {
            this.mMap.remove(key);
        }

        public void set(String key, String value) {
            if (key.indexOf(61) != -1 || key.indexOf(59) != -1 || key.indexOf(0) != -1) {
                Log.e(Camera.TAG, "Key \"" + key + "\" contains invalid character (= or ; or \\0)");
            } else if (value.indexOf(61) == -1 && value.indexOf(59) == -1 && value.indexOf(0) == -1) {
                put(key, value);
            } else {
                Log.e(Camera.TAG, "Value \"" + value + "\" contains invalid character (= or ; or \\0)");
            }
        }

        public void set(String key, int value) {
            put(key, Integer.toString(value));
        }

        private void put(String key, String value) {
            this.mMap.remove(key);
            this.mMap.put(key, value);
        }

        private void set(String key, List<Area> areas) {
            if (areas == null) {
                set(key, "(0,0,0,0,0)");
                return;
            }
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < areas.size(); i++) {
                Area area = (Area) areas.get(i);
                Rect rect = area.rect;
                buffer.append('(');
                buffer.append(rect.left);
                buffer.append(',');
                buffer.append(rect.top);
                buffer.append(',');
                buffer.append(rect.right);
                buffer.append(',');
                buffer.append(rect.bottom);
                buffer.append(',');
                buffer.append(area.weight);
                buffer.append(')');
                if (i != areas.size() - 1) {
                    buffer.append(',');
                }
            }
            set(key, buffer.toString());
        }

        public String get(String key) {
            return (String) this.mMap.get(key);
        }

        public int getInt(String key) {
            return Integer.parseInt((String) this.mMap.get(key));
        }

        public void setPreviewSize(int width, int height) {
            set(KEY_PREVIEW_SIZE, Integer.toString(width) + "x" + Integer.toString(height));
        }

        public Size getPreviewSize() {
            return strToSize(get(KEY_PREVIEW_SIZE));
        }

        public List<Size> getSupportedPreviewSizes() {
            return splitSize(get("preview-size-values"));
        }

        public List<Size> getSupportedVideoSizes() {
            return splitSize(get("video-size-values"));
        }

        public Size getPreferredPreviewSizeForVideo() {
            return strToSize(get(KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO));
        }

        public void setJpegThumbnailSize(int width, int height) {
            set(KEY_JPEG_THUMBNAIL_WIDTH, width);
            set(KEY_JPEG_THUMBNAIL_HEIGHT, height);
        }

        public Size getJpegThumbnailSize() {
            return new Size(getInt(KEY_JPEG_THUMBNAIL_WIDTH), getInt(KEY_JPEG_THUMBNAIL_HEIGHT));
        }

        public List<Size> getSupportedJpegThumbnailSizes() {
            return splitSize(get("jpeg-thumbnail-size-values"));
        }

        public void setJpegThumbnailQuality(int quality) {
            set(KEY_JPEG_THUMBNAIL_QUALITY, quality);
        }

        public int getJpegThumbnailQuality() {
            return getInt(KEY_JPEG_THUMBNAIL_QUALITY);
        }

        public void setJpegQuality(int quality) {
            set(KEY_JPEG_QUALITY, quality);
        }

        public int getJpegQuality() {
            return getInt(KEY_JPEG_QUALITY);
        }

        @Deprecated
        public void setPreviewFrameRate(int fps) {
            set(KEY_PREVIEW_FRAME_RATE, fps);
        }

        @Deprecated
        public int getPreviewFrameRate() {
            return getInt(KEY_PREVIEW_FRAME_RATE);
        }

        @Deprecated
        public List<Integer> getSupportedPreviewFrameRates() {
            return splitInt(get("preview-frame-rate-values"));
        }

        public void setPreviewFpsRange(int min, int max) {
            set(KEY_PREVIEW_FPS_RANGE, "" + min + "," + max);
        }

        public void getPreviewFpsRange(int[] range) {
            if (range == null || range.length != 2) {
                throw new IllegalArgumentException("range must be an array with two elements.");
            }
            splitInt(get(KEY_PREVIEW_FPS_RANGE), range);
        }

        public List<int[]> getSupportedPreviewFpsRange() {
            return splitRange(get("preview-fps-range-values"));
        }

        public void setPreviewFormat(int pixel_format) {
            String s = cameraFormatForPixelFormat(pixel_format);
            if (s == null) {
                throw new IllegalArgumentException("Invalid pixel_format=" + pixel_format);
            }
            set(KEY_PREVIEW_FORMAT, s);
        }

        public int getPreviewFormat() {
            return pixelFormatForCameraFormat(get(KEY_PREVIEW_FORMAT));
        }

        public List<Integer> getSupportedPreviewFormats() {
            String str = get("preview-format-values");
            ArrayList<Integer> formats = new ArrayList();
            for (String s : split(str)) {
                int f = pixelFormatForCameraFormat(s);
                if (f != 0) {
                    formats.add(Integer.valueOf(f));
                }
            }
            return formats;
        }

        public void setPictureSize(int width, int height) {
            set(KEY_PICTURE_SIZE, Integer.toString(width) + "x" + Integer.toString(height));
        }

        public Size getPictureSize() {
            return strToSize(get(KEY_PICTURE_SIZE));
        }

        public List<Size> getSupportedPictureSizes() {
            String str = get("picture-size-values");
            boolean exposeAllSize = false;
            if (!SystemProperties.get("persist.version.confidential", FALSE).equals(TRUE)) {
                return splitSize(str);
            }
            String packageName = ActivityThread.currentOpPackageName();
            String packageList = SystemProperties.get("vendor.camera.aux.packagelist");
            if (packageList.length() > 0) {
                StringSplitter<String> splitter = new SimpleStringSplitter(',');
                splitter.setString(packageList);
                for (String mstr : splitter) {
                    if (packageName.equals(mstr)) {
                        exposeAllSize = true;
                        break;
                    }
                }
            }
            if (exposeAllSize) {
                return splitSize(str);
            }
            ArrayList<Size> msizeList = splitSize(str);
            int i = 0;
            while (i < msizeList.size()) {
                if (((Size) msizeList.get(i)).width > GLES10.GL_AMBIENT || ((Size) msizeList.get(i)).height > 3456) {
                    msizeList.remove(i);
                }
                i++;
            }
            if (msizeList.size() == 0) {
                return null;
            }
            return msizeList;
        }

        public void setPictureFormat(int pixel_format) {
            String s = cameraFormatForPixelFormat(pixel_format);
            if (s == null) {
                throw new IllegalArgumentException("Invalid pixel_format=" + pixel_format);
            }
            set(KEY_PICTURE_FORMAT, s);
        }

        public int getPictureFormat() {
            return pixelFormatForCameraFormat(get(KEY_PICTURE_FORMAT));
        }

        public List<Integer> getSupportedPictureFormats() {
            String str = get("picture-format-values");
            ArrayList<Integer> formats = new ArrayList();
            for (String s : split(str)) {
                int f = pixelFormatForCameraFormat(s);
                if (f != 0) {
                    formats.add(Integer.valueOf(f));
                }
            }
            return formats;
        }

        private String cameraFormatForPixelFormat(int pixel_format) {
            switch (pixel_format) {
                case 4:
                    return PIXEL_FORMAT_RGB565;
                case 16:
                    return PIXEL_FORMAT_YUV422SP;
                case 17:
                    return PIXEL_FORMAT_YUV420SP;
                case 20:
                    return PIXEL_FORMAT_YUV422I;
                case 256:
                    return PIXEL_FORMAT_JPEG;
                case ImageFormat.YV12 /*842094169*/:
                    return PIXEL_FORMAT_YUV420P;
                default:
                    return null;
            }
        }

        private int pixelFormatForCameraFormat(String format) {
            if (format == null) {
                return 0;
            }
            if (format.equals(PIXEL_FORMAT_YUV422SP)) {
                return 16;
            }
            if (format.equals(PIXEL_FORMAT_YUV420SP)) {
                return 17;
            }
            if (format.equals(PIXEL_FORMAT_YUV422I)) {
                return 20;
            }
            if (format.equals(PIXEL_FORMAT_YUV420P)) {
                return ImageFormat.YV12;
            }
            if (format.equals(PIXEL_FORMAT_RGB565)) {
                return 4;
            }
            if (format.equals(PIXEL_FORMAT_JPEG)) {
                return 256;
            }
            return 0;
        }

        public void setRotation(int rotation) {
            if (rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270) {
                set(KEY_ROTATION, Integer.toString(rotation));
                return;
            }
            throw new IllegalArgumentException("Invalid rotation=" + rotation);
        }

        public void setGpsLatitude(double latitude) {
            set(KEY_GPS_LATITUDE, Double.toString(latitude));
        }

        public void setGpsLongitude(double longitude) {
            set(KEY_GPS_LONGITUDE, Double.toString(longitude));
        }

        public void setGpsAltitude(double altitude) {
            set(KEY_GPS_ALTITUDE, Double.toString(altitude));
        }

        public void setGpsTimestamp(long timestamp) {
            set(KEY_GPS_TIMESTAMP, Long.toString(timestamp));
        }

        public void setGpsProcessingMethod(String processing_method) {
            set(KEY_GPS_PROCESSING_METHOD, processing_method);
        }

        public void removeGpsData() {
            remove(KEY_QC_GPS_LATITUDE_REF);
            remove(KEY_GPS_LATITUDE);
            remove(KEY_QC_GPS_LONGITUDE_REF);
            remove(KEY_GPS_LONGITUDE);
            remove(KEY_QC_GPS_ALTITUDE_REF);
            remove(KEY_GPS_ALTITUDE);
            remove(KEY_GPS_TIMESTAMP);
            remove(KEY_GPS_PROCESSING_METHOD);
        }

        public String getWhiteBalance() {
            return get(KEY_WHITE_BALANCE);
        }

        public void setWhiteBalance(String value) {
            if (!same(value, get(KEY_WHITE_BALANCE))) {
                set(KEY_WHITE_BALANCE, value);
                set(KEY_AUTO_WHITEBALANCE_LOCK, FALSE);
            }
        }

        public List<String> getSupportedWhiteBalance() {
            return split(get("whitebalance-values"));
        }

        public String getColorEffect() {
            return get(KEY_EFFECT);
        }

        public void setColorEffect(String value) {
            set(KEY_EFFECT, value);
        }

        public List<String> getSupportedColorEffects() {
            return split(get("effect-values"));
        }

        public String getAntibanding() {
            return get(KEY_ANTIBANDING);
        }

        public void setAntibanding(String antibanding) {
            set(KEY_ANTIBANDING, antibanding);
        }

        public List<String> getSupportedAntibanding() {
            return split(get("antibanding-values"));
        }

        public String getSceneMode() {
            return get(KEY_SCENE_MODE);
        }

        public void setSceneMode(String value) {
            set(KEY_SCENE_MODE, value);
        }

        public List<String> getSupportedSceneModes() {
            return split(get("scene-mode-values"));
        }

        public String getFlashMode() {
            return get(KEY_FLASH_MODE);
        }

        public void setFlashMode(String value) {
            set(KEY_FLASH_MODE, value);
        }

        public List<String> getSupportedFlashModes() {
            return split(get("flash-mode-values"));
        }

        public String getFocusMode() {
            return get(KEY_FOCUS_MODE);
        }

        public void setFocusMode(String value) {
            set(KEY_FOCUS_MODE, value);
        }

        public List<String> getSupportedFocusModes() {
            return split(get("focus-mode-values"));
        }

        public float getFocalLength() {
            return Float.parseFloat(get(KEY_FOCAL_LENGTH));
        }

        public float getHorizontalViewAngle() {
            return Float.parseFloat(get(KEY_HORIZONTAL_VIEW_ANGLE));
        }

        public float getVerticalViewAngle() {
            return Float.parseFloat(get(KEY_VERTICAL_VIEW_ANGLE));
        }

        public int getExposureCompensation() {
            return getInt(KEY_EXPOSURE_COMPENSATION, 0);
        }

        public void setExposureCompensation(int value) {
            set(KEY_EXPOSURE_COMPENSATION, value);
        }

        public int getMaxExposureCompensation() {
            return getInt(KEY_MAX_EXPOSURE_COMPENSATION, 0);
        }

        public int getMinExposureCompensation() {
            return getInt(KEY_MIN_EXPOSURE_COMPENSATION, 0);
        }

        public float getExposureCompensationStep() {
            return getFloat(KEY_EXPOSURE_COMPENSATION_STEP, 0.0f);
        }

        public void setAutoExposureLock(boolean toggle) {
            set(KEY_AUTO_EXPOSURE_LOCK, toggle ? TRUE : FALSE);
        }

        public boolean getAutoExposureLock() {
            return TRUE.equals(get(KEY_AUTO_EXPOSURE_LOCK));
        }

        public boolean isAutoExposureLockSupported() {
            return TRUE.equals(get(KEY_AUTO_EXPOSURE_LOCK_SUPPORTED));
        }

        public void setAutoWhiteBalanceLock(boolean toggle) {
            set(KEY_AUTO_WHITEBALANCE_LOCK, toggle ? TRUE : FALSE);
        }

        public boolean getAutoWhiteBalanceLock() {
            return TRUE.equals(get(KEY_AUTO_WHITEBALANCE_LOCK));
        }

        public boolean isAutoWhiteBalanceLockSupported() {
            return TRUE.equals(get(KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED));
        }

        public int getZoom() {
            return getInt(KEY_ZOOM, 0);
        }

        public void setZoom(int value) {
            set(KEY_ZOOM, value);
        }

        public boolean isZoomSupported() {
            return TRUE.equals(get(KEY_ZOOM_SUPPORTED));
        }

        public int getMaxZoom() {
            return getInt(KEY_MAX_ZOOM, 0);
        }

        public List<Integer> getZoomRatios() {
            return splitInt(get(KEY_ZOOM_RATIOS));
        }

        public boolean isSmoothZoomSupported() {
            return TRUE.equals(get(KEY_SMOOTH_ZOOM_SUPPORTED));
        }

        public void getFocusDistances(float[] output) {
            if (output == null || output.length != 3) {
                throw new IllegalArgumentException("output must be a float array with three elements.");
            }
            splitFloat(get(KEY_FOCUS_DISTANCES), output);
        }

        public int getMaxNumFocusAreas() {
            return getInt(KEY_MAX_NUM_FOCUS_AREAS, 0);
        }

        public List<Area> getFocusAreas() {
            return splitArea(get(KEY_FOCUS_AREAS));
        }

        public void setFocusAreas(List<Area> focusAreas) {
            set(KEY_FOCUS_AREAS, (List) focusAreas);
        }

        public int getMaxNumMeteringAreas() {
            return getInt(KEY_MAX_NUM_METERING_AREAS, 0);
        }

        public List<Area> getMeteringAreas() {
            return splitArea(get(KEY_METERING_AREAS));
        }

        public void setMeteringAreas(List<Area> meteringAreas) {
            set(KEY_METERING_AREAS, (List) meteringAreas);
        }

        public int getMaxNumDetectedFaces() {
            return getInt(KEY_MAX_NUM_DETECTED_FACES_HW, 0);
        }

        public void setRecordingHint(boolean hint) {
            set(KEY_RECORDING_HINT, hint ? TRUE : FALSE);
        }

        public boolean isVideoSnapshotSupported() {
            return TRUE.equals(get(KEY_VIDEO_SNAPSHOT_SUPPORTED));
        }

        public void setVideoStabilization(boolean toggle) {
            set(KEY_VIDEO_STABILIZATION, toggle ? TRUE : FALSE);
        }

        public boolean getVideoStabilization() {
            return TRUE.equals(get(KEY_VIDEO_STABILIZATION));
        }

        public boolean isVideoStabilizationSupported() {
            return TRUE.equals(get(KEY_VIDEO_STABILIZATION_SUPPORTED));
        }

        private ArrayList<String> split(String str) {
            if (str == null) {
                return null;
            }
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<String> substrings = new ArrayList();
            for (String s : splitter) {
                substrings.add(s);
            }
            return substrings;
        }

        private ArrayList<Integer> splitInt(String str) {
            if (str == null) {
                return null;
            }
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<Integer> substrings = new ArrayList();
            for (String s : splitter) {
                substrings.add(Integer.valueOf(Integer.parseInt(s)));
            }
            if (substrings.size() == 0) {
                return null;
            }
            return substrings;
        }

        private void splitInt(String str, int[] output) {
            if (str != null) {
                StringSplitter<String> splitter = new SimpleStringSplitter(',');
                splitter.setString(str);
                int index = 0;
                for (String s : splitter) {
                    int index2 = index + 1;
                    output[index] = Integer.parseInt(s);
                    index = index2;
                }
            }
        }

        private void splitFloat(String str, float[] output) {
            if (str != null) {
                StringSplitter<String> splitter = new SimpleStringSplitter(',');
                splitter.setString(str);
                int index = 0;
                for (String s : splitter) {
                    int index2 = index + 1;
                    output[index] = Float.parseFloat(s);
                    index = index2;
                }
            }
        }

        private float getFloat(String key, float defaultValue) {
            try {
                return Float.parseFloat((String) this.mMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt((String) this.mMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private ArrayList<Size> splitSize(String str) {
            if (str == null) {
                return null;
            }
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<Size> sizeList = new ArrayList();
            for (String s : splitter) {
                Size size = strToSize(s);
                if (size != null) {
                    sizeList.add(size);
                }
            }
            if (sizeList.size() == 0) {
                return null;
            }
            return sizeList;
        }

        private Size strToSize(String str) {
            if (str == null) {
                return null;
            }
            int pos = str.indexOf(120);
            if (pos != -1) {
                return new Size(Integer.parseInt(str.substring(0, pos)), Integer.parseInt(str.substring(pos + 1)));
            }
            Log.e(Camera.TAG, "Invalid size parameter string=" + str);
            return null;
        }

        private ArrayList<int[]> splitRange(String str) {
            if (str != null && str.charAt(0) == '(' && str.charAt(str.length() - 1) == ')') {
                ArrayList<int[]> rangeList = new ArrayList();
                int fromIndex = 1;
                int endIndex;
                do {
                    int[] range = new int[2];
                    endIndex = str.indexOf("),(", fromIndex);
                    if (endIndex == -1) {
                        endIndex = str.length() - 1;
                    }
                    splitInt(str.substring(fromIndex, endIndex), range);
                    rangeList.add(range);
                    fromIndex = endIndex + 3;
                } while (endIndex != str.length() - 1);
                if (rangeList.size() == 0) {
                    return null;
                }
                return rangeList;
            }
            Log.e(Camera.TAG, "Invalid range list string=" + str);
            return null;
        }

        private ArrayList<Area> splitArea(String str) {
            if (str != null && str.charAt(0) == '(' && str.charAt(str.length() - 1) == ')') {
                ArrayList<Area> result = new ArrayList();
                int fromIndex = 1;
                int[] array = new int[5];
                int endIndex;
                do {
                    endIndex = str.indexOf("),(", fromIndex);
                    if (endIndex == -1) {
                        endIndex = str.length() - 1;
                    }
                    splitInt(str.substring(fromIndex, endIndex), array);
                    result.add(new Area(new Rect(array[0], array[1], array[2], array[3]), array[4]));
                    fromIndex = endIndex + 3;
                } while (endIndex != str.length() - 1);
                if (result.size() == 0) {
                    return null;
                }
                if (result.size() == 1) {
                    Area area = (Area) result.get(0);
                    Rect rect = area.rect;
                    if (rect.left == 0 && rect.top == 0 && rect.right == 0 && rect.bottom == 0 && area.weight == 0) {
                        return null;
                    }
                    return result;
                }
                return result;
            }
            Log.e(Camera.TAG, "Invalid area string=" + str);
            return null;
        }

        private boolean same(String s1, String s2) {
            if (s1 == null && s2 == null) {
                return true;
            }
            if (s1 == null || !s1.equals(s2)) {
                return false;
            }
            return true;
        }

        public List<Size> getSupportedHfrSizes() {
            return splitSize(get("hfr-size-values"));
        }

        public List<String> getSupportedTouchAfAec() {
            return split(get("touch-af-aec-values"));
        }

        public List<String> getSupportedPreviewFrameRateModes() {
            return split(get("preview-frame-rate-mode-values"));
        }

        public List<String> getSupportedSceneDetectModes() {
            return split(get("scene-detect-values"));
        }

        public List<String> getSupportedIsoValues() {
            return split(get("iso-values"));
        }

        public List<String> getSupportedLensShadeModes() {
            return split(get("lensshade-values"));
        }

        public List<String> getSupportedHistogramModes() {
            return split(get("histogram-values"));
        }

        public List<String> getSupportedSkinToneEnhancementModes() {
            return split(get("skinToneEnhancement-values"));
        }

        public List<String> getSupportedAutoexposure() {
            return split(get("auto-exposure-values"));
        }

        public List<String> getSupportedMemColorEnhanceModes() {
            return split(get("mce-values"));
        }

        public List<String> getSupportedZSLModes() {
            return split(get("zsl-values"));
        }

        public List<String> getSupportedVideoHDRModes() {
            return split(get("video-hdr-values"));
        }

        public List<String> getSupportedVideoHighFrameRateModes() {
            return split(get("video-hfr-values"));
        }

        public List<String> getSupportedContinuousAfModes() {
            return split(get("continuous-af-values"));
        }

        public List<String> getSupportedDenoiseModes() {
            return split(get("denoise-values"));
        }

        public List<String> getSupportedSelectableZoneAf() {
            return split(get("selectable-zone-af-values"));
        }

        public List<String> getSupportedFaceDetectionModes() {
            return split(get("face-detection-values"));
        }

        public List<String> getSupportedRedeyeReductionModes() {
            return split(get("redeye-reduction-values"));
        }

        public void setGpsAltitudeRef(double altRef) {
            set(KEY_QC_GPS_ALTITUDE_REF, Double.toString(altRef));
        }

        public void setGpsStatus(double status) {
            set(KEY_QC_GPS_STATUS, Double.toString(status));
        }

        public void setTouchIndexAec(int x, int y) {
            set(KEY_QC_TOUCH_INDEX_AEC, Integer.toString(x) + "x" + Integer.toString(y));
        }

        public Coordinate getTouchIndexAec() {
            return strToCoordinate(get(KEY_QC_TOUCH_INDEX_AEC));
        }

        public void setTouchIndexAf(int x, int y) {
            set(KEY_QC_TOUCH_INDEX_AF, Integer.toString(x) + "x" + Integer.toString(y));
        }

        public Coordinate getTouchIndexAf() {
            return strToCoordinate(get(KEY_QC_TOUCH_INDEX_AF));
        }

        public void setSharpness(int sharpness) {
            if (sharpness < 0 || sharpness > getMaxSharpness()) {
                throw new IllegalArgumentException("Invalid Sharpness " + sharpness);
            }
            set(KEY_QC_SHARPNESS, String.valueOf(sharpness));
        }

        public void setContrast(int contrast) {
            if (contrast < 0 || contrast > getMaxContrast()) {
                throw new IllegalArgumentException("Invalid Contrast " + contrast);
            }
            set(KEY_QC_CONTRAST, String.valueOf(contrast));
        }

        public void setSaturation(int saturation) {
            if (saturation < 0 || saturation > getMaxSaturation()) {
                throw new IllegalArgumentException("Invalid Saturation " + saturation);
            }
            set(KEY_QC_SATURATION, String.valueOf(saturation));
        }

        public boolean isPowerModeSupported() {
            return TRUE.equals(get(KEY_QC_POWER_MODE_SUPPORTED));
        }

        public int getSharpness() {
            return getInt(KEY_QC_SHARPNESS);
        }

        public int getMaxSharpness() {
            return getInt(KEY_QC_MAX_SHARPNESS);
        }

        public int getContrast() {
            return getInt(KEY_QC_CONTRAST);
        }

        public int getMaxContrast() {
            return getInt(KEY_QC_MAX_CONTRAST);
        }

        public int getSaturation() {
            return getInt(KEY_QC_SATURATION);
        }

        public int getMaxSaturation() {
            return getInt(KEY_QC_MAX_SATURATION);
        }

        public void setGpsLatitudeRef(String latRef) {
            set(KEY_QC_GPS_LATITUDE_REF, latRef);
        }

        public void setGpsLongitudeRef(String lonRef) {
            set(KEY_QC_GPS_LONGITUDE_REF, lonRef);
        }

        public void setExifDateTime(String dateTime) {
            set(KEY_QC_EXIF_DATETIME, dateTime);
        }

        public String getTouchAfAec() {
            return get(KEY_QC_TOUCH_AF_AEC);
        }

        public void setTouchAfAec(String value) {
            set(KEY_QC_TOUCH_AF_AEC, value);
        }

        public String getRedeyeReductionMode() {
            return get(KEY_QC_REDEYE_REDUCTION);
        }

        public void setRedeyeReductionMode(String value) {
            set(KEY_QC_REDEYE_REDUCTION, value);
        }

        public String getPreviewFrameRateMode() {
            return get(KEY_QC_PREVIEW_FRAME_RATE_MODE);
        }

        public void setPreviewFrameRateMode(String value) {
            set(KEY_QC_PREVIEW_FRAME_RATE_MODE, value);
        }

        public String getSceneDetectMode() {
            return get(KEY_QC_SCENE_DETECT);
        }

        public void setSceneDetectMode(String value) {
            set(KEY_QC_SCENE_DETECT, value);
        }

        public String getAEBracket() {
            return get(KEY_QC_AE_BRACKET_HDR);
        }

        public void setPowerMode(String value) {
            set(KEY_QC_POWER_MODE, value);
        }

        public String getPowerMode() {
            return get(KEY_QC_POWER_MODE);
        }

        public int getLPMMode() {
            return getInt(DUAL_CAM_LPM_MODE, 0);
        }

        public void setAEBracket(String value) {
            set(KEY_QC_AE_BRACKET_HDR, value);
        }

        public String getISOValue() {
            return get(KEY_QC_ISO_MODE);
        }

        public void setISOValue(String iso) {
            set(KEY_QC_ISO_MODE, iso);
        }

        public void setExposureTime(int value) {
            set(KEY_QC_EXPOSURE_TIME, Integer.toString(value));
        }

        public String getExposureTime() {
            return get(KEY_QC_EXPOSURE_TIME);
        }

        public String getMinExposureTime() {
            return get(KEY_QC_MIN_EXPOSURE_TIME);
        }

        public String getMaxExposureTime() {
            return get(KEY_QC_MAX_EXPOSURE_TIME);
        }

        public String getLensShade() {
            return get(KEY_QC_LENSSHADE);
        }

        public void setLensShade(String lensshade) {
            set(KEY_QC_LENSSHADE, lensshade);
        }

        public String getAutoExposure() {
            return get(KEY_QC_AUTO_EXPOSURE);
        }

        public void setAutoExposure(String value) {
            set(KEY_QC_AUTO_EXPOSURE, value);
        }

        public String getMemColorEnhance() {
            return get(KEY_QC_MEMORY_COLOR_ENHANCEMENT);
        }

        public void setMemColorEnhance(String mce) {
            set(KEY_QC_MEMORY_COLOR_ENHANCEMENT, mce);
        }

        public void setWBManualCCT(int cct) {
            set(KEY_QC_WB_MANUAL_CCT, Integer.toString(cct));
        }

        public String getWBMinCCT() {
            return get(KEY_QC_MIN_WB_CCT);
        }

        public String getMaxWBCCT() {
            return get(KEY_QC_MAX_WB_CCT);
        }

        public String getWBCurrentCCT() {
            return get(KEY_QC_WB_MANUAL_CCT);
        }

        public String getZSLMode() {
            return get(KEY_QC_ZSL);
        }

        public void setZSLMode(String zsl) {
            set(KEY_QC_ZSL, zsl);
        }

        public void setAutoHDRMode(String auto_hdr) {
            set(KEY_QC_AUTO_HDR_ENABLE, auto_hdr);
        }

        public String getCameraMode() {
            return get(KEY_QC_CAMERA_MODE);
        }

        public void setCameraMode(int cameraMode) {
            set(KEY_QC_CAMERA_MODE, cameraMode);
        }

        public void setFocusPosition(int type, int pos) {
            set(KEY_QC_MANUAL_FOCUS_POS_TYPE, Integer.toString(type));
            set(KEY_QC_MANUAL_FOCUS_POSITION, Integer.toString(pos));
        }

        public String getCurrentFocusPosition() {
            return get(KEY_QC_MANUAL_FOCUS_POSITION);
        }

        public String getVideoHighFrameRate() {
            return get(KEY_QC_VIDEO_HIGH_FRAME_RATE);
        }

        public void setVideoHighFrameRate(String hfr) {
            set(KEY_QC_VIDEO_HIGH_FRAME_RATE, hfr);
        }

        public String getVideoHDRMode() {
            return get(KEY_QC_VIDEO_HDR);
        }

        public void setVideoHDRMode(String videohdr) {
            set(KEY_QC_VIDEO_HDR, videohdr);
        }

        public String getDenoise() {
            return get(KEY_QC_DENOISE);
        }

        public String getContinuousAf() {
            return get(KEY_QC_CONTINUOUS_AF);
        }

        public void setDenoise(String value) {
            set(KEY_QC_DENOISE, value);
        }

        public void setContinuousAf(String value) {
            set(KEY_QC_CONTINUOUS_AF, value);
        }

        public String getSelectableZoneAf() {
            return get(KEY_QC_SELECTABLE_ZONE_AF);
        }

        public void setSelectableZoneAf(String value) {
            set(KEY_QC_SELECTABLE_ZONE_AF, value);
        }

        public String getFaceDetectionMode() {
            return get(KEY_QC_FACE_DETECTION);
        }

        public void setFaceDetectionMode(String value) {
            set(KEY_QC_FACE_DETECTION, value);
        }

        public String getVideoRotation() {
            return get(KEY_QC_VIDEO_ROTATION);
        }

        public void setVideoRotation(String value) {
            set(KEY_QC_VIDEO_ROTATION, value);
        }

        public List<String> getSupportedVideoRotationValues() {
            return split(get("video-rotation-values"));
        }

        private ArrayList<Coordinate> splitCoordinate(String str) {
            if (str == null) {
                return null;
            }
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<Coordinate> coordinateList = new ArrayList();
            for (String s : splitter) {
                Coordinate coordinate = strToCoordinate(s);
                if (coordinate != null) {
                    coordinateList.add(coordinate);
                }
            }
            if (coordinateList.size() == 0) {
                return null;
            }
            return coordinateList;
        }

        private Coordinate strToCoordinate(String str) {
            if (str == null) {
                return null;
            }
            int pos = str.indexOf(120);
            if (pos != -1) {
                return new Coordinate(Integer.parseInt(str.substring(0, pos)), Integer.parseInt(str.substring(pos + 1)));
            }
            Log.e(Camera.TAG, "Invalid Coordinate parameter string=" + str);
            return null;
        }
    }

    @Deprecated
    public interface PictureCallback {
        void onPictureTaken(byte[] bArr, Camera camera);
    }

    @Deprecated
    public interface PreviewCallback {
        void onPreviewFrame(byte[] bArr, Camera camera);
    }

    @Deprecated
    public interface ShutterCallback {
        void onShutter();
    }

    @Deprecated
    public class Size {
        public int height;
        public int width;

        public Size(int w, int h) {
            this.width = w;
            this.height = h;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            if (this.width == s.width && this.height == s.height) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (this.width * 32713) + this.height;
        }
    }

    private final native void _addCallbackBuffer(byte[] bArr, int i);

    private final native boolean _enableShutterSound(boolean z);

    private static native void _getCameraInfo(int i, CameraInfo cameraInfo);

    public static native int _getNumberOfCameras();

    private final native void _startFaceDetection(int i);

    private final native void _stopFaceDetection();

    private final native void _stopPreview();

    private native void enableFocusMoveCallback(int i);

    private static native int native_CameraHardwareCheck(int i);

    private final native void native_autoFocus();

    private final native void native_cancelAutoFocus();

    private final native String native_getParameters();

    private final native void native_release();

    private final native void native_sendHistogramData();

    private final native void native_sendMetaData();

    private final native void native_setHistogramMode(boolean z);

    private final native void native_setLongshot(boolean z);

    private final native void native_setMetadataCb(boolean z);

    private final native void native_setParameters(String str);

    private final native int native_setup(Object obj, int i, int i2, String str);

    private final native void native_takePicture(int i);

    private final native void setHasPreviewCallback(boolean z, boolean z2);

    private final native void setPreviewCallbackSurface(Surface surface);

    public final native void lock();

    public final native boolean previewEnabled();

    public final native void reconnect() throws IOException;

    public final native void setDisplayOrientation(int i);

    public final native void setPreviewSurface(Surface surface) throws IOException;

    public final native void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException;

    public final native void startPreview();

    public final native void startSmoothZoom(int i);

    public final native void stopSmoothZoom();

    public final native void unlock();

    public static int getNumberOfCameras() {
        boolean exposeAuxCamera = false;
        String packageName = ActivityThread.currentOpPackageName();
        String packageList = SystemProperties.get("vendor.camera.aux.packagelist");
        if (packageList.length() > 0) {
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(packageList);
            for (String str : splitter) {
                if (packageName.equals(str)) {
                    exposeAuxCamera = true;
                    break;
                }
            }
        }
        int numberOfCameras = _getNumberOfCameras();
        if (exposeAuxCamera || numberOfCameras <= 2) {
            return numberOfCameras;
        }
        return 2;
    }

    public static void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        if (cameraId >= getNumberOfCameras()) {
            throw new RuntimeException("Unknown camera ID");
        }
        _getCameraInfo(cameraId, cameraInfo);
        try {
            if (Stub.asInterface(ServiceManager.getService("audio")).isCameraSoundForced()) {
                cameraInfo.canDisableShutterSound = false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Audio service is unavailable for queries");
        }
    }

    public static Camera open(int cameraId) {
        return new Camera(cameraId);
    }

    public static Camera open() {
        int numberOfCameras = getNumberOfCameras();
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == 0) {
                return new Camera(i);
            }
        }
        return null;
    }

    public static Camera openLegacy(int cameraId, int halVersion) {
        if (halVersion >= 256) {
            return new Camera(cameraId, halVersion);
        }
        throw new IllegalArgumentException("Invalid HAL version " + halVersion);
    }

    private Camera(int cameraId, int halVersion) {
        int err = cameraInitVersion(cameraId, halVersion);
        if (!checkInitErrors(err)) {
            return;
        }
        if (err == (-OsConstants.EACCES)) {
            throw new RuntimeException("Fail to connect to camera service");
        } else if (err == 19) {
            throw new RuntimeException("Camera initialization failed");
        } else if (err == 38) {
            throw new RuntimeException("Camera initialization failed because some methods are not implemented");
        } else if (err == 95) {
            throw new RuntimeException("Camera initialization failed because the hal version is not supported by this device");
        } else if (err == 22) {
            throw new RuntimeException("Camera initialization failed because the input arugments are invalid");
        } else if (err == 16) {
            throw new RuntimeException("Camera initialization failed because the camera device was already opened");
        } else if (err == 87) {
            throw new RuntimeException("Camera initialization failed because the max number of camera devices were already opened");
        } else {
            throw new RuntimeException("Unknown camera error");
        }
    }

    private int cameraInitVersion(int cameraId, int halVersion) {
        this.mShutterCallback = null;
        this.mRawImageCallback = null;
        this.mJpegCallback = null;
        this.mPreviewCallback = null;
        this.mPostviewCallback = null;
        this.mUsingPreviewAllocation = false;
        this.mZoomListener = null;
        this.mCameraDataCallback = null;
        this.mCameraMetaDataCallback = null;
        this.mCameraId = cameraId;
        if ("off".equals(SystemProperties.get("oppo.camera.hypnus.contrl", "on")) || "android".equals(ActivityThread.currentOpPackageName())) {
            this.mHypnusCtrl = false;
        } else {
            this.mHypnusCtrl = true;
        }
        Log.d(TAG, "mHypnusCtrl is " + this.mHypnusCtrl);
        if (sHM == null) {
            sHM = new HypnusManager();
        }
        if (sHM != null && this.mHypnusCtrl) {
            sHM.hypnusSetAction(15, CAMERA_TIME_HYPNUS_2500);
        }
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
        String packageName = ActivityThread.currentOpPackageName();
        String packageList = SystemProperties.get("camera.hal1.packagelist", "");
        if (packageList.length() > 0) {
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(packageList);
            for (String str : splitter) {
                if (packageName.equals(str)) {
                    halVersion = 256;
                    break;
                }
            }
        }
        startVideoCallEffectFloatService(packageName, EXTRA_CAMERA_STATUS_OPEN);
        HashMap<String, String> eventMap = new HashMap();
        eventMap.put("pkgName", packageName);
        eventMap.put(EXTRA_CAMERA_ID, String.valueOf(this.mCameraId));
        OppoStatistics.onCommon(ActivityThread.currentApplication().getApplicationContext(), "2012002", "openCamera", eventMap, false);
        return native_setup(new WeakReference(this), cameraId, halVersion, packageName);
    }

    private int cameraInitNormal(int cameraId) {
        return cameraInitVersion(cameraId, -2);
    }

    public int cameraInitUnspecified(int cameraId) {
        return cameraInitVersion(cameraId, -1);
    }

    Camera(int cameraId) {
        if (cameraId >= getNumberOfCameras()) {
            throw new RuntimeException("Unknown camera ID");
        }
        int err = cameraInitNormal(cameraId);
        if (!checkInitErrors(err)) {
            return;
        }
        if (err == (-OsConstants.EACCES)) {
            throw new RuntimeException("Fail to connect to camera service");
        } else if (err == 19) {
            throw new RuntimeException("Camera initialization failed");
        } else {
            throw new RuntimeException("Unknown camera error");
        }
    }

    public static boolean checkInitErrors(int err) {
        return err != 0;
    }

    public static Camera openUninitialized() {
        return new Camera();
    }

    Camera() {
    }

    protected void finalize() {
        release();
    }

    public final void release() {
        if (sHM == null) {
            sHM = new HypnusManager();
        }
        if (sHM != null && this.mHypnusCtrl) {
            sHM.hypnusSetAction(15, CAMERA_TIME_HYPNUS_1500);
        }
        native_release();
        this.mFaceDetectionRunning = false;
        startVideoCallEffectFloatService(ActivityThread.currentOpPackageName(), EXTRA_CAMERA_STATUS_RELEASE);
    }

    public final void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        if (holder != null) {
            setPreviewSurface(holder.getSurface());
        } else {
            setPreviewSurface((Surface) null);
        }
    }

    public final void stopPreview() {
        if (sHM == null) {
            sHM = new HypnusManager();
        }
        if (sHM != null && this.mHypnusCtrl) {
            sHM.hypnusSetAction(15, CAMERA_TIME_HYPNUS_1500);
        }
        _stopPreview();
        this.mFaceDetectionRunning = false;
        this.mShutterCallback = null;
        this.mRawImageCallback = null;
        this.mPostviewCallback = null;
        this.mJpegCallback = null;
        synchronized (this.mAutoFocusCallbackLock) {
            this.mAutoFocusCallback = null;
        }
        this.mAutoFocusMoveCallback = null;
    }

    public final void setPreviewCallback(PreviewCallback cb) {
        boolean z;
        SeempLog.record(66);
        this.mPreviewCallback = cb;
        this.mOneShot = false;
        this.mWithBuffer = false;
        if (cb != null) {
            this.mUsingPreviewAllocation = false;
        }
        if (cb != null) {
            z = true;
        } else {
            z = false;
        }
        setHasPreviewCallback(z, false);
    }

    public final void setOneShotPreviewCallback(PreviewCallback cb) {
        boolean z = true;
        SeempLog.record(68);
        this.mPreviewCallback = cb;
        this.mOneShot = true;
        this.mWithBuffer = false;
        if (cb != null) {
            this.mUsingPreviewAllocation = false;
        }
        if (cb == null) {
            z = false;
        }
        setHasPreviewCallback(z, false);
    }

    public final void setPreviewCallbackWithBuffer(PreviewCallback cb) {
        boolean z = false;
        SeempLog.record(67);
        this.mPreviewCallback = cb;
        this.mOneShot = false;
        this.mWithBuffer = true;
        if (cb != null) {
            this.mUsingPreviewAllocation = false;
        }
        if (cb != null) {
            z = true;
        }
        setHasPreviewCallback(z, true);
    }

    public final void addCallbackBuffer(byte[] callbackBuffer) {
        _addCallbackBuffer(callbackBuffer, 16);
    }

    public final void addRawImageCallbackBuffer(byte[] callbackBuffer) {
        addCallbackBuffer(callbackBuffer, 128);
    }

    private final void addCallbackBuffer(byte[] callbackBuffer, int msgType) {
        if (msgType == 16 || msgType == 128) {
            _addCallbackBuffer(callbackBuffer, msgType);
            return;
        }
        throw new IllegalArgumentException("Unsupported message type: " + msgType);
    }

    public final Allocation createPreviewAllocation(RenderScript rs, int usage) throws RSIllegalArgumentException {
        Size previewSize = getParameters().getPreviewSize();
        Builder yuvBuilder = new Builder(rs, Element.createPixel(rs, DataType.UNSIGNED_8, DataKind.PIXEL_YUV));
        yuvBuilder.setYuvFormat(ImageFormat.YV12);
        yuvBuilder.setX(previewSize.width);
        yuvBuilder.setY(previewSize.height);
        return Allocation.createTyped(rs, yuvBuilder.create(), usage | 32);
    }

    public final void setPreviewCallbackAllocation(Allocation previewAllocation) throws IOException {
        Surface previewSurface = null;
        if (previewAllocation != null) {
            Size previewSize = getParameters().getPreviewSize();
            if (previewSize.width != previewAllocation.getType().getX() || previewSize.height != previewAllocation.getType().getY()) {
                throw new IllegalArgumentException("Allocation dimensions don't match preview dimensions: Allocation is " + previewAllocation.getType().getX() + ", " + previewAllocation.getType().getY() + ". Preview is " + previewSize.width + ", " + previewSize.height);
            } else if ((previewAllocation.getUsage() & 32) == 0) {
                throw new IllegalArgumentException("Allocation usage does not include USAGE_IO_INPUT");
            } else if (previewAllocation.getType().getElement().getDataKind() != DataKind.PIXEL_YUV) {
                throw new IllegalArgumentException("Allocation is not of a YUV type");
            } else {
                previewSurface = previewAllocation.getSurface();
                this.mUsingPreviewAllocation = true;
            }
        } else {
            this.mUsingPreviewAllocation = false;
        }
        setPreviewCallbackSurface(previewSurface);
    }

    private static void postEventFromNative(Object camera_ref, int what, int arg1, int arg2, Object obj) {
        Camera c = (Camera) ((WeakReference) camera_ref).get();
        if (!(c == null || c.mEventHandler == null)) {
            c.mEventHandler.sendMessage(c.mEventHandler.obtainMessage(what, arg1, arg2, obj));
        }
    }

    public final void autoFocus(AutoFocusCallback cb) {
        synchronized (this.mAutoFocusCallbackLock) {
            this.mAutoFocusCallback = cb;
        }
        native_autoFocus();
    }

    public final void cancelAutoFocus() {
        synchronized (this.mAutoFocusCallbackLock) {
            this.mAutoFocusCallback = null;
        }
        native_cancelAutoFocus();
        this.mEventHandler.removeMessages(4);
    }

    public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
        this.mAutoFocusMoveCallback = cb;
        enableFocusMoveCallback(this.mAutoFocusMoveCallback != null ? 1 : 0);
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg) {
        SeempLog.record(65);
        takePicture(shutter, raw, null, jpeg);
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg) {
        SeempLog.record(65);
        this.mShutterCallback = shutter;
        this.mRawImageCallback = raw;
        this.mPostviewCallback = postview;
        this.mJpegCallback = jpeg;
        int msgType = 0;
        if (this.mShutterCallback != null) {
            msgType = 2;
        }
        if (this.mRawImageCallback != null) {
            msgType |= 128;
        }
        if (this.mPostviewCallback != null) {
            msgType |= 64;
        }
        if (this.mJpegCallback != null) {
            msgType |= 256;
        }
        if (sHM == null) {
            sHM = new HypnusManager();
        }
        if (!(sHM == null || !this.mHypnusCtrl || (this.mLongshotEnable ^ 1) == 0)) {
            sHM.hypnusSetAction(15, 4000);
        }
        native_takePicture(msgType);
        this.mFaceDetectionRunning = false;
        String pkgName = ActivityThread.currentPackageName();
        HashMap<String, String> eventMap = new HashMap();
        eventMap.put("pkgName", pkgName);
        eventMap.put(EXTRA_CAMERA_ID, String.valueOf(this.mCameraId));
        OppoStatistics.onCommon(ActivityThread.currentApplication().getApplicationContext(), "2012002", "capture", eventMap, false);
    }

    public final boolean enableShutterSound(boolean enabled) {
        if (!enabled) {
            try {
                if (Stub.asInterface(ServiceManager.getService("audio")).isCameraSoundForced()) {
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Audio service is unavailable for queries");
            }
        }
        return _enableShutterSound(enabled);
    }

    public final boolean disableShutterSound() {
        return _enableShutterSound(false);
    }

    public final void setZoomChangeListener(OnZoomChangeListener listener) {
        this.mZoomListener = listener;
    }

    public final void setFaceDetectionListener(FaceDetectionListener listener) {
        this.mFaceListener = listener;
    }

    public final void startFaceDetection() {
        if (this.mFaceDetectionRunning) {
            throw new RuntimeException("Face detection is already running");
        }
        _startFaceDetection(0);
        this.mFaceDetectionRunning = true;
    }

    public final void stopFaceDetection() {
        _stopFaceDetection();
        this.mFaceDetectionRunning = false;
    }

    public final void setErrorCallback(ErrorCallback cb) {
        this.mErrorCallback = cb;
    }

    public void setParameters(Parameters params) {
        if (this.mUsingPreviewAllocation) {
            Size newPreviewSize = params.getPreviewSize();
            Size currentPreviewSize = getParameters().getPreviewSize();
            if (!(newPreviewSize.width == currentPreviewSize.width && newPreviewSize.height == currentPreviewSize.height)) {
                throw new IllegalStateException("Cannot change preview size while a preview allocation is configured.");
            }
        }
        if (params.getLPMMode() > 0) {
            if (sHM == null) {
                sHM = new HypnusManager();
            }
            if (sHM != null && this.mHypnusCtrl) {
                sHM.hypnusSetAction(15, 1000);
            }
            Log.d(TAG, "set LPM MODE 1,open Hypnus ");
        }
        native_setParameters(params.flatten());
    }

    public Parameters getParameters() {
        Parameters p = new Parameters();
        p.unflatten(native_getParameters());
        return p;
    }

    public int getWBCurrentCCT() {
        Parameters p = new Parameters();
        p.unflatten(native_getParameters());
        if (p.getWBCurrentCCT() != null) {
            return Integer.parseInt(p.getWBCurrentCCT());
        }
        return 0;
    }

    public static Parameters getEmptyParameters() {
        Camera camera = new Camera();
        camera.getClass();
        return new Parameters(camera, null);
    }

    private static int byteToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value += (b[(3 - i) + offset] & 255) << ((3 - i) * 8);
        }
        return value;
    }

    public final void setHistogramMode(CameraDataCallback cb) {
        this.mCameraDataCallback = cb;
        native_setHistogramMode(cb != null);
    }

    public final void sendHistogramData() {
        native_sendHistogramData();
    }

    public final void setMetadataCb(CameraMetaDataCallback cb) {
        this.mCameraMetaDataCallback = cb;
        native_setMetadataCb(cb != null);
    }

    public final void sendMetaData() {
        native_sendMetaData();
    }

    public static int cameraHardwareCheck(int cameraId) {
        return native_CameraHardwareCheck(cameraId);
    }

    public final void setLongshot(boolean enable) {
        if (sHM == null) {
            sHM = new HypnusManager();
        }
        if (sHM != null && this.mHypnusCtrl) {
            if (enable) {
                this.mLongshotEnable = true;
                sHM.hypnusSetAction(15, 4000);
            } else {
                this.mLongshotEnable = false;
                sHM.hypnusSetAction(15, 0);
            }
        }
        native_setLongshot(enable);
    }

    private void startVideoCallEffectFloatService(String packageName, String status) {
        if (!packageName.equals(CAMERA_APP_PACKAGE)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(VIDEO_CALL_FLOAT_PACKAGE, "com.oppo.videocallfloat.VideoFloatWindowService"));
            intent.setAction("com.oppo.videocallfloat.VideoFloatWindowService");
            intent.putExtra("pkgName", packageName);
            intent.putExtra(EXTRA_CAMERA_ID, this.mCameraId);
            intent.putExtra("status", status);
            ActivityThread.currentApplication().getApplicationContext().startService(intent);
        }
    }

    public int getCurrentFocusPosition() {
        Parameters p = new Parameters();
        p.unflatten(native_getParameters());
        if (p.getCurrentFocusPosition() != null) {
            return Integer.parseInt(p.getCurrentFocusPosition());
        }
        return -1;
    }

    public static Parameters getParametersCopy(Parameters parameters) {
        if (parameters == null) {
            throw new NullPointerException("parameters must not be null");
        }
        Camera camera = parameters.getOuter();
        camera.getClass();
        Parameters p = new Parameters(camera, null);
        p.copyFrom(parameters);
        return p;
    }
}
