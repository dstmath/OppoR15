package com.android.server.notification;

import com.android.server.display.OppoBrightUtils;

public class RateEstimator {
    private static final double MINIMUM_DT = 5.0E-4d;
    private static final double RATE_ALPHA = 0.8d;
    private double mInterarrivalTime = 1000.0d;
    private Long mLastEventTime;

    public float update(long now) {
        float rate;
        if (this.mLastEventTime == null) {
            rate = OppoBrightUtils.MIN_LUX_LIMITI;
        } else {
            this.mInterarrivalTime = getInterarrivalEstimate(now);
            rate = (float) (1.0d / this.mInterarrivalTime);
        }
        this.mLastEventTime = Long.valueOf(now);
        return rate;
    }

    public float getRate(long now) {
        if (this.mLastEventTime == null) {
            return OppoBrightUtils.MIN_LUX_LIMITI;
        }
        return (float) (1.0d / getInterarrivalEstimate(now));
    }

    private double getInterarrivalEstimate(long now) {
        return (this.mInterarrivalTime * RATE_ALPHA) + (0.19999999999999996d * Math.max(((double) (now - this.mLastEventTime.longValue())) / 1000.0d, MINIMUM_DT));
    }
}
