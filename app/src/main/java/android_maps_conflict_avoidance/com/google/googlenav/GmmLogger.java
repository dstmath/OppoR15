package android_maps_conflict_avoidance.com.google.googlenav;

import android_maps_conflict_avoidance.com.google.common.Log;

public class GmmLogger {
    public static void logTimingTileLatency(String tileType, int timeToWrite, int timeToFirstByteMsec, int timeToLastByteMsec, int numTiles, int numBytes) {
        String str = "TL";
        Log.addEvent((short) 22, str, Log.createEventTuple(new String[]{"t=" + tileType, "tw=" + timeToWrite, "tf=" + timeToFirstByteMsec, "tl=" + timeToLastByteMsec, "n=" + numTiles, "b=" + numBytes}));
    }
}
