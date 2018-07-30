package com.android.server.am;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.FileObserver;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class OppoMultiAppManagerUtil {
    private static final Object ALIASMAPLOCK = new Object();
    private static final Object ALLOWEDAPPLOCK = new Object();
    private static final Object CREATEDMULTIAPPLOCK = new Object();
    private static boolean DEBUG_MULTI_APP = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    private static final String OPPO_ALLOWED_APP_FILE = "/data/oppo/coloros/multiapp/oppo_allowed_app.xml";
    private static final List<String> OPPO_INIT_ALLOWED_APP = Arrays.asList(new String[]{"com.tencent.mm", "com.tencent.mobileqq", "jp.naver.line.android", "com.facebook.orca", "com.whatsapp", "com.bbm", "com.zing.zalo", "com.viber.voip", "com.imo.android.imoim", "com.instagram.android", "org.telegram.messenger", "com.skype.raider", "com.facebook.katana", "com.bsb.hike"});
    private static final List<String> OPPO_MULTIAPP_FILTER_COMPONET = Arrays.asList(new String[]{"wxapi.WXEntryActivity", "QQCallbackUI", "plugin.accountsync.ui.ContactsSyncUI", "ui.activities.OpenInBbmActivity.Chat", "ui.activities.OpenInBbmActivity.VideoChat", "ui.activities.OpenInBbmActivity.VoiceChat"});
    private static final List<String> OPPO_MULTIAPP_GMS_PACKAGE = Arrays.asList(new String[]{"com.google.android.gms"});
    private static final List<String> OPPO_MULTIAPP_INSTALL_PACKAGE = Arrays.asList(new String[]{ActivityManagerService.OPPO_LAUNCHER, "com.android.providers.media", "com.google.android.gms"});
    private static final String OPPO_MULTI_APP_ALIAS_FILE = "/data/oppo/coloros/multiapp/oppo_multi_app_alias.xml";
    private static final String OPPO_MULTI_APP_CREATED_FILE = "/data/oppo/coloros/multiapp/oppo_multi_app.xml";
    private static final String TAG = OppoMultiAppManagerUtil.class.getName();
    private static OppoMultiAppManagerUtil sUtil;
    private FileObserverUtil mAliasFileObserver;
    private HashMap<String, String> mAliasMap = new HashMap();
    private List<String> mAllowedAppList = new ArrayList();
    private FileObserverUtil mAllowedFileObserver;
    private Context mContext;
    private List<String> mCreatedMultiAppList = new ArrayList();
    private FileObserverUtil mObserver;

    private class FileObserverUtil extends FileObserver {
        private String mFileName;

        public FileObserverUtil(String path) {
            super(path, 8);
            this.mFileName = path;
        }

        public void onEvent(int event, String path) {
            int e = event & 4095;
            switch (e) {
                case 8:
                    if (OppoMultiAppManagerUtil.DEBUG_MULTI_APP) {
                        Log.v(OppoMultiAppManagerUtil.TAG, "file is changing: " + this.mFileName + " event= " + e);
                    }
                    OppoMultiAppManagerUtil.this.reactOnFileChanged(this.mFileName);
                    return;
                default:
                    return;
            }
        }
    }

    public static OppoMultiAppManagerUtil getInstance() {
        if (sUtil == null) {
            sUtil = new OppoMultiAppManagerUtil();
        }
        return sUtil;
    }

    public void init(Context context) {
        this.mContext = context;
        initFile(OPPO_ALLOWED_APP_FILE);
        initFile(OPPO_MULTI_APP_CREATED_FILE);
        initFile(OPPO_MULTI_APP_ALIAS_FILE);
        readAllowedListFromFile(OPPO_ALLOWED_APP_FILE);
        readCreatedListFromFile(OPPO_MULTI_APP_CREATED_FILE);
        readAliasFromFile(OPPO_MULTI_APP_ALIAS_FILE);
        initObserver(OPPO_ALLOWED_APP_FILE);
        initObserver(OPPO_MULTI_APP_CREATED_FILE);
        initObserver(OPPO_MULTI_APP_ALIAS_FILE);
    }

    public boolean isInFilter(String tempClass) {
        if (tempClass == null) {
            return false;
        }
        for (String name : OPPO_MULTIAPP_FILTER_COMPONET) {
            if (tempClass.endsWith(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMultiUserInstallApp(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        for (String name : OPPO_MULTIAPP_INSTALL_PACKAGE) {
            if (name.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGms(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        for (String name : OPPO_MULTIAPP_GMS_PACKAGE) {
            if (name.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMultiAllowedApp(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        return (this.mAllowedAppList.isEmpty() && OPPO_INIT_ALLOWED_APP.contains(pkgName)) || this.mAllowedAppList.contains(pkgName);
    }

    public List<String> getAllowedMultiApp() {
        List<String> list;
        synchronized (ALLOWEDAPPLOCK) {
            list = this.mAllowedAppList;
        }
        return list;
    }

    public List<String> getCreatedMultiApp() {
        List<String> list;
        synchronized (CREATEDMULTIAPPLOCK) {
            list = this.mCreatedMultiAppList;
        }
        return list;
    }

    public String getAliasByPackage(String pkgName) {
        String alias;
        synchronized (ALIASMAPLOCK) {
            alias = (String) this.mAliasMap.get(pkgName);
        }
        if (this.mContext == null) {
            return alias;
        }
        if (alias != null && !"".equals(alias)) {
            return alias;
        }
        String label;
        try {
            label = (String) this.mContext.getPackageManager().getApplicationLabel(this.mContext.getPackageManager().getApplicationInfo(pkgName, 128));
        } catch (NameNotFoundException e) {
            label = "Application";
        }
        String multiPrefix = this.mContext.getResources().getString(201590120);
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1) {
            return label + multiPrefix + "‏";
        }
        return label + multiPrefix;
    }

    public boolean isMultiApp(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        boolean contains;
        synchronized (CREATEDMULTIAPPLOCK) {
            contains = this.mCreatedMultiAppList.contains(pkgName);
        }
        return contains;
    }

    public boolean isMainApp(int userId, String pkgName) {
        if (userId == OppoMultiAppManager.USER_ID || pkgName == null) {
            return false;
        }
        boolean contains;
        synchronized (CREATEDMULTIAPPLOCK) {
            contains = this.mCreatedMultiAppList.contains(pkgName);
        }
        return contains;
    }

    public boolean isMultiApp(int userId, String pkgName) {
        if (userId != OppoMultiAppManager.USER_ID || pkgName == null) {
            return false;
        }
        boolean contains;
        synchronized (CREATEDMULTIAPPLOCK) {
            contains = this.mCreatedMultiAppList.contains(pkgName);
        }
        return contains;
    }

    public void addToCreatedMultiApp(String pkgName) {
        if (pkgName != null) {
            synchronized (CREATEDMULTIAPPLOCK) {
                if (this.mCreatedMultiAppList.contains(pkgName)) {
                    return;
                }
                this.mCreatedMultiAppList.add(pkgName);
                writeListToFile(this.mCreatedMultiAppList, OPPO_MULTI_APP_CREATED_FILE);
            }
        }
    }

    public void removeFromCreatedMultiApp(String pkgName) {
        if (pkgName != null) {
            synchronized (CREATEDMULTIAPPLOCK) {
                if (this.mCreatedMultiAppList.contains(pkgName)) {
                    this.mCreatedMultiAppList.remove(pkgName);
                    this.mAliasMap.remove(pkgName);
                    writeListToFile(this.mCreatedMultiAppList, OPPO_MULTI_APP_CREATED_FILE);
                    return;
                }
            }
        }
    }

    private void writeListToFile(List<String> list, String filePath) {
        IOException ie;
        Throwable th;
        if (list != null && filePath != null) {
            FileOutputStream stream = null;
            try {
                FileOutputStream stream2 = new FileOutputStream(new File(filePath));
                try {
                    XmlSerializer out = Xml.newSerializer();
                    out.setOutput(stream2, "UTF-8");
                    out.startDocument(null, Boolean.valueOf(true));
                    out.startTag(null, "gs");
                    int size = list.size();
                    int i;
                    String pkg;
                    if (filePath.equals(OPPO_MULTI_APP_CREATED_FILE)) {
                        for (i = 0; i < size; i++) {
                            pkg = (String) list.get(i);
                            if (pkg != null) {
                                out.startTag(null, OppoCrashClearManager.CRASH_CLEAR_NAME);
                                out.attribute(null, "att", pkg);
                                out.endTag(null, OppoCrashClearManager.CRASH_CLEAR_NAME);
                            }
                        }
                    } else if (filePath.equals(OPPO_ALLOWED_APP_FILE)) {
                        for (i = 0; i < size; i++) {
                            pkg = (String) list.get(i);
                            if (pkg != null) {
                                out.startTag(null, "allowedapp");
                                out.text(pkg);
                                out.endTag(null, "allowedapp");
                            }
                        }
                    }
                    out.endTag(null, "gs");
                    out.endDocument();
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    stream = stream2;
                } catch (IOException e2) {
                    ie = e2;
                    stream = stream2;
                    try {
                        ie.printStackTrace();
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e3) {
                                e3.printStackTrace();
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e32) {
                                e32.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    stream = stream2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e322) {
                            e322.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                ie = e4;
                ie.printStackTrace();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e3222) {
                        e3222.printStackTrace();
                    }
                }
            }
        }
    }

    private OppoMultiAppManagerUtil() {
    }

    private void initFile(String fileName) {
        if (DEBUG_MULTI_APP) {
            Log.d(TAG, "initFile() " + fileName);
        }
        File file = new File(fileName);
        try {
            if (file.getParentFile().exists() || file.getParentFile().mkdir()) {
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        if (DEBUG_MULTI_APP) {
                            Log.e(TAG, "init file failed: file-" + fileName);
                        }
                        return;
                    } else if (fileName.equals(OPPO_ALLOWED_APP_FILE)) {
                        writeListToFile(OPPO_INIT_ALLOWED_APP, OPPO_ALLOWED_APP_FILE);
                    }
                }
                changeModFile(fileName);
                return;
            }
            if (DEBUG_MULTI_APP) {
                Log.d(TAG, "initFile() " + fileName + " mkdir() failed");
            }
        } catch (IOException e) {
        }
    }

    private void changeModFile(String fileName) {
        try {
            Runtime.getRuntime().exec("chmod 700 /data/oppo/coloros/multiapp");
            Runtime.getRuntime().exec("chmod 600 " + fileName);
        } catch (IOException e) {
            Log.w(TAG, " " + e);
        }
    }

    private void readAllowedListFromFile(String filePath) {
        Exception e;
        Throwable th;
        if (DEBUG_MULTI_APP) {
            Log.d(TAG, " read list from file: " + filePath);
        }
        if (filePath != null) {
            List<String> tempList = new ArrayList();
            FileInputStream stream = null;
            try {
                FileInputStream stream2 = new FileInputStream(new File(filePath));
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream2, "UTF-8");
                    int type;
                    do {
                        type = parser.next();
                        if (type == 2 && parser.getName().equals("allowedapp")) {
                            String pkgName = parser.nextText();
                            if (pkgName != null) {
                                tempList.add(pkgName);
                            }
                        }
                    } while (type != 1);
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    stream = stream2;
                } catch (IOException e3) {
                    e = e3;
                    stream = stream2;
                    try {
                        e.printStackTrace();
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e22) {
                                e22.printStackTrace();
                            }
                        }
                        if (filePath.equals(OPPO_ALLOWED_APP_FILE)) {
                            if (!this.mAllowedAppList.isEmpty()) {
                                this.mAllowedAppList.clear();
                            }
                            if (!tempList.isEmpty()) {
                                this.mAllowedAppList.addAll(tempList);
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e222) {
                                e222.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    stream = stream2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2222) {
                            e2222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                e = e4;
                e.printStackTrace();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e22222) {
                        e22222.printStackTrace();
                    }
                }
                if (filePath.equals(OPPO_ALLOWED_APP_FILE)) {
                    if (!this.mAllowedAppList.isEmpty()) {
                        this.mAllowedAppList.clear();
                    }
                    if (!tempList.isEmpty()) {
                        this.mAllowedAppList.addAll(tempList);
                    }
                }
            }
            if (filePath.equals(OPPO_ALLOWED_APP_FILE)) {
                if (!this.mAllowedAppList.isEmpty()) {
                    this.mAllowedAppList.clear();
                }
                if (!tempList.isEmpty()) {
                    this.mAllowedAppList.addAll(tempList);
                }
            }
        }
    }

    private void readCreatedListFromFile(String filePath) {
        Exception e;
        Throwable th;
        if (DEBUG_MULTI_APP) {
            Log.d(TAG, " read list from file: " + filePath);
        }
        if (filePath != null) {
            List<String> tempList = new ArrayList();
            FileInputStream stream = null;
            try {
                FileInputStream stream2 = new FileInputStream(new File(filePath));
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream2, "UTF-8");
                    int type;
                    do {
                        type = parser.next();
                        if (type == 2 && parser.getName().equals(OppoCrashClearManager.CRASH_CLEAR_NAME)) {
                            String packageName = parser.getAttributeValue(null, "att");
                            if (packageName != null) {
                                tempList.add(packageName);
                            }
                        }
                    } while (type != 1);
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    stream = stream2;
                } catch (IOException e3) {
                    e = e3;
                    stream = stream2;
                    try {
                        e.printStackTrace();
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e22) {
                                e22.printStackTrace();
                            }
                        }
                        if (filePath.equals(OPPO_MULTI_APP_CREATED_FILE)) {
                            if (!this.mCreatedMultiAppList.isEmpty()) {
                                this.mCreatedMultiAppList.clear();
                            }
                            this.mCreatedMultiAppList.addAll(tempList);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e222) {
                                e222.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    stream = stream2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2222) {
                            e2222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                e = e4;
                e.printStackTrace();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e22222) {
                        e22222.printStackTrace();
                    }
                }
                if (filePath.equals(OPPO_MULTI_APP_CREATED_FILE)) {
                    if (!this.mCreatedMultiAppList.isEmpty()) {
                        this.mCreatedMultiAppList.clear();
                    }
                    this.mCreatedMultiAppList.addAll(tempList);
                }
            }
            if (filePath.equals(OPPO_MULTI_APP_CREATED_FILE)) {
                if (!this.mCreatedMultiAppList.isEmpty()) {
                    this.mCreatedMultiAppList.clear();
                }
                this.mCreatedMultiAppList.addAll(tempList);
            }
        }
    }

    private void readAliasFromFile(String filePath) {
        Exception e;
        Throwable th;
        if (DEBUG_MULTI_APP) {
            Log.d(TAG, " read list from file: " + filePath);
        }
        if (filePath != null) {
            this.mAliasMap.clear();
            FileInputStream stream = null;
            try {
                FileInputStream stream2 = new FileInputStream(new File(filePath));
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream2, "UTF-8");
                    int type;
                    do {
                        type = parser.next();
                        if (type == 2 && parser.getName().equals(OppoCrashClearManager.CRASH_CLEAR_NAME)) {
                            String pkgName = parser.getAttributeValue(null, "pkg");
                            String name = parser.getAttributeValue(null, "name");
                            if (!(pkgName == null || !getAllowedMultiApp().contains(pkgName) || name == null)) {
                                this.mAliasMap.put(pkgName, name);
                            }
                        }
                    } while (type != 1);
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    stream = stream2;
                } catch (IOException e3) {
                    e = e3;
                    stream = stream2;
                    try {
                        e.printStackTrace();
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e22) {
                                e22.printStackTrace();
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e222) {
                                e222.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    stream = stream2;
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2222) {
                            e2222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                e = e4;
                e.printStackTrace();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e22222) {
                        e22222.printStackTrace();
                    }
                }
            }
        }
    }

    private void initObserver(String fileName) {
        if (fileName != null && fileName.equals(OPPO_MULTI_APP_CREATED_FILE) && this.mObserver == null) {
            this.mObserver = new FileObserverUtil(fileName);
            this.mObserver.startWatching();
        }
        if (fileName != null && fileName.equals(OPPO_ALLOWED_APP_FILE) && this.mAllowedFileObserver == null) {
            this.mAllowedFileObserver = new FileObserverUtil(fileName);
            this.mAllowedFileObserver.startWatching();
        }
        if (fileName != null && fileName.equals(OPPO_MULTI_APP_ALIAS_FILE) && this.mAliasFileObserver == null) {
            this.mAliasFileObserver = new FileObserverUtil(fileName);
            this.mAliasFileObserver.startWatching();
        }
    }

    private void reactOnFileChanged(String fileName) {
        Object obj;
        if (fileName != null && fileName.equals(OPPO_ALLOWED_APP_FILE)) {
            obj = ALLOWEDAPPLOCK;
            synchronized (obj) {
                readAllowedListFromFile(fileName);
            }
        } else if (fileName != null && fileName.equals(OPPO_MULTI_APP_CREATED_FILE)) {
            obj = CREATEDMULTIAPPLOCK;
            synchronized (obj) {
                readCreatedListFromFile(fileName);
            }
        } else if (fileName != null && fileName.equals(OPPO_MULTI_APP_ALIAS_FILE)) {
            obj = ALIASMAPLOCK;
            synchronized (obj) {
                readAliasFromFile(fileName);
            }
        } else {
            return;
        }
    }
}
