package android.icu.util;

import android.icu.impl.Grego;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

public class VTimeZone extends BasicTimeZone {
    static final /* synthetic */ boolean -assertionsDisabled = (VTimeZone.class.desiredAssertionStatus() ^ 1);
    private static final String COLON = ":";
    private static final String COMMA = ",";
    private static final int DEF_DSTSAVINGS = 3600000;
    private static final long DEF_TZSTARTTIME = 0;
    private static final String EQUALS_SIGN = "=";
    private static final int ERR = 3;
    private static final String ICAL_BEGIN = "BEGIN";
    private static final String ICAL_BEGIN_VTIMEZONE = "BEGIN:VTIMEZONE";
    private static final String ICAL_BYDAY = "BYDAY";
    private static final String ICAL_BYMONTH = "BYMONTH";
    private static final String ICAL_BYMONTHDAY = "BYMONTHDAY";
    private static final String ICAL_DAYLIGHT = "DAYLIGHT";
    private static final String[] ICAL_DOW_NAMES = new String[]{"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
    private static final String ICAL_DTSTART = "DTSTART";
    private static final String ICAL_END = "END";
    private static final String ICAL_END_VTIMEZONE = "END:VTIMEZONE";
    private static final String ICAL_FREQ = "FREQ";
    private static final String ICAL_LASTMOD = "LAST-MODIFIED";
    private static final String ICAL_RDATE = "RDATE";
    private static final String ICAL_RRULE = "RRULE";
    private static final String ICAL_STANDARD = "STANDARD";
    private static final String ICAL_TZID = "TZID";
    private static final String ICAL_TZNAME = "TZNAME";
    private static final String ICAL_TZOFFSETFROM = "TZOFFSETFROM";
    private static final String ICAL_TZOFFSETTO = "TZOFFSETTO";
    private static final String ICAL_TZURL = "TZURL";
    private static final String ICAL_UNTIL = "UNTIL";
    private static final String ICAL_VTIMEZONE = "VTIMEZONE";
    private static final String ICAL_YEARLY = "YEARLY";
    private static final String ICU_TZINFO_PROP = "X-TZINFO";
    private static String ICU_TZVERSION = null;
    private static final int INI = 0;
    private static final long MAX_TIME = Long.MAX_VALUE;
    private static final long MIN_TIME = Long.MIN_VALUE;
    private static final int[] MONTHLENGTH = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final String NEWLINE = "\r\n";
    private static final String SEMICOLON = ";";
    private static final int TZI = 2;
    private static final int VTZ = 1;
    private static final long serialVersionUID = -6851467294127795902L;
    private volatile transient boolean isFrozen = false;
    private Date lastmod = null;
    private String olsonzid = null;
    private BasicTimeZone tz;
    private String tzurl = null;
    private List<String> vtzlines;

    public static VTimeZone create(String tzid) {
        BasicTimeZone basicTimeZone = TimeZone.getFrozenICUTimeZone(tzid, true);
        if (basicTimeZone == null) {
            return null;
        }
        VTimeZone vtz = new VTimeZone(tzid);
        vtz.tz = (BasicTimeZone) basicTimeZone.cloneAsThawed();
        vtz.olsonzid = vtz.tz.getID();
        return vtz;
    }

    public static VTimeZone create(Reader reader) {
        VTimeZone vtz = new VTimeZone();
        if (vtz.load(reader)) {
            return vtz;
        }
        return null;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return this.tz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    public void getOffset(long date, boolean local, int[] offsets) {
        this.tz.getOffset(date, local, offsets);
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        this.tz.getOffsetFromLocal(date, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
    }

    public int getRawOffset() {
        return this.tz.getRawOffset();
    }

    public boolean inDaylightTime(Date date) {
        return this.tz.inDaylightTime(date);
    }

    public void setRawOffset(int offsetMillis) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tz.setRawOffset(offsetMillis);
    }

    public boolean useDaylightTime() {
        return this.tz.useDaylightTime();
    }

    public boolean observesDaylightTime() {
        return this.tz.observesDaylightTime();
    }

    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (other instanceof VTimeZone) {
            return this.tz.hasSameRules(((VTimeZone) other).tz);
        }
        return this.tz.hasSameRules(other);
    }

    public String getTZURL() {
        return this.tzurl;
    }

    public void setTZURL(String url) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tzurl = url;
    }

    public Date getLastModified() {
        return this.lastmod;
    }

