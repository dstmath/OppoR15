package com.android.internal.telephony.oem.rus;

import android.net.ConnectivityManager;
import android.util.Xml;
import com.android.internal.telephony.uicc.SpnOverride;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class RusUpdateWlanAssitant extends RusBase {
    private static final String TAG = "RusUpdateWlanAssitant";
    private static final String mSubPath = "/data/data/com.android.phone/";
    private static final int mWlanAssitantIndex_count = 14;
    private static final String mWlanAssitantIndex_end = "/WlanAssitant>";
    private static final String mWlanAssitantIndex_start = "<WlanAssitant";
    private static final String mWlanAssitantInfoFileName = "wlan_assistant_DC.xml";
    private final ConnectivityManager mCm;
    private String mWlanAssitantContent = null;

    public RusUpdateWlanAssitant() {
        setPath(mSubPath);
        this.mCm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
    }

    public void execute() {
        valiateAndUpateWlanAssitant();
    }

    private void valiateAndUpateWlanAssitant() {
        this.mWlanAssitantContent = createNetworkRomupdateXmlFile(getContent(), mWlanAssitantInfoFileName, mWlanAssitantIndex_start, mWlanAssitantIndex_end, 14);
        this.mFileUtils.saveToFile(this.mWlanAssitantContent, this.mPath + mWlanAssitantInfoFileName);
        parseContentFromXML(this.mWlanAssitantContent);
    }

    public void parseContentFromXML(String content) {
        IOException e;
        XmlPullParserException e2;
        Throwable th;
        printLog(TAG, "getWlanAssitantXml:");
        if (content != null) {
            StringReader strReader = null;
            try {
                XmlPullParser parser = Xml.newPullParser();
                StringReader strReader2 = new StringReader(content);
                try {
                    parser.setInput(strReader2);
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        switch (eventType) {
                            case 2:
                                String name = parser.getName();
                                eventType = parser.next();
                                this.mCm.updateDataNetworkConfig(name, parser.getText());
                                break;
                            default:
                                break;
                        }
                    }
                    if (strReader2 != null) {
                        try {
                            strReader2.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                    }
                } catch (XmlPullParserException e4) {
                    e2 = e4;
                    strReader = strReader2;
                    try {
                        e2.printStackTrace();
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e32) {
                                e32.printStackTrace();
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e322) {
                                e322.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (IOException e5) {
                    e322 = e5;
                    strReader = strReader2;
                    e322.printStackTrace();
                    if (strReader != null) {
                        try {
                            strReader.close();
                        } catch (IOException e3222) {
                            e3222.printStackTrace();
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    strReader = strReader2;
                    if (strReader != null) {
                        try {
                            strReader.close();
                        } catch (IOException e32222) {
                            e32222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (XmlPullParserException e6) {
                e2 = e6;
                e2.printStackTrace();
                if (strReader != null) {
                    try {
                        strReader.close();
                    } catch (IOException e322222) {
                        e322222.printStackTrace();
                    }
                }
            } catch (IOException e7) {
                e322222 = e7;
                e322222.printStackTrace();
                if (strReader != null) {
                    try {
                        strReader.close();
                    } catch (IOException e3222222) {
                        e3222222.printStackTrace();
                    }
                }
            }
        }
    }

    public String readFromFile() {
        IOException e;
        FileNotFoundException e2;
        Throwable th;
        File path = new File(this.mPath + mWlanAssitantInfoFileName);
        if (!path.exists()) {
            printLog(TAG, this.mPath + mWlanAssitantInfoFileName + " not exist!");
        }
        InputStream is = null;
        try {
            InputStream is2 = new FileInputStream(path);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(is2));
                StringBuffer buffer = new StringBuffer();
                String str = SpnOverride.MVNO_TYPE_NONE;
                while (true) {
                    str = in.readLine();
                    if (str == null) {
                        break;
                    }
                    buffer.append(str);
                }
                String stringBuffer = buffer.toString();
                if (is2 != null) {
                    try {
                        is2.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                return stringBuffer;
            } catch (FileNotFoundException e4) {
                e2 = e4;
                is = is2;
            } catch (IOException e5) {
                e3 = e5;
                is = is2;
                e3.printStackTrace();
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e32) {
                        e32.printStackTrace();
                    }
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                is = is2;
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e322) {
                        e322.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e6) {
            e2 = e6;
            try {
                e2.printStackTrace();
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e3222) {
                        e3222.printStackTrace();
                    }
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e32222) {
                        e32222.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e7) {
            e32222 = e7;
            e32222.printStackTrace();
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e322222) {
                    e322222.printStackTrace();
                }
            }
            return null;
        }
    }
}
