package org.apache.xml.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.apache.xalan.templates.Constants;

class ObjectFactory {
    private static final boolean DEBUG = false;
    private static final String DEFAULT_PROPERTIES_FILENAME = "xalan.properties";
    private static final String SERVICES_PATH = "META-INF/services/";
    private static long fLastModified = -1;
    private static Properties fXalanProperties = null;

    static class ConfigurationError extends Error {
        static final long serialVersionUID = 2036619216663421552L;
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }

        Exception getException() {
            return this.exception;
        }
    }

    ObjectFactory() {
    }

    static Object createObject(String factoryId, String fallbackClassName) throws ConfigurationError {
        return createObject(factoryId, null, fallbackClassName);
    }

    static Object createObject(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        Class factoryClass = lookUpFactoryClass(factoryId, propertiesFilename, fallbackClassName);
        if (factoryClass == null) {
            throw new ConfigurationError("Provider for " + factoryId + " cannot be found", null);
        }
        try {
            Object instance = factoryClass.newInstance();
            debugPrintln("created new instance of factory " + factoryId);
            return instance;
        } catch (Exception x) {
            throw new ConfigurationError("Provider for factory " + factoryId + " could not be instantiated: " + x, x);
        }
    }

    static Class lookUpFactoryClass(String factoryId) throws ConfigurationError {
        return lookUpFactoryClass(factoryId, null, null);
    }

    static Class lookUpFactoryClass(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        String factoryClassName = lookUpFactoryClassName(factoryId, propertiesFilename, fallbackClassName);
        ClassLoader cl = findClassLoader();
        if (factoryClassName == null) {
            factoryClassName = fallbackClassName;
        }
        try {
            Class providerClass = findProviderClass(factoryClassName, cl, true);
            debugPrintln("created new instance of " + providerClass + " using ClassLoader: " + cl);
            return providerClass;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + factoryClassName + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + factoryClassName + " could not be instantiated: " + x2, x2);
        }
    }

    static String lookUpFactoryClassName(String factoryId, String propertiesFilename, String fallbackClassName) {
        SecuritySupport ss = SecuritySupport.getInstance();
        try {
            String systemProp = ss.getSystemProperty(factoryId);
            if (systemProp != null) {
                debugPrintln("found system property, value=" + systemProp);
                return systemProp;
            }
        } catch (SecurityException e) {
        }
        String factoryClassName = null;
        FileInputStream fis;
        if (propertiesFilename == null) {
            File propertiesFile = null;
            boolean propertiesFileExists = false;
            try {
                propertiesFilename = ss.getSystemProperty("java.home") + File.separator + "lib" + File.separator + DEFAULT_PROPERTIES_FILENAME;
                File propertiesFile2 = new File(propertiesFilename);
                try {
                    propertiesFileExists = ss.getFileExists(propertiesFile2);
                    propertiesFile = propertiesFile2;
                } catch (SecurityException e2) {
                    propertiesFile = propertiesFile2;
                    fLastModified = -1;
                    fXalanProperties = null;
                    synchronized (ObjectFactory.class) {
                        boolean loadProperties = false;
                        fis = null;
                        try {
                            if (fLastModified >= 0) {
                                if (propertiesFileExists) {
                                    long j = fLastModified;
                                    long lastModified = ss.getLastModified(propertiesFile);
                                    fLastModified = lastModified;
                                    if (j < lastModified) {
                                        loadProperties = true;
                                    }
                                }
                                if (!propertiesFileExists) {
                                    fLastModified = -1;
                                    fXalanProperties = null;
                                }
                            } else if (propertiesFileExists) {
                                loadProperties = true;
                                fLastModified = ss.getLastModified(propertiesFile);
                            }
                            if (loadProperties) {
                                fXalanProperties = new Properties();
                                fis = ss.getFileInputStream(propertiesFile);
                                fXalanProperties.load(fis);
                            }
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (IOException e3) {
                                }
                            }
                        } catch (Exception e4) {
                            fXalanProperties = null;
                            fLastModified = -1;
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (IOException e5) {
                                }
                            }
                        } catch (Throwable th) {
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (IOException e6) {
                                }
                            }
                        }
                    }
                    if (fXalanProperties != null) {
                        factoryClassName = fXalanProperties.getProperty(factoryId);
                    }
                    if (factoryClassName != null) {
                        return findJarServiceProviderName(factoryId);
                    }
                    debugPrintln("found in " + propertiesFilename + ", value=" + factoryClassName);
                    return factoryClassName;
                }
            } catch (SecurityException e7) {
                fLastModified = -1;
                fXalanProperties = null;
                synchronized (ObjectFactory.class) {
                    boolean loadProperties2 = false;
                    fis = null;
                    try {
                        if (fLastModified >= 0) {
                            if (propertiesFileExists) {
                                long j2 = fLastModified;
                                long lastModified2 = ss.getLastModified(propertiesFile);
                                fLastModified = lastModified2;
                                if (j2 < lastModified2) {
                                    loadProperties2 = true;
                                }
                            }
                            if (!propertiesFileExists) {
                                fLastModified = -1;
                                fXalanProperties = null;
                            }
                        } else if (propertiesFileExists) {
                            loadProperties2 = true;
                            fLastModified = ss.getLastModified(propertiesFile);
                        }
                        if (loadProperties2) {
                            fXalanProperties = new Properties();
                            fis = ss.getFileInputStream(propertiesFile);
                            fXalanProperties.load(fis);
                        }
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e32) {
                            }
                        }
                    } catch (Exception e42) {
                        fXalanProperties = null;
                        fLastModified = -1;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e52) {
                            }
                        }
                    } catch (Throwable th2) {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e62) {
                            }
                        }
                    }
                }
                if (fXalanProperties != null) {
                    factoryClassName = fXalanProperties.getProperty(factoryId);
                }
                if (factoryClassName != null) {
                    return findJarServiceProviderName(factoryId);
                }
                debugPrintln("found in " + propertiesFilename + ", value=" + factoryClassName);
                return factoryClassName;
            }
            synchronized (ObjectFactory.class) {
                boolean loadProperties22 = false;
                fis = null;
                try {
                    if (fLastModified >= 0) {
                        if (propertiesFileExists) {
                            long j22 = fLastModified;
                            long lastModified22 = ss.getLastModified(propertiesFile);
                            fLastModified = lastModified22;
                            if (j22 < lastModified22) {
                                loadProperties22 = true;
                            }
                        }
                        if (!propertiesFileExists) {
                            fLastModified = -1;
                            fXalanProperties = null;
                        }
                    } else if (propertiesFileExists) {
                        loadProperties22 = true;
                        fLastModified = ss.getLastModified(propertiesFile);
                    }
                    if (loadProperties22) {
                        fXalanProperties = new Properties();
                        fis = ss.getFileInputStream(propertiesFile);
                        fXalanProperties.load(fis);
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e322) {
                        }
                    }
                } catch (Exception e422) {
                    fXalanProperties = null;
                    fLastModified = -1;
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e522) {
                        }
                    }
                } catch (Throwable th22) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e622) {
                        }
                    }
                }
            }
            if (fXalanProperties != null) {
                factoryClassName = fXalanProperties.getProperty(factoryId);
            }
        } else {
            fis = null;
            try {
                fis = ss.getFileInputStream(new File(propertiesFilename));
                Properties props = new Properties();
                props.load(fis);
                factoryClassName = props.getProperty(factoryId);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e8) {
                    }
                }
            } catch (Exception e9) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e10) {
                    }
                }
            } catch (Throwable th3) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e11) {
                    }
                }
            }
        }
        if (factoryClassName != null) {
            return findJarServiceProviderName(factoryId);
        }
        debugPrintln("found in " + propertiesFilename + ", value=" + factoryClassName);
        return factoryClassName;
    }

    private static void debugPrintln(String msg) {
    }

    static ClassLoader findClassLoader() throws ConfigurationError {
        ClassLoader chain;
        SecuritySupport ss = SecuritySupport.getInstance();
        ClassLoader context = ss.getContextClassLoader();
        ClassLoader system = ss.getSystemClassLoader();
        for (chain = system; context != chain; chain = ss.getParentClassLoader(chain)) {
            if (chain == null) {
                return context;
            }
        }
        ClassLoader current = ObjectFactory.class.getClassLoader();
        for (chain = system; current != chain; chain = ss.getParentClassLoader(chain)) {
            if (chain == null) {
                return current;
            }
        }
        return system;
    }

    static Object newInstance(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError {
        try {
            Class providerClass = findProviderClass(className, cl, doFallback);
            Object instance = providerClass.newInstance();
            debugPrintln("created new instance of " + providerClass + " using ClassLoader: " + cl);
            return instance;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError("Provider " + className + " not found", x);
        } catch (Exception x2) {
            throw new ConfigurationError("Provider " + className + " could not be instantiated: " + x2, x2);
        }
    }

    static Class findProviderClass(String className, ClassLoader cl, boolean doFallback) throws ClassNotFoundException, ConfigurationError {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            try {
                int lastDot = className.lastIndexOf(Constants.ATTRVAL_THIS);
                String packageName = className;
                if (lastDot != -1) {
                    packageName = className.substring(0, lastDot);
                }
                security.checkPackageAccess(packageName);
            } catch (SecurityException e) {
                throw e;
            }
        }
        if (cl == null) {
            return Class.forName(className);
        }
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException x) {
            if (doFallback) {
                ClassLoader current = ObjectFactory.class.getClassLoader();
                if (current == null) {
                    return Class.forName(className);
                }
                if (cl != current) {
                    cl = current;
                    return current.loadClass(className);
                }
                throw x;
            }
            throw x;
        }
    }

    private static String findJarServiceProviderName(String factoryId) {
        SecuritySupport ss = SecuritySupport.getInstance();
        String serviceId = SERVICES_PATH + factoryId;
        ClassLoader cl = findClassLoader();
        InputStream is = ss.getResourceAsStream(cl, serviceId);
        if (is == null) {
            ClassLoader current = ObjectFactory.class.getClassLoader();
            if (cl != current) {
                cl = current;
                is = ss.getResourceAsStream(current, serviceId);
            }
        }
        if (is == null) {
            return null;
        }
        BufferedReader rd;
        debugPrintln("found jar resource=" + serviceId + " using ClassLoader: " + cl);
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }
        try {
            String factoryClassName = rd.readLine();
            try {
                rd.close();
            } catch (IOException e2) {
            }
            if (factoryClassName == null || ("".equals(factoryClassName) ^ 1) == 0) {
                return null;
            }
            debugPrintln("found in resource, value=" + factoryClassName);
            return factoryClassName;
        } catch (IOException e3) {
            try {
                rd.close();
            } catch (IOException e4) {
            }
            return null;
        } catch (Throwable th) {
            try {
                rd.close();
            } catch (IOException e5) {
            }
            throw th;
        }
    }
}
