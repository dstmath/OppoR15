package com.oppo.luckymoney;

import android.content.Context;
import android.util.Xml;
import com.oppo.RomUpdateHelper;
import java.io.IOException;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LuckyMoneyHelper extends RomUpdateHelper {
    private static final String DATA_FILE_DIR = "data/system/sys_luckymoney_config_list.xml";
    public static final String FILTER_NAME = "sys_luckymoney_config_list";
    private static final String SYS_FILE_DIR = "system/etc/sys_luckymoney_config_list.xml";

    private class LuckyMoneyUpdateInfo extends UpdateInfo {
        static final String DELAYTIMEOUT = "delayTimeout";
        static final String KEYDNSINFO = "keyDNSinfo";
        static final String KEYURLINFO = "keyURLinfo";
        static final String QMSG = "Qmsg";
        static final String QTAG = "Qtag";
        int delayTimeout = 0;
        String keyDNSInfo = null;
        String keyURLInfo = null;
        String qmsgInfo = null;
        String qtagInfo = null;

        public LuckyMoneyUpdateInfo() {
            super();
        }

        public void parseContentFromXML(String content) {
            IOException e;
            XmlPullParserException e2;
            Throwable th;
            if (content != null) {
                StringReader stringReader = null;
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    StringReader strReader = new StringReader(content);
                    try {
                        parser.setInput(strReader);
                        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                            switch (eventType) {
                                case 2:
                                    String tmp = parser.getName();
                                    if (!KEYURLINFO.equals(tmp)) {
                                        if (!KEYDNSINFO.equals(tmp)) {
                                            if (!DELAYTIMEOUT.equals(tmp)) {
                                                if (!QTAG.equals(tmp)) {
                                                    if (!QMSG.equals(tmp)) {
                                                        break;
                                                    }
                                                    eventType = parser.next();
                                                    this.qmsgInfo = parser.getText();
                                                    break;
                                                }
                                                eventType = parser.next();
                                                this.qtagInfo = parser.getText();
                                                break;
                                            }
                                            eventType = parser.next();
                                            try {
                                                this.delayTimeout = Integer.parseInt(parser.getText());
                                                break;
                                            } catch (RuntimeException e3) {
                                                break;
                                            }
                                        }
                                        eventType = parser.next();
                                        this.keyDNSInfo = parser.getText();
                                        break;
                                    }
                                    eventType = parser.next();
                                    this.keyURLInfo = parser.getText();
                                    break;
                                default:
                                    break;
                            }
                        }
                        if (strReader != null) {
                            try {
                                strReader.close();
                            } catch (IOException e4) {
                                LuckyMoneyHelper.this.log("Got execption close permReader.", e4);
                            }
                        }
                    } catch (XmlPullParserException e5) {
                        e2 = e5;
                        stringReader = strReader;
                        try {
                            LuckyMoneyHelper.this.log("Got execption parsing permissions.", e2);
                            if (stringReader != null) {
                                try {
                                    stringReader.close();
                                } catch (IOException e42) {
                                    LuckyMoneyHelper.this.log("Got execption close permReader.", e42);
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            if (stringReader != null) {
                                try {
                                    stringReader.close();
                                } catch (IOException e422) {
                                    LuckyMoneyHelper.this.log("Got execption close permReader.", e422);
                                }
                            }
                            throw th;
                        }
                    } catch (IOException e6) {
                        e422 = e6;
                        stringReader = strReader;
                        LuckyMoneyHelper.this.log("Got execption parsing permissions.", e422);
                        if (stringReader != null) {
                            try {
                                stringReader.close();
                            } catch (IOException e4222) {
                                LuckyMoneyHelper.this.log("Got execption close permReader.", e4222);
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        stringReader = strReader;
                        if (stringReader != null) {
                            try {
                                stringReader.close();
                            } catch (IOException e42222) {
                                LuckyMoneyHelper.this.log("Got execption close permReader.", e42222);
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e7) {
                    e2 = e7;
                    LuckyMoneyHelper.this.log("Got execption parsing permissions.", e2);
                    if (stringReader != null) {
                        try {
                            stringReader.close();
                        } catch (IOException e422222) {
                            LuckyMoneyHelper.this.log("Got execption close permReader.", e422222);
                        }
                    }
                } catch (IOException e8) {
                    e422222 = e8;
                    LuckyMoneyHelper.this.log("Got execption parsing permissions.", e422222);
                    if (stringReader != null) {
                        try {
                            stringReader.close();
                        } catch (IOException e4222222) {
                            LuckyMoneyHelper.this.log("Got execption close permReader.", e4222222);
                        }
                    }
                }
            }
        }

        public int getDelayTimeout() {
            return this.delayTimeout;
        }

        public String getKeyURLInfo() {
            return this.keyURLInfo;
        }

        public String getkeyDNSInfo() {
            return this.keyDNSInfo;
        }

        public String getLuckyMoneyInfo(int type) {
            switch (type) {
                case 0:
                    return this.keyURLInfo;
                case 1:
                    return this.keyDNSInfo;
                case 2:
                    return this.qtagInfo;
                case 3:
                    return this.qmsgInfo;
                default:
                    return "";
            }
        }

        public String dumpToString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("Lucky Money Info:\n");
            strBuilder.append("keyURL info: ").append(this.keyURLInfo).append("\n");
            strBuilder.append("keyDNS Info: ").append(this.keyDNSInfo).append("\n");
            strBuilder.append("qTag Info: ").append(this.qtagInfo).append("\n");
            strBuilder.append("qMSG Info: ").append(this.qmsgInfo).append("\n");
            strBuilder.append("delayTimeout: ").append(this.delayTimeout).append("\n");
            return strBuilder.toString();
        }
    }

    public LuckyMoneyHelper(Context context) {
        super(context, FILTER_NAME, SYS_FILE_DIR, DATA_FILE_DIR);
        setUpdateInfo(new LuckyMoneyUpdateInfo(), new LuckyMoneyUpdateInfo());
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLuckyMoneyInfo(int type) {
        return ((LuckyMoneyUpdateInfo) getUpdateInfo(true)).getLuckyMoneyInfo(type);
    }

    public int getDelayTimeout() {
        return ((LuckyMoneyUpdateInfo) getUpdateInfo(true)).getDelayTimeout();
    }

    public String getKeyURLInfo() {
        return ((LuckyMoneyUpdateInfo) getUpdateInfo(true)).getKeyURLInfo();
    }

    public String getkeyDNSInfo() {
        return ((LuckyMoneyUpdateInfo) getUpdateInfo(true)).getkeyDNSInfo();
    }

    public String dumpToString() {
        return ((LuckyMoneyUpdateInfo) getUpdateInfo(true)).dumpToString();
    }
}
