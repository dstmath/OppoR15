package org.apache.xml.dtm;

import java.io.PrintStream;
import java.io.PrintWriter;
import javax.xml.transform.SourceLocator;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;

public class DTMException extends RuntimeException {
    static final long serialVersionUID = -775576419181334734L;
    Throwable containedException;
    SourceLocator locator;

    public SourceLocator getLocator() {
        return this.locator;
    }

    public void setLocator(SourceLocator location) {
        this.locator = location;
    }

    public Throwable getException() {
        return this.containedException;
    }

    public Throwable getCause() {
        if (this.containedException == this) {
            return null;
        }
        return this.containedException;
    }

    public synchronized Throwable initCause(Throwable cause) {
        if (this.containedException == null && cause != null) {
            throw new IllegalStateException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CANNOT_OVERWRITE_CAUSE, null));
        } else if (cause == this) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_SELF_CAUSATION_NOT_PERMITTED, null));
        } else {
            this.containedException = cause;
        }
        return this;
    }

    public DTMException(String message) {
        super(message);
        this.containedException = null;
        this.locator = null;
    }

    public DTMException(Throwable e) {
        super(e.getMessage());
        this.containedException = e;
        this.locator = null;
    }

    public DTMException(String message, Throwable e) {
        if (message == null || message.length() == 0) {
            message = e.getMessage();
        }
        super(message);
        this.containedException = e;
        this.locator = null;
    }

    public DTMException(String message, SourceLocator locator) {
        super(message);
        this.containedException = null;
        this.locator = locator;
    }

    public DTMException(String message, SourceLocator locator, Throwable e) {
        super(message);
        this.containedException = e;
        this.locator = locator;
    }

    public String getMessageAndLocation() {
        StringBuffer sbuffer = new StringBuffer();
        String message = super.getMessage();
        if (message != null) {
            sbuffer.append(message);
        }
        if (this.locator != null) {
            String systemID = this.locator.getSystemId();
            int line = this.locator.getLineNumber();
            int column = this.locator.getColumnNumber();
            if (systemID != null) {
                sbuffer.append("; SystemID: ");
                sbuffer.append(systemID);
            }
            if (line != 0) {
                sbuffer.append("; Line#: ");
                sbuffer.append(line);
            }
            if (column != 0) {
                sbuffer.append("; Column#: ");
                sbuffer.append(column);
            }
        }
        return sbuffer.toString();
    }

    public String getLocationAsString() {
        if (this.locator == null) {
            return null;
        }
        StringBuffer sbuffer = new StringBuffer();
        String systemID = this.locator.getSystemId();
        int line = this.locator.getLineNumber();
        int column = this.locator.getColumnNumber();
        if (systemID != null) {
            sbuffer.append("; SystemID: ");
            sbuffer.append(systemID);
        }
        if (line != 0) {
            sbuffer.append("; Line#: ");
            sbuffer.append(line);
        }
        if (column != 0) {
            sbuffer.append("; Column#: ");
            sbuffer.append(column);
        }
        return sbuffer.toString();
    }

    public void printStackTrace() {
        printStackTrace(new PrintWriter(System.err, true));
    }

    public void printStackTrace(PrintStream s) {
        printStackTrace(new PrintWriter(s));
    }

    public void printStackTrace(java.io.PrintWriter r17) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r17_1 's' java.io.PrintWriter) in PHI: PHI: (r17_2 's' java.io.PrintWriter) = (r17_0 's' java.io.PrintWriter), (r17_1 's' java.io.PrintWriter) binds: {(r17_0 's' java.io.PrintWriter)=B:0:0x0000, (r17_1 's' java.io.PrintWriter)=B:1:0x0002}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r16 = this;
        if (r17 != 0) goto L_0x000c;
    L_0x0002:
        r17 = new java.io.PrintWriter;
        r11 = java.lang.System.err;
        r12 = 1;
        r0 = r17;
        r0.<init>(r11, r12);
    L_0x000c:
        r7 = r16.getLocationAsString();	 Catch:{ Throwable -> 0x009c }
        if (r7 == 0) goto L_0x0017;	 Catch:{ Throwable -> 0x009c }
    L_0x0012:
        r0 = r17;	 Catch:{ Throwable -> 0x009c }
        r0.println(r7);	 Catch:{ Throwable -> 0x009c }
    L_0x0017:
        super.printStackTrace(r17);	 Catch:{ Throwable -> 0x009c }
    L_0x001a:
        r5 = 0;
        r12 = java.lang.Throwable.class;	 Catch:{ NoSuchMethodException -> 0x009a }
        r13 = "getCause";	 Catch:{ NoSuchMethodException -> 0x009a }
        r11 = 1;	 Catch:{ NoSuchMethodException -> 0x009a }
        r14 = new java.lang.Class[r11];	 Catch:{ NoSuchMethodException -> 0x009a }
        r11 = 0;	 Catch:{ NoSuchMethodException -> 0x009a }
        r11 = (java.lang.Class) r11;	 Catch:{ NoSuchMethodException -> 0x009a }
        r15 = 0;	 Catch:{ NoSuchMethodException -> 0x009a }
        r14[r15] = r11;	 Catch:{ NoSuchMethodException -> 0x009a }
        r12.getMethod(r13, r14);	 Catch:{ NoSuchMethodException -> 0x009a }
        r5 = 1;
    L_0x002d:
        if (r5 != 0) goto L_0x0082;
    L_0x002f:
        r2 = r16.getException();
        r3 = 0;
    L_0x0034:
        r11 = 10;
        if (r3 >= r11) goto L_0x0082;
    L_0x0038:
        if (r2 == 0) goto L_0x0082;
    L_0x003a:
        r11 = "---------";
        r0 = r17;
        r0.println(r11);
        r11 = r2 instanceof org.apache.xml.dtm.DTMException;	 Catch:{ Throwable -> 0x0083 }
        if (r11 == 0) goto L_0x0055;	 Catch:{ Throwable -> 0x0083 }
    L_0x0046:
        r0 = r2;	 Catch:{ Throwable -> 0x0083 }
        r0 = (org.apache.xml.dtm.DTMException) r0;	 Catch:{ Throwable -> 0x0083 }
        r11 = r0;	 Catch:{ Throwable -> 0x0083 }
        r7 = r11.getLocationAsString();	 Catch:{ Throwable -> 0x0083 }
        if (r7 == 0) goto L_0x0055;	 Catch:{ Throwable -> 0x0083 }
    L_0x0050:
        r0 = r17;	 Catch:{ Throwable -> 0x0083 }
        r0.println(r7);	 Catch:{ Throwable -> 0x0083 }
    L_0x0055:
        r0 = r17;	 Catch:{ Throwable -> 0x0083 }
        r2.printStackTrace(r0);	 Catch:{ Throwable -> 0x0083 }
    L_0x005a:
        r12 = r2.getClass();	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r13 = "getException";	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r11 = 1;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r14 = new java.lang.Class[r11];	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r11 = 0;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r11 = (java.lang.Class) r11;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r15 = 0;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r14[r15] = r11;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r8 = r12.getMethod(r13, r14);	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        if (r8 == 0) goto L_0x008d;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
    L_0x0070:
        r10 = r2;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r11 = 1;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r12 = new java.lang.Object[r11];	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r11 = 0;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r11 = (java.lang.Class) r11;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r13 = 0;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r12[r13] = r11;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r2 = r8.invoke(r2, r12);	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        r2 = (java.lang.Throwable) r2;	 Catch:{ InvocationTargetException -> 0x0097, IllegalAccessException -> 0x0094, NoSuchMethodException -> 0x0091 }
        if (r10 != r2) goto L_0x008e;
    L_0x0082:
        return;
    L_0x0083:
        r1 = move-exception;
        r11 = "Could not print stack trace...";
        r0 = r17;
        r0.println(r11);
        goto L_0x005a;
    L_0x008d:
        r2 = 0;
    L_0x008e:
        r3 = r3 + 1;
        goto L_0x0034;
    L_0x0091:
        r9 = move-exception;
        r2 = 0;
        goto L_0x008e;
    L_0x0094:
        r4 = move-exception;
        r2 = 0;
        goto L_0x008e;
    L_0x0097:
        r6 = move-exception;
        r2 = 0;
        goto L_0x008e;
    L_0x009a:
        r9 = move-exception;
        goto L_0x002d;
    L_0x009c:
        r1 = move-exception;
        goto L_0x001a;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.xml.dtm.DTMException.printStackTrace(java.io.PrintWriter):void");
    }
}
