package com.oppo.neuron;

import android.os.RemoteException;
import java.util.ArrayList;

public interface INeoService {
    public static final int AI_ALGORITHM_IS_PRIOR_APP = 201;
    public static final int AI_APP_CLEAN_PREDICT = 301;
    public static final int AI_APP_PRELOAD_PREDICT = 302;
    public static final String DESCRIPTOR = "neoservice";
    public static final int MAX_WIFI_SCAN_RESULT = 10;

    float appCleanPredict(boolean z, String str, String str2, boolean z2, float f, String str3, String str4, ArrayList<String> arrayList);

    String[] appPreloadPredict();

    int isPriorApp(int i, int i2, long j, long j2) throws RemoteException;
}