    public void setLastModified(Date date) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.lastmod = date;
    }

    public void write(Writer writer) throws IOException {
        BufferedWriter bw = new BufferedWriter(writer);
        if (this.vtzlines != null) {
            for (String line : this.vtzlines) {
                if (line.startsWith("TZURL:")) {
                    if (this.tzurl != null) {
                        bw.write(ICAL_TZURL);
                        bw.write(COLON);
                        bw.write(this.tzurl);
                        bw.write(NEWLINE);
                    }
                } else if (!line.startsWith("LAST-MODIFIED:")) {
                    bw.write(line);
                    bw.write(NEWLINE);
                } else if (this.lastmod != null) {
                    bw.write(ICAL_LASTMOD);
                    bw.write(COLON);
                    bw.write(getUTCDateTimeString(this.lastmod.getTime()));
                    bw.write(NEWLINE);
                }
            }
            bw.flush();
            return;
        }
        String[] customProperties = null;
        if (!(this.olsonzid == null || ICU_TZVERSION == null)) {
            customProperties = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "]"};
        }
        writeZone(writer, this.tz, customProperties);
    }

    public void write(Writer writer, long start) throws IOException {
        TimeZoneRule[] rules = this.tz.getTimeZoneRules(start);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) rules[0]);
        for (int i = 1; i < rules.length; i++) {
            rbtz.addTransitionRule(rules[i]);
        }
        String[] customProperties = null;
        if (!(this.olsonzid == null || ICU_TZVERSION == null)) {
            customProperties = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "/Partial@" + start + "]"};
        }
        writeZone(writer, rbtz, customProperties);
    }

    public void writeSimple(Writer writer, long time) throws IOException {
        TimeZoneRule[] rules = this.tz.getSimpleTimeZoneRulesNear(time);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) rules[0]);
        for (int i = 1; i < rules.length; i++) {
            rbtz.addTransitionRule(rules[i]);
        }
        String[] customProperties = null;
        if (!(this.olsonzid == null || ICU_TZVERSION == null)) {
            customProperties = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "/Simple@" + time + "]"};
        }
        writeZone(writer, rbtz, customProperties);
    }

    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        return this.tz.getNextTransition(base, inclusive);
    }

    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        return this.tz.getPreviousTransition(base, inclusive);
    }

    public boolean hasEquivalentTransitions(TimeZone other, long start, long end) {
        if (this == other) {
            return true;
        }
        return this.tz.hasEquivalentTransitions(other, start, end);
    }

    public TimeZoneRule[] getTimeZoneRules() {
        return this.tz.getTimeZoneRules();
    }

    public TimeZoneRule[] getTimeZoneRules(long start) {
        return this.tz.getTimeZoneRules(start);
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    static {
        try {
            ICU_TZVERSION = TimeZone.getTZDataVersion();
        } catch (MissingResourceException e) {
            ICU_TZVERSION = null;
        }
    }

    private VTimeZone() {
    }

    private VTimeZone(String tzid) {
        super(tzid);
    }

    private boolean load(Reader reader) {
        try {
            this.vtzlines = new LinkedList();
            boolean eol = false;
            boolean start = false;
            boolean success = false;
            StringBuilder line = new StringBuilder();
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    if (start && line.toString().startsWith(ICAL_END_VTIMEZONE)) {
                        this.vtzlines.add(line.toString());
                        success = true;
                    }
                } else if (ch != 13) {
                    if (eol) {
                        if (!(ch == 9 || ch == 32)) {
                            if (start && line.length() > 0) {
                                this.vtzlines.add(line.toString());
                            }
                            line.setLength(0);
                            if (ch != 10) {
                                line.append((char) ch);
                            }
                        }
                        eol = false;
                    } else if (ch == 10) {
                        eol = true;
                        if (start) {
                            if (line.toString().startsWith(ICAL_END_VTIMEZONE)) {
                                this.vtzlines.add(line.toString());
                                success = true;
                                break;
                            }
                        } else if (line.toString().startsWith(ICAL_BEGIN_VTIMEZONE)) {
                            this.vtzlines.add(line.toString());
                            line.setLength(0);
                            start = true;
                            eol = false;
                        }
                    } else {
                        line.append((char) ch);
                    }
                }
            }
            if (success) {
                return parse();
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean parse() {
        if (this.vtzlines == null || this.vtzlines.size() == 0) {
            return false;
        }
        String tzid = null;
        int state = 0;
        boolean dst = false;
        String from = null;
        String to = null;
        String tzname = null;
        String dtstart = null;
        boolean isRRULE = false;
        List dates = null;
        List<TimeZoneRule> rules = new ArrayList();
        int initialRawOffset = 0;
        int initialDSTSavings = 0;
        long firstStart = MAX_TIME;
        for (String line : this.vtzlines) {
            int valueSep = line.indexOf(COLON);
            if (valueSep >= 0) {
                String name = line.substring(0, valueSep);
                String value = line.substring(valueSep + 1);
                switch (state) {
                    case 0:
                        if (name.equals(ICAL_BEGIN)) {
                            if (value.equals(ICAL_VTIMEZONE)) {
                                state = 1;
                                break;
                            }
                        }
                        break;
                    case 1:
                        if (!name.equals(ICAL_TZID)) {
                            if (!name.equals(ICAL_TZURL)) {
                                if (!name.equals(ICAL_LASTMOD)) {
                                    if (!name.equals(ICAL_BEGIN)) {
                                        boolean equals = name.equals(ICAL_END);
                                        break;
                                    }
                                    boolean isDST = value.equals(ICAL_DAYLIGHT);
                                    if (value.equals(ICAL_STANDARD) || isDST) {
                                        if (tzid != null) {
                                            dates = null;
                                            isRRULE = false;
                                            from = null;
                                            to = null;
                                            tzname = null;
                                            dst = isDST;
                                            state = 2;
                                            break;
                                        }
                                        state = 3;
                                        break;
                                    }
                                    state = 3;
                                    break;
                                }
                                this.lastmod = new Date(parseDateTimeString(value, 0));
                                break;
                            }
                            this.tzurl = value;
                            break;
                        }
                        tzid = value;
                        break;
                        break;
                    case 2:
                        if (!name.equals(ICAL_DTSTART)) {
                            if (!name.equals(ICAL_TZNAME)) {
                                if (!name.equals(ICAL_TZOFFSETFROM)) {
                                    if (!name.equals(ICAL_TZOFFSETTO)) {
                                        if (name.equals(ICAL_RDATE)) {
                                            if (!isRRULE) {
                                                if (dates == null) {
                                                    dates = new LinkedList();
                                                }
                                                StringTokenizer stringTokenizer = new StringTokenizer(value, COMMA);
                                                while (stringTokenizer.hasMoreTokens()) {
                                                    dates.add(stringTokenizer.nextToken());
                                                }
                                                break;
                                            }
                                            state = 3;
                                            break;
                                        }
                                        if (name.equals(ICAL_RRULE)) {
                                            if (!isRRULE && dates != null) {
                                                state = 3;
                                                break;
                                            }
                                            if (dates == null) {
                                                dates = new LinkedList();
                                            }
                                            isRRULE = true;
                                            dates.add(value);
                                            break;
                                        }
                                        if (name.equals(ICAL_END)) {
                                            if (dtstart != null && from != null && to != null) {
                                                if (tzname == null) {
                                                    tzname = getDefaultTZName(tzid, dst);
                                                }
                                                TimeZoneRule rule = null;
                                                try {
                                                    int rawOffset;
                                                    int dstSavings;
                                                    int fromOffset = offsetStrToMillis(from);
                                                    int toOffset = offsetStrToMillis(to);
                                                    if (!dst) {
                                                        rawOffset = toOffset;
                                                        dstSavings = 0;
                                                    } else if (toOffset - fromOffset > 0) {
                                                        rawOffset = fromOffset;
                                                        dstSavings = toOffset - fromOffset;
                                                    } else {
                                                        rawOffset = toOffset - 3600000;
                                                        dstSavings = 3600000;
                                                    }
                                                    long start = parseDateTimeString(dtstart, fromOffset);
                                                    if (isRRULE) {
                                                        rule = createRuleByRRULE(tzname, rawOffset, dstSavings, start, dates, fromOffset);
                                                    } else {
                                                        rule = createRuleByRDATE(tzname, rawOffset, dstSavings, start, dates, fromOffset);
                                                    }
                                                    if (rule != null) {
                                                        Date actualStart = rule.getFirstStart(fromOffset, 0);
                                                        if (actualStart.getTime() < firstStart) {
                                                            firstStart = actualStart.getTime();
                                                            if (dstSavings > 0) {
                                                                initialRawOffset = fromOffset;
                                                                initialDSTSavings = 0;
                                                            } else if (fromOffset - toOffset == 3600000) {
                                                                initialRawOffset = fromOffset - 3600000;
                                                                initialDSTSavings = 3600000;
                                                            } else {
                                                                initialRawOffset = fromOffset;
                                                                initialDSTSavings = 0;
                                                            }
                                                        }
                                                    }
                                                } catch (IllegalArgumentException e) {
                                                }
                                                if (rule != null) {
                                                    rules.add(rule);
                                                    state = 1;
                                                    break;
                                                }
                                                state = 3;
                                                break;
                                            }
                                            state = 3;
                                            break;
                                        }
                                    }
                                    to = value;
                                    break;
                                }
                                from = value;
                                break;
                            }
                            tzname = value;
                            break;
                        }
                        dtstart = value;
                        break;
                        break;
                }
                if (state == 3) {
                    this.vtzlines = null;
                    return false;
                }
            }
        }
        if (rules.size() == 0) {
            return false;
        }
        int i;
        TimeZoneRule r;
        BasicTimeZone ruleBasedTimeZone = new RuleBasedTimeZone(tzid, new InitialTimeZoneRule(getDefaultTZName(tzid, false), initialRawOffset, initialDSTSavings));
        int finalRuleIdx = -1;
        int finalRuleCount = 0;
        for (i = 0; i < rules.size(); i++) {
            r = (TimeZoneRule) rules.get(i);
            if ((r instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) r).getEndYear() == Integer.MAX_VALUE) {
                finalRuleCount++;
                finalRuleIdx = i;
            }
        }
        if (finalRuleCount > 2) {
            return false;
        }
        if (finalRuleCount == 1) {
            if (rules.size() == 1) {
                rules.clear();
            } else {
                TimeZoneRule newRule;
                AnnualTimeZoneRule finalRule = (AnnualTimeZoneRule) rules.get(finalRuleIdx);
                int tmpRaw = finalRule.getRawOffset();
                int tmpDST = finalRule.getDSTSavings();
                Date finalStart = finalRule.getFirstStart(initialRawOffset, initialDSTSavings);
                Date start2 = finalStart;
                for (i = 0; i < rules.size(); i++) {
                    if (finalRuleIdx != i) {
                        r = (TimeZoneRule) rules.get(i);
                        Date lastStart = r.getFinalStart(tmpRaw, tmpDST);
                        if (lastStart.after(start2)) {
                            start2 = finalRule.getNextStart(lastStart.getTime(), r.getRawOffset(), r.getDSTSavings(), false);
                        }
                    }
                }
                if (start2 == finalStart) {
                    newRule = new TimeArrayTimeZoneRule(finalRule.getName(), finalRule.getRawOffset(), finalRule.getDSTSavings(), new long[]{finalStart.getTime()}, 2);
                } else {
                    newRule = new AnnualTimeZoneRule(finalRule.getName(), finalRule.getRawOffset(), finalRule.getDSTSavings(), finalRule.getRule(), finalRule.getStartYear(), Grego.timeToFields(start2.getTime(), null)[0]);
                }
                rules.set(finalRuleIdx, newRule);
            }
        }
        for (TimeZoneRule r2 : rules) {
            ruleBasedTimeZone.addTransitionRule(r2);
        }
        this.tz = ruleBasedTimeZone;
        setID(tzid);
        return true;
    }

    private static String getDefaultTZName(String tzid, boolean isDST) {
        if (isDST) {
            return tzid + "(DST)";
        }
        return tzid + "(STD)";
    }

    private static android.icu.util.TimeZoneRule createRuleByRRULE(java.lang.String r35, int r36, int r37, long r38, java.util.List<java.lang.String> r40, int r41) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r2_4 'adtr' android.icu.util.DateTimeRule) in PHI: PHI: (r2_2 'adtr' android.icu.util.DateTimeRule) = (r2_1 'adtr' android.icu.util.DateTimeRule), (r2_3 'adtr' android.icu.util.DateTimeRule), (r2_4 'adtr' android.icu.util.DateTimeRule) binds: {(r2_1 'adtr' android.icu.util.DateTimeRule)=B:60:0x00dd, (r2_3 'adtr' android.icu.util.DateTimeRule)=B:148:0x0206, (r2_4 'adtr' android.icu.util.DateTimeRule)=B:152:0x0214}
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
        if (r40 == 0) goto L_0x0008;
    L_0x0002:
        r7 = r40.size();
        if (r7 != 0) goto L_0x000a;
    L_0x0008:
        r7 = 0;
        return r7;
    L_0x000a:
        r7 = 0;
        r0 = r40;
        r31 = r0.get(r7);
        r31 = (java.lang.String) r31;
        r7 = 1;
        r0 = new long[r7];
        r34 = r0;
        r0 = r31;
        r1 = r34;
        r32 = parseRRULE(r0, r1);
        if (r32 != 0) goto L_0x0024;
    L_0x0022:
        r7 = 0;
        return r7;
    L_0x0024:
        r7 = 0;
        r3 = r32[r7];
        r7 = 1;
        r5 = r32[r7];
        r7 = 2;
        r4 = r32[r7];
        r7 = 3;
        r9 = r32[r7];
        r7 = r40.size();
        r8 = 1;
        if (r7 != r8) goto L_0x00f0;
    L_0x0037:
        r0 = r32;
        r7 = r0.length;
        r8 = 4;
        if (r7 <= r8) goto L_0x00a0;
    L_0x003d:
        r0 = r32;
        r7 = r0.length;
        r8 = 10;
        if (r7 != r8) goto L_0x0047;
    L_0x0044:
        r7 = -1;
        if (r3 != r7) goto L_0x0049;
    L_0x0047:
        r7 = 0;
        return r7;
    L_0x0049:
        if (r5 == 0) goto L_0x0047;
    L_0x004b:
        r27 = 31;
        r7 = 7;
        r0 = new int[r7];
        r19 = r0;
        r29 = 0;
    L_0x0054:
        r7 = 7;
        r0 = r29;
        if (r0 >= r7) goto L_0x007c;
    L_0x0059:
        r7 = r29 + 3;
        r7 = r32[r7];
        r19[r29] = r7;
        r7 = r19[r29];
        if (r7 <= 0) goto L_0x0072;
    L_0x0063:
        r7 = r19[r29];
    L_0x0065:
        r19[r29] = r7;
        r7 = r19[r29];
        r0 = r27;
        if (r7 >= r0) goto L_0x006f;
    L_0x006d:
        r27 = r19[r29];
    L_0x006f:
        r29 = r29 + 1;
        goto L_0x0054;
    L_0x0072:
        r7 = MONTHLENGTH;
        r7 = r7[r3];
        r8 = r19[r29];
        r7 = r7 + r8;
        r7 = r7 + 1;
        goto L_0x0065;
    L_0x007c:
        r29 = 1;
    L_0x007e:
        r7 = 7;
        r0 = r29;
        if (r0 >= r7) goto L_0x009e;
    L_0x0083:
        r28 = 0;
        r30 = 0;
    L_0x0087:
        r7 = 7;
        r0 = r30;
        if (r0 >= r7) goto L_0x0094;
    L_0x008c:
        r7 = r19[r30];
        r8 = r27 + r29;
        if (r7 != r8) goto L_0x0098;
    L_0x0092:
        r28 = 1;
    L_0x0094:
        if (r28 != 0) goto L_0x009b;
    L_0x0096:
        r7 = 0;
        return r7;
    L_0x0098:
        r30 = r30 + 1;
        goto L_0x0087;
    L_0x009b:
        r29 = r29 + 1;
        goto L_0x007e;
    L_0x009e:
        r9 = r27;
    L_0x00a0:
        r0 = r41;
        r10 = (long) r0;
        r10 = r10 + r38;
        r7 = 0;
        r21 = android.icu.impl.Grego.timeToFields(r10, r7);
        r7 = 0;
        r15 = r21[r7];
        r7 = -1;
        if (r3 != r7) goto L_0x00b3;
    L_0x00b0:
        r7 = 1;
        r3 = r21[r7];
    L_0x00b3:
        if (r5 != 0) goto L_0x00bc;
    L_0x00b5:
        if (r4 != 0) goto L_0x00bc;
    L_0x00b7:
        if (r9 != 0) goto L_0x00bc;
    L_0x00b9:
        r7 = 2;
        r9 = r21[r7];
    L_0x00bc:
        r7 = 5;
        r6 = r21[r7];
        r16 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r7 = 0;
        r10 = r34[r7];
        r12 = -9223372036854775808;
        r7 = (r10 > r12 ? 1 : (r10 == r12 ? 0 : -1));
        if (r7 == 0) goto L_0x00d6;
    L_0x00cb:
        r7 = 0;
        r10 = r34[r7];
        r0 = r21;
        android.icu.impl.Grego.timeToFields(r10, r0);
        r7 = 0;
        r16 = r21[r7];
    L_0x00d6:
        r2 = 0;
        if (r5 != 0) goto L_0x0200;
    L_0x00d9:
        if (r4 != 0) goto L_0x0200;
    L_0x00db:
        if (r9 == 0) goto L_0x0200;
    L_0x00dd:
        r2 = new android.icu.util.DateTimeRule;
        r7 = 0;
        r2.<init>(r3, r9, r6, r7);
    L_0x00e3:
        r10 = new android.icu.util.AnnualTimeZoneRule;
        r11 = r35;
        r12 = r36;
        r13 = r37;
        r14 = r2;
        r10.<init>(r11, r12, r13, r14, r15, r16);
        return r10;
    L_0x00f0:
        r7 = -1;
        if (r3 == r7) goto L_0x00f5;
    L_0x00f3:
        if (r5 != 0) goto L_0x00f7;
    L_0x00f5:
        r7 = 0;
        return r7;
    L_0x00f7:
        if (r9 == 0) goto L_0x00f5;
    L_0x00f9:
        r7 = r40.size();
        r8 = 7;
        if (r7 <= r8) goto L_0x0102;
    L_0x0100:
        r7 = 0;
        return r7;
    L_0x0102:
        r25 = r3;
        r0 = r32;
        r7 = r0.length;
        r20 = r7 + -3;
        r24 = 31;
        r29 = 0;
    L_0x010d:
        r0 = r29;
        r1 = r20;
        if (r0 >= r1) goto L_0x012d;
    L_0x0113:
        r7 = r29 + 3;
        r23 = r32[r7];
        if (r23 <= 0) goto L_0x0124;
    L_0x0119:
        r0 = r23;
        r1 = r24;
        if (r0 >= r1) goto L_0x0121;
    L_0x011f:
        r24 = r23;
    L_0x0121:
        r29 = r29 + 1;
        goto L_0x010d;
    L_0x0124:
        r7 = MONTHLENGTH;
        r7 = r7[r3];
        r7 = r7 + r23;
        r23 = r7 + 1;
        goto L_0x0119;
    L_0x012d:
        r17 = -1;
        r29 = 1;
    L_0x0131:
        r7 = r40.size();
        r0 = r29;
        if (r0 >= r7) goto L_0x01f3;
    L_0x0139:
        r0 = r40;
        r1 = r29;
        r31 = r0.get(r1);
        r31 = (java.lang.String) r31;
        r7 = 1;
        r0 = new long[r7];
        r33 = r0;
        r0 = r31;
        r1 = r33;
        r26 = parseRRULE(r0, r1);
        r7 = 0;
        r10 = r33[r7];
        r7 = 0;
        r12 = r34[r7];
        r7 = (r10 > r12 ? 1 : (r10 == r12 ? 0 : -1));
        if (r7 <= 0) goto L_0x015c;
    L_0x015a:
        r34 = r33;
    L_0x015c:
        r7 = 0;
        r7 = r26[r7];
        r8 = -1;
        if (r7 == r8) goto L_0x0167;
    L_0x0162:
        r7 = 1;
        r7 = r26[r7];
        if (r7 != 0) goto L_0x0169;
    L_0x0167:
        r7 = 0;
        return r7;
    L_0x0169:
        r7 = 3;
        r7 = r26[r7];
        if (r7 == 0) goto L_0x0167;
    L_0x016e:
        r0 = r26;
        r7 = r0.length;
        r18 = r7 + -3;
        r7 = r20 + r18;
        r8 = 7;
        if (r7 <= r8) goto L_0x017a;
    L_0x0178:
        r7 = 0;
        return r7;
    L_0x017a:
        r7 = 1;
        r7 = r26[r7];
        if (r7 == r5) goto L_0x0181;
    L_0x017f:
        r7 = 0;
        return r7;
    L_0x0181:
        r7 = 0;
        r7 = r26[r7];
        if (r7 == r3) goto L_0x01a2;
    L_0x0186:
        r7 = -1;
        r0 = r17;
        if (r0 != r7) goto L_0x01d3;
    L_0x018b:
        r7 = 0;
        r7 = r26[r7];
        r22 = r7 - r3;
        r7 = -11;
        r0 = r22;
        if (r0 == r7) goto L_0x019b;
    L_0x0196:
        r7 = -1;
        r0 = r22;
        if (r0 != r7) goto L_0x01c2;
    L_0x019b:
        r7 = 0;
        r17 = r26[r7];
        r25 = r17;
        r24 = 31;
    L_0x01a2:
        r7 = 0;
        r7 = r26[r7];
        r0 = r25;
        if (r7 != r0) goto L_0x01ed;
    L_0x01a9:
        r30 = 0;
    L_0x01ab:
        r0 = r30;
        r1 = r18;
        if (r0 >= r1) goto L_0x01ed;
    L_0x01b1:
        r7 = r30 + 3;
        r23 = r26[r7];
        if (r23 <= 0) goto L_0x01e1;
    L_0x01b7:
        r0 = r23;
        r1 = r24;
        if (r0 >= r1) goto L_0x01bf;
    L_0x01bd:
        r24 = r23;
    L_0x01bf:
        r30 = r30 + 1;
        goto L_0x01ab;
    L_0x01c2:
        r7 = 11;
        r0 = r22;
        if (r0 == r7) goto L_0x01cd;
    L_0x01c8:
        r7 = 1;
        r0 = r22;
        if (r0 != r7) goto L_0x01d1;
    L_0x01cd:
        r7 = 0;
        r17 = r26[r7];
        goto L_0x01a2;
    L_0x01d1:
        r7 = 0;
        return r7;
    L_0x01d3:
        r7 = 0;
        r7 = r26[r7];
        if (r7 == r3) goto L_0x01a2;
    L_0x01d8:
        r7 = 0;
        r7 = r26[r7];
        r0 = r17;
        if (r7 == r0) goto L_0x01a2;
    L_0x01df:
        r7 = 0;
        return r7;
    L_0x01e1:
        r7 = MONTHLENGTH;
        r8 = 0;
        r8 = r26[r8];
        r7 = r7[r8];
        r7 = r7 + r23;
        r23 = r7 + 1;
        goto L_0x01b7;
    L_0x01ed:
        r20 = r20 + r18;
        r29 = r29 + 1;
        goto L_0x0131;
    L_0x01f3:
        r7 = 7;
        r0 = r20;
        if (r0 == r7) goto L_0x01fa;
    L_0x01f8:
        r7 = 0;
        return r7;
    L_0x01fa:
        r3 = r25;
        r9 = r24;
        goto L_0x00a0;
    L_0x0200:
        if (r5 == 0) goto L_0x020e;
    L_0x0202:
        if (r4 == 0) goto L_0x020e;
    L_0x0204:
        if (r9 != 0) goto L_0x020e;
    L_0x0206:
        r2 = new android.icu.util.DateTimeRule;
        r7 = 0;
        r2.<init>(r3, r4, r5, r6, r7);
        goto L_0x00e3;
    L_0x020e:
        if (r5 == 0) goto L_0x0221;
    L_0x0210:
        if (r4 != 0) goto L_0x0221;
    L_0x0212:
        if (r9 == 0) goto L_0x0221;
    L_0x0214:
        r2 = new android.icu.util.DateTimeRule;
        r11 = 1;
        r13 = 0;
        r7 = r2;
        r8 = r3;
        r10 = r5;
        r12 = r6;
        r7.<init>(r8, r9, r10, r11, r12, r13);
        goto L_0x00e3;
    L_0x0221:
        r7 = 0;
        return r7;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.util.VTimeZone.createRuleByRRULE(java.lang.String, int, int, long, java.util.List, int):android.icu.util.TimeZoneRule");
    }

    private static int[] parseRRULE(String rrule, long[] until) {
        int month = -1;
        int dayOfWeek = 0;
        int nthDayOfWeek = 0;
        int[] dayOfMonth = null;
        long untilTime = MIN_TIME;
        boolean yearly = false;
        boolean parseError = false;
        StringTokenizer stringTokenizer = new StringTokenizer(rrule, SEMICOLON);
        while (stringTokenizer.hasMoreTokens()) {
            String prop = stringTokenizer.nextToken();
            int sep = prop.indexOf(EQUALS_SIGN);
            if (sep == -1) {
                parseError = true;
                break;
            }
            String attr = prop.substring(0, sep);
            String value = prop.substring(sep + 1);
            if (attr.equals(ICAL_FREQ)) {
                if (!value.equals(ICAL_YEARLY)) {
                    parseError = true;
                    break;
                }
                yearly = true;
            } else if (attr.equals(ICAL_UNTIL)) {
                try {
                    untilTime = parseDateTimeString(value, 0);
                } catch (IllegalArgumentException e) {
                    parseError = true;
                }
            } else if (attr.equals(ICAL_BYMONTH)) {
                if (value.length() > 2) {
                    parseError = true;
                    break;
                }
                try {
                    month = Integer.parseInt(value) - 1;
                    if (month < 0 || month >= 12) {
                        parseError = true;
                        break;
                    }
                } catch (NumberFormatException e2) {
                    parseError = true;
                }
            } else if (attr.equals(ICAL_BYDAY)) {
                int length = value.length();
                if (length >= 2 && length <= 4) {
                    if (length > 2) {
                        int sign = 1;
                        if (value.charAt(0) != '+') {
                            if (value.charAt(0) != '-') {
                                if (length == 4) {
                                    parseError = true;
                                    break;
                                }
                            }
                            sign = -1;
                        } else {
                            sign = 1;
                        }
                        try {
                            int n = Integer.parseInt(value.substring(length - 3, length - 2));
                            if (n == 0 || n > 4) {
                                parseError = true;
                                break;
                            }
                            nthDayOfWeek = n * sign;
                            value = value.substring(length - 2);
                        } catch (NumberFormatException e3) {
                            parseError = true;
                        }
                    }
                    int wday = 0;
                    while (wday < ICAL_DOW_NAMES.length && !value.equals(ICAL_DOW_NAMES[wday])) {
                        wday++;
                    }
                    if (wday >= ICAL_DOW_NAMES.length) {
                        parseError = true;
                        break;
                    }
                    dayOfWeek = wday + 1;
                } else {
                    parseError = true;
                    break;
                }
            } else if (attr.equals(ICAL_BYMONTHDAY)) {
                StringTokenizer days = new StringTokenizer(value, COMMA);
                dayOfMonth = new int[days.countTokens()];
                int index = 0;
                while (days.hasMoreTokens()) {
                    int index2 = index + 1;
                    try {
                        dayOfMonth[index] = Integer.parseInt(days.nextToken());
                        index = index2;
                    } catch (NumberFormatException e4) {
                        parseError = true;
                    }
                }
            }
        }
        if (parseError) {
            return null;
        }
        if (!yearly) {
            return null;
        }
        int[] results;
        until[0] = untilTime;
        if (dayOfMonth == null) {
            results = new int[4];
            results[3] = 0;
        } else {
            results = new int[(dayOfMonth.length + 3)];
            for (int i = 0; i < dayOfMonth.length; i++) {
                results[i + 3] = dayOfMonth[i];
            }
        }
        results[0] = month;
        results[1] = dayOfWeek;
        results[2] = nthDayOfWeek;
        return results;
    }

    private static TimeZoneRule createRuleByRDATE(String tzname, int rawOffset, int dstSavings, long start, List<String> dates, int fromOffset) {
        long[] times;
        if (dates == null || dates.size() == 0) {
            times = new long[]{start};
        } else {
            times = new long[dates.size()];
            int idx = 0;
            try {
                Iterator date$iterator = dates.iterator();
                while (true) {
                    int idx2;
                    try {
                        idx2 = idx;
                        if (!date$iterator.hasNext()) {
                            break;
                        }
                        idx = idx2 + 1;
                        times[idx2] = parseDateTimeString((String) date$iterator.next(), fromOffset);
                    } catch (IllegalArgumentException e) {
                        idx = idx2;
                        return null;
                    }
                }
            } catch (IllegalArgumentException e2) {
            }
        }
        return new TimeArrayTimeZoneRule(tzname, rawOffset, dstSavings, times, 2);
    }

    private void writeZone(Writer w, BasicTimeZone basictz, String[] customProperties) throws IOException {
        boolean isDst;
        writeHeader(w);
        if (customProperties != null && customProperties.length > 0) {
            for (int i = 0; i < customProperties.length; i++) {
                if (customProperties[i] != null) {
                    w.write(customProperties[i]);
                    w.write(NEWLINE);
                }
            }
        }
        long t = MIN_TIME;
        String dstName = null;
        int dstFromOffset = 0;
        int dstFromDSTSavings = 0;
        int dstToOffset = 0;
        int dstStartYear = 0;
        int dstMonth = 0;
        int dstDayOfWeek = 0;
        int dstWeekInMonth = 0;
        int dstMillisInDay = 0;
        long dstStartTime = DEF_TZSTARTTIME;
        long dstUntilTime = DEF_TZSTARTTIME;
        int dstCount = 0;
        AnnualTimeZoneRule finalDstRule = null;
        String stdName = null;
        int stdFromOffset = 0;
        int stdFromDSTSavings = 0;
        int stdToOffset = 0;
        int stdStartYear = 0;
        int stdMonth = 0;
        int stdDayOfWeek = 0;
        int stdWeekInMonth = 0;
        int stdMillisInDay = 0;
        long stdStartTime = DEF_TZSTARTTIME;
        long stdUntilTime = DEF_TZSTARTTIME;
        int stdCount = 0;
        AnnualTimeZoneRule finalStdRule = null;
        int[] dtfields = new int[6];
        boolean hasTransitions = false;
        while (true) {
            TimeZoneTransition tzt = basictz.getNextTransition(t, false);
            if (tzt != null) {
                hasTransitions = true;
                t = tzt.getTime();
                String name = tzt.getTo().getName();
                isDst = tzt.getTo().getDSTSavings() != 0;
                int fromOffset = tzt.getFrom().getRawOffset() + tzt.getFrom().getDSTSavings();
                int fromDSTSavings = tzt.getFrom().getDSTSavings();
                int toOffset = tzt.getTo().getRawOffset() + tzt.getTo().getDSTSavings();
                Grego.timeToFields(tzt.getTime() + ((long) fromOffset), dtfields);
                int weekInMonth = Grego.getDayOfWeekInMonth(dtfields[0], dtfields[1], dtfields[2]);
                int year = dtfields[0];
                boolean sameRule = false;
                if (!isDst) {
                    if (finalStdRule == null && (tzt.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) tzt.getTo()).getEndYear() == Integer.MAX_VALUE) {
                        finalStdRule = (AnnualTimeZoneRule) tzt.getTo();
                    }
                    if (stdCount > 0) {
                        if (year == stdStartYear + stdCount && name.equals(stdName) && stdFromOffset == fromOffset && stdToOffset == toOffset && stdMonth == dtfields[1] && stdDayOfWeek == dtfields[3] && stdWeekInMonth == weekInMonth && stdMillisInDay == dtfields[5]) {
                            stdUntilTime = t;
                            stdCount++;
                            sameRule = true;
                        }
                        if (!sameRule) {
                            if (stdCount == 1) {
                                writeZonePropsByTime(w, false, stdName, stdFromOffset, stdToOffset, stdStartTime, true);
                            } else {
                                writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, stdUntilTime);
                            }
                        }
                    }
                    if (!sameRule) {
                        stdName = name;
                        stdFromOffset = fromOffset;
                        stdFromDSTSavings = fromDSTSavings;
                        stdToOffset = toOffset;
                        stdStartYear = year;
                        stdMonth = dtfields[1];
                        stdDayOfWeek = dtfields[3];
                        stdWeekInMonth = weekInMonth;
                        stdMillisInDay = dtfields[5];
                        stdUntilTime = t;
                        stdStartTime = t;
                        stdCount = 1;
                    }
                    if (!(finalStdRule == null || finalDstRule == null)) {
                        break;
                    }
                }
                if (finalDstRule == null && (tzt.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) tzt.getTo()).getEndYear() == Integer.MAX_VALUE) {
                    finalDstRule = (AnnualTimeZoneRule) tzt.getTo();
                }
                if (dstCount > 0) {
                    if (year == dstStartYear + dstCount && name.equals(dstName) && dstFromOffset == fromOffset && dstToOffset == toOffset && dstMonth == dtfields[1] && dstDayOfWeek == dtfields[3] && dstWeekInMonth == weekInMonth && dstMillisInDay == dtfields[5]) {
                        dstUntilTime = t;
                        dstCount++;
                        sameRule = true;
                    }
                    if (!sameRule) {
                        if (dstCount == 1) {
                            writeZonePropsByTime(w, true, dstName, dstFromOffset, dstToOffset, dstStartTime, true);
                        } else {
                            writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, dstUntilTime);
                        }
                    }
                }
                if (!sameRule) {
                    dstName = name;
                    dstFromOffset = fromOffset;
                    dstFromDSTSavings = fromDSTSavings;
                    dstToOffset = toOffset;
                    dstStartYear = year;
                    dstMonth = dtfields[1];
                    dstDayOfWeek = dtfields[3];
                    dstWeekInMonth = weekInMonth;
                    dstMillisInDay = dtfields[5];
                    dstUntilTime = t;
                    dstStartTime = t;
                    dstCount = 1;
                }
                if (!(finalStdRule == null || finalDstRule == null)) {
                    break;
                }
            }
            break;
        }
        if (hasTransitions) {
            Date nextStart;
            if (dstCount > 0) {
                if (finalDstRule == null) {
                    if (dstCount == 1) {
                        writeZonePropsByTime(w, true, dstName, dstFromOffset, dstToOffset, dstStartTime, true);
                    } else {
                        writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, dstUntilTime);
                    }
                } else if (dstCount == 1) {
                    writeFinalRule(w, true, finalDstRule, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, dstStartTime);
                } else {
                    if (isEquivalentDateRule(dstMonth, dstWeekInMonth, dstDayOfWeek, finalDstRule.getRule())) {
                        writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, MAX_TIME);
                    } else {
                        writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, dstUntilTime);
                        nextStart = finalDstRule.getNextStart(dstUntilTime, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, false);
                        if (!-assertionsDisabled && nextStart == null) {
                            throw new AssertionError();
                        } else if (nextStart != null) {
                            writeFinalRule(w, true, finalDstRule, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, nextStart.getTime());
                        }
                    }
                }
            }
            if (stdCount > 0) {
                if (finalStdRule == null) {
                    if (stdCount == 1) {
                        writeZonePropsByTime(w, false, stdName, stdFromOffset, stdToOffset, stdStartTime, true);
                    } else {
                        writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, stdUntilTime);
                    }
                } else if (stdCount == 1) {
                    writeFinalRule(w, false, finalStdRule, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, stdStartTime);
                } else {
                    if (isEquivalentDateRule(stdMonth, stdWeekInMonth, stdDayOfWeek, finalStdRule.getRule())) {
                        writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, MAX_TIME);
                    } else {
                        writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, stdUntilTime);
                        nextStart = finalStdRule.getNextStart(stdUntilTime, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, false);
                        if (!-assertionsDisabled && nextStart == null) {
                            throw new AssertionError();
                        } else if (nextStart != null) {
                            writeFinalRule(w, false, finalStdRule, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, nextStart.getTime());
                        }
                    }
                }
            }
        } else {
            int offset = basictz.getOffset(DEF_TZSTARTTIME);
            isDst = offset != basictz.getRawOffset();
            writeZonePropsByTime(w, isDst, getDefaultTZName(basictz.getID(), isDst), offset, offset, DEF_TZSTARTTIME - ((long) offset), false);
        }
        writeFooter(w);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isEquivalentDateRule(int month, int weekInMonth, int dayOfWeek, DateTimeRule dtrule) {
        if (month != dtrule.getRuleMonth() || dayOfWeek != dtrule.getRuleDayOfWeek() || dtrule.getTimeRuleType() != 0) {
            return false;
        }
        if (dtrule.getDateRuleType() == 1 && dtrule.getRuleWeekInMonth() == weekInMonth) {
            return true;
        }
        int ruleDOM = dtrule.getRuleDayOfMonth();
        if (dtrule.getDateRuleType() == 2) {
            if (ruleDOM % 7 == 1 && (ruleDOM + 6) / 7 == weekInMonth) {
                return true;
            }
            if (month != 1 && (MONTHLENGTH[month] - ruleDOM) % 7 == 6 && weekInMonth == (((MONTHLENGTH[month] - ruleDOM) + 1) / 7) * -1) {
                return true;
            }
        }
        if (dtrule.getDateRuleType() == 3) {
            if (ruleDOM % 7 == 0 && ruleDOM / 7 == weekInMonth) {
                return true;
            }
            return month != 1 && (MONTHLENGTH[month] - ruleDOM) % 7 == 0 && weekInMonth == (((MONTHLENGTH[month] - ruleDOM) / 7) + 1) * -1;
        }
    }

    private static void writeZonePropsByTime(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, long time, boolean withRDATE) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, time);
        if (withRDATE) {
            writer.write(ICAL_RDATE);
            writer.write(COLON);
            writer.write(getDateTimeString(((long) fromOffset) + time));
            writer.write(NEWLINE);
        }
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, long startTime, long untilTime) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
        beginRRULE(writer, month);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(dayOfMonth));
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOW(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int weekInMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
        beginRRULE(writer, month);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(weekInMonth));
        writer.write(ICAL_DOW_NAMES[dayOfWeek - 1]);
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOW_GEQ_DOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        if (dayOfMonth % 7 == 1) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, (dayOfMonth + 6) / 7, dayOfWeek, startTime, untilTime);
        } else if (month == 1 || (MONTHLENGTH[month] - dayOfMonth) % 7 != 6) {
            beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
            int startDay = dayOfMonth;
            int currentMonthDays = 7;
            if (dayOfMonth <= 0) {
                int prevMonthDays = 1 - dayOfMonth;
                currentMonthDays = 7 - prevMonthDays;
                writeZonePropsByDOW_GEQ_DOM_sub(writer, month + -1 < 0 ? 11 : month - 1, -prevMonthDays, dayOfWeek, prevMonthDays, MAX_TIME, fromOffset);
                startDay = 1;
            } else if (dayOfMonth + 6 > MONTHLENGTH[month]) {
                int nextMonthDays = (dayOfMonth + 6) - MONTHLENGTH[month];
                currentMonthDays = 7 - nextMonthDays;
                writeZonePropsByDOW_GEQ_DOM_sub(writer, month + 1 > 11 ? 0 : month + 1, 1, dayOfWeek, nextMonthDays, MAX_TIME, fromOffset);
            }
            writeZonePropsByDOW_GEQ_DOM_sub(writer, month, startDay, dayOfWeek, currentMonthDays, untilTime, fromOffset);
            endZoneProps(writer, isDst);
        } else {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, (((MONTHLENGTH[month] - dayOfMonth) + 1) / 7) * -1, dayOfWeek, startTime, untilTime);
        }
    }

    private static void writeZonePropsByDOW_GEQ_DOM_sub(Writer writer, int month, int dayOfMonth, int dayOfWeek, int numDays, long untilTime, int fromOffset) throws IOException {
        int startDayNum = dayOfMonth;
        boolean isFeb = month == 1;
        if (dayOfMonth < 0 && (isFeb ^ 1) != 0) {
            startDayNum = (MONTHLENGTH[month] + dayOfMonth) + 1;
        }
        beginRRULE(writer, month);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_DOW_NAMES[dayOfWeek - 1]);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(startDayNum));
        for (int i = 1; i < numDays; i++) {
            writer.write(COMMA);
            writer.write(Integer.toString(startDayNum + i));
        }
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
    }

    private static void writeZonePropsByDOW_LEQ_DOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        if (dayOfMonth % 7 == 0) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, dayOfMonth / 7, dayOfWeek, startTime, untilTime);
        } else if (month != 1 && (MONTHLENGTH[month] - dayOfMonth) % 7 == 0) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, (((MONTHLENGTH[month] - dayOfMonth) / 7) + 1) * -1, dayOfWeek, startTime, untilTime);
        } else if (month == 1 && dayOfMonth == 29) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, 1, -1, dayOfWeek, startTime, untilTime);
        } else {
            writeZonePropsByDOW_GEQ_DOM(writer, isDst, tzname, fromOffset, toOffset, month, dayOfMonth - 6, dayOfWeek, startTime, untilTime);
        }
    }

    private static void writeFinalRule(Writer writer, boolean isDst, AnnualTimeZoneRule rule, int fromRawOffset, int fromDSTSavings, long startTime) throws IOException {
        DateTimeRule dtrule = toWallTimeRule(rule.getRule(), fromRawOffset, fromDSTSavings);
        int timeInDay = dtrule.getRuleMillisInDay();
        if (timeInDay < 0) {
            startTime += (long) (0 - timeInDay);
        } else if (timeInDay >= 86400000) {
            startTime -= (long) (timeInDay - 86399999);
        }
        int toOffset = rule.getRawOffset() + rule.getDSTSavings();
        switch (dtrule.getDateRuleType()) {
            case 0:
                writeZonePropsByDOM(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), startTime, MAX_TIME);
                return;
            case 1:
                writeZonePropsByDOW(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleWeekInMonth(), dtrule.getRuleDayOfWeek(), startTime, MAX_TIME);
                return;
            case 2:
                writeZonePropsByDOW_GEQ_DOM(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), dtrule.getRuleDayOfWeek(), startTime, MAX_TIME);
                return;
            case 3:
                writeZonePropsByDOW_LEQ_DOM(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), dtrule.getRuleDayOfWeek(), startTime, MAX_TIME);
                return;
            default:
                return;
        }
    }

    private static DateTimeRule toWallTimeRule(DateTimeRule rule, int rawOffset, int dstSavings) {
        if (rule.getTimeRuleType() == 0) {
            return rule;
        }
        DateTimeRule modifiedRule;
        int wallt = rule.getRuleMillisInDay();
        if (rule.getTimeRuleType() == 2) {
            wallt += rawOffset + dstSavings;
        } else if (rule.getTimeRuleType() == 1) {
            wallt += dstSavings;
        }
        int dshift = 0;
        if (wallt < 0) {
            dshift = -1;
            wallt += Grego.MILLIS_PER_DAY;
        } else if (wallt >= Grego.MILLIS_PER_DAY) {
            dshift = 1;
            wallt -= Grego.MILLIS_PER_DAY;
        }
        int month = rule.getRuleMonth();
        int dom = rule.getRuleDayOfMonth();
        int dow = rule.getRuleDayOfWeek();
        int dtype = rule.getDateRuleType();
        if (dshift != 0) {
            if (dtype == 1) {
                int wim = rule.getRuleWeekInMonth();
                if (wim > 0) {
                    dtype = 2;
                    dom = ((wim - 1) * 7) + 1;
                } else {
                    dtype = 3;
                    dom = MONTHLENGTH[month] + ((wim + 1) * 7);
                }
            }
            dom += dshift;
            if (dom == 0) {
                month--;
                if (month < 0) {
                    month = 11;
                }
                dom = MONTHLENGTH[month];
            } else if (dom > MONTHLENGTH[month]) {
                month++;
                if (month > 11) {
                    month = 0;
                }
                dom = 1;
            }
            if (dtype != 0) {
                dow += dshift;
                if (dow < 1) {
                    dow = 7;
                } else if (dow > 7) {
                    dow = 1;
                }
            }
        }
        if (dtype == 0) {
            modifiedRule = new DateTimeRule(month, dom, wallt, 0);
        } else {
            modifiedRule = new DateTimeRule(month, dom, dow, dtype == 2, wallt, 0);
        }
        return modifiedRule;
    }

    private static void beginZoneProps(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, long startTime) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        if (isDst) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETTO);
        writer.write(COLON);
        writer.write(millisToOffset(toOffset));
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETFROM);
        writer.write(COLON);
        writer.write(millisToOffset(fromOffset));
        writer.write(NEWLINE);
        writer.write(ICAL_TZNAME);
        writer.write(COLON);
        writer.write(tzname);
        writer.write(NEWLINE);
        writer.write(ICAL_DTSTART);
        writer.write(COLON);
        writer.write(getDateTimeString(((long) fromOffset) + startTime));
        writer.write(NEWLINE);
    }

    private static void endZoneProps(Writer writer, boolean isDst) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        if (isDst) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
    }

    private static void beginRRULE(Writer writer, int month) throws IOException {
        writer.write(ICAL_RRULE);
        writer.write(COLON);
        writer.write(ICAL_FREQ);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_YEARLY);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTH);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(month + 1));
        writer.write(SEMICOLON);
    }

    private static void appendUNTIL(Writer writer, String until) throws IOException {
        if (until != null) {
            writer.write(SEMICOLON);
            writer.write(ICAL_UNTIL);
            writer.write(EQUALS_SIGN);
            writer.write(until);
        }
    }

    private void writeHeader(Writer writer) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
        writer.write(ICAL_TZID);
        writer.write(COLON);
        writer.write(this.tz.getID());
        writer.write(NEWLINE);
        if (this.tzurl != null) {
            writer.write(ICAL_TZURL);
            writer.write(COLON);
            writer.write(this.tzurl);
            writer.write(NEWLINE);
        }
        if (this.lastmod != null) {
            writer.write(ICAL_LASTMOD);
            writer.write(COLON);
            writer.write(getUTCDateTimeString(this.lastmod.getTime()));
            writer.write(NEWLINE);
        }
    }

    private static void writeFooter(Writer writer) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
    }

    private static String getDateTimeString(long time) {
        int[] fields = Grego.timeToFields(time, null);
        StringBuilder sb = new StringBuilder(15);
        sb.append(numToString(fields[0], 4));
        sb.append(numToString(fields[1] + 1, 2));
        sb.append(numToString(fields[2], 2));
        sb.append('T');
        int t = fields[5];
        int hour = t / 3600000;
        t %= 3600000;
        int min = t / 60000;
        int sec = (t % 60000) / 1000;
        sb.append(numToString(hour, 2));
        sb.append(numToString(min, 2));
        sb.append(numToString(sec, 2));
        return sb.toString();
    }

    private static String getUTCDateTimeString(long time) {
        return getDateTimeString(time) + "Z";
    }

    private static long parseDateTimeString(String str, int offset) {
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int min = 0;
        int sec = 0;
        boolean isUTC = false;
        boolean isValid = false;
        if (str != null) {
            int length = str.length();
            if ((length == 15 || length == 16) && str.charAt(8) == 'T') {
                if (length == 16) {
                    if (str.charAt(15) == 'Z') {
                        isUTC = true;
                    }
                }
                try {
                    year = Integer.parseInt(str.substring(0, 4));
                    month = Integer.parseInt(str.substring(4, 6)) - 1;
                    day = Integer.parseInt(str.substring(6, 8));
                    hour = Integer.parseInt(str.substring(9, 11));
                    min = Integer.parseInt(str.substring(11, 13));
                    sec = Integer.parseInt(str.substring(13, 15));
                    int maxDayOfMonth = Grego.monthLength(year, month);
                    if (year >= 0 && month >= 0 && month <= 11 && day >= 1 && day <= maxDayOfMonth && hour >= 0 && hour < 24 && min >= 0 && min < 60 && sec >= 0 && sec < 60) {
                        isValid = true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        if (isValid) {
            long time = (Grego.fieldsToDay(year, month, day) * 86400000) + ((long) (((3600000 * hour) + (60000 * min)) + (sec * 1000)));
            if (isUTC) {
                return time;
            }
            return time - ((long) offset);
        }
        throw new IllegalArgumentException("Invalid date time string format");
    }

    private static int offsetStrToMillis(String str) {
        boolean isValid = false;
        int sign = 0;
        int hour = 0;
        int min = 0;
        int sec = 0;
        if (str != null) {
            int length = str.length();
            if (length == 5 || length == 7) {
                char s = str.charAt(0);
                if (s == '+') {
                    sign = 1;
                } else if (s == '-') {
                    sign = -1;
                }
                try {
                    hour = Integer.parseInt(str.substring(1, 3));
                    min = Integer.parseInt(str.substring(3, 5));
                    if (length == 7) {
                        sec = Integer.parseInt(str.substring(5, 7));
                    }
                    isValid = true;
                } catch (NumberFormatException e) {
                }
            }
        }
        if (isValid) {
            return (((((hour * 60) + min) * 60) + sec) * sign) * 1000;
        }
        throw new IllegalArgumentException("Bad offset string");
    }

    private static String millisToOffset(int millis) {
        StringBuilder sb = new StringBuilder(7);
        if (millis >= 0) {
            sb.append('+');
        } else {
            sb.append('-');
            millis = -millis;
        }
        int t = millis / 1000;
        int sec = t % 60;
        t = (t - sec) / 60;
        int min = t % 60;
        sb.append(numToString(t / 60, 2));
        sb.append(numToString(min, 2));
        sb.append(numToString(sec, 2));
        return sb.toString();
    }

    private static String numToString(int num, int width) {
        String str = Integer.toString(num);
        int len = str.length();
        if (len >= width) {
            return str.substring(len - width, len);
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = len; i < width; i++) {
            sb.append('0');
        }
        sb.append(str);
        return sb.toString();
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    public TimeZone cloneAsThawed() {
        VTimeZone vtz = (VTimeZone) super.cloneAsThawed();
        vtz.tz = (BasicTimeZone) this.tz.cloneAsThawed();
        vtz.isFrozen = false;
        return vtz;
    }
}
