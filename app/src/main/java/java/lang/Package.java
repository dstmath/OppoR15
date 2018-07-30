package java.lang;

import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import sun.reflect.CallerSensitive;

public class Package implements AnnotatedElement {
    private static Map<String, Manifest> mans = new HashMap(10);
    private static Map<String, Package> pkgs = new HashMap(31);
    private static Map<String, URL> urls = new HashMap(10);
    private final String implTitle;
    private final String implVendor;
    private final String implVersion;
    private final transient ClassLoader loader;
    private transient Class<?> packageInfo;
    private final String pkgName;
    private final URL sealBase;
    private final String specTitle;
    private final String specVendor;
    private final String specVersion;

    /* synthetic */ Package(String name, Manifest man, URL url, ClassLoader loader, Package -this4) {
        this(name, man, url, loader);
    }

    private static native String getSystemPackage0(String str);

    private static native String[] getSystemPackages0();

    public String getName() {
        return this.pkgName;
    }

    public String getSpecificationTitle() {
        return this.specTitle;
    }

    public String getSpecificationVersion() {
        return this.specVersion;
    }

    public String getSpecificationVendor() {
        return this.specVendor;
    }

    public String getImplementationTitle() {
        return this.implTitle;
    }

    public String getImplementationVersion() {
        return this.implVersion;
    }

    public String getImplementationVendor() {
        return this.implVendor;
    }

    public boolean isSealed() {
        return this.sealBase != null;
    }

    public boolean isSealed(URL url) {
        return url.equals(this.sealBase);
    }

    public boolean isCompatibleWith(String desired) throws NumberFormatException {
        if (this.specVersion == null || this.specVersion.length() < 1) {
            throw new NumberFormatException("Empty version string");
        }
        int i;
        String[] sa = this.specVersion.split("\\.", -1);
        int[] si = new int[sa.length];
        for (i = 0; i < sa.length; i++) {
            si[i] = Integer.parseInt(sa[i]);
            if (si[i] < 0) {
                throw NumberFormatException.forInputString("" + si[i]);
            }
        }
        String[] da = desired.split("\\.", -1);
        int[] di = new int[da.length];
        for (i = 0; i < da.length; i++) {
            di[i] = Integer.parseInt(da[i]);
            if (di[i] < 0) {
                throw NumberFormatException.forInputString("" + di[i]);
            }
        }
        int len = Math.max(di.length, si.length);
        i = 0;
        while (i < len) {
            int d = i < di.length ? di[i] : 0;
            int s = i < si.length ? si[i] : 0;
            if (s < d) {
                return false;
            }
            if (s > d) {
                return true;
            }
            i++;
        }
        return true;
    }

    @CallerSensitive
    public static Package getPackage(String name) {
        ClassLoader l = VMStack.getCallingClassLoader();
        if (l != null) {
            return l.getPackage(name);
        }
        return getSystemPackage(name);
    }

    @CallerSensitive
    public static Package[] getPackages() {
        ClassLoader l = VMStack.getCallingClassLoader();
        if (l != null) {
            return l.getPackages();
        }
        return getSystemPackages();
    }

    static Package getPackage(Class<?> c) {
        String name = c.getName();
        int i = name.lastIndexOf(46);
        if (i == -1) {
            return null;
        }
        name = name.substring(0, i);
        ClassLoader cl = c.getClassLoader();
        if (cl != null) {
            return cl.getPackage(name);
        }
        return getSystemPackage(name);
    }

    public int hashCode() {
        return this.pkgName.hashCode();
    }

