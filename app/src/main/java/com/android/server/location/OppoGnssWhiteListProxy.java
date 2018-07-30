package com.android.server.location;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.oppo.RomUpdateHelper;
import com.oppo.RomUpdateHelper.UpdateInfo;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class OppoGnssWhiteListProxy extends RomUpdateHelper {
    private static final String ATTR_ALL_CHINA_WITHOUT_GMS = "all_china_without_gms";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NAVIGATION_MAP_ALWAY_ON = "navigation_map_alway_on";
    private static final String ATTR_NAVIGATION_MAP_WHITE_LIST = "default_navigation_map_whitelist";
    private static final String ATTR_NETWORK_LOCATION_ALWAY_ON = "networklocation_alway_on";
    private static final String ATTR_NETWORK_LOCATION_WHITE_LIST = "networklocation_whitelist";
    private static final String ATTR_WITHOUT_GMS_WHITE_LIST = "without_gms_country_whitelist";
    private static final String DATA_FILE_PATH = "/data/system/oppo_gnss_whitelist.xml";
    private static final String FILE_NAME = "oppo_gnss_whitelist";
    private static final String SWITCH_OFF = "off";
    private static final String SWITCH_ON = "on";
    private static final String TAG = "OppoGnssWhiteList";
    private static final String TAG_ITEM = "item";
    private static final String TAG_STRING = "string";
    private static final String TAG_STRING_ARRAY = "string-array";
    private static final String TAG_VERSION = "version";
    private static OppoGnssWhiteListProxy mInstall = null;
    private boolean mAllChinaWithoutGms;
    private Context mContext = null;
    private ArrayList<String> mCurrList = null;
    private boolean mIsDebug = false;
    private boolean mIsNavigationMapAlwayOn;
    private boolean mIsNetworkLocationAlwayOn;
    private ArrayList<String> mNavigationMapWhiteList;
    private ArrayList<String> mNetworkLocationWhiteList;
    private int mVersion;
    private ArrayList<String> mWithoutCountryWhiteList;

    public class GnssRomUpdateInfo extends UpdateInfo {
        public /* bridge */ /* synthetic */ void clear() {
            super.clear();
        }

        public /* bridge */ /* synthetic */ boolean clone(UpdateInfo updateInfo) {
            return super.clone(updateInfo);
        }

        public /* bridge */ /* synthetic */ void dump() {
            super.dump();
        }

        public /* bridge */ /* synthetic */ long getVersion() {
            return super.getVersion();
        }

        public /* bridge */ /* synthetic */ boolean insert(int i, String str) {
            return super.insert(i, str);
        }

        public /* bridge */ /* synthetic */ boolean updateToLowerVersion(String str) {
            return super.updateToLowerVersion(str);
        }

        public GnssRomUpdateInfo() {
            super(OppoGnssWhiteListProxy.this);
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void parseContentFromXML(String content) {
            if (content == null) {
                Log.e(OppoGnssWhiteListProxy.TAG, "parse content is null");
            }
            int version = 0;
            try {
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                if (!TextUtils.isEmpty(content)) {
                    parser.setInput(new StringReader(content));
                }
                parser.nextTag();
                for (int evenType = parser.getEventType(); evenType != 1; evenType = parser.next()) {
                    if (2 == evenType) {
                        String tagName = parser.getName();
                        String name;
                        if ("version".equals(tagName)) {
                            try {
                                version = Integer.valueOf(parser.nextText()).intValue();
                            } catch (NumberFormatException e) {
                                Log.e(OppoGnssWhiteListProxy.TAG, "Get An Error When Parsing Version From XML File!");
                            }
                            if (this.mVersion > ((long) version)) {
                                Log.e(OppoGnssWhiteListProxy.TAG, "Version is old, Don't need update anything from the xml file!");
                                break;
                            }
                            this.mVersion = (long) version;
                        } else if (OppoGnssWhiteListProxy.TAG_STRING.equals(tagName)) {
                            name = parser.getAttributeValue(null, OppoGnssWhiteListProxy.ATTR_NAME);
                            String value = parser.nextText();
                            if (OppoGnssWhiteListProxy.ATTR_NETWORK_LOCATION_ALWAY_ON.equals(name)) {
                                OppoGnssWhiteListProxy.this.setNetworkLocationAlwayOn(value);
                            } else if (OppoGnssWhiteListProxy.ATTR_NAVIGATION_MAP_ALWAY_ON.equals(name)) {
                                OppoGnssWhiteListProxy.this.setNavigationMapAlwayOn(value);
                            } else if (OppoGnssWhiteListProxy.ATTR_ALL_CHINA_WITHOUT_GMS.equals(name)) {
                                OppoGnssWhiteListProxy.this.setAllChinaWithoutGms(value);
                            }
                        } else if (OppoGnssWhiteListProxy.TAG_STRING_ARRAY.equals(tagName)) {
                            name = parser.getAttributeValue(null, OppoGnssWhiteListProxy.ATTR_NAME);
                            if (OppoGnssWhiteListProxy.ATTR_NETWORK_LOCATION_WHITE_LIST.equals(name)) {
                                OppoGnssWhiteListProxy.this.mCurrList = OppoGnssWhiteListProxy.this.mNetworkLocationWhiteList;
                            } else if (OppoGnssWhiteListProxy.ATTR_NAVIGATION_MAP_WHITE_LIST.equals(name)) {
                                OppoGnssWhiteListProxy.this.mCurrList = OppoGnssWhiteListProxy.this.mNavigationMapWhiteList;
                            } else if (OppoGnssWhiteListProxy.ATTR_WITHOUT_GMS_WHITE_LIST.equals(name)) {
                                OppoGnssWhiteListProxy.this.mCurrList = OppoGnssWhiteListProxy.this.mWithoutCountryWhiteList;
                            }
                        } else if (OppoGnssWhiteListProxy.TAG_ITEM.equals(tagName)) {
                            OppoGnssWhiteListProxy.this.insertWhiteList(parser.nextText());
                        }
                    }
                }
                if (OppoGnssWhiteListProxy.this.mIsDebug) {
                    Log.d(OppoGnssWhiteListProxy.TAG, "Parse gnss content done!");
                }
            } catch (XmlPullParserException e2) {
                Log.e(OppoGnssWhiteListProxy.TAG, "Got XmlPullParser exception parsing!");
                if (OppoGnssWhiteListProxy.this.mIsDebug) {
                    Log.d(OppoGnssWhiteListProxy.TAG, "Parse gnss content done!");
                }
            } catch (IOException e3) {
                Log.e(OppoGnssWhiteListProxy.TAG, "Got IO exception parsing!!");
                if (OppoGnssWhiteListProxy.this.mIsDebug) {
                    Log.d(OppoGnssWhiteListProxy.TAG, "Parse gnss content done!");
                }
            } catch (Throwable th) {
                if (OppoGnssWhiteListProxy.this.mIsDebug) {
                    Log.d(OppoGnssWhiteListProxy.TAG, "Parse gnss content done!");
                }
            }
        }
    }

    private OppoGnssWhiteListProxy(Context context) {
        super(context, FILE_NAME, null, DATA_FILE_PATH);
        initValues();
        setUpdateInfo(new GnssRomUpdateInfo(), new GnssRomUpdateInfo());
    }

    public static OppoGnssWhiteListProxy getInstall(Context context) {
        if (mInstall == null) {
            mInstall = new OppoGnssWhiteListProxy(context);
        }
        return mInstall;
    }

    public void setIsDebug(boolean isDebug) {
        this.mIsDebug = isDebug;
    }

    private void initValues() {
        this.mVersion = 20170120;
        this.mIsNetworkLocationAlwayOn = true;
        this.mNetworkLocationWhiteList = new ArrayList();
        this.mNetworkLocationWhiteList.add("com.coloros.weather");
        this.mNetworkLocationWhiteList.add("com.coloros.weather.service");
        this.mNetworkLocationWhiteList.add("com.coloros.speechassist.engine");
        this.mNetworkLocationWhiteList.add("com.qualcomm.location");
        this.mNetworkLocationWhiteList.add("com.ted.number");
        this.mNetworkLocationWhiteList.add("com.android.mms");
        this.mNetworkLocationWhiteList.add("com.coloros.assistantscreen");
        this.mNetworkLocationWhiteList.add("com.coloros.colordirectservice");
        this.mIsNavigationMapAlwayOn = true;
        this.mNavigationMapWhiteList = new ArrayList();
        this.mNavigationMapWhiteList.add("com.baidu.BaiduMap");
        this.mNavigationMapWhiteList.add("com.baidu.location.fused");
        this.mNavigationMapWhiteList.add("com.autonavi.minimap");
        this.mNavigationMapWhiteList.add("com.amap.android.ams");
        this.mNavigationMapWhiteList.add("com.google.android.apps.maps");
        this.mNavigationMapWhiteList.add("com.sogou.android.maps");
        this.mNavigationMapWhiteList.add("com.tencent.map");
        this.mNavigationMapWhiteList.add("com.tencent.android.location");
        this.mAllChinaWithoutGms = true;
        this.mWithoutCountryWhiteList = new ArrayList();
        this.mWithoutCountryWhiteList.add("460");
        this.mWithoutCountryWhiteList.add("454");
        this.mWithoutCountryWhiteList.add("455");
    }

    private void setNetworkLocationAlwayOn(String onOff) {
        if (onOff.equals(SWITCH_ON)) {
            this.mIsNetworkLocationAlwayOn = true;
        } else if (onOff.equals(SWITCH_OFF)) {
            this.mIsNetworkLocationAlwayOn = false;
        }
    }

    public boolean isNetworkLocationAlwayOn() {
        return this.mIsNetworkLocationAlwayOn;
    }

    private void setNavigationMapAlwayOn(String onOff) {
        if (onOff.equals(SWITCH_ON)) {
            this.mIsNavigationMapAlwayOn = true;
        } else if (onOff.equals(SWITCH_OFF)) {
            this.mIsNavigationMapAlwayOn = false;
        }
    }

    public boolean isNavigationMapAlwayOn() {
        return this.mIsNavigationMapAlwayOn;
    }

    private void setAllChinaWithoutGms(String onOff) {
        if (onOff.equals(SWITCH_ON)) {
            this.mAllChinaWithoutGms = true;
        } else if (onOff.equals(SWITCH_OFF)) {
            this.mAllChinaWithoutGms = false;
        }
    }

    private void insertWhiteList(String packageName) {
        if (!(this.mCurrList == null || this.mCurrList.contains(packageName))) {
            this.mCurrList.add(packageName);
        }
    }

    public boolean inNetworkLocationWhiteList(String packageName) {
        if (!this.mIsNetworkLocationAlwayOn || packageName == null) {
            return false;
        }
        return this.mNetworkLocationWhiteList.contains(packageName);
    }

    public boolean inNavigationMapWhiteList(String packageName) {
        if (!this.mIsNavigationMapAlwayOn || packageName == null) {
            return false;
        }
        return this.mNavigationMapWhiteList.contains(packageName);
    }

    public boolean inWithoutGmsContryList(String mcc) {
        if (mcc == null || mcc.length() < 3) {
            return false;
        }
        String strMcc = mcc.substring(0, 3);
        if (strMcc.equals("460")) {
            return true;
        }
        if (this.mAllChinaWithoutGms) {
            return this.mWithoutCountryWhiteList.contains(strMcc);
        }
        return false;
    }
}
