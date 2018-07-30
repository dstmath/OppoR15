package sun.misc;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import sun.net.util.URLUtil;
import sun.net.www.ParseUtil;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

public class URLClassPath {
    private static final boolean DEBUG;
    private static final boolean DEBUG_LOOKUP_CACHE;
    private static final boolean DISABLE_ACC_CHECKING;
    private static final boolean DISABLE_JAR_CHECKING;
    static final String JAVA_VERSION = ((String) AccessController.doPrivileged(new GetPropertyAction("java.version")));
    static final String USER_AGENT_JAVA_VERSION = "UA-Java-Version";
    private static volatile boolean lookupCacheEnabled = false;
    private final AccessControlContext acc;
    private boolean closed;
    private URLStreamHandler jarHandler;
    HashMap<String, Loader> lmap;
    ArrayList<Loader> loaders;
    private ClassLoader lookupCacheLoader;
    private URL[] lookupCacheURLs;
    private ArrayList<URL> path;
    Stack<URL> urls;

    private static class Loader implements Closeable {
        private final URL base;
        private JarFile jarfile;

        Loader(URL url) {
            this.base = url;
        }

        URL getBaseURL() {
            return this.base;
        }

        URL findResource(String name, boolean check) {
            try {
                URL url = new URL(this.base, ParseUtil.encodePath(name, false));
                if (check) {
                    try {
                        URLClassPath.check(url);
                    } catch (Exception e) {
                        return null;
                    }
                }
                URLConnection uc = url.openConnection();
                if (uc instanceof HttpURLConnection) {
                    HttpURLConnection hconn = (HttpURLConnection) uc;
                    hconn.setRequestMethod("HEAD");
                    if (hconn.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        return null;
                    }
                }
                uc.setUseCaches(false);
                uc.getInputStream().close();
                return url;
            } catch (MalformedURLException e2) {
                throw new IllegalArgumentException("name");
            }
        }

        Resource getResource(final String name, boolean check) {
            try {
                final URL url = new URL(this.base, ParseUtil.encodePath(name, false));
                if (check) {
                    try {
                        URLClassPath.check(url);
                    } catch (Exception e) {
                        return null;
                    }
                }
                final URLConnection uc = url.openConnection();
                InputStream in = uc.getInputStream();
                if (uc instanceof JarURLConnection) {
                    this.jarfile = JarLoader.checkJar(((JarURLConnection) uc).getJarFile());
                }
                return new Resource() {
                    public String getName() {
                        return name;
                    }

                    public URL getURL() {
                        return url;
                    }

                    public URL getCodeSourceURL() {
                        return Loader.this.base;
                    }

                    public InputStream getInputStream() throws IOException {
                        return uc.getInputStream();
                    }

                    public int getContentLength() throws IOException {
                        return uc.getContentLength();
                    }
                };
            } catch (MalformedURLException e2) {
                throw new IllegalArgumentException("name");
            }
        }

        Resource getResource(String name) {
            return getResource(name, true);
        }

        public void close() throws IOException {
            if (this.jarfile != null) {
                this.jarfile.close();
            }
        }

        URL[] getClassPath() throws IOException {
            return null;
        }
    }

    private static class FileLoader extends Loader {
        private File dir;

        FileLoader(URL url) throws IOException {
            super(url);
            if ("file".equals(url.getProtocol())) {
                this.dir = new File(ParseUtil.decode(url.getFile().replace('/', File.separatorChar))).getCanonicalFile();
                return;
            }
            throw new IllegalArgumentException("url");
        }

        URL findResource(String name, boolean check) {
            Resource rsc = getResource(name, check);
            if (rsc != null) {
                return rsc.getURL();
            }
            return null;
        }

