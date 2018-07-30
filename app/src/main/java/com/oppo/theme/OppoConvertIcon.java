package com.oppo.theme;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import java.io.IOException;
import java.util.zip.ZipFile;

public final class OppoConvertIcon {
    private static final boolean DEBUG_ENABLE = false;
    private static final boolean DEBUG_NORMAL = false;
    private static final String IPHONE_STYLE_BG_NAME = "iphone_style_bg.png";
    private static final String IPHONE_STYLE_FG_NAME = "iphone_style_fg.png";
    private static final String NEW_IPHONE_STYLE_BG_NAME = "new_iphone_style_bg.png";
    private static final String NEW_IPHONE_STYLE_MASK = "new_iphone_style_mask.png";
    private static final String TAG = "OppoConvertIcon";
    private static boolean drawForeground = false;
    private static final String[] drawabledirs = new String[]{"res/drawable-hdpi/", "res/drawable-xhdpi/", "res/drawable-xxhdpi/"};
    private static Drawable mIconBackground = null;
    private static IconBgType mIconBgType = IconBgType.MASK;
    private static Drawable mIconForeground = null;
    public static Bitmap mMaskBitmap = null;
    private static final Canvas sCanvas = new Canvas();
    private static String sCoverBackgroundPic = null;
    private static int sIconHeight = -1;
    private static int sIconSize = -1;
    private static int sIconWidth = -1;
    private static String sMaskBackgroundPic = null;
    private static String sMaskForegroundPic = null;
    private static final Rect sOldBounds = new Rect();
    private static int sThemeParamScale = 128;
    private static int sThemeParamXOffset = 0;
    private static int sThemeParamYOffset = 0;

    public enum IconBgType {
        MASK,
        COVER,
        SCALE
    }

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    public static boolean hasInit() {
        if (sCoverBackgroundPic == null && sMaskBackgroundPic == null && sMaskForegroundPic == null) {
            return false;
        }
        return true;
    }

