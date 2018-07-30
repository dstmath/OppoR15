package vendor.oppo.hardware.biometrics.face.V1_0;

import java.util.ArrayList;

public final class FaceAcquiredInfo {
    public static final int ACQUIRED_BRIGHT = 106;
    public static final int ACQUIRED_DARK = 103;
    public static final int ACQUIRED_DOWN = 110;
    public static final int ACQUIRED_FAR_FACE = 6;
    public static final int ACQUIRED_GOOD = 0;
    public static final int ACQUIRED_HACKER = 104;
    public static final int ACQUIRED_IMAGER_DIRTY = 3;
    public static final int ACQUIRED_INSUFFICIENT = 2;
    public static final int ACQUIRED_LEFT = 107;
    public static final int ACQUIRED_LOW_SIMILARITY = 105;
    public static final int ACQUIRED_NEAR_FACE = 7;
    public static final int ACQUIRED_NO_FACE = 101;
    public static final int ACQUIRED_PARTIAL = 1;
    public static final int ACQUIRED_RIGHT = 108;
    public static final int ACQUIRED_SHIFTING = 102;
    public static final int ACQUIRED_TOO_FAST = 5;
    public static final int ACQUIRED_TOO_SLOW = 4;
    public static final int ACQUIRED_UP = 109;
    public static final int ACQUIRED_VENDOR_BASE = 1000;

    public static final String toString(int o) {
        if (o == 0) {
            return "ACQUIRED_GOOD";
        }
        if (o == 1) {
            return "ACQUIRED_PARTIAL";
        }
        if (o == 2) {
            return "ACQUIRED_INSUFFICIENT";
        }
        if (o == 3) {
            return "ACQUIRED_IMAGER_DIRTY";
        }
        if (o == 4) {
            return "ACQUIRED_TOO_SLOW";
        }
        if (o == 5) {
            return "ACQUIRED_TOO_FAST";
        }
        if (o == 6) {
            return "ACQUIRED_FAR_FACE";
        }
        if (o == 7) {
            return "ACQUIRED_NEAR_FACE";
        }
        if (o == ACQUIRED_NO_FACE) {
            return "ACQUIRED_NO_FACE";
        }
        if (o == ACQUIRED_SHIFTING) {
            return "ACQUIRED_SHIFTING";
        }
        if (o == ACQUIRED_DARK) {
            return "ACQUIRED_DARK";
        }
        if (o == ACQUIRED_HACKER) {
            return "ACQUIRED_HACKER";
        }
        if (o == ACQUIRED_LOW_SIMILARITY) {
            return "ACQUIRED_LOW_SIMILARITY";
        }
        if (o == ACQUIRED_BRIGHT) {
            return "ACQUIRED_BRIGHT";
        }
        if (o == ACQUIRED_LEFT) {
            return "ACQUIRED_LEFT";
        }
        if (o == ACQUIRED_RIGHT) {
            return "ACQUIRED_RIGHT";
        }
        if (o == ACQUIRED_UP) {
            return "ACQUIRED_UP";
        }
        if (o == ACQUIRED_DOWN) {
            return "ACQUIRED_DOWN";
        }
        if (o == 1000) {
            return "ACQUIRED_VENDOR_BASE";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("ACQUIRED_GOOD");
        if ((o & 1) == 1) {
            list.add("ACQUIRED_PARTIAL");
            flipped = 1;
        }
        if ((o & 2) == 2) {
            list.add("ACQUIRED_INSUFFICIENT");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("ACQUIRED_IMAGER_DIRTY");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("ACQUIRED_TOO_SLOW");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("ACQUIRED_TOO_FAST");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("ACQUIRED_FAR_FACE");
            flipped |= 6;
        }
        if ((o & 7) == 7) {
            list.add("ACQUIRED_NEAR_FACE");
            flipped |= 7;
        }
        if ((o & ACQUIRED_NO_FACE) == ACQUIRED_NO_FACE) {
            list.add("ACQUIRED_NO_FACE");
            flipped |= ACQUIRED_NO_FACE;
        }
        if ((o & ACQUIRED_SHIFTING) == ACQUIRED_SHIFTING) {
            list.add("ACQUIRED_SHIFTING");
            flipped |= ACQUIRED_SHIFTING;
        }
        if ((o & ACQUIRED_DARK) == ACQUIRED_DARK) {
            list.add("ACQUIRED_DARK");
            flipped |= ACQUIRED_DARK;
        }
        if ((o & ACQUIRED_HACKER) == ACQUIRED_HACKER) {
            list.add("ACQUIRED_HACKER");
            flipped |= ACQUIRED_HACKER;
        }
        if ((o & ACQUIRED_LOW_SIMILARITY) == ACQUIRED_LOW_SIMILARITY) {
            list.add("ACQUIRED_LOW_SIMILARITY");
            flipped |= ACQUIRED_LOW_SIMILARITY;
        }
        if ((o & ACQUIRED_BRIGHT) == ACQUIRED_BRIGHT) {
            list.add("ACQUIRED_BRIGHT");
            flipped |= ACQUIRED_BRIGHT;
        }
        if ((o & ACQUIRED_LEFT) == ACQUIRED_LEFT) {
            list.add("ACQUIRED_LEFT");
            flipped |= ACQUIRED_LEFT;
        }
        if ((o & ACQUIRED_RIGHT) == ACQUIRED_RIGHT) {
            list.add("ACQUIRED_RIGHT");
            flipped |= ACQUIRED_RIGHT;
        }
        if ((o & ACQUIRED_UP) == ACQUIRED_UP) {
            list.add("ACQUIRED_UP");
            flipped |= ACQUIRED_UP;
        }
        if ((o & ACQUIRED_DOWN) == ACQUIRED_DOWN) {
            list.add("ACQUIRED_DOWN");
            flipped |= ACQUIRED_DOWN;
        }
        if ((o & 1000) == 1000) {
            list.add("ACQUIRED_VENDOR_BASE");
            flipped |= 1000;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
