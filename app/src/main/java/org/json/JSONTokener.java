package org.json;

public class JSONTokener {
    private final String in;
    private int pos;

    public JSONTokener(String in) {
        if (in != null && in.startsWith("﻿")) {
            in = in.substring(1);
        }
        this.in = in;
    }

    public Object nextValue() throws JSONException {
        int c = nextCleanInternal();
        switch (c) {
            case -1:
                throw syntaxError("End of input");
            case 34:
            case 39:
                return nextString((char) c);
            case 91:
                return readArray();
            case 123:
                return readObject();
            default:
                this.pos--;
                return readLiteral();
        }
    }

    private int nextCleanInternal() throws JSONException {
        while (this.pos < this.in.length()) {
            String str = this.in;
            int i = this.pos;
            this.pos = i + 1;
            int c = str.charAt(i);
            switch (c) {
                case 9:
                case 10:
                case 13:
                case 32:
                    break;
                case 35:
                    skipToEndOfLine();
                    break;
                case 47:
                    if (this.pos == this.in.length()) {
                        return c;
                    }
                    switch (this.in.charAt(this.pos)) {
                        case '*':
                            this.pos++;
                            int commentEnd = this.in.indexOf("*/", this.pos);
                            if (commentEnd != -1) {
                                this.pos = commentEnd + 2;
                                break;
                            }
                            throw syntaxError("Unterminated comment");
                        case '/':
                            this.pos++;
                            skipToEndOfLine();
                            break;
                        default:
                            return c;
                    }
                default:
                    return c;
            }
        }
        return -1;
    }

    private void skipToEndOfLine() {
        while (this.pos < this.in.length()) {
            char c = this.in.charAt(this.pos);
            if (c == 13 || c == 10) {
                this.pos++;
                return;
            }
            this.pos++;
        }
    }

