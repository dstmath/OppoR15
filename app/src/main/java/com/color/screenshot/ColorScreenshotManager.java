package com.color.screenshot;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.color.content.ColorContext;
import com.color.screenshot.IColorScreenshotManager.Stub;

public final class ColorScreenshotManager {
    public static final String GLOBAL_ACTION_VISIBLE = "global_action_visible";
    public static final String NAVIGATIONBAR_VISIBLE = "navigationbar_visible";
    public static final String SCREENSHOT_DIRECTION = "screenshot_direction";
    public static final String SCREENSHOT_ORIENTATION = "screenshot_orientation";
    public static final String SCREENSHOT_SOURCE = "screenshot_source";
    public static final String STATUSBAR_VISIBLE = "statusbar_visible";
    private static final String TAG = "LongshotDump";
    private static volatile ColorScreenshotManager sInstance = null;
    private final IColorScreenshotManager mService = Stub.asInterface(ServiceManager.getService(ColorContext.SCREENSHOT_SERVICE));

    private ColorScreenshotManager() {
    }

    public static ColorScreenshotManager getInstance() {
        if (sInstance == null) {
            synchronized (ColorScreenshotManager.class) {
                if (sInstance == null) {
                    sInstance = new ColorScreenshotManager();
                }
            }
        }
        return sInstance;
    }

    public static ColorScreenshotManager peekInstance() {
        return sInstance;
    }

    public void takeScreenshot(Bundle extras) {
        try {
            if (this.mService != null) {
                this.mService.takeScreenshot(extras);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
        }
    }

    public boolean isScreenshotMode() {
        try {
            if (this.mService != null) {
                return this.mService.isScreenshotMode();
            }
            return false;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
            return false;
        }
    }

    public boolean isScreenshotEdit() {
        try {
            if (this.mService != null) {
                return this.mService.isScreenshotEdit();
            }
            return false;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
            return false;
        }
    }

    public void takeLongshot(boolean statusBarVisible, boolean navBarVisible) {
        try {
            if (this.mService != null) {
                this.mService.takeLongshot(statusBarVisible, navBarVisible);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
        }
    }

    public void stopLongshot() {
        try {
            if (this.mService != null) {
                this.mService.stopLongshot();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
        }
    }

    public boolean isLongshotMode() {
        try {
            if (this.mService != null) {
                return this.mService.isLongshotMode();
            }
            return false;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
            return false;
        }
    }

    public boolean isLongshotDisabled() {
        try {
            if (this.mService != null) {
                return this.mService.isLongshotDisabled();
            }
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
            return true;
        }
    }

    public void reportLongshotDumpResult(ColorLongshotDump result) {
        try {
            if (this.mService != null) {
                this.mService.reportLongshotDumpResult(result);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
        }
    }

    public void setScreenshotEnabled(boolean enabled) {
        try {
            if (this.mService != null) {
                this.mService.setScreenshotEnabled(enabled);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
        }
    }

    public boolean isScreenshotEnabled() {
        try {
            if (this.mService != null) {
                return this.mService.isScreenshotEnabled();
            }
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
            return true;
        }
    }

    public void setLongshotEnabled(boolean enabled) {
        try {
            if (this.mService != null) {
                this.mService.setLongshotEnabled(enabled);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
        }
    }

    public boolean isLongshotEnabled() {
        try {
            if (this.mService != null) {
                return this.mService.isLongshotEnabled();
            }
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e2) {
            return true;
        }
    }
}