    public static Bitmap convertIconBitmap(Drawable icon, Resources res, boolean isThirdPart) {
        return convertIconBitmap(icon, res, isThirdPart, false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Bitmap convertIconBitmap(Drawable icon, Resources res, boolean isThirdPart, boolean forceCutAndScale) {
        if (icon == null) {
            return null;
        }
        synchronized (sCanvas) {
            Bitmap bitmap;
            Canvas canvas;
            if (sIconWidth == -1) {
                initIconSize(res);
            }
            int width = sIconWidth;
            int height = sIconHeight;
            Bitmap originalBitmap = null;
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) icon).getBitmap();
                if (isThirdPart) {
                    originalBitmap = bitmap;
                    if (bitmap.getConfig() == null) {
                        Log.i(TAG, "convertIconBitmap...set the bitmap null config to ARGB_8888");
                        originalBitmap = bitmap.copy(Config.ARGB_8888, true);
                    }
                }
            } else if ((icon instanceof NinePatchDrawable) || (icon instanceof VectorDrawable) || (icon instanceof AdaptiveIconDrawable)) {
                originalBitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Config.ARGB_8888);
                canvas = new Canvas(originalBitmap);
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                icon.draw(canvas);
            }
            if (sIconWidth <= 0) {
                return null;
            }
            bitmap = Bitmap.createBitmap(sIconWidth, sIconHeight, Config.ARGB_8888);
            canvas = sCanvas;
            canvas.setBitmap(bitmap);
            if (!isThirdPart) {
                int sourceWidth = icon.getIntrinsicWidth();
                int sourceHeight = icon.getIntrinsicHeight();
                if (sourceWidth > 0 && sourceHeight > 0) {
                    float ratio = ((float) sourceWidth) / ((float) sourceHeight);
                    if (sourceWidth > sourceHeight) {
                        height = (int) (((float) width) / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (((float) height) * ratio);
                    }
                }
                int left = (sIconWidth - width) / 2;
                int top = (sIconHeight - height) / 2;
                sOldBounds.set(icon.getBounds());
                icon.setBounds(left, top, left + width, top + height);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                canvas.setBitmap(null);
            } else if (forceCutAndScale) {
                cutAndScaleBitmap(icon, originalBitmap, res, canvas);
            } else {
                if (mIconBgType == IconBgType.COVER) {
                    coverBitmap(icon, originalBitmap, res, canvas);
                    if (drawForeground) {
                        drawForeground(res, canvas);
                    }
                } else {
                    if (mIconBgType != IconBgType.MASK || mMaskBitmap == null) {
                        cutAndScaleBitmap(icon, originalBitmap, res, canvas);
                    } else {
                        maskBitmap(icon, originalBitmap, res, canvas);
                        if (drawForeground) {
                            drawForeground(res, canvas);
                        }
                    }
                }
            }
        }
    }

    static void coverBitmapNoCut(Drawable icon, Bitmap originalBitmap, Resources res, Canvas canvas) {
        if (originalBitmap != null) {
            Drawable mIconBackground = OppoThirdPartUtil.getLauncherDrawableByName(res, sCoverBackgroundPic);
            if (mIconBackground != null) {
                sOldBounds.set(mIconBackground.getBounds());
                mIconBackground.setBounds(0, 0, sIconWidth, sIconHeight);
                mIconBackground.draw(canvas);
                mIconBackground.setBounds(sOldBounds);
            }
            double f = 1.0d;
            if (res.getDisplayMetrics().xdpi > 400.0f && OppoThirdPartUtil.mIsDefaultTheme) {
                f = 1.055d;
            }
            int width = (int) (((double) icon.getIntrinsicWidth()) * f);
            int height = (int) (((double) icon.getIntrinsicHeight()) * f);
            int l = (sIconWidth - width) / 2;
            int t = (sIconHeight - height) / 2;
            icon.setBounds(l, t, l + width, t + height);
            icon.draw(canvas);
        }
    }

    static void coverBitmap(Drawable icon, Bitmap originalBitmap, Resources res, Canvas canvas) {
        if (originalBitmap != null) {
            if (!originalBitmap.hasAlpha()) {
                originalBitmap.setHasAlpha(true);
            }
            if (mIconBackground == null) {
                mIconBackground = OppoThirdPartUtil.getLauncherDrawableByName(res, sCoverBackgroundPic);
            }
            if (mIconBackground != null) {
                sOldBounds.set(mIconBackground.getBounds());
                mIconBackground.setBounds(0, 0, sIconWidth, sIconHeight);
                mIconBackground.draw(canvas);
                mIconBackground.setBounds(sOldBounds);
            }
            Bitmap scale = originalBitmap.getConfig() != null ? OppoMaskBitmapUtilities.getInstance().cutAndScaleBitmap(originalBitmap) : originalBitmap;
            if (scale != null) {
                canvas.drawBitmap(scale, (float) (((sIconWidth - scale.getWidth()) / 2) + sThemeParamXOffset), (float) (((sIconHeight - scale.getHeight()) / 2) + sThemeParamYOffset), null);
            } else {
                Log.i(TAG, "coverBitmap -- scale == null");
            }
        }
    }

    static void cutAndScaleBitmap(Drawable icon, Bitmap originalBitmap, Resources res, Canvas canvas) {
        if (originalBitmap != null) {
            if (!originalBitmap.hasAlpha()) {
                originalBitmap.setHasAlpha(true);
            }
            Bitmap scale = originalBitmap.getConfig() != null ? OppoMaskBitmapUtilities.getInstance().cutAndScaleBitmap(originalBitmap) : originalBitmap;
            if (scale != null) {
                canvas.drawBitmap(scale, (float) ((sIconWidth - scale.getWidth()) / 2), (float) ((sIconHeight - scale.getHeight()) / 2), null);
            } else {
                Log.i(TAG, "cutAndScaleBitmap -- scale == null");
            }
        }
    }

    static void maskBitmap(Drawable icon, Bitmap originalBitmap, Resources res, Canvas canvas) {
        if (originalBitmap == null) {
            originalBitmap = Bitmap.createBitmap(sIconWidth, sIconHeight, Config.ARGB_8888);
            canvas.setBitmap(originalBitmap);
            sOldBounds.set(icon.getBounds());
            icon.setBounds(0, 0, sIconWidth, sIconHeight);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
        }
        if (originalBitmap != null) {
            if (!originalBitmap.hasAlpha()) {
                originalBitmap.setHasAlpha(true);
            }
            Bitmap scale = OppoMaskBitmapUtilities.getInstance().scaleAndMaskBitmap(originalBitmap);
            if (mIconBackground == null) {
                mIconBackground = OppoThirdPartUtil.getLauncherDrawableByName(res, sMaskBackgroundPic);
            }
            if (mIconBackground != null) {
                sOldBounds.set(mIconBackground.getBounds());
                mIconBackground.setBounds(0, 0, sIconWidth, sIconHeight);
                mIconBackground.draw(canvas);
                mIconBackground.setBounds(sOldBounds);
            } else {
                Log.i(TAG, "maskBitmap -- mIconBackground == null");
            }
            if (scale != null) {
                int w = scale.getWidth();
                int h = scale.getHeight();
                if (((w - sIconWidth) / 2) + sThemeParamXOffset > -1) {
                    canvas.drawBitmap(scale, (float) ((sIconWidth - w) / 2), (float) ((sIconHeight - h) / 2), null);
                    return;
                } else {
                    canvas.drawBitmap(scale, (float) (((sIconWidth - w) / 2) + sThemeParamXOffset), (float) (((sIconHeight - h) / 2) + sThemeParamYOffset), null);
                    return;
                }
            }
            Log.i(TAG, "maskBitmap -- scale == null");
            return;
        }
        Log.i(TAG, "maskBitmap -- originalBitmap == null");
    }

    public static void drawForeground(Resources res, Canvas canvas) {
        if (mIconForeground == null) {
            mIconForeground = OppoThirdPartUtil.getLauncherDrawableByName(res, sMaskForegroundPic);
        }
        if (mIconForeground != null) {
            sOldBounds.set(mIconForeground.getBounds());
            mIconForeground.setBounds(0, 0, sIconWidth, sIconHeight);
            mIconForeground.draw(canvas);
            mIconForeground.setBounds(sOldBounds);
        }
    }

    private static void setIconBgFgRes(String maskBg, String MaskFg, String coverBg) {
        sMaskBackgroundPic = maskBg;
        sMaskForegroundPic = MaskFg;
        sCoverBackgroundPic = coverBg;
        mIconBackground = null;
        mIconForeground = null;
    }

    private static void initIconSize(Resources res) {
        int width = 168;
        if (res != null) {
            DisplayMetrics dm = res.getDisplayMetrics();
            if (dm != null) {
                width = (dm.widthPixels * 168) / 1080;
            }
        }
        sIconHeight = width;
        sIconWidth = width;
        sIconSize = width;
    }

    public static void initThemeParam(Resources res, String maskBg, String MaskFg, String coverBg) {
        OppoIconParam oppoIconParam = new OppoIconParam("themeInfo.xml");
        oppoIconParam.parseXml();
        float tempRatio = oppoIconParam.getScale();
        if (tempRatio <= 0.0f) {
            if (mIconBgType == IconBgType.COVER) {
                tempRatio = 0.62f;
            } else if (mIconBgType == IconBgType.SCALE) {
                tempRatio = 1.0f;
            } else if (mIconBgType == IconBgType.MASK) {
                tempRatio = 1.0f;
            }
        }
        if (sIconSize == -1) {
            initIconSize(res);
        }
        sThemeParamScale = (int) (((float) sIconSize) * tempRatio);
        sThemeParamXOffset = (int) (((float) sIconSize) * oppoIconParam.getXOffset());
        sThemeParamYOffset = (int) (((float) sIconSize) * oppoIconParam.getYOffset());
        setIconBgFgRes(maskBg, MaskFg, coverBg);
    }

    public static IconBgType getIconBgType() {
        Throwable th;
        String path = "/data/theme/";
        if (OppoThirdPartUtil.mIsDefaultTheme) {
            path = "/system/media/theme/default/";
        }
        ZipFile zipFile = null;
        IconBgType iconBgType;
        try {
            ZipFile zipFile2 = new ZipFile(path + OppoThirdPartUtil.ZIPLAUNCHER);
            try {
                if (judgePicExist(zipFile2, IPHONE_STYLE_BG_NAME)) {
                    if (judgePicExist(zipFile2, IPHONE_STYLE_FG_NAME)) {
                        drawForeground = true;
                    }
                    zipFile2.close();
                    iconBgType = IconBgType.COVER;
                    mIconBgType = iconBgType;
                    if (zipFile2 != null) {
                        try {
                            zipFile2.close();
                        } catch (IOException e) {
                        }
                    }
                    return iconBgType;
                } else if (judgePicExist(zipFile2, NEW_IPHONE_STYLE_MASK)) {
                    if (judgePicExist(zipFile2, IPHONE_STYLE_FG_NAME)) {
                        drawForeground = true;
                    }
                    zipFile2.close();
                    iconBgType = IconBgType.MASK;
                    mIconBgType = iconBgType;
                    if (zipFile2 != null) {
                        try {
                            zipFile2.close();
                        } catch (IOException e2) {
                        }
                    }
                    return iconBgType;
                } else {
                    if (zipFile2 != null) {
                        try {
                            zipFile2.close();
                        } catch (IOException e3) {
                        }
                    }
                    iconBgType = IconBgType.SCALE;
                    mIconBgType = iconBgType;
                    return iconBgType;
                }
            } catch (IOException e4) {
                zipFile = zipFile2;
                try {
                    iconBgType = IconBgType.MASK;
                    mIconBgType = iconBgType;
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException e5) {
                        }
                    }
                    return iconBgType;
                } catch (Throwable th2) {
                    th = th2;
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException e6) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                zipFile = zipFile2;
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e62) {
                    }
                }
                throw th;
            }
        } catch (IOException e7) {
            iconBgType = IconBgType.MASK;
            mIconBgType = iconBgType;
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e52) {
                }
            }
            return iconBgType;
        }
    }

    public static boolean judgePicExist(String zipFilePath, String picName) {
        try {
            ZipFile file = new ZipFile(zipFilePath);
            boolean flag = judgePicExist(file, picName);
            file.close();
            return flag;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean judgePicExist(ZipFile zipFile, String picName) {
        for (int i = drawabledirs.length - 1; i >= 0; i--) {
            if (zipFile.getEntry(drawabledirs[i] + picName) != null) {
                return true;
            }
        }
        return false;
    }

    public static Bitmap getMaskBitmap(Resources res, String picName) {
        if (mMaskBitmap != null) {
            mMaskBitmap.recycle();
            mMaskBitmap = null;
        }
        Drawable mask = OppoThirdPartUtil.getLauncherDrawableByName(res, picName);
        if (sIconWidth == -1) {
            initIconSize(res);
        }
        mMaskBitmap = Bitmap.createBitmap(sIconWidth, sIconHeight, Config.ARGB_8888);
        Canvas canvas = sCanvas;
        canvas.setBitmap(mMaskBitmap);
        if (mask != null) {
            mask.setBounds(0, 0, sIconWidth, sIconHeight);
            mask.draw(canvas);
        } else {
            canvas.drawColor(-16777216);
        }
        canvas.setBitmap(null);
        return mMaskBitmap;
    }

    public static int getThemeParamScale() {
        return sThemeParamScale;
    }

    public static int getIconSize() {
        return sIconSize;
    }

    public static void initConvertIcon(Resources res) {
        OppoThirdPartUtil.setDefaultTheme();
        if (getIconBgType() == IconBgType.MASK) {
            OppoMaskBitmapUtilities.getInstance().setMaskBitmap(getMaskBitmap(res, NEW_IPHONE_STYLE_MASK));
        }
        initThemeParam(res, NEW_IPHONE_STYLE_BG_NAME, IPHONE_STYLE_FG_NAME, IPHONE_STYLE_BG_NAME);
        OppoMaskBitmapUtilities.getInstance().setCutAndScalePram(getIconSize(), getThemeParamScale());
    }
}
