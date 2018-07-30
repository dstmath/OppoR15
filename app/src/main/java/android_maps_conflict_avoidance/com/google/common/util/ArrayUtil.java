package android_maps_conflict_avoidance.com.google.common.util;

import java.util.Vector;

public class ArrayUtil {
    public static void copyIntoVector(Vector src, int srcIndex, Vector dest) {
        synchronized (dest) {
            for (int i = srcIndex; i < src.size(); i++) {
                dest.insertElementAt(src.elementAt(i), i - srcIndex);
            }
        }
    }
}
