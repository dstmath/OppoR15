package android.icu.util;

import android.icu.impl.Grego;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

public class RuleBasedTimeZone extends BasicTimeZone {
    private static final long serialVersionUID = 7580833058949327935L;
    private AnnualTimeZoneRule[] finalRules;
    private List<TimeZoneRule> historicRules;
    private transient List<TimeZoneTransition> historicTransitions;
    private final InitialTimeZoneRule initialRule;
    private volatile transient boolean isFrozen = false;
    private transient boolean upToDate;

    public RuleBasedTimeZone(String id, InitialTimeZoneRule initialRule) {
        super(id);
        this.initialRule = initialRule;
    }

    public void addTransitionRule(TimeZoneRule rule) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen RuleBasedTimeZone instance.");
        } else if (rule.isTransitionRule()) {
            if (!(rule instanceof AnnualTimeZoneRule) || ((AnnualTimeZoneRule) rule).getEndYear() != Integer.MAX_VALUE) {
                if (this.historicRules == null) {
                    this.historicRules = new ArrayList();
                }
                this.historicRules.add(rule);
            } else if (this.finalRules == null) {
                this.finalRules = new AnnualTimeZoneRule[2];
                this.finalRules[0] = (AnnualTimeZoneRule) rule;
            } else if (this.finalRules[1] == null) {
                this.finalRules[1] = (AnnualTimeZoneRule) rule;
            } else {
                throw new IllegalStateException("Too many final rules");
            }
            this.upToDate = false;
        } else {
            throw new IllegalArgumentException("Rule must be a transition rule");
        }
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (era == 0) {
            year = 1 - year;
        }
        int[] offsets = new int[2];
        getOffset((Grego.fieldsToDay(year, month, day) * 86400000) + ((long) milliseconds), true, 3, 1, offsets);
        return offsets[0] + offsets[1];
    }

    public void getOffset(long time, boolean local, int[] offsets) {
        getOffset(time, local, 4, 12, offsets);
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        getOffset(date, true, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
    }

    public int getRawOffset() {
        int[] offsets = new int[2];
        getOffset(System.currentTimeMillis(), false, offsets);
        return offsets[0];
    }

    public boolean inDaylightTime(Date date) {
        int[] offsets = new int[2];
        getOffset(date.getTime(), false, offsets);
        if (offsets[1] != 0) {
            return true;
        }
        return false;
    }

    public void setRawOffset(int offsetMillis) {
        throw new UnsupportedOperationException("setRawOffset in RuleBasedTimeZone is not supported.");
    }

    public boolean useDaylightTime() {
        long now = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(now, false, offsets);
        if (offsets[1] != 0) {
            return true;
        }
        TimeZoneTransition tt = getNextTransition(now, false);
        return (tt == null || tt.getTo().getDSTSavings() == 0) ? false : true;
    }

    public boolean observesDaylightTime() {
        long time = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(time, false, offsets);
        if (offsets[1] != 0) {
            return true;
        }
        BitSet checkFinals = this.finalRules == null ? null : new BitSet(this.finalRules.length);
        while (true) {
            TimeZoneTransition tt = getNextTransition(time, false);
            if (tt == null) {
                break;
            }
            TimeZoneRule toRule = tt.getTo();
            if (toRule.getDSTSavings() != 0) {
                return true;
            }
            if (checkFinals != null) {
                for (int i = 0; i < this.finalRules.length; i++) {
                    if (this.finalRules[i].equals(toRule)) {
                        checkFinals.set(i);
                    }
                }
                if (checkFinals.cardinality() == this.finalRules.length) {
                    break;
                }
            }
            time = tt.getTime();
        }
        return false;
    }

    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuleBasedTimeZone)) {
            return false;
        }
        RuleBasedTimeZone otherRBTZ = (RuleBasedTimeZone) other;
        if (!this.initialRule.isEquivalentTo(otherRBTZ.initialRule)) {
            return false;
        }
        if (this.finalRules != null && otherRBTZ.finalRules != null) {
            int i = 0;
            while (i < this.finalRules.length) {
                if ((this.finalRules[i] != null || otherRBTZ.finalRules[i] != null) && (this.finalRules[i] == null || otherRBTZ.finalRules[i] == null || !this.finalRules[i].isEquivalentTo(otherRBTZ.finalRules[i]))) {
                    return false;
                }
                i++;
            }
        } else if (!(this.finalRules == null && otherRBTZ.finalRules == null)) {
            return false;
        }
        if (this.historicRules == null || otherRBTZ.historicRules == null) {
            if (!(this.historicRules == null && otherRBTZ.historicRules == null)) {
                return false;
            }
        } else if (this.historicRules.size() != otherRBTZ.historicRules.size()) {
            return false;
        } else {
            for (TimeZoneRule rule : this.historicRules) {
                boolean foundSameRule = false;
                for (TimeZoneRule orule : otherRBTZ.historicRules) {
                    if (rule.isEquivalentTo(orule)) {
                        foundSameRule = true;
                        continue;
                        break;
                    }
                }
                if (!foundSameRule) {
                    return false;
                }
            }
        }
        return true;
    }

    public TimeZoneRule[] getTimeZoneRules() {
        int size = 1;
        if (this.historicRules != null) {
            size = this.historicRules.size() + 1;
        }
        if (this.finalRules != null) {
            if (this.finalRules[1] != null) {
                size += 2;
            } else {
                size++;
            }
        }
        TimeZoneRule[] rules = new TimeZoneRule[size];
        rules[0] = this.initialRule;
        int idx = 1;
        if (this.historicRules != null) {
            while (idx < this.historicRules.size() + 1) {
                rules[idx] = (TimeZoneRule) this.historicRules.get(idx - 1);
                idx++;
            }
        }
        if (this.finalRules != null) {
            int idx2 = idx + 1;
            rules[idx] = this.finalRules[0];
            if (this.finalRules[1] != null) {
                rules[idx2] = this.finalRules[1];
                idx = idx2;
            }
        }
        return rules;
    }

    public android.icu.util.TimeZoneTransition getNextTransition(long r20, boolean r22) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r18_4 'tzt' android.icu.util.TimeZoneTransition) in PHI: PHI: (r18_5 'tzt' android.icu.util.TimeZoneTransition) = (r18_4 'tzt' android.icu.util.TimeZoneTransition), (r18_6 'tzt' android.icu.util.TimeZoneTransition) binds: {(r18_4 'tzt' android.icu.util.TimeZoneTransition)=B:28:0x00c2, (r18_6 'tzt' android.icu.util.TimeZoneTransition)=B:30:0x00e0}
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
        r19 = this;
        r19.complete();
        r0 = r19;
        r3 = r0.historicTransitions;
        if (r3 != 0) goto L_0x000b;
    L_0x0009:
        r3 = 0;
        return r3;
    L_0x000b:
        r10 = 0;
        r0 = r19;
        r3 = r0.historicTransitions;
        r4 = 0;
        r18 = r3.get(r4);
        r18 = (android.icu.util.TimeZoneTransition) r18;
        r16 = r18.getTime();
        r3 = (r16 > r20 ? 1 : (r16 == r20 ? 0 : -1));
        if (r3 > 0) goto L_0x0025;
    L_0x001f:
        if (r22 == 0) goto L_0x0047;
    L_0x0021:
        r3 = (r16 > r20 ? 1 : (r16 == r20 ? 0 : -1));
        if (r3 != 0) goto L_0x0047;
    L_0x0025:
        r12 = r18;
    L_0x0027:
        r2 = r12.getFrom();
        r15 = r12.getTo();
        r3 = r2.getRawOffset();
        r4 = r15.getRawOffset();
        if (r3 != r4) goto L_0x012d;
    L_0x0039:
        r3 = r2.getDSTSavings();
        r4 = r15.getDSTSavings();
        if (r3 != r4) goto L_0x012d;
    L_0x0043:
        if (r10 == 0) goto L_0x0122;
    L_0x0045:
        r3 = 0;
        return r3;
    L_0x0047:
        r0 = r19;
        r3 = r0.historicTransitions;
        r3 = r3.size();
        r9 = r3 + -1;
        r0 = r19;
        r3 = r0.historicTransitions;
        r18 = r3.get(r9);
        r18 = (android.icu.util.TimeZoneTransition) r18;
        r16 = r18.getTime();
        if (r22 == 0) goto L_0x0068;
    L_0x0061:
        r3 = (r16 > r20 ? 1 : (r16 == r20 ? 0 : -1));
        if (r3 != 0) goto L_0x0068;
    L_0x0065:
        r12 = r18;
        goto L_0x0027;
    L_0x0068:
        r3 = (r16 > r20 ? 1 : (r16 == r20 ? 0 : -1));
        if (r3 > 0) goto L_0x00fc;
    L_0x006c:
        r0 = r19;
        r3 = r0.finalRules;
        if (r3 == 0) goto L_0x00fa;
    L_0x0072:
        r0 = r19;
        r3 = r0.finalRules;
        r4 = 0;
        r3 = r3[r4];
        r0 = r19;
        r4 = r0.finalRules;
        r5 = 1;
        r4 = r4[r5];
        r6 = r4.getRawOffset();
        r0 = r19;
        r4 = r0.finalRules;
        r5 = 1;
        r4 = r4[r5];
        r7 = r4.getDSTSavings();
        r4 = r20;
        r8 = r22;
        r13 = r3.getNextStart(r4, r6, r7, r8);
        r0 = r19;
        r3 = r0.finalRules;
        r4 = 1;
        r3 = r3[r4];
        r0 = r19;
        r4 = r0.finalRules;
        r5 = 0;
        r4 = r4[r5];
        r6 = r4.getRawOffset();
        r0 = r19;
        r4 = r0.finalRules;
        r5 = 0;
        r4 = r4[r5];
        r7 = r4.getDSTSavings();
        r4 = r20;
        r8 = r22;
        r14 = r3.getNextStart(r4, r6, r7, r8);
        r3 = r14.after(r13);
        if (r3 == 0) goto L_0x00e0;
    L_0x00c2:
        r18 = new android.icu.util.TimeZoneTransition;
        r4 = r13.getTime();
        r0 = r19;
        r3 = r0.finalRules;
        r6 = 1;
        r3 = r3[r6];
        r0 = r19;
        r6 = r0.finalRules;
        r7 = 0;
        r6 = r6[r7];
        r0 = r18;
        r0.<init>(r4, r3, r6);
    L_0x00db:
        r12 = r18;
        r10 = 1;
        goto L_0x0027;
    L_0x00e0:
        r18 = new android.icu.util.TimeZoneTransition;
        r4 = r14.getTime();
        r0 = r19;
        r3 = r0.finalRules;
        r6 = 0;
        r3 = r3[r6];
        r0 = r19;
        r6 = r0.finalRules;
        r7 = 1;
        r6 = r6[r7];
        r0 = r18;
        r0.<init>(r4, r3, r6);
        goto L_0x00db;
    L_0x00fa:
        r3 = 0;
        return r3;
    L_0x00fc:
        r9 = r9 + -1;
        r11 = r18;
    L_0x0100:
        if (r9 <= 0) goto L_0x011a;
    L_0x0102:
        r0 = r19;
        r3 = r0.historicTransitions;
        r18 = r3.get(r9);
        r18 = (android.icu.util.TimeZoneTransition) r18;
        r16 = r18.getTime();
        r3 = (r16 > r20 ? 1 : (r16 == r20 ? 0 : -1));
        if (r3 < 0) goto L_0x011a;
    L_0x0114:
        if (r22 != 0) goto L_0x011d;
    L_0x0116:
        r3 = (r16 > r20 ? 1 : (r16 == r20 ? 0 : -1));
        if (r3 != 0) goto L_0x011d;
    L_0x011a:
        r12 = r11;
        goto L_0x0027;
    L_0x011d:
        r9 = r9 + -1;
        r11 = r18;
        goto L_0x0100;
    L_0x0122:
        r4 = r12.getTime();
        r3 = 0;
        r0 = r19;
        r12 = r0.getNextTransition(r4, r3);
    L_0x012d:
        return r12;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.util.RuleBasedTimeZone.getNextTransition(long, boolean):android.icu.util.TimeZoneTransition");
    }

    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        complete();
        if (this.historicTransitions == null) {
            return null;
        }
        TimeZoneTransition result;
        TimeZoneTransition tzt = (TimeZoneTransition) this.historicTransitions.get(0);
        long tt = tzt.getTime();
        if (inclusive && tt == base) {
            result = tzt;
        } else if (tt >= base) {
            return null;
        } else {
            int idx = this.historicTransitions.size() - 1;
            tzt = (TimeZoneTransition) this.historicTransitions.get(idx);
            tt = tzt.getTime();
            if (inclusive && tt == base) {
                result = tzt;
            } else if (tt < base) {
                if (this.finalRules != null) {
                    Date start0 = this.finalRules[0].getPreviousStart(base, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), inclusive);
                    Date start1 = this.finalRules[1].getPreviousStart(base, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), inclusive);
                    if (start1.before(start0)) {
                        tzt = new TimeZoneTransition(start0.getTime(), this.finalRules[1], this.finalRules[0]);
                    } else {
                        tzt = new TimeZoneTransition(start1.getTime(), this.finalRules[0], this.finalRules[1]);
                    }
                }
                result = tzt;
            } else {
                for (idx--; idx >= 0; idx--) {
                    tzt = (TimeZoneTransition) this.historicTransitions.get(idx);
                    tt = tzt.getTime();
                    if (tt < base || (inclusive && tt == base)) {
                        break;
                    }
                }
                result = tzt;
            }
        }
        TimeZoneRule from = result.getFrom();
        TimeZoneRule to = result.getTo();
        if (from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
            result = getPreviousTransition(result.getTime(), false);
        }
        return result;
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    private void complete() {
        if (!this.upToDate) {
            if (this.finalRules == null || this.finalRules[1] != null) {
                if (!(this.historicRules == null && this.finalRules == null)) {
                    TimeZoneRule curRule = this.initialRule;
                    long lastTransitionTime = Grego.MIN_MILLIS;
                    if (this.historicRules != null) {
                        BitSet bitSet = new BitSet(this.historicRules.size());
                        while (true) {
                            int i;
                            Date d;
                            long tt;
                            int curStdOffset = curRule.getRawOffset();
                            int curDstSavings = curRule.getDSTSavings();
                            long nextTransitionTime = Grego.MAX_MILLIS;
                            TimeZoneRule nextRule = null;
                            for (i = 0; i < this.historicRules.size(); i++) {
                                if (!bitSet.get(i)) {
                                    TimeZoneRule r = (TimeZoneRule) this.historicRules.get(i);
                                    d = r.getNextStart(lastTransitionTime, curStdOffset, curDstSavings, false);
                                    if (d == null) {
                                        bitSet.set(i);
                                    } else if (!(r == curRule || (r.getName().equals(curRule.getName()) && r.getRawOffset() == curRule.getRawOffset() && r.getDSTSavings() == curRule.getDSTSavings()))) {
                                        tt = d.getTime();
                                        if (tt < nextTransitionTime) {
                                            nextTransitionTime = tt;
                                            nextRule = r;
                                        }
                                    }
                                }
                            }
                            if (nextRule == null) {
                                boolean bDoneAll = true;
                                for (int j = 0; j < this.historicRules.size(); j++) {
                                    if (!bitSet.get(j)) {
                                        bDoneAll = false;
                                        break;
                                    }
                                }
                                if (bDoneAll) {
                                    break;
                                }
                            }
                            if (this.finalRules != null) {
                                for (i = 0; i < 2; i++) {
                                    if (this.finalRules[i] != curRule) {
                                        d = this.finalRules[i].getNextStart(lastTransitionTime, curStdOffset, curDstSavings, false);
                                        if (d != null) {
                                            tt = d.getTime();
                                            if (tt < nextTransitionTime) {
                                                nextTransitionTime = tt;
                                                nextRule = this.finalRules[i];
                                            }
                                        }
                                    }
                                }
                            }
                            if (nextRule == null) {
                                break;
                            }
                            if (this.historicTransitions == null) {
                                this.historicTransitions = new ArrayList();
                            }
                            this.historicTransitions.add(new TimeZoneTransition(nextTransitionTime, curRule, nextRule));
                            lastTransitionTime = nextTransitionTime;
                            curRule = nextRule;
                        }
                    }
                    if (this.finalRules != null) {
                        if (this.historicTransitions == null) {
                            this.historicTransitions = new ArrayList();
                        }
                        Date d0 = this.finalRules[0].getNextStart(lastTransitionTime, curRule.getRawOffset(), curRule.getDSTSavings(), false);
                        Date d1 = this.finalRules[1].getNextStart(lastTransitionTime, curRule.getRawOffset(), curRule.getDSTSavings(), false);
                        if (d1.after(d0)) {
                            this.historicTransitions.add(new TimeZoneTransition(d0.getTime(), curRule, this.finalRules[0]));
                            this.historicTransitions.add(new TimeZoneTransition(this.finalRules[1].getNextStart(d0.getTime(), this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), false).getTime(), this.finalRules[0], this.finalRules[1]));
                        } else {
                            this.historicTransitions.add(new TimeZoneTransition(d1.getTime(), curRule, this.finalRules[1]));
                            this.historicTransitions.add(new TimeZoneTransition(this.finalRules[0].getNextStart(d1.getTime(), this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), false).getTime(), this.finalRules[1], this.finalRules[0]));
                        }
                    }
                }
                this.upToDate = true;
                return;
            }
            throw new IllegalStateException("Incomplete final rules");
        }
    }

    private void getOffset(long time, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt, int[] offsets) {
        complete();
        TimeZoneRule rule = null;
        if (this.historicTransitions == null) {
            rule = this.initialRule;
        } else if (time < getTransitionTime((TimeZoneTransition) this.historicTransitions.get(0), local, NonExistingTimeOpt, DuplicatedTimeOpt)) {
            rule = this.initialRule;
        } else {
            int idx = this.historicTransitions.size() - 1;
            if (time > getTransitionTime((TimeZoneTransition) this.historicTransitions.get(idx), local, NonExistingTimeOpt, DuplicatedTimeOpt)) {
                if (this.finalRules != null) {
                    rule = findRuleInFinal(time, local, NonExistingTimeOpt, DuplicatedTimeOpt);
                }
                if (rule == null) {
                    rule = ((TimeZoneTransition) this.historicTransitions.get(idx)).getTo();
                }
            } else {
                while (idx >= 0 && time < getTransitionTime((TimeZoneTransition) this.historicTransitions.get(idx), local, NonExistingTimeOpt, DuplicatedTimeOpt)) {
                    idx--;
                }
                rule = ((TimeZoneTransition) this.historicTransitions.get(idx)).getTo();
            }
        }
        offsets[0] = rule.getRawOffset();
        offsets[1] = rule.getDSTSavings();
    }

    private TimeZoneRule findRuleInFinal(long time, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        if (this.finalRules == null || this.finalRules.length != 2 || this.finalRules[0] == null || this.finalRules[1] == null) {
            return null;
        }
        long base = time;
        if (local) {
            base = time - ((long) getLocalDelta(this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), NonExistingTimeOpt, DuplicatedTimeOpt));
        }
        Date start0 = this.finalRules[0].getPreviousStart(base, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), true);
        base = time;
        if (local) {
            base = time - ((long) getLocalDelta(this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), NonExistingTimeOpt, DuplicatedTimeOpt));
        }
        Date start1 = this.finalRules[1].getPreviousStart(base, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), true);
        if (start0 != null && start1 != null) {
            return start0.after(start1) ? this.finalRules[0] : this.finalRules[1];
        } else if (start0 != null) {
            return this.finalRules[0];
        } else {
            if (start1 != null) {
                return this.finalRules[1];
            }
            return null;
        }
    }

    private static long getTransitionTime(TimeZoneTransition tzt, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        long time = tzt.getTime();
        if (local) {
            return time + ((long) getLocalDelta(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings(), tzt.getTo().getRawOffset(), tzt.getTo().getDSTSavings(), NonExistingTimeOpt, DuplicatedTimeOpt));
        }
        return time;
    }

    private static int getLocalDelta(int rawBefore, int dstBefore, int rawAfter, int dstAfter, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        int offsetBefore = rawBefore + dstBefore;
        int offsetAfter = rawAfter + dstAfter;
        boolean dstToStd = dstBefore != 0 && dstAfter == 0;
        boolean stdToDst = dstBefore == 0 && dstAfter != 0;
        if (offsetAfter - offsetBefore >= 0) {
            if (((NonExistingTimeOpt & 3) == 1 && dstToStd) || ((NonExistingTimeOpt & 3) == 3 && stdToDst)) {
                return offsetBefore;
            }
            if (((NonExistingTimeOpt & 3) == 1 && stdToDst) || ((NonExistingTimeOpt & 3) == 3 && dstToStd)) {
                return offsetAfter;
            }
            if ((NonExistingTimeOpt & 12) == 12) {
                return offsetBefore;
            }
            return offsetAfter;
        } else if (((DuplicatedTimeOpt & 3) == 1 && dstToStd) || ((DuplicatedTimeOpt & 3) == 3 && stdToDst)) {
            return offsetAfter;
        } else {
            if (((DuplicatedTimeOpt & 3) == 1 && stdToDst) || ((DuplicatedTimeOpt & 3) == 3 && dstToStd)) {
                return offsetBefore;
            }
            if ((DuplicatedTimeOpt & 12) == 4) {
                return offsetBefore;
            }
            return offsetAfter;
        }
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public TimeZone freeze() {
        complete();
        this.isFrozen = true;
        return this;
    }

    public TimeZone cloneAsThawed() {
        RuleBasedTimeZone tz = (RuleBasedTimeZone) super.cloneAsThawed();
        if (this.historicRules != null) {
            tz.historicRules = new ArrayList(this.historicRules);
        }
        if (this.finalRules != null) {
            tz.finalRules = (AnnualTimeZoneRule[]) this.finalRules.clone();
        }
        tz.isFrozen = false;
        return tz;
    }
}
