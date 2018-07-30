package com.android.internal.telephony.oem.rus;

import android.os.Message;
import android.util.Xml;
import com.android.internal.telephony.regionlock.RegionLockPlmnListService.PlmnCodeEntry;
import com.android.internal.util.XmlUtils;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import org.xmlpull.v1.XmlPullParser;

public final class RusUpdateQcomFplmn extends RusBase {
    private static final int FRAGMENT_END = 2;
    private static final int FRAGMENT_MIDDLE = 1;
    private static final int FRAGMENT_START = 0;
    private static final int NOT_FRAGMENT = 3;
    private static final String TAG = "RusUpdateQcomFplmn";
    private static final int TYPE_CMCCPLMN = 0;
    private static final int TYPE_CTPLMN = 2;
    private static final int TYPE_CUPLMN = 1;
    private static final int UPDATE_FPLMN = 4;
    private static final int mPlmnIndex_count = 11;
    private static final String mPlmnIndex_end = "/QcomFplmn>";
    private static final String mPlmnIndex_start = "<QcomFplmn";
    private static final String mPlmnInfoFileName = "qual_update_fplmn.xml";
    private static final String mSubPath = "/data/data/com.android.phone/";
    public final int FINAL_NV_LENGTH = 127;
    public final int PLMN_MAX_LENGTH = 1000;
    public final int PLMN_NV_LENGTH = 125;
    public final int PLMN_NV_LENGTH2 = 127;
    public final int SIGNAL_NV_LENGTH = 40;
    private String mPlmnContent = null;
    int[][] mPlmnIdPlmn = ((int[][]) Array.newInstance(Integer.TYPE, new int[]{2, 1000}));

    public RusUpdateQcomFplmn() {
        setPath(mSubPath);
    }

    public void execute() {
        sendEmptyMessage(valiateAndUpatePlmn());
    }

