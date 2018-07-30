package android.location;

import android.util.SparseArray;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Deprecated
public final class GpsStatus {
    private static final int BEIDOU_SVID_OFFSET = 200;
    private static final int GLONASS_SVID_OFFSET = 64;
    public static final int GPS_EVENT_FIRST_FIX = 3;
    public static final int GPS_EVENT_SATELLITE_STATUS = 4;
    public static final int GPS_EVENT_STARTED = 1;
    public static final int GPS_EVENT_STOPPED = 2;
    private static final int NUM_SATELLITES = 255;
    private static final int SBAS_SVID_OFFSET = -87;
    private Iterable<GpsSatellite> mSatelliteList = new Iterable<GpsSatellite>() {
        public Iterator<GpsSatellite> iterator() {
            return new SatelliteIterator();
        }
    };
    private final SparseArray<GpsSatellite> mSatellites = new SparseArray();
    private int mTimeToFirstFix;

    @Deprecated
    public interface Listener {
        void onGpsStatusChanged(int i);
    }

    @Deprecated
    public interface NmeaListener {
        void onNmeaReceived(long j, String str);
    }

    private final class SatelliteIterator implements Iterator<GpsSatellite> {
        private int mIndex = 0;
        private final int mSatellitesCount;

        SatelliteIterator() {
            this.mSatellitesCount = GpsStatus.this.mSatellites.size();
        }

        public boolean hasNext() {
            while (this.mIndex < this.mSatellitesCount) {
                if (((GpsSatellite) GpsStatus.this.mSatellites.valueAt(this.mIndex)).mValid) {
                    return true;
                }
                this.mIndex++;
            }
            return false;
        }

        public GpsSatellite next() {
            while (this.mIndex < this.mSatellitesCount) {
                GpsSatellite satellite = (GpsSatellite) GpsStatus.this.mSatellites.valueAt(this.mIndex);
                this.mIndex++;
                if (satellite.mValid) {
                    return satellite;
                }
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    GpsStatus() {
    }

    private void setStatus(int svCount, int[] svidWithFlags, float[] cn0s, float[] elevations, float[] azimuths) {
        clearSatellites();
        for (int i = 0; i < svCount; i++) {
            if (0.0d < ((double) cn0s[i])) {
                int constellationType = (svidWithFlags[i] >> 4) & 15;
                int prn = svidWithFlags[i] >> 8;
                if (constellationType == 3) {
                    prn += 64;
                } else if (constellationType == 5) {
                    prn += 200;
                } else if (constellationType == 2) {
                    prn += SBAS_SVID_OFFSET;
                } else if (!(constellationType == 1 || constellationType == 4)) {
                }
                if (prn > 0 && prn <= 255) {
                    GpsSatellite satellite = (GpsSatellite) this.mSatellites.get(prn);
                    if (satellite == null) {
                        satellite = new GpsSatellite(prn);
                        this.mSatellites.put(prn, satellite);
                    }
                    satellite.mValid = true;
                    satellite.mSnr = cn0s[i];
                    satellite.mElevation = elevations[i];
                    satellite.mAzimuth = azimuths[i];
                    satellite.mHasEphemeris = (svidWithFlags[i] & 1) != 0;
                    satellite.mHasAlmanac = (svidWithFlags[i] & 2) != 0;
                    satellite.mUsedInFix = (svidWithFlags[i] & 4) != 0;
                }
            }
        }
    }

    void setStatus(GnssStatus status, int timeToFirstFix) {
        this.mTimeToFirstFix = timeToFirstFix;
        setStatus(status.mSvCount, status.mSvidWithFlags, status.mCn0DbHz, status.mElevations, status.mAzimuths);
    }

    void setTimeToFirstFix(int ttff) {
        this.mTimeToFirstFix = ttff;
    }

    public int getTimeToFirstFix() {
        return this.mTimeToFirstFix;
    }

    public Iterable<GpsSatellite> getSatellites() {
        return this.mSatelliteList;
    }

    public int getMaxSatellites() {
        return 255;
    }

    private void clearSatellites() {
        int satellitesCount = this.mSatellites.size();
        for (int i = 0; i < satellitesCount; i++) {
            ((GpsSatellite) this.mSatellites.valueAt(i)).mValid = false;
        }
    }
}