        Resource getResource(final String name, boolean check) {
            try {
                URL normalizedBase = new URL(getBaseURL(), ".");
                final URL url = new URL(getBaseURL(), ParseUtil.encodePath(name, false));
                if (!url.getFile().startsWith(normalizedBase.getFile())) {
                    return null;
                }
                File file;
                if (check) {
                    URLClassPath.check(url);
                }
                if (name.indexOf("..") != -1) {
                    file = new File(this.dir, name.replace('/', File.separatorChar)).getCanonicalFile();
                    if (!file.getPath().startsWith(this.dir.getPath())) {
                        return null;
                    }
                }
                file = new File(this.dir, name.replace('/', File.separatorChar));
                if (file.exists()) {
                    return new Resource() {
                        public String getName() {
                            return name;
                        }

                        public URL getURL() {
                            return url;
                        }

                        public URL getCodeSourceURL() {
                            return FileLoader.this.getBaseURL();
                        }

                        public InputStream getInputStream() throws IOException {
                            return new FileInputStream(file);
                        }

                        public int getContentLength() throws IOException {
                            return (int) file.length();
                        }
                    };
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }

    static class JarLoader extends Loader {
        private final AccessControlContext acc;
        private boolean closed = false;
        private final URL csu;
        private URLStreamHandler handler;
        private JarIndex index;
        private JarFile jar;
        private final HashMap<String, Loader> lmap;
        private MetaIndex metaIndex;

        JarLoader(URL url, URLStreamHandler jarHandler, HashMap<String, Loader> loaderMap, AccessControlContext acc) throws IOException {
            super(new URL("jar", "", -1, url + "!/", jarHandler));
            this.csu = url;
            this.handler = jarHandler;
            this.lmap = loaderMap;
            this.acc = acc;
            if (isOptimizable(url)) {
                String fileName = url.getFile();
                if (fileName != null) {
                    File f = new File(ParseUtil.decode(fileName));
                    this.metaIndex = MetaIndex.forJar(f);
                    if (!(this.metaIndex == null || (f.exists() ^ 1) == 0)) {
                        this.metaIndex = null;
                    }
                }
                if (this.metaIndex == null) {
                    ensureOpen();
                    return;
                }
                return;
            }
            ensureOpen();
        }

        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                ensureOpen();
                this.jar.close();
            }
        }

        JarFile getJarFile() {
            return this.jar;
        }

        private boolean isOptimizable(URL url) {
            return "file".equals(url.getProtocol());
        }

        private void ensureOpen() throws IOException {
            if (this.jar == null) {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        public Void run() throws IOException {
                            if (URLClassPath.DEBUG) {
                                System.err.println("Opening " + JarLoader.this.csu);
                                Thread.dumpStack();
                            }
                            JarLoader.this.jar = JarLoader.this.getJarFile(JarLoader.this.csu);
                            JarLoader.this.index = JarIndex.getJarIndex(JarLoader.this.jar, JarLoader.this.metaIndex);
                            if (JarLoader.this.index != null) {
                                String[] jarfiles = JarLoader.this.index.getJarFiles();
                                for (String url : jarfiles) {
                                    try {
                                        String urlNoFragString = URLUtil.urlNoFragString(new URL(JarLoader.this.csu, url));
                                        if (!JarLoader.this.lmap.containsKey(urlNoFragString)) {
                                            JarLoader.this.lmap.put(urlNoFragString, null);
                                        }
                                    } catch (MalformedURLException e) {
                                    }
                                }
                            }
                            return null;
                        }
                    }, this.acc);
                } catch (PrivilegedActionException pae) {
                    throw ((IOException) pae.getException());
                }
            }
        }

        static JarFile checkJar(JarFile jar) throws IOException {
            if (System.getSecurityManager() == null || (URLClassPath.DISABLE_JAR_CHECKING ^ 1) == 0 || (jar.startsWithLocHeader() ^ 1) == 0) {
                return jar;
            }
            IOException x = new IOException("Invalid Jar file");
            try {
                jar.close();
            } catch (IOException ex) {
                x.addSuppressed(ex);
            }
            throw x;
        }

        private JarFile getJarFile(URL url) throws IOException {
            if (isOptimizable(url)) {
                FileURLMapper p = new FileURLMapper(url);
                if (p.exists()) {
                    return checkJar(new JarFile(p.getPath()));
                }
                throw new FileNotFoundException(p.getPath());
            }
            URLConnection uc = getBaseURL().openConnection();
            uc.setRequestProperty(URLClassPath.USER_AGENT_JAVA_VERSION, URLClassPath.JAVA_VERSION);
            return checkJar(((JarURLConnection) uc).getJarFile());
        }

        JarIndex getIndex() {
            try {
                ensureOpen();
                return this.index;
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        Resource checkResource(final String name, boolean check, final JarEntry entry) {
            try {
                final URL url = new URL(getBaseURL(), ParseUtil.encodePath(name, false));
                if (check) {
                    URLClassPath.check(url);
                }
                return new Resource() {
                    public String getName() {
                        return name;
                    }

                    public URL getURL() {
                        return url;
                    }

                    public URL getCodeSourceURL() {
                        return JarLoader.this.csu;
                    }

                    public InputStream getInputStream() throws IOException {
                        return JarLoader.this.jar.getInputStream(entry);
                    }

                    public int getContentLength() {
                        return (int) entry.getSize();
                    }

                    public Manifest getManifest() throws IOException {
                        return JarLoader.this.jar.getManifest();
                    }

                    public Certificate[] getCertificates() {
                        return entry.getCertificates();
                    }

                    public CodeSigner[] getCodeSigners() {
                        return entry.getCodeSigners();
                    }
                };
            } catch (MalformedURLException e) {
                return null;
            } catch (IOException e2) {
                return null;
            } catch (AccessControlException e3) {
                return null;
            }
        }

        boolean validIndex(String name) {
            String packageName = name;
            int pos = name.lastIndexOf("/");
            if (pos != -1) {
                packageName = name.substring(0, pos);
            }
            Enumeration<JarEntry> enum_ = this.jar.entries();
            while (enum_.hasMoreElements()) {
                String entryName = ((ZipEntry) enum_.nextElement()).getName();
                pos = entryName.lastIndexOf("/");
                if (pos != -1) {
                    entryName = entryName.substring(0, pos);
                }
                if (entryName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        URL findResource(String name, boolean check) {
            Resource rsc = getResource(name, check);
            if (rsc != null) {
                return rsc.getURL();
            }
            return null;
        }

        Resource getResource(String name, boolean check) {
            if (this.metaIndex != null && !this.metaIndex.mayContain(name)) {
                return null;
            }
            try {
                ensureOpen();
                JarEntry entry = this.jar.getJarEntry(name);
                if (entry != null) {
                    return checkResource(name, check, entry);
                }
                if (this.index == null) {
                    return null;
                }
                return getResource(name, check, new HashSet());
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        Resource getResource(String name, boolean check, Set<String> visited) {
            int count = 0;
            LinkedList<String> jarFilesList = this.index.get(name);
            if (jarFilesList == null) {
                return null;
            }
            while (true) {
                int count2;
                int size = jarFilesList.size();
                String[] jarFiles = (String[]) jarFilesList.toArray(new String[size]);
                while (true) {
                    count2 = count;
                    if (count2 >= size) {
                        break;
                    }
                    count = count2 + 1;
                    String jarName = jarFiles[count2];
                    try {
                        URL url = new URL(this.csu, jarName);
                        String urlNoFragString = URLUtil.urlNoFragString(url);
                        JarLoader newLoader = (JarLoader) this.lmap.get(urlNoFragString);
                        if (newLoader == null) {
                            final URL url2 = url;
                            newLoader = (JarLoader) AccessController.doPrivileged(new PrivilegedExceptionAction<JarLoader>() {
                                public JarLoader run() throws IOException {
                                    return new JarLoader(url2, JarLoader.this.handler, JarLoader.this.lmap, JarLoader.this.acc);
                                }
                            }, this.acc);
                            JarIndex newIndex = newLoader.getIndex();
                            if (newIndex != null) {
                                int pos = jarName.lastIndexOf("/");
                                newIndex.merge(this.index, pos == -1 ? null : jarName.substring(0, pos + 1));
                            }
                            this.lmap.put(urlNoFragString, newLoader);
                        }
                        boolean visitedURL = visited.add(URLUtil.urlNoFragString(url)) ^ 1;
                        if (!visitedURL) {
                            try {
                                newLoader.ensureOpen();
                                JarEntry entry = newLoader.jar.getJarEntry(name);
                                if (entry != null) {
                                    return newLoader.checkResource(name, check, entry);
                                }
                                if (!newLoader.validIndex(name)) {
                                    throw new InvalidJarIndexException("Invalid index");
                                }
                            } catch (Throwable e) {
                                throw new InternalError(e);
                            }
                        }
                        if (!visitedURL && newLoader != this && newLoader.getIndex() != null) {
                            Resource res = newLoader.getResource(name, check, visited);
                            if (res != null) {
                                return res;
                            }
                        }
                    } catch (PrivilegedActionException e2) {
                    } catch (MalformedURLException e3) {
                    }
                }
                count = count2;
            }
        }

        URL[] getClassPath() throws IOException {
            if (this.index != null || this.metaIndex != null) {
                return null;
            }
            ensureOpen();
            parseExtensionsDependencies();
            if (this.jar.hasClassPathAttribute()) {
                Manifest man = this.jar.getManifest();
                if (man != null) {
                    Attributes attr = man.getMainAttributes();
                    if (attr != null) {
                        String value = attr.getValue(Name.CLASS_PATH);
                        if (value != null) {
                            return parseClassPath(this.csu, value);
                        }
                    }
                }
            }
            return null;
        }

        private void parseExtensionsDependencies() throws IOException {
        }

        private URL[] parseClassPath(URL base, String value) throws MalformedURLException {
            StringTokenizer st = new StringTokenizer(value);
            URL[] urls = new URL[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                urls[i] = new URL(base, st.nextToken());
                i++;
            }
            return urls;
        }
    }

    static {
        boolean z;
        boolean z2 = true;
        if (AccessController.doPrivileged(new GetPropertyAction("sun.misc.URLClassPath.debug")) != null) {
            z = true;
        } else {
            z = false;
        }
        DEBUG = z;
        if (AccessController.doPrivileged(new GetPropertyAction("sun.misc.URLClassPath.debugLookupCache")) != null) {
            z = true;
        } else {
            z = false;
        }
        DEBUG_LOOKUP_CACHE = z;
        String p = (String) AccessController.doPrivileged(new GetPropertyAction("sun.misc.URLClassPath.disableJarChecking"));
        z = p != null ? !p.equals("true") ? p.equals("") : true : false;
        DISABLE_JAR_CHECKING = z;
        p = (String) AccessController.doPrivileged(new GetPropertyAction("jdk.net.URLClassPath.disableRestrictedPermissions"));
        if (p == null) {
            z2 = false;
        } else if (!p.equals("true")) {
            z2 = p.equals("");
        }
        DISABLE_ACC_CHECKING = z2;
    }

    public URLClassPath(URL[] urls, URLStreamHandlerFactory factory, AccessControlContext acc) {
        this.path = new ArrayList();
        this.urls = new Stack();
        this.loaders = new ArrayList();
        this.lmap = new HashMap();
        this.closed = false;
        for (Object add : urls) {
            this.path.add(add);
        }
        push(urls);
        if (factory != null) {
            this.jarHandler = factory.createURLStreamHandler("jar");
        }
        if (DISABLE_ACC_CHECKING) {
            this.acc = null;
        } else {
            this.acc = acc;
        }
    }

    public URLClassPath(URL[] urls) {
        this(urls, null, null);
    }

    public URLClassPath(URL[] urls, AccessControlContext acc) {
        this(urls, null, acc);
    }

    public synchronized List<IOException> closeLoaders() {
        if (this.closed) {
            return Collections.emptyList();
        }
        List<IOException> result = new LinkedList();
        for (Loader loader : this.loaders) {
            try {
                loader.close();
            } catch (IOException e) {
                result.add(e);
            }
        }
        this.closed = true;
        return result;
    }

    public synchronized void addURL(URL url) {
        if (!this.closed) {
            synchronized (this.urls) {
                if (url != null) {
                    if (!this.path.contains(url)) {
                        this.urls.add(0, url);
                        this.path.add(url);
                        if (this.lookupCacheURLs != null) {
                            disableAllLookupCaches();
                        }
                        return;
                    }
                }
            }
        }
    }

    public URL[] getURLs() {
        URL[] urlArr;
        synchronized (this.urls) {
            urlArr = (URL[]) this.path.toArray(new URL[this.path.size()]);
        }
        return urlArr;
    }

    public URL findResource(String name, boolean check) {
        int[] cache = getLookupCache(name);
        int i = 0;
        while (true) {
            Loader loader = getNextLoader(cache, i);
            if (loader == null) {
                return null;
            }
            URL url = loader.findResource(name, check);
            if (url != null) {
                return url;
            }
            i++;
        }
    }

    public Resource getResource(String name, boolean check) {
        if (DEBUG) {
            System.err.println("URLClassPath.getResource(\"" + name + "\")");
        }
        int[] cache = getLookupCache(name);
        int i = 0;
        while (true) {
            Loader loader = getNextLoader(cache, i);
            if (loader == null) {
                return null;
            }
            Resource res = loader.getResource(name, check);
            if (res != null) {
                return res;
            }
            i++;
        }
    }

    public Enumeration<URL> findResources(final String name, final boolean check) {
        return new Enumeration<URL>() {
            private int[] cache = URLClassPath.this.getLookupCache(name);
            private int index = 0;
            private URL url = null;

            private boolean next() {
                if (this.url != null) {
                    return true;
                }
                do {
                    URLClassPath uRLClassPath = URLClassPath.this;
                    int[] iArr = this.cache;
                    int i = this.index;
                    this.index = i + 1;
                    Loader loader = uRLClassPath.getNextLoader(iArr, i);
                    if (loader == null) {
                        return false;
                    }
                    this.url = loader.findResource(name, check);
                } while (this.url == null);
                return true;
            }

            public boolean hasMoreElements() {
                return next();
            }

            public URL nextElement() {
                if (next()) {
                    URL u = this.url;
                    this.url = null;
                    return u;
                }
                throw new NoSuchElementException();
            }
        };
    }

    public Resource getResource(String name) {
        return getResource(name, true);
    }

    public Enumeration<Resource> getResources(final String name, final boolean check) {
        return new Enumeration<Resource>() {
            private int[] cache = URLClassPath.this.getLookupCache(name);
            private int index = 0;
            private Resource res = null;

            private boolean next() {
                if (this.res != null) {
                    return true;
                }
                do {
                    URLClassPath uRLClassPath = URLClassPath.this;
                    int[] iArr = this.cache;
                    int i = this.index;
                    this.index = i + 1;
                    Loader loader = uRLClassPath.getNextLoader(iArr, i);
                    if (loader == null) {
                        return false;
                    }
                    this.res = loader.getResource(name, check);
                } while (this.res == null);
                return true;
            }

            public boolean hasMoreElements() {
                return next();
            }

            public Resource nextElement() {
                if (next()) {
                    Resource r = this.res;
                    this.res = null;
                    return r;
                }
                throw new NoSuchElementException();
            }
        };
    }

    public Enumeration<Resource> getResources(String name) {
        return getResources(name, true);
    }

    synchronized void initLookupCache(ClassLoader loader) {
        URL[] lookupCacheURLs = getLookupCacheURLs(loader);
        this.lookupCacheURLs = lookupCacheURLs;
        if (lookupCacheURLs != null) {
            this.lookupCacheLoader = loader;
        } else {
            disableAllLookupCaches();
        }
    }

    static void disableAllLookupCaches() {
        lookupCacheEnabled = false;
    }

    private URL[] getLookupCacheURLs(ClassLoader loader) {
        return null;
    }

    private static int[] getLookupCacheForClassLoader(ClassLoader loader, String name) {
        return null;
    }

    private static boolean knownToNotExist0(ClassLoader loader, String className) {
        return false;
    }

    synchronized boolean knownToNotExist(String className) {
        if (this.lookupCacheURLs == null || !lookupCacheEnabled) {
            return false;
        }
        return knownToNotExist0(this.lookupCacheLoader, className);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized int[] getLookupCache(String name) {
        if (this.lookupCacheURLs != null && (lookupCacheEnabled ^ 1) == 0) {
            int[] cache = getLookupCacheForClassLoader(this.lookupCacheLoader, name);
            if (cache != null && cache.length > 0) {
                int maxindex = cache[cache.length - 1];
                if (!ensureLoaderOpened(maxindex)) {
                    if (DEBUG_LOOKUP_CACHE) {
                        System.out.println("Expanded loaders FAILED " + this.loaders.size() + " for maxindex=" + maxindex);
                    }
                }
            }
        }
    }

    private boolean ensureLoaderOpened(int index) {
        if (this.loaders.size() <= index) {
            if (getLoader(index) == null || !lookupCacheEnabled) {
                return false;
            }
            if (DEBUG_LOOKUP_CACHE) {
                System.out.println("Expanded loaders " + this.loaders.size() + " to index=" + index);
            }
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void validateLookupCache(int index, String urlNoFragString) {
        if (this.lookupCacheURLs != null && lookupCacheEnabled) {
            if (index >= this.lookupCacheURLs.length || !urlNoFragString.equals(URLUtil.urlNoFragString(this.lookupCacheURLs[index]))) {
                if (DEBUG || DEBUG_LOOKUP_CACHE) {
                    System.out.println("WARNING: resource lookup cache invalidated for lookupCacheLoader at " + index);
                }
                disableAllLookupCaches();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized Loader getNextLoader(int[] cache, int index) {
        if (this.closed) {
            return null;
        }
        if (cache == null) {
            return getLoader(index);
        } else if (index >= cache.length) {
            return null;
        } else {
            Loader loader = (Loader) this.loaders.get(cache[index]);
            if (DEBUG_LOOKUP_CACHE) {
                System.out.println("HASCACHE: Loading from : " + cache[index] + " = " + loader.getBaseURL());
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized Loader getLoader(int index) {
        if (this.closed) {
            return null;
        }
        while (this.loaders.size() < index + 1) {
            synchronized (this.urls) {
                if (this.urls.empty()) {
                    return null;
                }
                Object url = (URL) this.urls.pop();
            }
        }
        if (DEBUG_LOOKUP_CACHE) {
            System.out.println("NOCACHE: Loading from : " + index);
        }
        return (Loader) this.loaders.get(index);
    }

    private Loader getLoader(final URL url) throws IOException {
        try {
            return (Loader) AccessController.doPrivileged(new PrivilegedExceptionAction<Loader>() {
                public Loader run() throws IOException {
                    String file = url.getFile();
                    if (file == null || !file.endsWith("/")) {
                        return new JarLoader(url, URLClassPath.this.jarHandler, URLClassPath.this.lmap, URLClassPath.this.acc);
                    }
                    if ("file".equals(url.getProtocol())) {
                        return new FileLoader(url);
                    }
                    return new Loader(url);
                }
            }, this.acc);
        } catch (PrivilegedActionException pae) {
            throw ((IOException) pae.getException());
        }
    }

    private void push(URL[] us) {
        synchronized (this.urls) {
            for (int i = us.length - 1; i >= 0; i--) {
                this.urls.push(us[i]);
            }
        }
    }

    public static URL[] pathToURLs(String path) {
        StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
        Object urls = new URL[st.countTokens()];
        int count = 0;
        while (st.hasMoreTokens()) {
            File f = new File(st.nextToken());
            try {
                f = new File(f.getCanonicalPath());
            } catch (IOException e) {
            }
            int count2 = count + 1;
            try {
                urls[count] = ParseUtil.fileToEncodedURL(f);
            } catch (IOException e2) {
            }
            count = count2;
        }
        if (urls.length == count) {
            return urls;
        }
        Object tmp = new URL[count];
        System.arraycopy(urls, 0, tmp, 0, count);
        return tmp;
    }

    public URL checkURL(URL url) {
        try {
            check(url);
            return url;
        } catch (Exception e) {
            return null;
        }
    }

    static void check(URL url) throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            URLConnection urlConnection = url.openConnection();
            Permission perm = urlConnection.getPermission();
            if (perm != null) {
                try {
                    security.checkPermission(perm);
                } catch (SecurityException se) {
                    if ((perm instanceof FilePermission) && perm.getActions().indexOf("read") != -1) {
                        security.checkRead(perm.getName());
                    } else if (!(perm instanceof SocketPermission) || perm.getActions().indexOf(SecurityConstants.SOCKET_CONNECT_ACTION) == -1) {
                        throw se;
                    } else {
                        URL locUrl = url;
                        if (urlConnection instanceof JarURLConnection) {
                            locUrl = ((JarURLConnection) urlConnection).getJarFileURL();
                        }
                        security.checkConnect(locUrl.getHost(), locUrl.getPort());
                    }
                }
            }
        }
    }
}
