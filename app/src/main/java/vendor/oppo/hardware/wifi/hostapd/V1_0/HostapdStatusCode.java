package vendor.oppo.hardware.wifi.hostapd.V1_0;

import java.util.ArrayList;

public final class HostapdStatusCode {
    public static final int FAILURE_ARGS_INVALID = 2;
    public static final int FAILURE_IFACE_DISABLED = 6;
    public static final int FAILURE_IFACE_EXISTS = 5;
    public static final int FAILURE_IFACE_INVALID = 3;
    public static final int FAILURE_IFACE_NOT_DISCONNECTED = 7;
    public static final int FAILURE_IFACE_UNKNOWN = 4;
    public static final int FAILURE_UNKNOWN = 1;
    public static final int NOT_SUPPORTED = 8;
    public static final int SUCCESS = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "SUCCESS";
        }
        if (o == 1) {
            return "FAILURE_UNKNOWN";
        }
        if (o == 2) {
            return "FAILURE_ARGS_INVALID";
        }
        if (o == 3) {
            return "FAILURE_IFACE_INVALID";
        }
        if (o == 4) {
            return "FAILURE_IFACE_UNKNOWN";
        }
        if (o == 5) {
            return "FAILURE_IFACE_EXISTS";
        }
        if (o == 6) {
            return "FAILURE_IFACE_DISABLED";
        }
        if (o == 7) {
            return "FAILURE_IFACE_NOT_DISCONNECTED";
        }
        if (o == 8) {
            return "NOT_SUPPORTED";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("SUCCESS");
        if ((o & 1) == 1) {
            list.add("FAILURE_UNKNOWN");
            flipped = 1;
        }
        if ((o & 2) == 2) {
            list.add("FAILURE_ARGS_INVALID");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("FAILURE_IFACE_INVALID");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("FAILURE_IFACE_UNKNOWN");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("FAILURE_IFACE_EXISTS");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("FAILURE_IFACE_DISABLED");
            flipped |= 6;
        }
        if ((o & 7) == 7) {
            list.add("FAILURE_IFACE_NOT_DISCONNECTED");
            flipped |= 7;
        }
        if ((o & 8) == 8) {
            list.add("NOT_SUPPORTED");
            flipped |= 8;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