    public void onSucceed(Message msg) {
        byte[] bytearray = msg.obj;
        printLog(TAG, "the data that will be write is:");
        int len = bytearray[6];
        if (len > 40) {
            len = 40;
        }
        printPlmnByteArray(bytearray, len * 3);
        try {
            this.mPhone.oppoUpdatePplmnList(bytearray, null);
        } catch (Exception e) {
            printLog(TAG, "hanlder doNVwrite wrong");
            e.printStackTrace();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int valiateAndUpatePlmn() {
        Throwable th;
        printLog(TAG, "getPlmnXmlFile");
        FileReader confreader = null;
        byte[] plmnarray = new byte[125];
        String providerContent = getContent();
        printLog(TAG, "getproviderContent" + providerContent);
        try {
            this.mPlmnContent = createNetworkRomupdateXmlFile(providerContent, mPlmnInfoFileName, mPlmnIndex_start, mPlmnIndex_end, 11);
            FileReader confreader2 = new FileReader(this.mFileUtils.saveToFile(this.mPlmnContent, this.mPath + mPlmnInfoFileName));
            try {
                XmlPullParser confparser = Xml.newPullParser();
                if (confparser == null) {
                    printLog(TAG, "confparser==null");
                    if (confreader2 != null) {
                        try {
                            confreader2.close();
                        } catch (IOException e) {
                        }
                    }
                    return 20;
                }
                confparser.setInput(confreader2);
                XmlUtils.beginDocument(confparser, "QcomFplmn");
                XmlUtils.nextElement(confparser);
                while (confparser.getEventType() != 1) {
                    if ("cmcc_plmn".equals(confparser.getName())) {
                        int plmnPos = 1;
                        XmlUtils.nextElement(confparser);
                        while (true) {
                            int plmnPos2;
                            try {
                                plmnPos2 = plmnPos;
                                if (!"plmn".equals(confparser.getName())) {
                                    break;
                                }
                                printLog(TAG, "this first cmcc element is = " + confparser.getName());
                                if ("plmn".equals(confparser.getName())) {
                                    String carrier = confparser.getAttributeValue(null, "carrier");
                                    String mcc = confparser.getAttributeValue(null, PlmnCodeEntry.MCC_ATTR);
                                    String mnc = confparser.getAttributeValue(null, PlmnCodeEntry.MNC_ATTR);
                                    this.mPlmnIdPlmn[0][plmnPos2] = Integer.parseInt(mcc);
                                    int temp = Integer.parseInt(mnc);
                                    if (mnc.length() == 2) {
                                        temp = (-temp) - 1;
                                    }
                                    plmnPos = plmnPos2 + 1;
                                    this.mPlmnIdPlmn[1][plmnPos2] = temp;
                                    printLog(TAG, "carrier=" + carrier + "mcc=" + mcc + "mnc=" + mnc);
                                    XmlUtils.nextElement(confparser);
                                    printLog(TAG, "this last cmcc element is = " + confparser.getName());
                                } else {
                                    printLog(TAG, "this cmcc_plmn tag is not match");
                                    if (confreader2 != null) {
                                        try {
                                            confreader2.close();
                                        } catch (IOException e2) {
                                        }
                                    }
                                    return 20;
                                }
                            } catch (FileNotFoundException e3) {
                                plmnPos = plmnPos2;
                                confreader = confreader2;
                            } catch (Exception e4) {
                                plmnPos = plmnPos2;
                                confreader = confreader2;
                            } catch (Throwable th2) {
                                th = th2;
                                plmnPos = plmnPos2;
                                confreader = confreader2;
                            }
                        }
                    } else {
                        printLog(TAG, "this cmcc_plmn tag is not match");
                        if (confreader2 != null) {
                            try {
                                confreader2.close();
                            } catch (IOException e5) {
                            }
                        }
                        return 20;
                    }
                }
                if (confreader2 != null) {
                    try {
                        confreader2.close();
                    } catch (IOException e6) {
                    }
                }
                return 21;
                return 20;
                return 20;
                return 20;
                return 20;
            } catch (FileNotFoundException e7) {
                confreader = confreader2;
            } catch (Exception e8) {
                confreader = confreader2;
            } catch (Throwable th3) {
                th = th3;
                confreader = confreader2;
            }
        } catch (FileNotFoundException e9) {
            if (confreader != null) {
                try {
                    confreader.close();
                } catch (IOException e10) {
                }
            }
            return 20;
        } catch (Exception e11) {
            try {
                printLog(TAG, "getXmlFile Exception while parsing");
                if (confreader != null) {
                    try {
                        confreader.close();
                    } catch (IOException e12) {
                    }
                }
                return 20;
            } catch (Throwable th4) {
                th = th4;
                if (confreader != null) {
                    try {
                        confreader.close();
                    } catch (IOException e13) {
                    }
                }
                throw th;
            }
        }
    }

    private void setPlmnList(int[][] plmnIdList, int len, int type) {
        int i;
        int pos;
        Message msg;
        int pos2 = 6;
        int n = (len - 1) / 40;
        int m = (len - 1) % 40;
        printLog(TAG, "setPlmnlist n=" + n + "m=" + m);
        byte[] plmnsource = mccMncToByteArray(plmnIdList, len, type);
        if (n >= 1) {
            i = 0;
            while (i < n) {
                byte[] plmndata = new byte[127];
                plmndata[0] = plmnsource[0];
                plmndata[1] = plmnsource[1];
                plmndata[2] = plmnsource[2];
                plmndata[3] = plmnsource[3];
                plmndata[4] = i == 0 ? (byte) 0 : (byte) 1;
                plmndata[5] = plmnsource[5];
                plmndata[6] = i == 0 ? (byte) (len - 1) : (byte) 40;
                int j = 0;
                while (true) {
                    pos = pos2;
                    if (j >= 120) {
                        break;
                    }
                    pos2 = pos + 1;
                    plmndata[j + 7] = plmnsource[pos];
                    j++;
                }
                msg = obtainSucceedMsg();
                msg.obj = plmndata;
                sendMessage(msg);
                i++;
                pos2 = pos;
            }
        }
        byte[] plmndata2 = new byte[127];
        plmndata2[0] = plmnsource[0];
        plmndata2[1] = plmnsource[1];
        plmndata2[2] = plmnsource[2];
        plmndata2[3] = plmnsource[3];
        plmndata2[4] = n >= 1 ? (byte) 2 : (byte) 3;
        plmndata2[5] = plmnsource[5];
        plmndata2[6] = (byte) m;
        i = 0;
        while (true) {
            pos = pos2;
            if (i < m * 3) {
                pos2 = pos + 1;
                plmndata2[i + 7] = plmnsource[pos];
                i++;
            } else {
                msg = obtainSucceedMsg();
                msg.obj = plmndata2;
                sendMessage(msg);
                return;
            }
        }
    }

    private byte[] mccMncToByteArray(int[][] plmnIdList, int len, int type) {
        int[] mccDigit = new int[3];
        int[] mncDigit = new int[3];
        byte[] plmnarray = new byte[3006];
        boolean istwocodemnc = false;
        for (int i = 0; i < len; i++) {
            int plmn1 = plmnIdList[0][i];
            int plmn2 = plmnIdList[1][i];
            if (plmn2 < 0) {
                istwocodemnc = true;
                plmn2 = (-plmn2) - 1;
            }
            mccDigit[0] = plmn1 / 100;
            mccDigit[1] = (plmn1 - (mccDigit[0] * 100)) / 10;
            mccDigit[2] = (plmn1 - (mccDigit[0] * 100)) - (mccDigit[1] * 10);
            if (istwocodemnc) {
                mncDigit[0] = plmn2 / 10;
                mncDigit[1] = plmn2 - (mncDigit[0] * 10);
                mncDigit[2] = 15;
            } else {
                mncDigit[0] = plmn2 / 100;
                mncDigit[1] = (plmn2 - (mncDigit[0] * 100)) / 10;
                mncDigit[2] = (plmn2 - (mncDigit[0] * 100)) - (mncDigit[1] * 10);
            }
            if (i == 0) {
                plmnarray[0] = (byte) type;
                plmnarray[1] = (byte) ((mccDigit[1] << 4) + mccDigit[0]);
                plmnarray[2] = (byte) ((mncDigit[2] << 4) + mccDigit[2]);
                plmnarray[3] = (byte) ((mncDigit[1] << 4) + mncDigit[0]);
                plmnarray[4] = (byte) (len - 1);
                plmnarray[5] = (byte) 4;
            } else {
                plmnarray[(i * 3) + 3] = (byte) ((mccDigit[1] << 4) + mccDigit[0]);
                plmnarray[(i * 3) + 4] = (byte) ((mncDigit[2] << 4) + mccDigit[2]);
                plmnarray[(i * 3) + 5] = (byte) ((mncDigit[1] << 4) + mncDigit[0]);
            }
            istwocodemnc = false;
        }
        return plmnarray;
    }

    void printPlmnByteArray(byte[] plmnarray, int len) {
        printLog(TAG, "type=" + plmnarray[0]);
        printLog(TAG, "carrier=" + plmnarray[1] + " " + plmnarray[2] + " " + plmnarray[3]);
        printLog(TAG, "tag=" + plmnarray[4]);
        printLog(TAG, "update_type=" + plmnarray[5]);
        printLog(TAG, "length=" + plmnarray[6]);
        for (int i = 7; i < len + 7; i += 3) {
            printLog(TAG, "plmn_id=" + plmnarray[i] + " " + plmnarray[i + 1] + " " + plmnarray[i + 2]);
        }
    }
}
