package android.graphics;

import android.os.Parcel;
import android.os.Process;
import android.util.ArrayMap;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;

public class LeakyTypefaceStorage {
    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static final ArrayList<Typeface> sStorage = new ArrayList();
    @GuardedBy("sLock")
    private static final ArrayMap<Typeface, Integer> sTypefaceMap = new ArrayMap();

    public static void writeTypefaceToParcel(Typeface typeface, Parcel parcel) {
        parcel.writeInt(Process.myPid());
        synchronized (sLock) {
            int id;
            Integer i = (Integer) sTypefaceMap.get(typeface);
            if (i != null) {
                id = i.intValue();
            } else {
                id = sStorage.size();
                sStorage.add(typeface);
                sTypefaceMap.put(typeface, Integer.valueOf(id));
            }
            parcel.writeInt(id);
        }
    }

    public static Typeface readTypefaceFromParcel(Parcel parcel) {
        int pid = parcel.readInt();
        int typefaceId = parcel.readInt();
        if (pid != Process.myPid()) {
            return null;
        }
        Typeface typeface;
        synchronized (sLock) {
            typeface = (Typeface) sStorage.get(typefaceId);
        }
        return typeface;
    }
}
