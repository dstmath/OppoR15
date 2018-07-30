package android.icu.text;

import android.icu.impl.IllegalIcuArgumentException;
import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Normalizer.Mode;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransliteratorParser {
    private static final char ALT_FORWARD_RULE_OP = '→';
    private static final char ALT_FUNCTION = '∆';
    private static final char ALT_FWDREV_RULE_OP = '↔';
    private static final char ALT_REVERSE_RULE_OP = '←';
    private static final char ANCHOR_START = '^';
    private static final char CONTEXT_ANTE = '{';
    private static final char CONTEXT_POST = '}';
    private static final char CURSOR_OFFSET = '@';
    private static final char CURSOR_POS = '|';
    private static final char DOT = '.';
    private static final String DOT_SET = "[^[:Zp:][:Zl:]\\r\\n$]";
    private static final char END_OF_RULE = ';';
    private static final char ESCAPE = '\\';
    private static final char FORWARD_RULE_OP = '>';
    private static final char FUNCTION = '&';
    private static final char FWDREV_RULE_OP = '~';
    private static final String HALF_ENDERS = "=><←→↔;";
    private static final String ID_TOKEN = "::";
    private static final int ID_TOKEN_LEN = 2;
    private static UnicodeSet ILLEGAL_FUNC = new UnicodeSet("[\\^\\(\\.\\*\\+\\?\\{\\}\\|\\@]");
    private static UnicodeSet ILLEGAL_SEG = new UnicodeSet("[\\{\\}\\|\\@]");
    private static UnicodeSet ILLEGAL_TOP = new UnicodeSet("[\\)]");
    private static final char KLEENE_STAR = '*';
    private static final char ONE_OR_MORE = '+';
    private static final String OPERATORS = "=><←→↔";
    private static final char QUOTE = '\'';
    private static final char REVERSE_RULE_OP = '<';
    private static final char RULE_COMMENT_CHAR = '#';
    private static final char SEGMENT_CLOSE = ')';
    private static final char SEGMENT_OPEN = '(';
    private static final char VARIABLE_DEF_OP = '=';
    private static final char ZERO_OR_ONE = '?';
    public UnicodeSet compoundFilter;
    private Data curData;
    public List<Data> dataVector;
    private int direction;
    private int dotStandIn = -1;
    public List<String> idBlockVector;
    private ParseData parseData;
    private List<StringMatcher> segmentObjects;
    private StringBuffer segmentStandins;
    private String undefinedVariableName;
    private char variableLimit;
    private Map<String, char[]> variableNames;
    private char variableNext;
    private List<Object> variablesVector;

    private class ParseData implements SymbolTable {
        /* synthetic */ ParseData(TransliteratorParser this$0, ParseData -this1) {
            this();
        }

        private ParseData() {
        }

        public char[] lookup(String name) {
            return (char[]) TransliteratorParser.this.variableNames.get(name);
        }

        public UnicodeMatcher lookupMatcher(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i < 0 || i >= TransliteratorParser.this.variablesVector.size()) {
                return null;
            }
            return (UnicodeMatcher) TransliteratorParser.this.variablesVector.get(i);
        }

        public String parseReference(String text, ParsePosition pos, int limit) {
            int start = pos.getIndex();
            int i = start;
            while (i < limit) {
                char c = text.charAt(i);
                if ((i == start && (UCharacter.isUnicodeIdentifierStart(c) ^ 1) != 0) || (UCharacter.isUnicodeIdentifierPart(c) ^ 1) != 0) {
                    break;
                }
                i++;
            }
            if (i == start) {
                return null;
            }
            pos.setIndex(i);
            return text.substring(start, i);
        }

        public boolean isMatcher(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i < 0 || i >= TransliteratorParser.this.variablesVector.size()) {
                return true;
            }
            return TransliteratorParser.this.variablesVector.get(i) instanceof UnicodeMatcher;
        }

        public boolean isReplacer(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i < 0 || i >= TransliteratorParser.this.variablesVector.size()) {
                return true;
            }
            return TransliteratorParser.this.variablesVector.get(i) instanceof UnicodeReplacer;
        }
    }

    private static abstract class RuleBody {
        /* synthetic */ RuleBody(RuleBody -this0) {
            this();
        }

        abstract String handleNextLine();

        abstract void reset();

        private RuleBody() {
        }

        String nextLine() {
            String s = handleNextLine();
            if (s == null || s.length() <= 0 || s.charAt(s.length() - 1) != '\\') {
                return s;
            }
            StringBuilder b = new StringBuilder(s);
            while (true) {
                b.deleteCharAt(b.length() - 1);
                s = handleNextLine();
                if (s != null) {
                    b.append(s);
                    if (s.length() <= 0 || s.charAt(s.length() - 1) != '\\') {
                        break;
                    }
                } else {
                    break;
                }
            }
            return b.toString();
        }
    }

    private static class RuleArray extends RuleBody {
        String[] array;
        int i = 0;

        public RuleArray(String[] array) {
            super();
            this.array = array;
        }

        public String handleNextLine() {
            if (this.i >= this.array.length) {
                return null;
            }
            String[] strArr = this.array;
            int i = this.i;
            this.i = i + 1;
            return strArr[i];
        }

        public void reset() {
            this.i = 0;
        }
    }

    private static class RuleHalf {
        public boolean anchorEnd;
        public boolean anchorStart;
        public int ante;
        public int cursor;
        public int cursorOffset;
        private int cursorOffsetPos;
        private int nextSegmentNumber;
        public int post;
        public String text;

        /* synthetic */ RuleHalf(RuleHalf -this0) {
            this();
        }

        private RuleHalf() {
            this.cursor = -1;
            this.ante = -1;
            this.post = -1;
            this.cursorOffset = 0;
            this.cursorOffsetPos = 0;
            this.anchorStart = false;
            this.anchorEnd = false;
            this.nextSegmentNumber = 1;
        }

        public int parse(String rule, int pos, int limit, TransliteratorParser parser) {
            int start = pos;
            StringBuffer buf = new StringBuffer();
            pos = parseSection(rule, pos, limit, parser, buf, TransliteratorParser.ILLEGAL_TOP, false);
            this.text = buf.toString();
            if (this.cursorOffset > 0 && this.cursor != this.cursorOffsetPos) {
                TransliteratorParser.syntaxError("Misplaced |", rule, start);
            }
            return pos;
        }

        private int parseSection(java.lang.String r38, int r39, int r40, android.icu.text.TransliteratorParser r41, java.lang.StringBuffer r42, android.icu.text.UnicodeSet r43, boolean r44) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r25_2 'pp' java.text.ParsePosition) in PHI: PHI: (r25_3 'pp' java.text.ParsePosition) = (r25_1 'pp' java.text.ParsePosition), (r25_2 'pp' java.text.ParsePosition) binds: {(r25_1 'pp' java.text.ParsePosition)=B:15:0x0060, (r25_2 'pp' java.text.ParsePosition)=B:16:0x0062}
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
            r37 = this;
            r33 = r39;
            r25 = 0;
            r28 = -1;
            r27 = -1;
            r36 = -1;
            r35 = -1;
            r4 = 1;
            r0 = new int[r4];
            r17 = r0;
            r12 = r42.length();
            r23 = r39;
        L_0x0017:
            r0 = r23;
            r1 = r40;
            if (r0 >= r1) goto L_0x053c;
        L_0x001d:
            r39 = r23 + 1;
            r0 = r38;
            r1 = r23;
            r13 = r0.charAt(r1);
            r4 = android.icu.impl.PatternProps.isWhiteSpace(r13);
            if (r4 == 0) goto L_0x0030;
        L_0x002d:
            r23 = r39;
            goto L_0x0017;
        L_0x0030:
            r4 = "=><←→↔;";
            r4 = r4.indexOf(r13);
            if (r4 < 0) goto L_0x0046;
        L_0x0039:
            if (r44 == 0) goto L_0x0045;
        L_0x003b:
            r4 = "Unclosed segment";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0045:
            return r39;
        L_0x0046:
            r0 = r37;
            r4 = r0.anchorEnd;
            if (r4 == 0) goto L_0x0056;
        L_0x004c:
            r4 = "Malformed variable reference";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0056:
            r4 = r39 + -1;
            r0 = r38;
            r4 = android.icu.text.UnicodeSet.resemblesPattern(r0, r4);
            if (r4 == 0) goto L_0x0087;
        L_0x0060:
            if (r25 != 0) goto L_0x006a;
        L_0x0062:
            r25 = new java.text.ParsePosition;
            r4 = 0;
            r0 = r25;
            r0.<init>(r4);
        L_0x006a:
            r4 = r39 + -1;
            r0 = r25;
            r0.setIndex(r4);
            r0 = r41;
            r1 = r38;
            r2 = r25;
            r4 = r0.parseSet(r1, r2);
            r0 = r42;
            r0.append(r4);
            r39 = r25.getIndex();
            r23 = r39;
            goto L_0x0017;
        L_0x0087:
            r4 = 92;
            if (r13 != r4) goto L_0x00c8;
        L_0x008b:
            r0 = r39;
            r1 = r40;
            if (r0 != r1) goto L_0x009b;
        L_0x0091:
            r4 = "Trailing backslash";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x009b:
            r4 = 0;
            r17[r4] = r39;
            r0 = r38;
            r1 = r17;
            r15 = android.icu.impl.Utility.unescapeAt(r0, r1);
            r4 = 0;
            r39 = r17[r4];
            r4 = -1;
            if (r15 != r4) goto L_0x00b6;
        L_0x00ac:
            r4 = "Malformed escape";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x00b6:
            r0 = r41;
            r1 = r38;
            r2 = r33;
            r0.checkVariableRange(r15, r1, r2);
            r0 = r42;
            android.icu.text.UTF16.append(r0, r15);
            r23 = r39;
            goto L_0x0017;
        L_0x00c8:
            r4 = 39;
            if (r13 != r4) goto L_0x0141;
        L_0x00cc:
            r4 = 39;
            r0 = r38;
            r1 = r39;
            r16 = r0.indexOf(r4, r1);
            r0 = r16;
            r1 = r39;
            if (r0 != r1) goto L_0x00e7;
        L_0x00dc:
            r0 = r42;
            r0.append(r13);
            r39 = r39 + 1;
        L_0x00e3:
            r23 = r39;
            goto L_0x0017;
        L_0x00e7:
            r28 = r42.length();
        L_0x00eb:
            if (r16 >= 0) goto L_0x00f7;
        L_0x00ed:
            r4 = "Unterminated quote";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x00f7:
            r0 = r38;
            r1 = r39;
            r2 = r16;
            r4 = r0.substring(r1, r2);
            r0 = r42;
            r0.append(r4);
            r39 = r16 + 1;
            r0 = r39;
            r1 = r40;
            if (r0 >= r1) goto L_0x0121;
        L_0x010e:
            r4 = r38.charAt(r39);
            r7 = 39;
            if (r4 != r7) goto L_0x0121;
        L_0x0116:
            r4 = 39;
            r7 = r39 + 1;
            r0 = r38;
            r16 = r0.indexOf(r4, r7);
            goto L_0x00eb;
        L_0x0121:
            r27 = r42.length();
            r16 = r28;
        L_0x0127:
            r0 = r16;
            r1 = r27;
            if (r0 >= r1) goto L_0x00e3;
        L_0x012d:
            r0 = r42;
            r1 = r16;
            r4 = r0.charAt(r1);
            r0 = r41;
            r1 = r38;
            r2 = r33;
            r0.checkVariableRange(r4, r1, r2);
            r16 = r16 + 1;
            goto L_0x0127;
        L_0x0141:
            r0 = r41;
            r1 = r38;
            r2 = r33;
            r0.checkVariableRange(r13, r1, r2);
            r0 = r43;
            r4 = r0.contains(r13);
            if (r4 == 0) goto L_0x0173;
        L_0x0152:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = "Illegal character '";
            r4 = r4.append(r7);
            r4 = r4.append(r13);
            r7 = 39;
            r4 = r4.append(r7);
            r4 = r4.toString();
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0173:
            switch(r13) {
                case 36: goto L_0x02a8;
                case 38: goto L_0x022d;
                case 40: goto L_0x01d9;
                case 41: goto L_0x0045;
                case 42: goto L_0x033f;
                case 43: goto L_0x033f;
                case 46: goto L_0x0334;
                case 63: goto L_0x033f;
                case 64: goto L_0x0487;
                case 94: goto L_0x01ba;
                case 123: goto L_0x0439;
                case 124: goto L_0x046d;
                case 125: goto L_0x0453;
                case 8710: goto L_0x022d;
                default: goto L_0x0176;
            };
        L_0x0176:
            r4 = 33;
            if (r13 < r4) goto L_0x01b1;
        L_0x017a:
            r4 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
            if (r13 > r4) goto L_0x01b1;
        L_0x017e:
            r4 = 48;
            if (r13 < r4) goto L_0x0186;
        L_0x0182:
            r4 = 57;
            if (r13 <= r4) goto L_0x01b1;
        L_0x0186:
            r4 = 65;
            if (r13 < r4) goto L_0x018e;
        L_0x018a:
            r4 = 90;
            if (r13 <= r4) goto L_0x01b1;
        L_0x018e:
            r4 = 97;
            if (r13 < r4) goto L_0x0196;
        L_0x0192:
            r4 = 122; // 0x7a float:1.71E-43 double:6.03E-322;
            if (r13 <= r4) goto L_0x01b1;
        L_0x0196:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = "Unquoted ";
            r4 = r4.append(r7);
            r4 = r4.append(r13);
            r4 = r4.toString();
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x01b1:
            r0 = r42;
            r0.append(r13);
        L_0x01b6:
            r23 = r39;
            goto L_0x0017;
        L_0x01ba:
            r4 = r42.length();
            if (r4 != 0) goto L_0x01ce;
        L_0x01c0:
            r0 = r37;
            r4 = r0.anchorStart;
            r4 = r4 ^ 1;
            if (r4 == 0) goto L_0x01ce;
        L_0x01c8:
            r4 = 1;
            r0 = r37;
            r0.anchorStart = r4;
            goto L_0x01b6;
        L_0x01ce:
            r4 = "Misplaced anchor start";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
            goto L_0x01b6;
        L_0x01d9:
            r11 = r42.length();
            r0 = r37;
            r0 = r0.nextSegmentNumber;
            r31 = r0;
            r4 = r31 + 1;
            r0 = r37;
            r0.nextSegmentNumber = r4;
            r9 = android.icu.text.TransliteratorParser.ILLEGAL_SEG;
            r10 = 1;
            r3 = r37;
            r4 = r38;
            r5 = r39;
            r6 = r40;
            r7 = r41;
            r8 = r42;
            r39 = r3.parseSection(r4, r5, r6, r7, r8, r9, r10);
            r18 = new android.icu.text.StringMatcher;
            r0 = r42;
            r4 = r0.substring(r11);
            r7 = r41.curData;
            r0 = r18;
            r1 = r31;
            r0.<init>(r4, r1, r7);
            r0 = r41;
            r1 = r31;
            r2 = r18;
            r0.setSegmentObject(r1, r2);
            r0 = r42;
            r0.setLength(r11);
            r0 = r41;
            r1 = r31;
            r4 = r0.getSegmentStandin(r1);
            r0 = r42;
            r0.append(r4);
            goto L_0x01b6;
        L_0x022d:
            r4 = 0;
            r17[r4] = r39;
            r0 = r38;
            r1 = r17;
            r32 = android.icu.text.TransliteratorIDParser.parseFilterID(r0, r1);
            if (r32 == 0) goto L_0x0248;
        L_0x023a:
            r4 = 40;
            r0 = r38;
            r1 = r17;
            r4 = android.icu.impl.Utility.parseChar(r0, r1, r4);
            r4 = r4 ^ 1;
            if (r4 == 0) goto L_0x0252;
        L_0x0248:
            r4 = "Invalid function";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0252:
            r34 = r32.getInstance();
            if (r34 != 0) goto L_0x0262;
        L_0x0258:
            r4 = "Invalid function ID";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0262:
            r11 = r42.length();
            r4 = 0;
            r5 = r17[r4];
            r9 = android.icu.text.TransliteratorParser.ILLEGAL_FUNC;
            r10 = 1;
            r3 = r37;
            r4 = r38;
            r6 = r40;
            r7 = r41;
            r8 = r42;
            r39 = r3.parseSection(r4, r5, r6, r7, r8, r9, r10);
            r30 = new android.icu.text.FunctionReplacer;
            r4 = new android.icu.text.StringReplacer;
            r0 = r42;
            r7 = r0.substring(r11);
            r8 = r41.curData;
            r4.<init>(r7, r8);
            r0 = r30;
            r1 = r34;
            r0.<init>(r1, r4);
            r0 = r42;
            r0.setLength(r11);
            r0 = r41;
            r1 = r30;
            r4 = r0.generateStandInFor(r1);
            r0 = r42;
            r0.append(r4);
            goto L_0x01b6;
        L_0x02a8:
            r0 = r39;
            r1 = r40;
            if (r0 != r1) goto L_0x02b5;
        L_0x02ae:
            r4 = 1;
            r0 = r37;
            r0.anchorEnd = r4;
            goto L_0x01b6;
        L_0x02b5:
            r13 = r38.charAt(r39);
            r4 = 10;
            r29 = android.icu.lang.UCharacter.digit(r13, r4);
            r4 = 1;
            r0 = r29;
            if (r0 < r4) goto L_0x02f5;
        L_0x02c4:
            r4 = 9;
            r0 = r29;
            if (r0 > r4) goto L_0x02f5;
        L_0x02ca:
            r4 = 0;
            r17[r4] = r39;
            r4 = 10;
            r0 = r38;
            r1 = r17;
            r29 = android.icu.impl.Utility.parseNumber(r0, r1, r4);
            if (r29 >= 0) goto L_0x02e3;
        L_0x02d9:
            r4 = "Undefined segment reference";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x02e3:
            r4 = 0;
            r39 = r17[r4];
            r0 = r41;
            r1 = r29;
            r4 = r0.getSegmentStandin(r1);
            r0 = r42;
            r0.append(r4);
            goto L_0x01b6;
        L_0x02f5:
            if (r25 != 0) goto L_0x02ff;
        L_0x02f7:
            r25 = new java.text.ParsePosition;
            r4 = 0;
            r0 = r25;
            r0.<init>(r4);
        L_0x02ff:
            r0 = r25;
            r1 = r39;
            r0.setIndex(r1);
            r4 = r41.parseData;
            r0 = r38;
            r1 = r25;
            r2 = r40;
            r22 = r4.parseReference(r0, r1, r2);
            if (r22 != 0) goto L_0x031d;
        L_0x0316:
            r4 = 1;
            r0 = r37;
            r0.anchorEnd = r4;
            goto L_0x01b6;
        L_0x031d:
            r39 = r25.getIndex();
            r36 = r42.length();
            r0 = r41;
            r1 = r22;
            r2 = r42;
            r0.appendVariableDef(r1, r2);
            r35 = r42.length();
            goto L_0x01b6;
        L_0x0334:
            r4 = r41.getDotStandIn();
            r0 = r42;
            r0.append(r4);
            goto L_0x01b6;
        L_0x033f:
            if (r44 == 0) goto L_0x0353;
        L_0x0341:
            r4 = r42.length();
            if (r4 != r12) goto L_0x0353;
        L_0x0347:
            r4 = "Misplaced quantifier";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
            goto L_0x01b6;
        L_0x0353:
            r4 = r42.length();
            r0 = r27;
            if (r4 != r0) goto L_0x0394;
        L_0x035b:
            r5 = r28;
            r6 = r27;
        L_0x035f:
            r3 = new android.icu.text.StringMatcher;	 Catch:{ RuntimeException -> 0x03aa }
            r4 = r42.toString();	 Catch:{ RuntimeException -> 0x03aa }
            r8 = r41.curData;	 Catch:{ RuntimeException -> 0x03aa }
            r7 = 0;	 Catch:{ RuntimeException -> 0x03aa }
            r3.<init>(r4, r5, r6, r7, r8);	 Catch:{ RuntimeException -> 0x03aa }
            r21 = 0;
            r20 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
            switch(r13) {
                case 43: goto L_0x042f;
                case 63: goto L_0x0433;
                default: goto L_0x0375;
            };
        L_0x0375:
            r19 = new android.icu.text.Quantifier;
            r0 = r19;
            r1 = r21;
            r2 = r20;
            r0.<init>(r3, r1, r2);
            r0 = r42;
            r0.setLength(r5);
            r0 = r41;
            r1 = r19;
            r4 = r0.generateStandInFor(r1);
            r0 = r42;
            r0.append(r4);
            goto L_0x01b6;
        L_0x0394:
            r4 = r42.length();
            r0 = r35;
            if (r4 != r0) goto L_0x03a1;
        L_0x039c:
            r5 = r36;
            r6 = r35;
            goto L_0x035f;
        L_0x03a1:
            r4 = r42.length();
            r5 = r4 + -1;
            r6 = r5 + 1;
            goto L_0x035f;
        L_0x03aa:
            r14 = move-exception;
            r4 = 50;
            r0 = r39;
            if (r0 >= r4) goto L_0x03f1;
        L_0x03b1:
            r4 = 0;
            r0 = r38;
            r1 = r39;
            r26 = r0.substring(r4, r1);
        L_0x03ba:
            r4 = r40 - r39;
            r7 = 50;
            if (r4 > r7) goto L_0x0410;
        L_0x03c0:
            r24 = r38.substring(r39, r40);
        L_0x03c4:
            r4 = new android.icu.impl.IllegalIcuArgumentException;
            r7 = new java.lang.StringBuilder;
            r7.<init>();
            r8 = "Failure in rule: ";
            r7 = r7.append(r8);
            r0 = r26;
            r7 = r7.append(r0);
            r8 = "$$$";
            r7 = r7.append(r8);
            r0 = r24;
            r7 = r7.append(r0);
            r7 = r7.toString();
            r4.<init>(r7);
            r4 = r4.initCause(r14);
            throw r4;
        L_0x03f1:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = "...";
            r4 = r4.append(r7);
            r7 = r39 + -50;
            r0 = r38;
            r1 = r39;
            r7 = r0.substring(r7, r1);
            r4 = r4.append(r7);
            r26 = r4.toString();
            goto L_0x03ba;
        L_0x0410:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = r39 + 50;
            r0 = r38;
            r1 = r39;
            r7 = r0.substring(r1, r7);
            r4 = r4.append(r7);
            r7 = "...";
            r4 = r4.append(r7);
            r24 = r4.toString();
            goto L_0x03c4;
        L_0x042f:
            r21 = 1;
            goto L_0x0375;
        L_0x0433:
            r21 = 0;
            r20 = 1;
            goto L_0x0375;
        L_0x0439:
            r0 = r37;
            r4 = r0.ante;
            if (r4 < 0) goto L_0x0449;
        L_0x043f:
            r4 = "Multiple ante contexts";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0449:
            r4 = r42.length();
            r0 = r37;
            r0.ante = r4;
            goto L_0x01b6;
        L_0x0453:
            r0 = r37;
            r4 = r0.post;
            if (r4 < 0) goto L_0x0463;
        L_0x0459:
            r4 = "Multiple post contexts";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x0463:
            r4 = r42.length();
            r0 = r37;
            r0.post = r4;
            goto L_0x01b6;
        L_0x046d:
            r0 = r37;
            r4 = r0.cursor;
            if (r4 < 0) goto L_0x047d;
        L_0x0473:
            r4 = "Multiple cursors";
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x047d:
            r4 = r42.length();
            r0 = r37;
            r0.cursor = r4;
            goto L_0x01b6;
        L_0x0487:
            r0 = r37;
            r4 = r0.cursorOffset;
            if (r4 >= 0) goto L_0x04ba;
        L_0x048d:
            r4 = r42.length();
            if (r4 <= 0) goto L_0x04ae;
        L_0x0493:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = "Misplaced ";
            r4 = r4.append(r7);
            r4 = r4.append(r13);
            r4 = r4.toString();
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x04ae:
            r0 = r37;
            r4 = r0.cursorOffset;
            r4 = r4 + -1;
            r0 = r37;
            r0.cursorOffset = r4;
            goto L_0x01b6;
        L_0x04ba:
            r0 = r37;
            r4 = r0.cursorOffset;
            if (r4 <= 0) goto L_0x04f7;
        L_0x04c0:
            r4 = r42.length();
            r0 = r37;
            r7 = r0.cursorOffsetPos;
            if (r4 != r7) goto L_0x04d0;
        L_0x04ca:
            r0 = r37;
            r4 = r0.cursor;
            if (r4 < 0) goto L_0x04eb;
        L_0x04d0:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = "Misplaced ";
            r4 = r4.append(r7);
            r4 = r4.append(r13);
            r4 = r4.toString();
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
        L_0x04eb:
            r0 = r37;
            r4 = r0.cursorOffset;
            r4 = r4 + 1;
            r0 = r37;
            r0.cursorOffset = r4;
            goto L_0x01b6;
        L_0x04f7:
            r0 = r37;
            r4 = r0.cursor;
            if (r4 != 0) goto L_0x050a;
        L_0x04fd:
            r4 = r42.length();
            if (r4 != 0) goto L_0x050a;
        L_0x0503:
            r4 = -1;
            r0 = r37;
            r0.cursorOffset = r4;
            goto L_0x01b6;
        L_0x050a:
            r0 = r37;
            r4 = r0.cursor;
            if (r4 >= 0) goto L_0x051f;
        L_0x0510:
            r4 = r42.length();
            r0 = r37;
            r0.cursorOffsetPos = r4;
            r4 = 1;
            r0 = r37;
            r0.cursorOffset = r4;
            goto L_0x01b6;
        L_0x051f:
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r7 = "Misplaced ";
            r4 = r4.append(r7);
            r4 = r4.append(r13);
            r4 = r4.toString();
            r0 = r38;
            r1 = r33;
            android.icu.text.TransliteratorParser.syntaxError(r4, r0, r1);
            goto L_0x01b6;
        L_0x053c:
            r39 = r23;
            goto L_0x0045;
            */
            throw new UnsupportedOperationException("Method not decompiled: android.icu.text.TransliteratorParser.RuleHalf.parseSection(java.lang.String, int, int, android.icu.text.TransliteratorParser, java.lang.StringBuffer, android.icu.text.UnicodeSet, boolean):int");
        }

        void removeContext() {
            this.text = this.text.substring(this.ante < 0 ? 0 : this.ante, this.post < 0 ? this.text.length() : this.post);
            this.post = -1;
            this.ante = -1;
            this.anchorEnd = false;
            this.anchorStart = false;
        }

        public boolean isValidOutput(TransliteratorParser parser) {
            int i = 0;
            while (i < this.text.length()) {
                int c = UTF16.charAt(this.text, i);
                i += UTF16.getCharCount(c);
                if (!parser.parseData.isReplacer(c)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isValidInput(TransliteratorParser parser) {
            int i = 0;
            while (i < this.text.length()) {
                int c = UTF16.charAt(this.text, i);
                i += UTF16.getCharCount(c);
                if (!parser.parseData.isMatcher(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void parse(String rules, int dir) {
        parseRules(new RuleArray(new String[]{rules}), dir);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void parseRules(RuleBody ruleArray, int dir) {
        int i;
        boolean parsingIDs = true;
        int ruleCount = 0;
        this.dataVector = new ArrayList();
        this.idBlockVector = new ArrayList();
        this.curData = null;
        this.direction = dir;
        this.compoundFilter = null;
        this.variablesVector = new ArrayList();
        this.variableNames = new HashMap();
        this.parseData = new ParseData(this, null);
        List<RuntimeException> errors = new ArrayList();
        int errorCount = 0;
        ruleArray.reset();
        StringBuilder idBlockResult = new StringBuilder();
        this.compoundFilter = null;
        int compoundFilterOffset = -1;
        loop0:
        while (true) {
            String rule = ruleArray.nextLine();
            if (rule != null) {
                int pos = 0;
                int limit = rule.length();
                while (true) {
                    int pos2 = pos;
                    if (pos2 >= limit) {
                        pos = pos2;
                        break;
                    }
                    pos = pos2 + 1;
                    char c = rule.charAt(pos2);
                    if (!PatternProps.isWhiteSpace(c)) {
                        if (c == '#') {
                            pos = rule.indexOf("\n", pos) + 1;
                            if (pos == 0) {
                                break;
                            }
                        } else if (c != ';') {
                            ruleCount++;
                            pos--;
                            if ((pos + 2) + 1 <= limit) {
                                try {
                                    if (rule.regionMatches(pos, ID_TOKEN, 0, 2)) {
                                        pos += 2;
                                        c = rule.charAt(pos);
                                        while (PatternProps.isWhiteSpace(c) && pos < limit) {
                                            pos++;
                                            c = rule.charAt(pos);
                                        }
                                        int[] p = new int[]{pos};
                                        if (!parsingIDs) {
                                            if (this.curData != null) {
                                                if (this.direction == 0) {
                                                    this.dataVector.add(this.curData);
                                                } else {
                                                    this.dataVector.add(0, this.curData);
                                                }
                                                this.curData = null;
                                            }
                                            parsingIDs = true;
                                        }
                                        SingleID id = TransliteratorIDParser.parseSingleID(rule, p, this.direction);
                                        if (p[0] == pos || !Utility.parseChar(rule, p, END_OF_RULE)) {
                                            int[] withParens = new int[]{-1};
                                            UnicodeSet f = TransliteratorIDParser.parseGlobalFilter(rule, p, this.direction, withParens, null);
                                            if (f == null || !Utility.parseChar(rule, p, END_OF_RULE)) {
                                                syntaxError("Invalid ::ID", rule, pos);
                                            } else {
                                                if ((this.direction == 0 ? 1 : null) == (withParens[0] == 0 ? 1 : null)) {
                                                    if (this.compoundFilter != null) {
                                                        syntaxError("Multiple global filters", rule, pos);
                                                    }
                                                    this.compoundFilter = f;
                                                    compoundFilterOffset = ruleCount;
                                                }
                                            }
                                        } else if (this.direction == 0) {
                                            idBlockResult.append(id.canonID).append(END_OF_RULE);
                                        } else {
                                            idBlockResult.insert(0, id.canonID + END_OF_RULE);
                                        }
                                        pos = p[0];
                                    }
                                } catch (Throwable e) {
                                    if (errorCount == 30) {
                                        IllegalIcuArgumentException icuEx = new IllegalIcuArgumentException("\nMore than 30 errors; further messages squelched");
                                        icuEx.initCause(e);
                                        errors.add(icuEx);
                                        break loop0;
                                    }
                                    e.fillInStackTrace();
                                    errors.add(e);
                                    errorCount++;
                                    pos = ruleEnd(rule, pos, limit) + 1;
                                }
                            }
                            if (parsingIDs) {
                                if (this.direction == 0) {
                                    this.idBlockVector.add(idBlockResult.toString());
                                } else {
                                    this.idBlockVector.add(0, idBlockResult.toString());
                                }
                                idBlockResult.delete(0, idBlockResult.length());
                                parsingIDs = false;
                                this.curData = new Data();
                                setVariableRange(61440, 63743);
                            }
                            if (resemblesPragma(rule, pos, limit)) {
                                int ppp = parsePragma(rule, pos, limit);
                                if (ppp < 0) {
                                    syntaxError("Unrecognized pragma", rule, pos);
                                }
                                pos = ppp;
                            } else {
                                pos = parseRule(rule, pos, limit);
                            }
                        }
                    }
                }
            } else {
                break;
            }
        }
        if (!parsingIDs || idBlockResult.length() <= 0) {
            if (!(parsingIDs || this.curData == null)) {
                if (this.direction == 0) {
                    this.dataVector.add(this.curData);
                } else {
                    this.dataVector.add(0, this.curData);
                }
            }
        } else if (this.direction == 0) {
            this.idBlockVector.add(idBlockResult.toString());
        } else {
            this.idBlockVector.add(0, idBlockResult.toString());
        }
        for (i = 0; i < this.dataVector.size(); i++) {
            Data data = (Data) this.dataVector.get(i);
            data.variables = new Object[this.variablesVector.size()];
            this.variablesVector.toArray(data.variables);
            data.variableNames = new HashMap();
            data.variableNames.putAll(this.variableNames);
        }
        this.variablesVector = null;
        try {
            if (this.compoundFilter != null) {
                if (this.direction != 0 || compoundFilterOffset == 1) {
                    if (this.direction == 1) {
                    }
                }
                throw new IllegalIcuArgumentException("Compound filters misplaced");
            }
            for (i = 0; i < this.dataVector.size(); i++) {
                ((Data) this.dataVector.get(i)).ruleSet.freeze();
            }
            if (this.idBlockVector.size() == 1 && ((String) this.idBlockVector.get(0)).length() == 0) {
                this.idBlockVector.remove(0);
            }
        } catch (IllegalArgumentException e2) {
            e2.fillInStackTrace();
            errors.add(e2);
        }
        if (errors.size() != 0) {
            for (i = errors.size() - 1; i > 0; i--) {
                RuntimeException previous = errors.get(i - 1);
                while (true) {
                    previous = previous;
                    if (previous.getCause() == null) {
                        break;
                    }
                    previous = previous.getCause();
                }
                previous.initCause((Throwable) errors.get(i));
            }
            throw ((RuntimeException) errors.get(0));
        }
    }

    private int parseRule(java.lang.String r27, int r28, int r29) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r17_0 'left' android.icu.text.TransliteratorParser$RuleHalf) in PHI: PHI: (r17_2 'left' android.icu.text.TransliteratorParser$RuleHalf) = (r17_0 'left' android.icu.text.TransliteratorParser$RuleHalf), (r17_1 'left' android.icu.text.TransliteratorParser$RuleHalf) binds: {(r17_0 'left' android.icu.text.TransliteratorParser$RuleHalf)=B:76:0x0203, (r17_1 'left' android.icu.text.TransliteratorParser$RuleHalf)=B:77:0x0205}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
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
        r26 = this;
        r22 = r28;
        r20 = 0;
        r5 = new java.lang.StringBuffer;
        r5.<init>();
        r0 = r26;
        r0.segmentStandins = r5;
        r5 = new java.util.ArrayList;
        r5.<init>();
        r0 = r26;
        r0.segmentObjects = r5;
        r17 = new android.icu.text.TransliteratorParser$RuleHalf;
        r5 = 0;
        r0 = r17;
        r0.<init>(r5);
        r21 = new android.icu.text.TransliteratorParser$RuleHalf;
        r5 = 0;
        r0 = r21;
        r0.<init>(r5);
        r5 = 0;
        r0 = r26;
        r0.undefinedVariableName = r5;
        r0 = r17;
        r1 = r27;
        r2 = r28;
        r3 = r29;
        r4 = r26;
        r28 = r0.parse(r1, r2, r3, r4);
        r0 = r28;
        r1 = r29;
        if (r0 == r1) goto L_0x0050;
    L_0x003f:
        r5 = "=><←→↔";
        r28 = r28 + -1;
        r20 = r27.charAt(r28);
        r0 = r20;
        r5 = r5.indexOf(r0);
        if (r5 >= 0) goto L_0x006d;
    L_0x0050:
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "No operator pos=";
        r5 = r5.append(r6);
        r0 = r28;
        r5 = r5.append(r0);
        r5 = r5.toString();
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x006d:
        r28 = r28 + 1;
        r5 = 60;
        r0 = r20;
        if (r0 != r5) goto L_0x0087;
    L_0x0075:
        r0 = r28;
        r1 = r29;
        if (r0 >= r1) goto L_0x0087;
    L_0x007b:
        r5 = r27.charAt(r28);
        r6 = 62;
        if (r5 != r6) goto L_0x0087;
    L_0x0083:
        r28 = r28 + 1;
        r20 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
    L_0x0087:
        switch(r20) {
            case 8592: goto L_0x013e;
            case 8593: goto L_0x008a;
            case 8594: goto L_0x013a;
            case 8595: goto L_0x008a;
            case 8596: goto L_0x0142;
            default: goto L_0x008a;
        };
    L_0x008a:
        r0 = r21;
        r1 = r27;
        r2 = r28;
        r3 = r29;
        r4 = r26;
        r28 = r0.parse(r1, r2, r3, r4);
        r0 = r28;
        r1 = r29;
        if (r0 >= r1) goto L_0x00aa;
    L_0x009e:
        r28 = r28 + -1;
        r5 = r27.charAt(r28);
        r6 = 59;
        if (r5 != r6) goto L_0x0146;
    L_0x00a8:
        r28 = r28 + 1;
    L_0x00aa:
        r5 = 61;
        r0 = r20;
        if (r0 != r5) goto L_0x0152;
    L_0x00b0:
        r0 = r26;
        r5 = r0.undefinedVariableName;
        if (r5 != 0) goto L_0x00c0;
    L_0x00b6:
        r5 = "Missing '$' or duplicate definition";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x00c0:
        r0 = r17;
        r5 = r0.text;
        r5 = r5.length();
        r6 = 1;
        if (r5 != r6) goto L_0x00da;
    L_0x00cb:
        r0 = r17;
        r5 = r0.text;
        r6 = 0;
        r5 = r5.charAt(r6);
        r0 = r26;
        r6 = r0.variableLimit;
        if (r5 == r6) goto L_0x00e4;
    L_0x00da:
        r5 = "Malformed LHS";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x00e4:
        r0 = r17;
        r5 = r0.anchorStart;
        if (r5 != 0) goto L_0x00fc;
    L_0x00ea:
        r0 = r17;
        r5 = r0.anchorEnd;
        if (r5 != 0) goto L_0x00fc;
    L_0x00f0:
        r0 = r21;
        r5 = r0.anchorStart;
        if (r5 != 0) goto L_0x00fc;
    L_0x00f6:
        r0 = r21;
        r5 = r0.anchorEnd;
        if (r5 == 0) goto L_0x0106;
    L_0x00fc:
        r5 = "Malformed variable def";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x0106:
        r0 = r21;
        r5 = r0.text;
        r19 = r5.length();
        r0 = r19;
        r0 = new char[r0];
        r24 = r0;
        r0 = r21;
        r5 = r0.text;
        r6 = 0;
        r7 = 0;
        r0 = r19;
        r1 = r24;
        r5.getChars(r6, r0, r1, r7);
        r0 = r26;
        r5 = r0.variableNames;
        r0 = r26;
        r6 = r0.undefinedVariableName;
        r0 = r24;
        r5.put(r6, r0);
        r0 = r26;
        r5 = r0.variableLimit;
        r5 = r5 + 1;
        r5 = (char) r5;
        r0 = r26;
        r0.variableLimit = r5;
        return r28;
    L_0x013a:
        r20 = 62;
        goto L_0x008a;
    L_0x013e:
        r20 = 60;
        goto L_0x008a;
    L_0x0142:
        r20 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        goto L_0x008a;
    L_0x0146:
        r5 = "Unquoted operator";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
        goto L_0x00aa;
    L_0x0152:
        r0 = r26;
        r5 = r0.undefinedVariableName;
        if (r5 == 0) goto L_0x0177;
    L_0x0158:
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Undefined variable $";
        r5 = r5.append(r6);
        r0 = r26;
        r6 = r0.undefinedVariableName;
        r5 = r5.append(r6);
        r5 = r5.toString();
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x0177:
        r0 = r26;
        r5 = r0.segmentStandins;
        r5 = r5.length();
        r0 = r26;
        r6 = r0.segmentObjects;
        r6 = r6.size();
        if (r5 <= r6) goto L_0x0193;
    L_0x0189:
        r5 = "Undefined segment reference";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x0193:
        r16 = 0;
    L_0x0195:
        r0 = r26;
        r5 = r0.segmentStandins;
        r5 = r5.length();
        r0 = r16;
        if (r0 >= r5) goto L_0x01ba;
    L_0x01a1:
        r0 = r26;
        r5 = r0.segmentStandins;
        r0 = r16;
        r5 = r5.charAt(r0);
        if (r5 != 0) goto L_0x01b7;
    L_0x01ad:
        r5 = "Internal error";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x01b7:
        r16 = r16 + 1;
        goto L_0x0195;
    L_0x01ba:
        r16 = 0;
    L_0x01bc:
        r0 = r26;
        r5 = r0.segmentObjects;
        r5 = r5.size();
        r0 = r16;
        if (r0 >= r5) goto L_0x01e1;
    L_0x01c8:
        r0 = r26;
        r5 = r0.segmentObjects;
        r0 = r16;
        r5 = r5.get(r0);
        if (r5 != 0) goto L_0x01de;
    L_0x01d4:
        r5 = "Internal error";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x01de:
        r16 = r16 + 1;
        goto L_0x01bc;
    L_0x01e1:
        r5 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        r0 = r20;
        if (r0 == r5) goto L_0x01fe;
    L_0x01e7:
        r0 = r26;
        r5 = r0.direction;
        if (r5 != 0) goto L_0x01f9;
    L_0x01ed:
        r5 = 1;
        r6 = r5;
    L_0x01ef:
        r5 = 62;
        r0 = r20;
        if (r0 != r5) goto L_0x01fc;
    L_0x01f5:
        r5 = 1;
    L_0x01f6:
        if (r6 == r5) goto L_0x01fe;
    L_0x01f8:
        return r28;
    L_0x01f9:
        r5 = 0;
        r6 = r5;
        goto L_0x01ef;
    L_0x01fc:
        r5 = 0;
        goto L_0x01f6;
    L_0x01fe:
        r0 = r26;
        r5 = r0.direction;
        r6 = 1;
        if (r5 != r6) goto L_0x020d;
    L_0x0205:
        r23 = r17;
        r18 = r21;
        r21 = r17;
        r17 = r18;
    L_0x020d:
        r5 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        r0 = r20;
        if (r0 != r5) goto L_0x0220;
    L_0x0213:
        r21.removeContext();
        r5 = -1;
        r0 = r17;
        r0.cursor = r5;
        r5 = 0;
        r0 = r17;
        r0.cursorOffset = r5;
    L_0x0220:
        r0 = r17;
        r5 = r0.ante;
        if (r5 >= 0) goto L_0x022b;
    L_0x0226:
        r5 = 0;
        r0 = r17;
        r0.ante = r5;
    L_0x022b:
        r0 = r17;
        r5 = r0.post;
        if (r5 >= 0) goto L_0x023d;
    L_0x0231:
        r0 = r17;
        r5 = r0.text;
        r5 = r5.length();
        r0 = r17;
        r0.post = r5;
    L_0x023d:
        r0 = r21;
        r5 = r0.ante;
        if (r5 >= 0) goto L_0x0249;
    L_0x0243:
        r0 = r21;
        r5 = r0.post;
        if (r5 < 0) goto L_0x02a6;
    L_0x0249:
        r5 = "Malformed rule";
        r0 = r27;
        r1 = r22;
        syntaxError(r5, r0, r1);
    L_0x0253:
        r12 = 0;
        r0 = r26;
        r5 = r0.segmentObjects;
        r5 = r5.size();
        if (r5 <= 0) goto L_0x026f;
    L_0x025e:
        r0 = r26;
        r5 = r0.segmentObjects;
        r5 = r5.size();
        r12 = new android.icu.text.UnicodeMatcher[r5];
        r0 = r26;
        r5 = r0.segmentObjects;
        r5.toArray(r12);
    L_0x026f:
        r0 = r26;
        r5 = r0.curData;
        r0 = r5.ruleSet;
        r25 = r0;
        r5 = new android.icu.text.TransliterationRule;
        r0 = r17;
        r6 = r0.text;
        r0 = r17;
        r7 = r0.ante;
        r0 = r17;
        r8 = r0.post;
        r0 = r21;
        r9 = r0.text;
        r0 = r21;
        r10 = r0.cursor;
        r0 = r21;
        r11 = r0.cursorOffset;
        r0 = r17;
        r13 = r0.anchorStart;
        r0 = r17;
        r14 = r0.anchorEnd;
        r0 = r26;
        r15 = r0.curData;
        r5.<init>(r6, r7, r8, r9, r10, r11, r12, r13, r14, r15);
        r0 = r25;
        r0.addRule(r5);
        return r28;
    L_0x02a6:
        r0 = r17;
        r5 = r0.cursor;
        if (r5 >= 0) goto L_0x0249;
    L_0x02ac:
        r0 = r21;
        r5 = r0.cursorOffset;
        if (r5 == 0) goto L_0x02b8;
    L_0x02b2:
        r0 = r21;
        r5 = r0.cursor;
        if (r5 < 0) goto L_0x0249;
    L_0x02b8:
        r0 = r21;
        r5 = r0.anchorStart;
        if (r5 != 0) goto L_0x0249;
    L_0x02be:
        r0 = r21;
        r5 = r0.anchorEnd;
        if (r5 != 0) goto L_0x0249;
    L_0x02c4:
        r0 = r17;
        r1 = r26;
        r5 = r0.isValidInput(r1);
        r5 = r5 ^ 1;
        if (r5 != 0) goto L_0x0249;
    L_0x02d0:
        r0 = r21;
        r1 = r26;
        r5 = r0.isValidOutput(r1);
        r5 = r5 ^ 1;
        if (r5 != 0) goto L_0x0249;
    L_0x02dc:
        r0 = r17;
        r5 = r0.ante;
        r0 = r17;
        r6 = r0.post;
        if (r5 <= r6) goto L_0x0253;
    L_0x02e6:
        goto L_0x0249;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.text.TransliteratorParser.parseRule(java.lang.String, int, int):int");
    }

    private void setVariableRange(int start, int end) {
        if (start > end || start < 0 || end > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            throw new IllegalIcuArgumentException("Invalid variable range " + start + ", " + end);
        }
        this.curData.variablesBase = (char) start;
        if (this.dataVector.size() == 0) {
            this.variableNext = (char) start;
            this.variableLimit = (char) (end + 1);
        }
    }

    private void checkVariableRange(int ch, String rule, int start) {
        if (ch >= this.curData.variablesBase && ch < this.variableLimit) {
            syntaxError("Variable range character in rule", rule, start);
        }
    }

    private void pragmaMaximumBackup(int backup) {
        throw new IllegalIcuArgumentException("use maximum backup pragma not implemented yet");
    }

    private void pragmaNormalizeRules(Mode mode) {
        throw new IllegalIcuArgumentException("use normalize rules pragma not implemented yet");
    }

    static boolean resemblesPragma(String rule, int pos, int limit) {
        return Utility.parsePattern(rule, pos, limit, "use ", null) >= 0;
    }

    private int parsePragma(String rule, int pos, int limit) {
        int[] array = new int[2];
        pos += 4;
        int p = Utility.parsePattern(rule, pos, limit, "~variable range # #~;", array);
        if (p >= 0) {
            setVariableRange(array[0], array[1]);
            return p;
        }
        p = Utility.parsePattern(rule, pos, limit, "~maximum backup #~;", array);
        if (p >= 0) {
            pragmaMaximumBackup(array[0]);
            return p;
        }
        p = Utility.parsePattern(rule, pos, limit, "~nfd rules~;", null);
        if (p >= 0) {
            pragmaNormalizeRules(Normalizer.NFD);
            return p;
        }
        p = Utility.parsePattern(rule, pos, limit, "~nfc rules~;", null);
        if (p < 0) {
            return -1;
        }
        pragmaNormalizeRules(Normalizer.NFC);
        return p;
    }

    static final void syntaxError(String msg, String rule, int start) {
        throw new IllegalIcuArgumentException(msg + " in \"" + Utility.escape(rule.substring(start, ruleEnd(rule, start, rule.length()))) + '\"');
    }

    static final int ruleEnd(String rule, int start, int limit) {
        int end = Utility.quotedIndexOf(rule, start, limit, ";");
        if (end < 0) {
            return limit;
        }
        return end;
    }

    private final char parseSet(String rule, ParsePosition pos) {
        UnicodeSet set = new UnicodeSet(rule, pos, this.parseData);
        if (this.variableNext >= this.variableLimit) {
            throw new RuntimeException("Private use variables exhausted");
        }
        set.compact();
        return generateStandInFor(set);
    }

    char generateStandInFor(Object obj) {
        for (int i = 0; i < this.variablesVector.size(); i++) {
            if (this.variablesVector.get(i) == obj) {
                return (char) (this.curData.variablesBase + i);
            }
        }
        if (this.variableNext >= this.variableLimit) {
            throw new RuntimeException("Variable range exhausted");
        }
        this.variablesVector.add(obj);
        char c = this.variableNext;
        this.variableNext = (char) (c + 1);
        return (char) c;
    }

    public char getSegmentStandin(int seg) {
        if (this.segmentStandins.length() < seg) {
            this.segmentStandins.setLength(seg);
        }
        char c = this.segmentStandins.charAt(seg - 1);
        if (c != 0) {
            return c;
        }
        if (this.variableNext >= this.variableLimit) {
            throw new RuntimeException("Variable range exhausted");
        }
        char c2 = this.variableNext;
        this.variableNext = (char) (c2 + 1);
        c = (char) c2;
        this.variablesVector.add(null);
        this.segmentStandins.setCharAt(seg - 1, c);
        return c;
    }

    public void setSegmentObject(int seg, StringMatcher obj) {
        while (this.segmentObjects.size() < seg) {
            this.segmentObjects.add(null);
        }
        int index = getSegmentStandin(seg) - this.curData.variablesBase;
        if (this.segmentObjects.get(seg - 1) == null && this.variablesVector.get(index) == null) {
            this.segmentObjects.set(seg - 1, obj);
            this.variablesVector.set(index, obj);
            return;
        }
        throw new RuntimeException();
    }

    char getDotStandIn() {
        if (this.dotStandIn == -1) {
            this.dotStandIn = generateStandInFor(new UnicodeSet(DOT_SET));
        }
        return (char) this.dotStandIn;
    }

    private void appendVariableDef(String name, StringBuffer buf) {
        char[] ch = (char[]) this.variableNames.get(name);
        if (ch != null) {
            buf.append(ch);
        } else if (this.undefinedVariableName == null) {
            this.undefinedVariableName = name;
            if (this.variableNext >= this.variableLimit) {
                throw new RuntimeException("Private use variables exhausted");
            }
            char c = (char) (this.variableLimit - 1);
            this.variableLimit = c;
            buf.append(c);
        } else {
            throw new IllegalIcuArgumentException("Undefined variable $" + name);
        }
    }
}
