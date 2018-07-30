package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import com.android.server.display.OppoBrightUtils;

public class FlpToGnssRequest {
    private static final String GPS_PROVIDER = "gps";
    private static final String TAG = "FlpToGnssRequest";
    private final Context mContext;
    private boolean mHasSessionOn = false;
    private final LocationManager mLocMgr;
    private Object mLock = new Object();
    private LocationListener myLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    public FlpToGnssRequest(Context context) {
        this.mContext = context;
        this.mLocMgr = (LocationManager) this.mContext.getSystemService("location");
    }

    public void makeGnssSessionOn() {
        Log.d(TAG, "make a gnss request by flp!!");
        this.mLocMgr.requestLocationUpdates(GPS_PROVIDER, 60000, OppoBrightUtils.MIN_LUX_LIMITI, this.myLocationListener);
        synchronized (this.mLock) {
            this.mHasSessionOn = true;
        }
    }

    public void makeGnssSessionOff() {
        Log.d(TAG, "remove a gnss request by flp!!");
        synchronized (this.mLock) {
            if (this.mHasSessionOn) {
                this.mLocMgr.removeUpdates(this.myLocationListener);
            }
            this.mHasSessionOn = false;
        }
    }
}
