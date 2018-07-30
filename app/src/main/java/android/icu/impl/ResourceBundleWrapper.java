package android.icu.impl;

import android.icu.impl.locale.BaseLocale;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class ResourceBundleWrapper extends UResourceBundle {
    private static CacheBase<String, ResourceBundleWrapper, Loader> BUNDLE_CACHE = new SoftCache<String, ResourceBundleWrapper, Loader>() {
        protected ResourceBundleWrapper createInstance(String unusedKey, Loader loader) {
            return loader.load();
        }
    };
    private static final boolean DEBUG = ICUDebug.enabled("resourceBundleWrapper");
    private String baseName;
    private ResourceBundle bundle;
    private List<String> keys;
    private String localeID;

    private static abstract class Loader {
        /* synthetic */ Loader(Loader -this0) {
            this();
        }

        abstract ResourceBundleWrapper load();

        private Loader() {
        }
    }

    /* synthetic */ ResourceBundleWrapper(ResourceBundle bundle, ResourceBundleWrapper -this1) {
        this(bundle);
    }

    private ResourceBundleWrapper(ResourceBundle bundle) {
        this.bundle = null;
        this.localeID = null;
        this.baseName = null;
        this.keys = null;
        this.bundle = bundle;
    }

    protected Object handleGetObject(String aKey) {
        ResourceBundleWrapper current = this;
        Object obj = null;
        while (current != null) {
            try {
                obj = current.bundle.getObject(aKey);
                break;
            } catch (MissingResourceException e) {
                current = (ResourceBundleWrapper) current.getParent();
            }
        }
        if (obj != null) {
            return obj;
        }
        throw new MissingResourceException("Can't find resource for bundle " + this.baseName + ", key " + aKey, getClass().getName(), aKey);
    }

    public Enumeration<String> getKeys() {
        return Collections.enumeration(this.keys);
    }

    private void initKeysVector() {
        this.keys = new ArrayList();
        for (ResourceBundleWrapper current = this; current != null; current = (ResourceBundleWrapper) current.getParent()) {
            Enumeration<String> e = current.bundle.getKeys();
            while (e.hasMoreElements()) {
                String elem = (String) e.nextElement();
                if (!this.keys.contains(elem)) {
                    this.keys.add(elem);
                }
            }
        }
    }

    protected String getLocaleID() {
        return this.localeID;
    }

    protected String getBaseName() {
        return this.bundle.getClass().getName().replace('.', '/');
    }

    public ULocale getULocale() {
        return new ULocale(this.localeID);
    }

    public UResourceBundle getParent() {
        return (UResourceBundle) this.parent;
    }

    public static ResourceBundleWrapper getBundleInstance(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        ResourceBundleWrapper b;
        if (root == null) {
            root = ClassLoaderUtil.getClassLoader();
        }
        if (disableFallback) {
            b = instantiateBundle(baseName, localeID, null, root, disableFallback);
        } else {
            b = instantiateBundle(baseName, localeID, ULocale.getDefault().getBaseName(), root, disableFallback);
        }
        if (b != null) {
            return b;
        }
        String separator = BaseLocale.SEP;
        if (baseName.indexOf(47) >= 0) {
            separator = "/";
        }
        throw new MissingResourceException("Could not find the bundle " + baseName + separator + localeID, "", "");
    }

    private static boolean localeIDStartsWithLangSubtag(String localeID, String lang) {
        if (localeID.startsWith(lang)) {
            return localeID.length() == lang.length() || localeID.charAt(lang.length()) == '_';
        } else {
            return false;
        }
    }

    private static ResourceBundleWrapper instantiateBundle(String baseName, String localeID, String defaultID, ClassLoader root, boolean disableFallback) {
        final String name = localeID.isEmpty() ? baseName : baseName + '_' + localeID;
        final String str = localeID;
        final String str2 = baseName;
        final String str3 = defaultID;
        final ClassLoader classLoader = root;
        final boolean z = disableFallback;
        return (ResourceBundleWrapper) BUNDLE_CACHE.getInstance(disableFallback ? name : name + '#' + defaultID, new Loader() {
            public ResourceBundleWrapper load() {
                ResourceBundleWrapper b;
                Exception e;
                Throwable th;
                ResourceBundle parent = null;
                int i = str.lastIndexOf(95);
                boolean loadFromProperties = false;
                boolean parentIsRoot = false;
                if (i != -1) {
                    String locName = str.substring(0, i);
                    parent = ResourceBundleWrapper.instantiateBundle(str2, locName, str3, classLoader, z);
                } else if (!str.isEmpty()) {
                    parent = ResourceBundleWrapper.instantiateBundle(str2, "", str3, classLoader, z);
                    parentIsRoot = true;
                }
                ResourceBundleWrapper b2 = null;
                try {
                    b = new ResourceBundleWrapper((ResourceBundle) classLoader.loadClass(name).asSubclass(ResourceBundle.class).newInstance(), null);
                    if (parent != null) {
                        try {
                            b.setParent(parent);
                        } catch (ClassNotFoundException e2) {
                            b2 = b;
                            loadFromProperties = true;
                            b = b2;
                            if (loadFromProperties) {
                                try {
                                    String resName = name.replace('.', '/') + ".properties";
                                    final ClassLoader classLoader = classLoader;
                                    final String str = resName;
                                    InputStream stream = (InputStream) AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                                        public InputStream run() {
                                            return classLoader.getResourceAsStream(str);
                                        }
                                    });
                                    if (stream != null) {
                                        InputStream bufferedInputStream = new BufferedInputStream(stream);
                                        try {
                                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream), null);
                                            if (parent != null) {
                                                try {
                                                    b2.setParent(parent);
                                                } catch (Exception e3) {
                                                    try {
                                                        bufferedInputStream.close();
                                                    } catch (Exception e4) {
                                                    }
                                                    stream = bufferedInputStream;
                                                    if (b2 == null) {
                                                        try {
                                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                            }
                                                        } catch (Exception e5) {
                                                            e = e5;
                                                        }
                                                    }
                                                    b2 = parent;
                                                    if (b2 != null) {
                                                        b2.initKeysVector();
                                                    } else if (ResourceBundleWrapper.DEBUG) {
                                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                                    }
                                                    return b2;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    try {
                                                        bufferedInputStream.close();
                                                    } catch (Exception e6) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                            b2.baseName = str2;
                                            b2.localeID = str;
                                            try {
                                                bufferedInputStream.close();
                                            } catch (Exception e7) {
                                            }
                                        } catch (Exception e8) {
                                            b2 = b;
                                            bufferedInputStream.close();
                                            stream = bufferedInputStream;
                                            if (b2 == null) {
                                                try {
                                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                    }
                                                } catch (Exception e52) {
                                                    e = e52;
                                                }
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            b2 = b;
                                            bufferedInputStream.close();
                                            throw th;
                                        }
                                        stream = bufferedInputStream;
                                    } else {
                                        b2 = b;
                                    }
                                    if (b2 == null) {
                                        try {
                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                        } catch (Exception e522) {
                                            e = e522;
                                        }
                                    }
                                    if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                                        b2 = parent;
                                    }
                                } catch (Exception e9) {
                                    e = e9;
                                    b2 = b;
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("failure");
                                    }
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println(e);
                                    }
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                }
                            }
                            b2 = b;
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        } catch (NoClassDefFoundError e10) {
                            b2 = b;
                            loadFromProperties = true;
                            b = b2;
                            if (loadFromProperties) {
                                b2 = b;
                            } else {
                                try {
                                    String resName2 = name.replace('.', '/') + ".properties";
                                    final ClassLoader classLoader2 = classLoader;
                                    final String str2 = resName2;
                                    InputStream stream2 = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                                    if (stream2 != null) {
                                        InputStream bufferedInputStream2 = new BufferedInputStream(stream2);
                                        try {
                                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream2), null);
                                            if (parent != null) {
                                                try {
                                                    b2.setParent(parent);
                                                } catch (Exception e32) {
                                                    try {
                                                        bufferedInputStream2.close();
                                                    } catch (Exception e42) {
                                                    }
                                                    stream2 = bufferedInputStream2;
                                                    if (b2 == null) {
                                                        try {
                                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                            }
                                                        } catch (Exception e5222) {
                                                            e = e5222;
                                                        }
                                                    }
                                                    b2 = parent;
                                                    if (b2 != null) {
                                                        b2.initKeysVector();
                                                    } else if (ResourceBundleWrapper.DEBUG) {
                                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                                    }
                                                    return b2;
                                                } catch (Throwable th22) {
                                                    th = th22;
                                                    try {
                                                        bufferedInputStream2.close();
                                                    } catch (Exception e62) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                            b2.baseName = str2;
                                            b2.localeID = str;
                                            try {
                                                bufferedInputStream2.close();
                                            } catch (Exception e72) {
                                            }
                                        } catch (Exception e82) {
                                            b2 = b;
                                            bufferedInputStream2.close();
                                            stream2 = bufferedInputStream2;
                                            if (b2 == null) {
                                                try {
                                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                    }
                                                } catch (Exception e52222) {
                                                    e = e52222;
                                                }
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th32) {
                                            th = th32;
                                            b2 = b;
                                            bufferedInputStream2.close();
                                            throw th;
                                        }
                                        stream2 = bufferedInputStream2;
                                    } else {
                                        b2 = b;
                                    }
                                    if (b2 == null) {
                                        try {
                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                        } catch (Exception e522222) {
                                            e = e522222;
                                        }
                                    }
                                    if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                                        b2 = parent;
                                    }
                                } catch (Exception e92) {
                                    e = e92;
                                    b2 = b;
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("failure");
                                    }
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println(e);
                                    }
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                }
                            }
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        } catch (Exception e11) {
                            e = e11;
                            b2 = b;
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("failure");
                            }
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println(e);
                                b = b2;
                            } else {
                                b = b2;
                            }
                            if (loadFromProperties) {
                                try {
                                    String resName22 = name.replace('.', '/') + ".properties";
                                    final ClassLoader classLoader22 = classLoader;
                                    final String str22 = resName22;
                                    InputStream stream22 = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                                    if (stream22 != null) {
                                        InputStream bufferedInputStream22 = new BufferedInputStream(stream22);
                                        try {
                                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream22), null);
                                            if (parent != null) {
                                                try {
                                                    b2.setParent(parent);
                                                } catch (Exception e322) {
                                                    try {
                                                        bufferedInputStream22.close();
                                                    } catch (Exception e422) {
                                                    }
                                                    stream22 = bufferedInputStream22;
                                                    if (b2 == null) {
                                                        try {
                                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                            }
                                                        } catch (Exception e5222222) {
                                                            e = e5222222;
                                                        }
                                                    }
                                                    b2 = parent;
                                                    if (b2 != null) {
                                                        b2.initKeysVector();
                                                    } else if (ResourceBundleWrapper.DEBUG) {
                                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                                    }
                                                    return b2;
                                                } catch (Throwable th222) {
                                                    th = th222;
                                                    try {
                                                        bufferedInputStream22.close();
                                                    } catch (Exception e622) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                            b2.baseName = str2;
                                            b2.localeID = str;
                                            try {
                                                bufferedInputStream22.close();
                                            } catch (Exception e722) {
                                            }
                                        } catch (Exception e822) {
                                            b2 = b;
                                            bufferedInputStream22.close();
                                            stream22 = bufferedInputStream22;
                                            if (b2 == null) {
                                                try {
                                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                    }
                                                } catch (Exception e52222222) {
                                                    e = e52222222;
                                                }
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th322) {
                                            th = th322;
                                            b2 = b;
                                            bufferedInputStream22.close();
                                            throw th;
                                        }
                                        stream22 = bufferedInputStream22;
                                    } else {
                                        b2 = b;
                                    }
                                    if (b2 == null) {
                                        try {
                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                        } catch (Exception e522222222) {
                                            e = e522222222;
                                        }
                                    }
                                    if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                                        b2 = parent;
                                    }
                                } catch (Exception e922) {
                                    e = e922;
                                    b2 = b;
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("failure");
                                    }
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println(e);
                                    }
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                }
                            }
                            b2 = b;
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        }
                    }
                    b.baseName = str2;
                    b.localeID = str;
                } catch (ClassNotFoundException e12) {
                    loadFromProperties = true;
                    b = b2;
                    if (loadFromProperties) {
                        b2 = b;
                    } else {
                        try {
                            String resName222 = name.replace('.', '/') + ".properties";
                            final ClassLoader classLoader222 = classLoader;
                            final String str222 = resName222;
                            InputStream stream222 = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                            if (stream222 != null) {
                                InputStream bufferedInputStream222 = new BufferedInputStream(stream222);
                                try {
                                    b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream222), null);
                                    if (parent != null) {
                                        try {
                                            b2.setParent(parent);
                                        } catch (Exception e3222) {
                                            try {
                                                bufferedInputStream222.close();
                                            } catch (Exception e4222) {
                                            }
                                            stream222 = bufferedInputStream222;
                                            if (b2 == null) {
                                                try {
                                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                    }
                                                } catch (Exception e5222222222) {
                                                    e = e5222222222;
                                                }
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th2222) {
                                            th = th2222;
                                            try {
                                                bufferedInputStream222.close();
                                            } catch (Exception e6222) {
                                            }
                                            throw th;
                                        }
                                    }
                                    b2.baseName = str2;
                                    b2.localeID = str;
                                    try {
                                        bufferedInputStream222.close();
                                    } catch (Exception e7222) {
                                    }
                                } catch (Exception e8222) {
                                    b2 = b;
                                    bufferedInputStream222.close();
                                    stream222 = bufferedInputStream222;
                                    if (b2 == null) {
                                        try {
                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                        } catch (Exception e52222222222) {
                                            e = e52222222222;
                                        }
                                    }
                                    b2 = parent;
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                } catch (Throwable th3222) {
                                    th = th3222;
                                    b2 = b;
                                    bufferedInputStream222.close();
                                    throw th;
                                }
                                stream222 = bufferedInputStream222;
                            } else {
                                b2 = b;
                            }
                            if (b2 == null) {
                                try {
                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                    }
                                } catch (Exception e522222222222) {
                                    e = e522222222222;
                                }
                            }
                            if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                                b2 = parent;
                            }
                        } catch (Exception e9222) {
                            e = e9222;
                            b2 = b;
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("failure");
                            }
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println(e);
                            }
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        }
                    }
                    if (b2 != null) {
                        b2.initKeysVector();
                    } else if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                    }
                    return b2;
                } catch (NoClassDefFoundError e13) {
                    loadFromProperties = true;
                    b = b2;
                    if (loadFromProperties) {
                        try {
                            String resName2222 = name.replace('.', '/') + ".properties";
                            final ClassLoader classLoader2222 = classLoader;
                            final String str2222 = resName2222;
                            InputStream stream2222 = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                            if (stream2222 != null) {
                                InputStream bufferedInputStream2222 = new BufferedInputStream(stream2222);
                                try {
                                    b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream2222), null);
                                    if (parent != null) {
                                        try {
                                            b2.setParent(parent);
                                        } catch (Exception e32222) {
                                            try {
                                                bufferedInputStream2222.close();
                                            } catch (Exception e42222) {
                                            }
                                            stream2222 = bufferedInputStream2222;
                                            if (b2 == null) {
                                                try {
                                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                    }
                                                } catch (Exception e5222222222222) {
                                                    e = e5222222222222;
                                                }
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th22222) {
                                            th = th22222;
                                            try {
                                                bufferedInputStream2222.close();
                                            } catch (Exception e62222) {
                                            }
                                            throw th;
                                        }
                                    }
                                    b2.baseName = str2;
                                    b2.localeID = str;
                                    try {
                                        bufferedInputStream2222.close();
                                    } catch (Exception e72222) {
                                    }
                                } catch (Exception e82222) {
                                    b2 = b;
                                    bufferedInputStream2222.close();
                                    stream2222 = bufferedInputStream2222;
                                    if (b2 == null) {
                                        try {
                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                        } catch (Exception e52222222222222) {
                                            e = e52222222222222;
                                        }
                                    }
                                    b2 = parent;
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                } catch (Throwable th32222) {
                                    th = th32222;
                                    b2 = b;
                                    bufferedInputStream2222.close();
                                    throw th;
                                }
                                stream2222 = bufferedInputStream2222;
                            } else {
                                b2 = b;
                            }
                            if (b2 == null) {
                                try {
                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                    }
                                } catch (Exception e522222222222222) {
                                    e = e522222222222222;
                                }
                            }
                            if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                                b2 = parent;
                            }
                        } catch (Exception e92222) {
                            e = e92222;
                            b2 = b;
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("failure");
                            }
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println(e);
                            }
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        }
                    }
                    b2 = b;
                    if (b2 != null) {
                        b2.initKeysVector();
                    } else if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                    }
                    return b2;
                } catch (Exception e14) {
                    e = e14;
                    if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("failure");
                    }
                    if (ResourceBundleWrapper.DEBUG) {
                        System.out.println(e);
                        b = b2;
                    } else {
                        b = b2;
                    }
                    if (loadFromProperties) {
                        b2 = b;
                    } else {
                        try {
                            String resName22222 = name.replace('.', '/') + ".properties";
                            final ClassLoader classLoader22222 = classLoader;
                            final String str22222 = resName22222;
                            InputStream stream22222 = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                            if (stream22222 != null) {
                                InputStream bufferedInputStream22222 = new BufferedInputStream(stream22222);
                                try {
                                    b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream22222), null);
                                    if (parent != null) {
                                        try {
                                            b2.setParent(parent);
                                        } catch (Exception e322222) {
                                            try {
                                                bufferedInputStream22222.close();
                                            } catch (Exception e422222) {
                                            }
                                            stream22222 = bufferedInputStream22222;
                                            if (b2 == null) {
                                                try {
                                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                    }
                                                } catch (Exception e5222222222222222) {
                                                    e = e5222222222222222;
                                                }
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th222222) {
                                            th = th222222;
                                            try {
                                                bufferedInputStream22222.close();
                                            } catch (Exception e622222) {
                                            }
                                            throw th;
                                        }
                                    }
                                    b2.baseName = str2;
                                    b2.localeID = str;
                                    try {
                                        bufferedInputStream22222.close();
                                    } catch (Exception e722222) {
                                    }
                                } catch (Exception e822222) {
                                    b2 = b;
                                    bufferedInputStream22222.close();
                                    stream22222 = bufferedInputStream22222;
                                    if (b2 == null) {
                                        try {
                                            if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                        } catch (Exception e52222222222222222) {
                                            e = e52222222222222222;
                                        }
                                    }
                                    b2 = parent;
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                } catch (Throwable th322222) {
                                    th = th322222;
                                    b2 = b;
                                    bufferedInputStream22222.close();
                                    throw th;
                                }
                                stream22222 = bufferedInputStream22222;
                            } else {
                                b2 = b;
                            }
                            if (b2 == null) {
                                try {
                                    if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                    }
                                } catch (Exception e522222222222222222) {
                                    e = e522222222222222222;
                                }
                            }
                            if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                                b2 = parent;
                            }
                        } catch (Exception e922222) {
                            e = e922222;
                            b2 = b;
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("failure");
                            }
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println(e);
                            }
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        }
                    }
                    if (b2 != null) {
                        b2.initKeysVector();
                    } else if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                    }
                    return b2;
                }
                if (loadFromProperties) {
                    try {
                        String resName222222 = name.replace('.', '/') + ".properties";
                        final ClassLoader classLoader222222 = classLoader;
                        final String str222222 = resName222222;
                        InputStream stream222222 = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                        if (stream222222 != null) {
                            InputStream bufferedInputStream222222 = new BufferedInputStream(stream222222);
                            try {
                                b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream222222), null);
                                if (parent != null) {
                                    try {
                                        b2.setParent(parent);
                                    } catch (Exception e3222222) {
                                        try {
                                            bufferedInputStream222222.close();
                                        } catch (Exception e4222222) {
                                        }
                                        stream222222 = bufferedInputStream222222;
                                        if (b2 == null) {
                                            try {
                                                if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                                    b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                }
                                            } catch (Exception e5222222222222222222) {
                                                e = e5222222222222222222;
                                            }
                                        }
                                        b2 = parent;
                                        if (b2 != null) {
                                            b2.initKeysVector();
                                        } else if (ResourceBundleWrapper.DEBUG) {
                                            System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                        }
                                        return b2;
                                    } catch (Throwable th2222222) {
                                        th = th2222222;
                                        try {
                                            bufferedInputStream222222.close();
                                        } catch (Exception e6222222) {
                                        }
                                        throw th;
                                    }
                                }
                                b2.baseName = str2;
                                b2.localeID = str;
                                try {
                                    bufferedInputStream222222.close();
                                } catch (Exception e7222222) {
                                }
                            } catch (Exception e8222222) {
                                b2 = b;
                                bufferedInputStream222222.close();
                                stream222222 = bufferedInputStream222222;
                                if (b2 == null) {
                                    try {
                                        if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                            b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                        }
                                    } catch (Exception e52222222222222222222) {
                                        e = e52222222222222222222;
                                    }
                                }
                                b2 = parent;
                                if (b2 != null) {
                                    b2.initKeysVector();
                                } else if (ResourceBundleWrapper.DEBUG) {
                                    System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                }
                                return b2;
                            } catch (Throwable th3222222) {
                                th = th3222222;
                                b2 = b;
                                bufferedInputStream222222.close();
                                throw th;
                            }
                            stream222222 = bufferedInputStream222222;
                        } else {
                            b2 = b;
                        }
                        if (b2 == null) {
                            try {
                                if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                                    b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                }
                            } catch (Exception e522222222222222222222) {
                                e = e522222222222222222222;
                            }
                        }
                        if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                            b2 = parent;
                        }
                    } catch (Exception e9222222) {
                        e = e9222222;
                        b2 = b;
                        if (ResourceBundleWrapper.DEBUG) {
                            System.out.println("failure");
                        }
                        if (ResourceBundleWrapper.DEBUG) {
                            System.out.println(e);
                        }
                        if (b2 != null) {
                            b2.initKeysVector();
                        } else if (ResourceBundleWrapper.DEBUG) {
                            System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                        }
                        return b2;
                    }
                }
                b2 = b;
                if (b2 != null) {
                    b2.initKeysVector();
                } else if (ResourceBundleWrapper.DEBUG) {
                    System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                }
                return b2;
            }
        });
    }
}
