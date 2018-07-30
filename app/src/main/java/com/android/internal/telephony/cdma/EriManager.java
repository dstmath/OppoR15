package com.android.internal.telephony.cdma;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.util.Xml;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.SpnOverride;
import com.android.internal.util.XmlUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class EriManager {
    private static final boolean DBG = true;
    static final int ERI_FROM_FILE_SYSTEM = 1;
    static final int ERI_FROM_MODEM = 2;
    public static final int ERI_FROM_XML = 0;
    private static final String LOG_TAG = "EriManager";
    private static final boolean VDBG = false;
    private Context mContext;
    private EriFile mEriFile;
    private int mEriFileSource = 0;
    private boolean mIsEriFileLoaded;
    private final Phone mPhone;

    class EriDisplayInformation {
        int mEriIconIndex;
        int mEriIconMode;
        String mEriIconText;

        EriDisplayInformation(int eriIconIndex, int eriIconMode, String eriIconText) {
            this.mEriIconIndex = eriIconIndex;
            this.mEriIconMode = eriIconMode;
            this.mEriIconText = eriIconText;
        }

        public String toString() {
            return "EriDisplayInformation: { IconIndex: " + this.mEriIconIndex + " EriIconMode: " + this.mEriIconMode + " EriIconText: " + this.mEriIconText + " }";
        }
    }

    class EriFile {
        String[] mCallPromptId = new String[]{SpnOverride.MVNO_TYPE_NONE, SpnOverride.MVNO_TYPE_NONE, SpnOverride.MVNO_TYPE_NONE};
        int mEriFileType = -1;
        int mNumberOfEriEntries = 0;
        HashMap<Integer, EriInfo> mRoamIndTable = new HashMap();
        int mVersionNumber = -1;

        EriFile() {
        }
    }

    public EriManager(Phone phone, Context context, int eriFileSource) {
        this.mPhone = phone;
        this.mContext = context;
        this.mEriFileSource = eriFileSource;
        this.mEriFile = new EriFile();
    }

    public void dispose() {
        this.mEriFile = new EriFile();
        this.mIsEriFileLoaded = false;
    }

    public void loadEriFile() {
        switch (this.mEriFileSource) {
            case 1:
                loadEriFileFromFileSystem();
                return;
            case 2:
                loadEriFileFromModem();
                return;
            default:
                loadEriFileFromXml();
                return;
        }
    }

    private void loadEriFileFromModem() {
    }

    private void loadEriFileFromFileSystem() {
    }

    private void loadEriFileFromXml() {
        XmlPullParser parser;
        InputStream stream;
        int parsedEriEntries;
        FileInputStream stream2 = null;
        Resources r = this.mContext.getResources();
        try {
            Rlog.d(LOG_TAG, "loadEriFileFromXml: check for alternate file");
            InputStream fileInputStream = new FileInputStream(r.getString(17039478));
            try {
                parser = Xml.newPullParser();
                parser.setInput(fileInputStream, null);
                Rlog.d(LOG_TAG, "loadEriFileFromXml: opened alternate file");
                stream2 = fileInputStream;
            } catch (FileNotFoundException e) {
                stream2 = fileInputStream;
                Rlog.d(LOG_TAG, "loadEriFileFromXml: no alternate file");
                parser = null;
                if (parser == null) {
                    String eriFile = null;
                    CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
                    if (configManager != null) {
                        PersistableBundle b = configManager.getConfigForSubId(this.mPhone.getSubId());
                        if (b != null) {
                            eriFile = b.getString("carrier_eri_file_name_string");
                        }
                    }
                    Rlog.d(LOG_TAG, "eriFile = " + eriFile);
                    if (eriFile == null) {
                        Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                        return;
                    }
                    try {
                        parser = Xml.newPullParser();
                        parser.setInput(this.mContext.getAssets().open(eriFile), null);
                    } catch (Exception e2) {
                        Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + eriFile + ". Exception = " + e2.toString());
                    }
                }
                XmlUtils.beginDocument(parser, "EriFile");
                this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
                this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
                this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
                parsedEriEntries = 0;
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name = parser.getName();
                    if (name == null) {
                        if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
                            Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + parsedEriEntries + " parsed!");
                        }
                        Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
                        this.mIsEriFileLoaded = true;
                        if (parser instanceof XmlResourceParser) {
                            ((XmlResourceParser) parser).close();
                        }
                        if (stream2 != null) {
                            try {
                                stream2.close();
                            } catch (IOException e3) {
                            }
                        }
                    } else {
                        if (name.equals("CallPromptId")) {
                            int id = Integer.parseInt(parser.getAttributeValue(null, "Id"));
                            String text = parser.getAttributeValue(null, "CallPromptText");
                            if (id < 0 || id > 2) {
                                Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + id + " CallPromptId");
                            } else {
                                this.mEriFile.mCallPromptId[id] = text;
                            }
                        } else {
                            if (name.equals("EriInfo")) {
                                int roamingIndicator = Integer.parseInt(parser.getAttributeValue(null, "RoamingIndicator"));
                                int iconIndex = Integer.parseInt(parser.getAttributeValue(null, "IconIndex"));
                                int iconMode = Integer.parseInt(parser.getAttributeValue(null, "IconMode"));
                                String eriText = parser.getAttributeValue(null, "EriText");
                                int callPromptId = Integer.parseInt(parser.getAttributeValue(null, "CallPromptId"));
                                int alertId = Integer.parseInt(parser.getAttributeValue(null, "AlertId"));
                                parsedEriEntries++;
                                HashMap hashMap = this.mEriFile.mRoamIndTable;
                                hashMap.put(Integer.valueOf(roamingIndicator), new EriInfo(roamingIndicator, iconIndex, iconMode, eriText, callPromptId, alertId));
                            }
                        }
                    }
                }
            } catch (XmlPullParserException e4) {
                stream2 = fileInputStream;
                Rlog.d(LOG_TAG, "loadEriFileFromXml: no parser for alternate file");
                parser = null;
                if (parser == null) {
                    String eriFile2 = null;
                    CarrierConfigManager configManager2 = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
                    if (configManager2 != null) {
                        PersistableBundle b2 = configManager2.getConfigForSubId(this.mPhone.getSubId());
                        if (b2 != null) {
                            eriFile2 = b2.getString("carrier_eri_file_name_string");
                        }
                    }
                    Rlog.d(LOG_TAG, "eriFile = " + eriFile2);
                    if (eriFile2 == null) {
                        Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                        return;
                    }
                    try {
                        parser = Xml.newPullParser();
                        parser.setInput(this.mContext.getAssets().open(eriFile2), null);
                    } catch (Exception e22) {
                        Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + eriFile2 + ". Exception = " + e22.toString());
                    }
                }
                XmlUtils.beginDocument(parser, "EriFile");
                this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
                this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
                this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
                parsedEriEntries = 0;
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name2 = parser.getName();
                    if (name2 == null) {
                        if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
                            Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + parsedEriEntries + " parsed!");
                        }
                        Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
                        this.mIsEriFileLoaded = true;
                        if (parser instanceof XmlResourceParser) {
                            ((XmlResourceParser) parser).close();
                        }
                        if (stream2 != null) {
                            try {
                                stream2.close();
                            } catch (IOException e32) {
                            }
                        }
                    } else {
                        if (name2.equals("CallPromptId")) {
                            int id2 = Integer.parseInt(parser.getAttributeValue(null, "Id"));
                            String text2 = parser.getAttributeValue(null, "CallPromptText");
                            if (id2 < 0 || id2 > 2) {
                                Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + id2 + " CallPromptId");
                            } else {
                                this.mEriFile.mCallPromptId[id2] = text2;
                            }
                        } else {
                            if (name2.equals("EriInfo")) {
                                int roamingIndicator2 = Integer.parseInt(parser.getAttributeValue(null, "RoamingIndicator"));
                                int iconIndex2 = Integer.parseInt(parser.getAttributeValue(null, "IconIndex"));
                                int iconMode2 = Integer.parseInt(parser.getAttributeValue(null, "IconMode"));
                                String eriText2 = parser.getAttributeValue(null, "EriText");
                                int callPromptId2 = Integer.parseInt(parser.getAttributeValue(null, "CallPromptId"));
                                int alertId2 = Integer.parseInt(parser.getAttributeValue(null, "AlertId"));
                                parsedEriEntries++;
                                HashMap hashMap2 = this.mEriFile.mRoamIndTable;
                                hashMap2.put(Integer.valueOf(roamingIndicator2), new EriInfo(roamingIndicator2, iconIndex2, iconMode2, eriText2, callPromptId2, alertId2));
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e5) {
            Rlog.d(LOG_TAG, "loadEriFileFromXml: no alternate file");
            parser = null;
            if (parser == null) {
                String eriFile22 = null;
                CarrierConfigManager configManager22 = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
                if (configManager22 != null) {
                    PersistableBundle b22 = configManager22.getConfigForSubId(this.mPhone.getSubId());
                    if (b22 != null) {
                        eriFile22 = b22.getString("carrier_eri_file_name_string");
                    }
                }
                Rlog.d(LOG_TAG, "eriFile = " + eriFile22);
                if (eriFile22 == null) {
                    Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                    return;
                }
                try {
                    parser = Xml.newPullParser();
                    parser.setInput(this.mContext.getAssets().open(eriFile22), null);
                } catch (Exception e222) {
                    Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + eriFile22 + ". Exception = " + e222.toString());
                }
            }
            XmlUtils.beginDocument(parser, "EriFile");
            this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
            this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
            this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
            parsedEriEntries = 0;
            while (true) {
                XmlUtils.nextElement(parser);
                String name22 = parser.getName();
                if (name22 == null) {
                    if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
                        Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + parsedEriEntries + " parsed!");
                    }
                    Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
                    this.mIsEriFileLoaded = true;
                    if (parser instanceof XmlResourceParser) {
                        ((XmlResourceParser) parser).close();
                    }
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e322) {
                        }
                    }
                } else {
                    if (name22.equals("CallPromptId")) {
                        int id22 = Integer.parseInt(parser.getAttributeValue(null, "Id"));
                        String text22 = parser.getAttributeValue(null, "CallPromptText");
                        if (id22 < 0 || id22 > 2) {
                            Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + id22 + " CallPromptId");
                        } else {
                            this.mEriFile.mCallPromptId[id22] = text22;
                        }
                    } else {
                        if (name22.equals("EriInfo")) {
                            int roamingIndicator22 = Integer.parseInt(parser.getAttributeValue(null, "RoamingIndicator"));
                            int iconIndex22 = Integer.parseInt(parser.getAttributeValue(null, "IconIndex"));
                            int iconMode22 = Integer.parseInt(parser.getAttributeValue(null, "IconMode"));
                            String eriText22 = parser.getAttributeValue(null, "EriText");
                            int callPromptId22 = Integer.parseInt(parser.getAttributeValue(null, "CallPromptId"));
                            int alertId22 = Integer.parseInt(parser.getAttributeValue(null, "AlertId"));
                            parsedEriEntries++;
                            HashMap hashMap22 = this.mEriFile.mRoamIndTable;
                            hashMap22.put(Integer.valueOf(roamingIndicator22), new EriInfo(roamingIndicator22, iconIndex22, iconMode22, eriText22, callPromptId22, alertId22));
                        }
                    }
                }
            }
        } catch (XmlPullParserException e6) {
            Rlog.d(LOG_TAG, "loadEriFileFromXml: no parser for alternate file");
            parser = null;
            if (parser == null) {
                String eriFile222 = null;
                CarrierConfigManager configManager222 = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
                if (configManager222 != null) {
                    PersistableBundle b222 = configManager222.getConfigForSubId(this.mPhone.getSubId());
                    if (b222 != null) {
                        eriFile222 = b222.getString("carrier_eri_file_name_string");
                    }
                }
                Rlog.d(LOG_TAG, "eriFile = " + eriFile222);
                if (eriFile222 == null) {
                    Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                    return;
                }
                try {
                    parser = Xml.newPullParser();
                    parser.setInput(this.mContext.getAssets().open(eriFile222), null);
                } catch (Exception e2222) {
                    Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + eriFile222 + ". Exception = " + e2222.toString());
                }
            }
            XmlUtils.beginDocument(parser, "EriFile");
            this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
            this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
            this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
            parsedEriEntries = 0;
            while (true) {
                XmlUtils.nextElement(parser);
                String name222 = parser.getName();
                if (name222 == null) {
                    if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
                        Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + parsedEriEntries + " parsed!");
                    }
                    Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
                    this.mIsEriFileLoaded = true;
                    if (parser instanceof XmlResourceParser) {
                        ((XmlResourceParser) parser).close();
                    }
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e3222) {
                        }
                    }
                } else {
                    if (name222.equals("CallPromptId")) {
                        int id222 = Integer.parseInt(parser.getAttributeValue(null, "Id"));
                        String text222 = parser.getAttributeValue(null, "CallPromptText");
                        if (id222 < 0 || id222 > 2) {
                            Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + id222 + " CallPromptId");
                        } else {
                            this.mEriFile.mCallPromptId[id222] = text222;
                        }
                    } else {
                        if (name222.equals("EriInfo")) {
                            int roamingIndicator222 = Integer.parseInt(parser.getAttributeValue(null, "RoamingIndicator"));
                            int iconIndex222 = Integer.parseInt(parser.getAttributeValue(null, "IconIndex"));
                            int iconMode222 = Integer.parseInt(parser.getAttributeValue(null, "IconMode"));
                            String eriText222 = parser.getAttributeValue(null, "EriText");
                            int callPromptId222 = Integer.parseInt(parser.getAttributeValue(null, "CallPromptId"));
                            int alertId222 = Integer.parseInt(parser.getAttributeValue(null, "AlertId"));
                            parsedEriEntries++;
                            HashMap hashMap222 = this.mEriFile.mRoamIndTable;
                            hashMap222.put(Integer.valueOf(roamingIndicator222), new EriInfo(roamingIndicator222, iconIndex222, iconMode222, eriText222, callPromptId222, alertId222));
                        }
                    }
                }
            }
        }
        if (parser == null) {
            String eriFile2222 = null;
            CarrierConfigManager configManager2222 = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
            if (configManager2222 != null) {
                PersistableBundle b2222 = configManager2222.getConfigForSubId(this.mPhone.getSubId());
                if (b2222 != null) {
                    eriFile2222 = b2222.getString("carrier_eri_file_name_string");
                }
            }
            Rlog.d(LOG_TAG, "eriFile = " + eriFile2222);
            if (eriFile2222 == null) {
                Rlog.e(LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                return;
            }
            try {
                parser = Xml.newPullParser();
                parser.setInput(this.mContext.getAssets().open(eriFile2222), null);
            } catch (Exception e22222) {
                Rlog.e(LOG_TAG, "loadEriFileFromXml: no parser for " + eriFile2222 + ". Exception = " + e22222.toString());
            }
        }
        try {
            XmlUtils.beginDocument(parser, "EriFile");
            this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
            this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
            this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
            parsedEriEntries = 0;
            while (true) {
                XmlUtils.nextElement(parser);
                String name2222 = parser.getName();
                if (name2222 == null) {
                    if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
                        Rlog.e(LOG_TAG, "Error Parsing ERI file: " + this.mEriFile.mNumberOfEriEntries + " defined, " + parsedEriEntries + " parsed!");
                    }
                    Rlog.d(LOG_TAG, "loadEriFileFromXml: eri parsing successful, file loaded. ver = " + this.mEriFile.mVersionNumber + ", # of entries = " + this.mEriFile.mNumberOfEriEntries);
                    this.mIsEriFileLoaded = true;
                    if (parser instanceof XmlResourceParser) {
                        ((XmlResourceParser) parser).close();
                    }
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e32222) {
                        }
                    }
                } else {
                    if (name2222.equals("CallPromptId")) {
                        int id2222 = Integer.parseInt(parser.getAttributeValue(null, "Id"));
                        String text2222 = parser.getAttributeValue(null, "CallPromptText");
                        if (id2222 < 0 || id2222 > 2) {
                            Rlog.e(LOG_TAG, "Error Parsing ERI file: found" + id2222 + " CallPromptId");
                        } else {
                            this.mEriFile.mCallPromptId[id2222] = text2222;
                        }
                    } else {
                        if (name2222.equals("EriInfo")) {
                            int roamingIndicator2222 = Integer.parseInt(parser.getAttributeValue(null, "RoamingIndicator"));
                            int iconIndex2222 = Integer.parseInt(parser.getAttributeValue(null, "IconIndex"));
                            int iconMode2222 = Integer.parseInt(parser.getAttributeValue(null, "IconMode"));
                            String eriText2222 = parser.getAttributeValue(null, "EriText");
                            int callPromptId2222 = Integer.parseInt(parser.getAttributeValue(null, "CallPromptId"));
                            int alertId2222 = Integer.parseInt(parser.getAttributeValue(null, "AlertId"));
                            parsedEriEntries++;
                            HashMap hashMap2222 = this.mEriFile.mRoamIndTable;
                            hashMap2222.put(Integer.valueOf(roamingIndicator2222), new EriInfo(roamingIndicator2222, iconIndex2222, iconMode2222, eriText2222, callPromptId2222, alertId2222));
                        }
                    }
                }
            }
        } catch (Exception e222222) {
            Rlog.e(LOG_TAG, "Got exception while loading ERI file.", e222222);
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser) parser).close();
            }
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e7) {
                }
            }
        } catch (Throwable th) {
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser) parser).close();
            }
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e8) {
                }
            }
        }
    }

    public int getEriFileVersion() {
        return this.mEriFile.mVersionNumber;
    }

    public int getEriNumberOfEntries() {
        return this.mEriFile.mNumberOfEriEntries;
    }

    public int getEriFileType() {
        return this.mEriFile.mEriFileType;
    }

    public boolean isEriFileLoaded() {
        return this.mIsEriFileLoaded;
    }

    private EriInfo getEriInfo(int roamingIndicator) {
        if (this.mEriFile.mRoamIndTable.containsKey(Integer.valueOf(roamingIndicator))) {
            return (EriInfo) this.mEriFile.mRoamIndTable.get(Integer.valueOf(roamingIndicator));
        }
        return null;
    }

    private EriDisplayInformation getEriDisplayInformation(int roamInd, int defRoamInd) {
        EriInfo eriInfo;
        EriDisplayInformation ret;
        if (this.mIsEriFileLoaded) {
            eriInfo = getEriInfo(roamInd);
            if (eriInfo != null) {
                return new EriDisplayInformation(eriInfo.iconIndex, eriInfo.iconMode, eriInfo.eriText);
            }
        }
        switch (roamInd) {
            case 0:
                ret = new EriDisplayInformation(0, 0, this.mContext.getText(17040788).toString());
                break;
            case 1:
                ret = new EriDisplayInformation(1, 0, this.mContext.getText(17040789).toString());
                break;
            case 2:
                ret = new EriDisplayInformation(2, 1, this.mContext.getText(17040793).toString());
                break;
            case 3:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040794).toString());
                break;
            case 4:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040795).toString());
                break;
            case 5:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040796).toString());
                break;
            case 6:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040797).toString());
                break;
            case 7:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040798).toString());
                break;
            case 8:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040799).toString());
                break;
            case 9:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040800).toString());
                break;
            case 10:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040790).toString());
                break;
            case 11:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040791).toString());
                break;
            case 12:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17040792).toString());
                break;
            default:
                if (!this.mIsEriFileLoaded) {
                    Rlog.d(LOG_TAG, "ERI File not loaded");
                    if (defRoamInd <= 2) {
                        switch (defRoamInd) {
                            case 0:
                                ret = new EriDisplayInformation(0, 0, this.mContext.getText(17040788).toString());
                                break;
                            case 1:
                                ret = new EriDisplayInformation(1, 0, this.mContext.getText(17040789).toString());
                                break;
                            case 2:
                                ret = new EriDisplayInformation(2, 1, this.mContext.getText(17040793).toString());
                                break;
                            default:
                                ret = new EriDisplayInformation(-1, -1, "ERI text");
                                break;
                        }
                    }
                    ret = new EriDisplayInformation(2, 1, this.mContext.getText(17040793).toString());
                    break;
                }
                eriInfo = getEriInfo(roamInd);
                EriInfo defEriInfo = getEriInfo(defRoamInd);
                if (eriInfo == null) {
                    if (defEriInfo != null) {
                        ret = new EriDisplayInformation(defEriInfo.iconIndex, defEriInfo.iconMode, defEriInfo.eriText);
                        break;
                    }
                    Rlog.e(LOG_TAG, "ERI defRoamInd " + defRoamInd + " not found in ERI file ...on");
                    ret = new EriDisplayInformation(0, 0, this.mContext.getText(17040788).toString());
                    break;
                }
                ret = new EriDisplayInformation(eriInfo.iconIndex, eriInfo.iconMode, eriInfo.eriText);
                break;
        }
        return ret;
    }

    public int getCdmaEriIconIndex(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconIndex;
    }

    public int getCdmaEriIconMode(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconMode;
    }

    public String getCdmaEriText(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconText;
    }
}