    public String nextString(char quote) throws JSONException {
        StringBuilder builder = null;
        int start = this.pos;
        while (this.pos < this.in.length()) {
            String str = this.in;
            int i = this.pos;
            this.pos = i + 1;
            char c = str.charAt(i);
            if (c == quote) {
                if (builder == null) {
                    return new String(this.in.substring(start, this.pos - 1));
                }
                builder.append(this.in, start, this.pos - 1);
                return builder.toString();
            } else if (c == '\\') {
                if (this.pos == this.in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append(this.in, start, this.pos - 1);
                builder.append(readEscapeCharacter());
                start = this.pos;
            }
        }
        throw syntaxError("Unterminated string");
    }

    private char readEscapeCharacter() throws JSONException {
        String str = this.in;
        int i = this.pos;
        this.pos = i + 1;
        char escaped = str.charAt(i);
        switch (escaped) {
            case 'b':
                return 8;
            case 'f':
                return 12;
            case 'n':
                return 10;
            case 'r':
                return 13;
            case 't':
                return 9;
            case 'u':
                if (this.pos + 4 > this.in.length()) {
                    throw syntaxError("Unterminated escape sequence");
                }
                String hex = this.in.substring(this.pos, this.pos + 4);
                this.pos += 4;
                try {
                    return (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    throw syntaxError("Invalid escape sequence: " + hex);
                }
            default:
                return escaped;
        }
    }

    private Object readLiteral() throws JSONException {
        String literal = nextToInternal("{}[]/\\:,=;# \t\f");
        if (literal.length() == 0) {
            throw syntaxError("Expected literal value");
        } else if ("null".equalsIgnoreCase(literal)) {
            return JSONObject.NULL;
        } else {
            if ("true".equalsIgnoreCase(literal)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(literal)) {
                return Boolean.FALSE;
            }
            if (literal.indexOf(46) == -1) {
                int base = 10;
                String number = literal;
                if (literal.startsWith("0x") || literal.startsWith("0X")) {
                    number = literal.substring(2);
                    base = 16;
                } else if (literal.startsWith(AndroidHardcodedSystemProperties.JAVA_VERSION) && literal.length() > 1) {
                    number = literal.substring(1);
                    base = 8;
                }
                try {
                    long longValue = Long.parseLong(number, base);
                    if (longValue > 2147483647L || longValue < -2147483648L) {
                        return Long.valueOf(longValue);
                    }
                    return Integer.valueOf((int) longValue);
                } catch (NumberFormatException e) {
                }
            }
            try {
                return Double.valueOf(literal);
            } catch (NumberFormatException e2) {
                return new String(literal);
            }
        }
    }

    private String nextToInternal(String excluded) {
        int start = this.pos;
        while (this.pos < this.in.length()) {
            char c = this.in.charAt(this.pos);
            if (c == 13 || c == 10 || excluded.indexOf(c) != -1) {
                return this.in.substring(start, this.pos);
            }
            this.pos++;
        }
        return this.in.substring(start);
    }

    private org.json.JSONObject readObject() throws org.json.JSONException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxOverflowException: Regions stack size limit reached
	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:37)
	at jadx.core.utils.ErrorsCounter.methodError(ErrorsCounter.java:61)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:29)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
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
        r6 = this;
        r2 = new org.json.JSONObject;
        r2.<init>();
        r0 = r6.nextCleanInternal();
        r4 = 125; // 0x7d float:1.75E-43 double:6.2E-322;
        if (r0 != r4) goto L_0x000e;
    L_0x000d:
        return r2;
    L_0x000e:
        r4 = -1;
        if (r0 == r4) goto L_0x0017;
    L_0x0011:
        r4 = r6.pos;
        r4 = r4 + -1;
        r6.pos = r4;
    L_0x0017:
        r1 = r6.nextValue();
        r4 = r1 instanceof java.lang.String;
        if (r4 != 0) goto L_0x0055;
    L_0x001f:
        if (r1 != 0) goto L_0x0029;
    L_0x0021:
        r4 = "Names cannot be null";
        r4 = r6.syntaxError(r4);
        throw r4;
    L_0x0029:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Names must be strings, but ";
        r4 = r4.append(r5);
        r4 = r4.append(r1);
        r5 = " is of type ";
        r4 = r4.append(r5);
        r5 = r1.getClass();
        r5 = r5.getName();
        r4 = r4.append(r5);
        r4 = r4.toString();
        r4 = r6.syntaxError(r4);
        throw r4;
    L_0x0055:
        r3 = r6.nextCleanInternal();
        r4 = 58;
        if (r3 == r4) goto L_0x007a;
    L_0x005d:
        r4 = 61;
        if (r3 == r4) goto L_0x007a;
    L_0x0061:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Expected ':' after ";
        r4 = r4.append(r5);
        r4 = r4.append(r1);
        r4 = r4.toString();
        r4 = r6.syntaxError(r4);
        throw r4;
    L_0x007a:
        r4 = r6.pos;
        r5 = r6.in;
        r5 = r5.length();
        if (r4 >= r5) goto L_0x0096;
    L_0x0084:
        r4 = r6.in;
        r5 = r6.pos;
        r4 = r4.charAt(r5);
        r5 = 62;
        if (r4 != r5) goto L_0x0096;
    L_0x0090:
        r4 = r6.pos;
        r4 = r4 + 1;
        r6.pos = r4;
    L_0x0096:
        r1 = (java.lang.String) r1;
        r4 = r6.nextValue();
        r2.put(r1, r4);
        r4 = r6.nextCleanInternal();
        switch(r4) {
            case 44: goto L_0x0017;
            case 59: goto L_0x0017;
            case 125: goto L_0x00ae;
            default: goto L_0x00a6;
        };
    L_0x00a6:
        r4 = "Unterminated object";
        r4 = r6.syntaxError(r4);
        throw r4;
    L_0x00ae:
        return r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.json.JSONTokener.readObject():org.json.JSONObject");
    }

    private JSONArray readArray() throws JSONException {
        JSONArray result = new JSONArray();
        boolean hasTrailingSeparator = false;
        while (true) {
            switch (nextCleanInternal()) {
                case -1:
                    throw syntaxError("Unterminated array");
                case 44:
                case 59:
                    result.put(null);
                    hasTrailingSeparator = true;
                    break;
                case 93:
                    if (hasTrailingSeparator) {
                        result.put(null);
                    }
                    return result;
                default:
                    this.pos--;
                    result.put(nextValue());
                    switch (nextCleanInternal()) {
                        case 44:
                        case 59:
                            hasTrailingSeparator = true;
                            break;
                        case 93:
                            return result;
                        default:
                            throw syntaxError("Unterminated array");
                    }
            }
        }
    }

    public JSONException syntaxError(String message) {
        return new JSONException(message + this);
    }

    public String toString() {
        return " at character " + this.pos + " of " + this.in;
    }

    public boolean more() {
        return this.pos < this.in.length();
    }

    public char next() {
        if (this.pos >= this.in.length()) {
            return 0;
        }
        String str = this.in;
        int i = this.pos;
        this.pos = i + 1;
        return str.charAt(i);
    }

    public char next(char c) throws JSONException {
        char result = next();
        if (result == c) {
            return result;
        }
        throw syntaxError("Expected " + c + " but was " + result);
    }

    public char nextClean() throws JSONException {
        int nextCleanInt = nextCleanInternal();
        return nextCleanInt == -1 ? 0 : (char) nextCleanInt;
    }

    public String next(int length) throws JSONException {
        if (this.pos + length > this.in.length()) {
            throw syntaxError(length + " is out of bounds");
        }
        String result = this.in.substring(this.pos, this.pos + length);
        this.pos += length;
        return result;
    }

    public String nextTo(String excluded) {
        if (excluded != null) {
            return nextToInternal(excluded).trim();
        }
        throw new NullPointerException("excluded == null");
    }

    public String nextTo(char excluded) {
        return nextToInternal(String.valueOf(excluded)).trim();
    }

    public void skipPast(String thru) {
        int thruStart = this.in.indexOf(thru, this.pos);
        this.pos = thruStart == -1 ? this.in.length() : thru.length() + thruStart;
    }

    public char skipTo(char to) {
        int index = this.in.indexOf(to, this.pos);
        if (index == -1) {
            return 0;
        }
        this.pos = index;
        return to;
    }

    public void back() {
        int i = this.pos - 1;
        this.pos = i;
        if (i == -1) {
            this.pos = 0;
        }
    }

    public static int dehexchar(char hex) {
        if (hex >= '0' && hex <= '9') {
            return hex - 48;
        }
        if (hex >= 'A' && hex <= 'F') {
            return (hex - 65) + 10;
        }
        if (hex < 'a' || hex > 'f') {
            return -1;
        }
        return (hex - 97) + 10;
    }
}