    public String toString() {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 0 && targetSdkVersion <= 24) {
            return "package " + this.pkgName;
        }
        String spec = this.specTitle;
        String ver = this.specVersion;
        if (spec == null || spec.length() <= 0) {
            spec = "";
        } else {
            spec = ", " + spec;
        }
        if (ver == null || ver.length() <= 0) {
            ver = "";
        } else {
            ver = ", version " + ver;
        }
        return "package " + this.pkgName + spec + ver;
    }

    private Class<?> getPackageInfo() {
        if (this.packageInfo == null) {
            try {
                this.packageInfo = Class.forName(this.pkgName + ".package-info", false, this.loader);
            } catch (ClassNotFoundException e) {
                this.packageInfo = AnonymousClass1PackageInfoProxy.class;
            }
        }
        return this.packageInfo;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return getPackageInfo().getAnnotation(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return super.isAnnotationPresent(annotationClass);
    }

    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        return getPackageInfo().getAnnotationsByType(annotationClass);
    }

    public Annotation[] getAnnotations() {
        return getPackageInfo().getAnnotations();
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return getPackageInfo().getDeclaredAnnotation(annotationClass);
    }

    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        return getPackageInfo().getDeclaredAnnotationsByType(annotationClass);
    }

    public Annotation[] getDeclaredAnnotations() {
        return getPackageInfo().getDeclaredAnnotations();
    }

    Package(String name, String spectitle, String specversion, String specvendor, String impltitle, String implversion, String implvendor, URL sealbase, ClassLoader loader) {
        this.pkgName = name;
        this.implTitle = impltitle;
        this.implVersion = implversion;
        this.implVendor = implvendor;
        this.specTitle = spectitle;
        this.specVersion = specversion;
        this.specVendor = specvendor;
        this.sealBase = sealbase;
        this.loader = loader;
    }

    private Package(String name, Manifest man, URL url, ClassLoader loader) {
        String sealed = null;
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        URL sealBase = null;
        Attributes attr = man.getAttributes(name.replace('.', '/').concat("/"));
        if (attr != null) {
            specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = url;
        }
        this.pkgName = name;
        this.specTitle = specTitle;
        this.specVersion = specVersion;
        this.specVendor = specVendor;
        this.implTitle = implTitle;
        this.implVersion = implVersion;
        this.implVendor = implVendor;
        this.sealBase = sealBase;
        this.loader = loader;
    }

    static Package getSystemPackage(String name) {
        Package pkg;
        synchronized (pkgs) {
            pkg = (Package) pkgs.get(name);
            if (pkg == null) {
                name = name.replace('.', '/').concat("/");
                String fn = getSystemPackage0(name);
                if (fn != null) {
                    pkg = defineSystemPackage(name, fn);
                }
            }
        }
        return pkg;
    }

    static Package[] getSystemPackages() {
        Package[] packageArr;
        String[] names = getSystemPackages0();
        synchronized (pkgs) {
            for (int i = 0; i < names.length; i++) {
                defineSystemPackage(names[i], getSystemPackage0(names[i]));
            }
            packageArr = (Package[]) pkgs.values().toArray(new Package[pkgs.size()]);
        }
        return packageArr;
    }

    private static Package defineSystemPackage(final String iname, final String fn) {
        return (Package) AccessController.doPrivileged(new PrivilegedAction<Package>() {
            public java.lang.Package run() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r1_2 'pkg' java.lang.Package) in PHI: PHI: (r1_1 'pkg' java.lang.Package) = (r1_0 'pkg' java.lang.Package), (r1_2 'pkg' java.lang.Package) binds: {(r1_0 'pkg' java.lang.Package)=B:11:0x006c, (r1_2 'pkg' java.lang.Package)=B:15:0x007d}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
                /*
                r17 = this;
                r0 = r17;
                r2 = r1;
                r5 = java.lang.Package.urls;
                r0 = r17;
                r6 = r2;
                r4 = r5.get(r6);
                r4 = (java.net.URL) r4;
                if (r4 != 0) goto L_0x0049;
            L_0x0014:
                r16 = new java.io.File;
                r0 = r17;
                r5 = r2;
                r0 = r16;
                r0.<init>(r5);
                r4 = sun.net.www.ParseUtil.fileToEncodedURL(r16);	 Catch:{ MalformedURLException -> 0x007b }
            L_0x0023:
                if (r4 == 0) goto L_0x0049;
            L_0x0025:
                r5 = java.lang.Package.urls;
                r0 = r17;
                r6 = r2;
                r5.put(r6, r4);
                r5 = r16.isFile();
                if (r5 == 0) goto L_0x0049;
            L_0x0036:
                r5 = java.lang.Package.mans;
                r0 = r17;
                r6 = r2;
                r0 = r17;
                r7 = r2;
                r7 = java.lang.Package.loadManifest(r7);
                r5.put(r6, r7);
            L_0x0049:
                r5 = r2.length();
                r5 = r5 + -1;
                r6 = 0;
                r5 = r2.substring(r6, r5);
                r6 = 47;
                r7 = 46;
                r2 = r5.replace(r6, r7);
                r5 = java.lang.Package.mans;
                r0 = r17;
                r6 = r2;
                r3 = r5.get(r6);
                r3 = (java.util.jar.Manifest) r3;
                if (r3 == 0) goto L_0x007d;
            L_0x006c:
                r1 = new java.lang.Package;
                r5 = 0;
                r6 = 0;
                r1.<init>(r2, r3, r4, r5, r6);
            L_0x0073:
                r5 = java.lang.Package.pkgs;
                r5.put(r2, r1);
                return r1;
            L_0x007b:
                r15 = move-exception;
                goto L_0x0023;
            L_0x007d:
                r1 = new java.lang.Package;
                r7 = 0;
                r8 = 0;
                r9 = 0;
                r10 = 0;
                r11 = 0;
                r12 = 0;
                r13 = 0;
                r14 = 0;
                r5 = r1;
                r6 = r2;
                r5.<init>(r6, r7, r8, r9, r10, r11, r12, r13, r14);
                goto L_0x0073;
                */
                throw new UnsupportedOperationException("Method not decompiled: java.lang.Package.1.run():java.lang.Package");
            }
        });
    }

    private static Manifest loadManifest(String fn) {
        Throwable th;
        Throwable th2;
        Throwable th3;
        FileInputStream fis = null;
        JarInputStream jis = null;
        try {
            FileInputStream fis2 = new FileInputStream(fn);
            try {
                JarInputStream jis2 = new JarInputStream(fis2, false);
                try {
                    Manifest manifest = jis2.getManifest();
                    if (jis2 != null) {
                        try {
                            jis2.close();
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    }
                    th = null;
                    if (fis2 != null) {
                        try {
                            fis2.close();
                        } catch (Throwable th5) {
                            th2 = th5;
                            if (th != null) {
                                if (th != th2) {
                                    th.addSuppressed(th2);
                                    th2 = th;
                                }
                            }
                        }
                    }
                    th2 = th;
                    if (th2 == null) {
                        return manifest;
                    }
                    try {
                        throw th2;
                    } catch (IOException e) {
                        fis = fis2;
                        return null;
                    }
                } catch (Throwable th6) {
                    th2 = th6;
                    jis = jis2;
                    fis = fis2;
                    th = null;
                    if (jis != null) {
                        try {
                            jis.close();
                        } catch (Throwable th7) {
                            th3 = th7;
                            if (th != null) {
                                if (th != th3) {
                                    th.addSuppressed(th3);
                                    th3 = th;
                                }
                            }
                        }
                    }
                    th3 = th;
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable th8) {
                            th = th8;
                            if (th3 != null) {
                                if (th3 != th) {
                                    th3.addSuppressed(th);
                                    th = th3;
                                }
                            }
                        }
                    }
                    th = th3;
                    if (th != null) {
                        try {
                            throw th;
                        } catch (IOException e2) {
                            return null;
                        }
                    }
                    throw th2;
                }
            } catch (Throwable th9) {
                th2 = th9;
                fis = fis2;
                th = null;
                if (jis != null) {
                    try {
                        jis.close();
                    } catch (Throwable th72) {
                        th3 = th72;
                        if (th != null) {
                            if (th != th3) {
                                th.addSuppressed(th3);
                                th3 = th;
                            }
                        }
                    }
                }
                th3 = th;
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable th82) {
                        th = th82;
                        if (th3 != null) {
                            if (th3 != th) {
                                th3.addSuppressed(th);
                                th = th3;
                            }
                        }
                    }
                }
                th = th3;
                if (th != null) {
                    throw th2;
                }
                try {
                    throw th;
                } catch (IOException e22) {
                    return null;
                }
            }
        } catch (Throwable th10) {
            th2 = th10;
            th = null;
            if (jis != null) {
                try {
                    jis.close();
                } catch (Throwable th722) {
                    th3 = th722;
                    if (th != null) {
                        if (th != th3) {
                            th.addSuppressed(th3);
                            th3 = th;
                        }
                    }
                }
            }
            th3 = th;
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable th822) {
                    th = th822;
                    if (th3 != null) {
                        if (th3 != th) {
                            th3.addSuppressed(th);
                            th = th3;
                        }
                    }
                }
            }
            th = th3;
            if (th != null) {
                try {
                    throw th;
                } catch (IOException e222) {
                    return null;
                }
            }
            throw th2;
        }
    }
}
