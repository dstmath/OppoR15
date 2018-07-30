package android.icu.util;

import android.icu.impl.Grego;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

public abstract class BasicTimeZone extends TimeZone {
    @Deprecated
    protected static final int FORMER_LATTER_MASK = 12;
    @Deprecated
    public static final int LOCAL_DST = 3;
    @Deprecated
    public static final int LOCAL_FORMER = 4;
    @Deprecated
    public static final int LOCAL_LATTER = 12;
    @Deprecated
    public static final int LOCAL_STD = 1;
    private static final long MILLIS_PER_YEAR = 31536000000L;
    @Deprecated
    protected static final int STD_DST_MASK = 3;
    private static final long serialVersionUID = -3204278532246180932L;

    public abstract TimeZoneTransition getNextTransition(long j, boolean z);

    public abstract TimeZoneTransition getPreviousTransition(long j, boolean z);

    public abstract TimeZoneRule[] getTimeZoneRules();

    public boolean hasEquivalentTransitions(TimeZone tz, long start, long end) {
        return hasEquivalentTransitions(tz, start, end, false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hasEquivalentTransitions(TimeZone tz, long start, long end, boolean ignoreDstAmount) {
        if (this == tz) {
            return true;
        }
        if (!(tz instanceof BasicTimeZone)) {
            return false;
        }
        int[] offsets1 = new int[2];
        int[] offsets2 = new int[2];
        getOffset(start, false, offsets1);
        tz.getOffset(start, false, offsets2);
        if (ignoreDstAmount) {
            if (offsets1[0] + offsets1[1] != offsets2[0] + offsets2[1] || ((offsets1[1] != 0 && offsets2[1] == 0) || (offsets1[1] == 0 && offsets2[1] != 0))) {
                return false;
            }
        } else if (!(offsets1[0] == offsets2[0] && offsets1[1] == offsets2[1])) {
            return false;
        }
        long time = start;
        while (true) {
            TimeZoneTransition tr1 = getNextTransition(time, false);
            TimeZoneTransition tr2 = ((BasicTimeZone) tz).getNextTransition(time, false);
            if (ignoreDstAmount) {
                while (tr1 != null && tr1.getTime() <= end && tr1.getFrom().getRawOffset() + tr1.getFrom().getDSTSavings() == tr1.getTo().getRawOffset() + tr1.getTo().getDSTSavings() && tr1.getFrom().getDSTSavings() != 0 && tr1.getTo().getDSTSavings() != 0) {
                    tr1 = getNextTransition(tr1.getTime(), false);
                }
                while (tr2 != null && tr2.getTime() <= end && tr2.getFrom().getRawOffset() + tr2.getFrom().getDSTSavings() == tr2.getTo().getRawOffset() + tr2.getTo().getDSTSavings() && tr2.getFrom().getDSTSavings() != 0 && tr2.getTo().getDSTSavings() != 0) {
                    tr2 = ((BasicTimeZone) tz).getNextTransition(tr2.getTime(), false);
                }
            }
            boolean inRange1 = false;
            boolean inRange2 = false;
            if (tr1 != null && tr1.getTime() <= end) {
                inRange1 = true;
            }
            if (tr2 != null && tr2.getTime() <= end) {
                inRange2 = true;
            }
            if (!inRange1 && (inRange2 ^ 1) != 0) {
                return true;
            }
            if (inRange1 && (inRange2 ^ 1) == 0) {
                if (tr1.getTime() != tr2.getTime()) {
                    return false;
                }
                if (ignoreDstAmount) {
                    if (tr1.getTo().getRawOffset() + tr1.getTo().getDSTSavings() != tr2.getTo().getRawOffset() + tr2.getTo().getDSTSavings() || ((tr1.getTo().getDSTSavings() != 0 && tr2.getTo().getDSTSavings() == 0) || (tr1.getTo().getDSTSavings() == 0 && tr2.getTo().getDSTSavings() != 0))) {
                    }
                } else if (tr1.getTo().getRawOffset() == tr2.getTo().getRawOffset() && tr1.getTo().getDSTSavings() == tr2.getTo().getDSTSavings()) {
                }
                time = tr1.getTime();
            }
        }
        return false;
    }

    public TimeZoneRule[] getTimeZoneRules(long start) {
        TimeZoneRule[] all = getTimeZoneRules();
        TimeZoneTransition tzt = getPreviousTransition(start, true);
        if (tzt == null) {
            return all;
        }
        BitSet bitSet = new BitSet(all.length);
        List<TimeZoneRule> filteredRules = new LinkedList();
        TimeZoneRule initialTimeZoneRule = new InitialTimeZoneRule(tzt.getTo().getName(), tzt.getTo().getRawOffset(), tzt.getTo().getDSTSavings());
        filteredRules.add(initialTimeZoneRule);
        bitSet.set(0);
        for (int i = 1; i < all.length; i++) {
            if (all[i].getNextStart(start, initialTimeZoneRule.getRawOffset(), initialTimeZoneRule.getDSTSavings(), false) == null) {
                bitSet.set(i);
            }
        }
        long time = start;
        boolean bFinalStd = false;
        boolean bFinalDst = false;
        while (true) {
            if (bFinalStd && (bFinalDst ^ 1) == 0) {
                break;
            }
            tzt = getNextTransition(time, false);
            if (tzt == null) {
                break;
            }
            time = tzt.getTime();
            TimeZoneRule toRule = tzt.getTo();
            int ruleIdx = 1;
            while (ruleIdx < all.length && !all[ruleIdx].equals(toRule)) {
                ruleIdx++;
            }
            if (ruleIdx >= all.length) {
                throw new IllegalStateException("The rule was not found");
            } else if (!bitSet.get(ruleIdx)) {
                if (toRule instanceof TimeArrayTimeZoneRule) {
                    TimeArrayTimeZoneRule tar = (TimeArrayTimeZoneRule) toRule;
                    long t = start;
                    while (true) {
                        tzt = getNextTransition(t, false);
                        if (!(tzt == null || tzt.getTo().equals(tar))) {
                            t = tzt.getTime();
                        }
                    }
                    if (tzt != null) {
                        if (tar.getFirstStart(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings()).getTime() > start) {
                            filteredRules.add(tar);
                        } else {
                            long[] times = tar.getStartTimes();
                            int timeType = tar.getTimeType();
                            int idx = 0;
                            while (idx < times.length) {
                                t = times[idx];
                                if (timeType == 1) {
                                    t -= (long) tzt.getFrom().getRawOffset();
                                }
                                if (timeType == 0) {
                                    t -= (long) tzt.getFrom().getDSTSavings();
                                }
                                if (t > start) {
                                    break;
                                }
                                idx++;
                            }
                            int asize = times.length - idx;
                            if (asize > 0) {
                                long[] newtimes = new long[asize];
                                System.arraycopy(times, idx, newtimes, 0, asize);
                                filteredRules.add(new TimeArrayTimeZoneRule(tar.getName(), tar.getRawOffset(), tar.getDSTSavings(), newtimes, tar.getTimeType()));
                            }
                        }
                    }
                } else if (toRule instanceof AnnualTimeZoneRule) {
                    AnnualTimeZoneRule ar = (AnnualTimeZoneRule) toRule;
                    if (ar.getFirstStart(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings()).getTime() == tzt.getTime()) {
                        filteredRules.add(ar);
                    } else {
                        int[] dfields = new int[6];
                        Grego.timeToFields(tzt.getTime(), dfields);
                        filteredRules.add(new AnnualTimeZoneRule(ar.getName(), ar.getRawOffset(), ar.getDSTSavings(), ar.getRule(), dfields[0], ar.getEndYear()));
                    }
                    if (ar.getEndYear() == Integer.MAX_VALUE) {
                        if (ar.getDSTSavings() == 0) {
                            bFinalStd = true;
                        } else {
                            bFinalDst = true;
                        }
                    }
                }
                bitSet.set(ruleIdx);
            }
        }
        return (TimeZoneRule[]) filteredRules.toArray(new TimeZoneRule[filteredRules.size()]);
    }

    public android.icu.util.TimeZoneRule[] getSimpleTimeZoneRulesNear(long r28) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r20_1 'initialRule' android.icu.util.TimeZoneRule) in PHI: PHI: (r20_2 'initialRule' android.icu.util.TimeZoneRule) = (r20_1 'initialRule' android.icu.util.TimeZoneRule), (r20_3 'initialRule' android.icu.util.TimeZoneRule), (r20_4 'initialRule' android.icu.util.TimeZoneRule) binds: {(r20_1 'initialRule' android.icu.util.TimeZoneRule)=B:41:0x0221, (r20_3 'initialRule' android.icu.util.TimeZoneRule)=B:61:0x02a0, (r20_4 'initialRule' android.icu.util.TimeZoneRule)=B:62:0x02c1}
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
        r27 = this;
        r14 = 0;
        r20 = 0;
        r5 = 0;
        r0 = r27;
        r1 = r28;
        r26 = r0.getNextTransition(r1, r5);
        if (r26 == 0) goto L_0x0295;
    L_0x000e:
        r5 = r26.getFrom();
        r18 = r5.getName();
        r5 = r26.getFrom();
        r19 = r5.getRawOffset();
        r5 = r26.getFrom();
        r17 = r5.getDSTSavings();
        r22 = r26.getTime();
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        if (r5 != 0) goto L_0x023b;
    L_0x0034:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x023b;
    L_0x003e:
        r8 = 31536000000; // 0x757b12c00 float:3.89605073E14 double:1.55808542072E-313;
        r8 = r8 + r28;
        r5 = (r8 > r22 ? 1 : (r8 == r22 ? 0 : -1));
        if (r5 <= 0) goto L_0x0221;
    L_0x0049:
        r5 = 2;
        r14 = new android.icu.util.AnnualTimeZoneRule[r5];
        r5 = r26.getFrom();
        r5 = r5.getRawOffset();
        r8 = (long) r5;
        r8 = r8 + r22;
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        r10 = (long) r5;
        r8 = r8 + r10;
        r5 = 0;
        r16 = android.icu.impl.Grego.timeToFields(r8, r5);
        r5 = 0;
        r5 = r16[r5];
        r8 = 1;
        r8 = r16[r8];
        r9 = 2;
        r9 = r16[r9];
        r6 = android.icu.impl.Grego.getDayOfWeekInMonth(r5, r8, r9);
        r4 = new android.icu.util.DateTimeRule;
        r5 = 1;
        r5 = r16[r5];
        r8 = 3;
        r7 = r16[r8];
        r8 = 5;
        r8 = r16[r8];
        r9 = 0;
        r4.<init>(r5, r6, r7, r8, r9);
        r25 = 0;
        r7 = new android.icu.util.AnnualTimeZoneRule;
        r5 = r26.getTo();
        r8 = r5.getName();
        r5 = r26.getTo();
        r10 = r5.getDSTSavings();
        r5 = 0;
        r12 = r16[r5];
        r13 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r9 = r19;
        r11 = r4;
        r7.<init>(r8, r9, r10, r11, r12, r13);
        r5 = 0;
        r14[r5] = r7;
        r5 = r26.getTo();
        r5 = r5.getRawOffset();
        r0 = r19;
        if (r5 != r0) goto L_0x02f9;
    L_0x00b1:
        r5 = 0;
        r0 = r27;
        r1 = r22;
        r26 = r0.getNextTransition(r1, r5);
        if (r26 == 0) goto L_0x02f9;
    L_0x00bc:
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        if (r5 != 0) goto L_0x0251;
    L_0x00c6:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x0251;
    L_0x00d0:
        r8 = 31536000000; // 0x757b12c00 float:3.89605073E14 double:1.55808542072E-313;
        r8 = r8 + r22;
        r10 = r26.getTime();
        r5 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1));
        if (r5 <= 0) goto L_0x0265;
    L_0x00df:
        r8 = r26.getTime();
        r5 = r26.getFrom();
        r5 = r5.getRawOffset();
        r10 = (long) r5;
        r8 = r8 + r10;
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        r10 = (long) r5;
        r8 = r8 + r10;
        r0 = r16;
        r16 = android.icu.impl.Grego.timeToFields(r8, r0);
        r5 = 0;
        r5 = r16[r5];
        r8 = 1;
        r8 = r16[r8];
        r9 = 2;
        r9 = r16[r9];
        r6 = android.icu.impl.Grego.getDayOfWeekInMonth(r5, r8, r9);
        r4 = new android.icu.util.DateTimeRule;
        r5 = 1;
        r5 = r16[r5];
        r8 = 3;
        r7 = r16[r8];
        r8 = 5;
        r8 = r16[r8];
        r9 = 0;
        r4.<init>(r5, r6, r7, r8, r9);
        r7 = new android.icu.util.AnnualTimeZoneRule;
        r5 = r26.getTo();
        r8 = r5.getName();
        r5 = r26.getTo();
        r9 = r5.getRawOffset();
        r5 = r26.getTo();
        r10 = r5.getDSTSavings();
        r5 = 0;
        r5 = r16[r5];
        r12 = r5 + -1;
        r13 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r11 = r4;
        r7.<init>(r8, r9, r10, r11, r12, r13);
        r5 = r26.getFrom();
        r10 = r5.getRawOffset();
        r5 = r26.getFrom();
        r11 = r5.getDSTSavings();
        r12 = 1;
        r8 = r28;
        r15 = r7.getPreviousStart(r8, r10, r11, r12);
        if (r15 == 0) goto L_0x017b;
    L_0x0158:
        r8 = r15.getTime();
        r5 = (r8 > r28 ? 1 : (r8 == r28 ? 0 : -1));
        if (r5 > 0) goto L_0x017b;
    L_0x0160:
        r5 = r26.getTo();
        r5 = r5.getRawOffset();
        r0 = r19;
        if (r0 != r5) goto L_0x017b;
    L_0x016c:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        r0 = r17;
        if (r0 != r5) goto L_0x017b;
    L_0x0178:
        r5 = 1;
        r14[r5] = r7;
    L_0x017b:
        r5 = 1;
        r5 = r14[r5];
        if (r5 != 0) goto L_0x021b;
    L_0x0180:
        r5 = 1;
        r0 = r27;
        r1 = r28;
        r26 = r0.getPreviousTransition(r1, r5);
        if (r26 == 0) goto L_0x021b;
    L_0x018b:
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        if (r5 != 0) goto L_0x0269;
    L_0x0195:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x0269;
    L_0x019f:
        r8 = r26.getTime();
        r5 = r26.getFrom();
        r5 = r5.getRawOffset();
        r10 = (long) r5;
        r8 = r8 + r10;
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        r10 = (long) r5;
        r8 = r8 + r10;
        r0 = r16;
        r16 = android.icu.impl.Grego.timeToFields(r8, r0);
        r5 = 0;
        r5 = r16[r5];
        r8 = 1;
        r8 = r16[r8];
        r9 = 2;
        r9 = r16[r9];
        r6 = android.icu.impl.Grego.getDayOfWeekInMonth(r5, r8, r9);
        r4 = new android.icu.util.DateTimeRule;
        r5 = 1;
        r9 = r16[r5];
        r5 = 3;
        r11 = r16[r5];
        r5 = 5;
        r12 = r16[r5];
        r13 = 0;
        r8 = r4;
        r10 = r6;
        r8.<init>(r9, r10, r11, r12, r13);
        r7 = new android.icu.util.AnnualTimeZoneRule;
        r5 = r26.getTo();
        r8 = r5.getName();
        r5 = 0;
        r5 = r14[r5];
        r5 = r5.getStartYear();
        r12 = r5 + -1;
        r13 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r9 = r19;
        r10 = r17;
        r11 = r4;
        r7.<init>(r8, r9, r10, r11, r12, r13);
        r5 = r26.getFrom();
        r10 = r5.getRawOffset();
        r5 = r26.getFrom();
        r11 = r5.getDSTSavings();
        r12 = 0;
        r8 = r28;
        r15 = r7.getNextStart(r8, r10, r11, r12);
        r8 = r15.getTime();
        r5 = (r8 > r22 ? 1 : (r8 == r22 ? 0 : -1));
        if (r5 <= 0) goto L_0x021b;
    L_0x0218:
        r5 = 1;
        r14[r5] = r7;
    L_0x021b:
        r5 = 1;
        r5 = r14[r5];
        if (r5 != 0) goto L_0x027f;
    L_0x0220:
        r14 = 0;
    L_0x0221:
        r20 = new android.icu.util.InitialTimeZoneRule;
        r0 = r20;
        r1 = r18;
        r2 = r19;
        r3 = r17;
        r0.<init>(r1, r2, r3);
    L_0x022e:
        r24 = 0;
        if (r14 != 0) goto L_0x02e3;
    L_0x0232:
        r5 = 1;
        r0 = new android.icu.util.TimeZoneRule[r5];
        r24 = r0;
        r5 = 0;
        r24[r5] = r20;
    L_0x023a:
        return r24;
    L_0x023b:
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x0221;
    L_0x0245:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        if (r5 != 0) goto L_0x0221;
    L_0x024f:
        goto L_0x003e;
    L_0x0251:
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x0265;
    L_0x025b:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x00d0;
    L_0x0265:
        r7 = r25;
        goto L_0x017b;
    L_0x0269:
        r5 = r26.getFrom();
        r5 = r5.getDSTSavings();
        if (r5 == 0) goto L_0x021b;
    L_0x0273:
        r5 = r26.getTo();
        r5 = r5.getDSTSavings();
        if (r5 != 0) goto L_0x021b;
    L_0x027d:
        goto L_0x019f;
    L_0x027f:
        r5 = 0;
        r5 = r14[r5];
        r18 = r5.getName();
        r5 = 0;
        r5 = r14[r5];
        r19 = r5.getRawOffset();
        r5 = 0;
        r5 = r14[r5];
        r17 = r5.getDSTSavings();
        goto L_0x0221;
    L_0x0295:
        r5 = 1;
        r0 = r27;
        r1 = r28;
        r26 = r0.getPreviousTransition(r1, r5);
        if (r26 == 0) goto L_0x02c1;
    L_0x02a0:
        r20 = new android.icu.util.InitialTimeZoneRule;
        r5 = r26.getTo();
        r5 = r5.getName();
        r8 = r26.getTo();
        r8 = r8.getRawOffset();
        r9 = r26.getTo();
        r9 = r9.getDSTSavings();
        r0 = r20;
        r0.<init>(r5, r8, r9);
        goto L_0x022e;
    L_0x02c1:
        r5 = 2;
        r0 = new int[r5];
        r21 = r0;
        r5 = 0;
        r0 = r27;
        r1 = r28;
        r3 = r21;
        r0.getOffset(r1, r5, r3);
        r20 = new android.icu.util.InitialTimeZoneRule;
        r5 = r27.getID();
        r8 = 0;
        r8 = r21[r8];
        r9 = 1;
        r9 = r21[r9];
        r0 = r20;
        r0.<init>(r5, r8, r9);
        goto L_0x022e;
    L_0x02e3:
        r5 = 3;
        r0 = new android.icu.util.TimeZoneRule[r5];
        r24 = r0;
        r5 = 0;
        r24[r5] = r20;
        r5 = 0;
        r5 = r14[r5];
        r8 = 1;
        r24[r8] = r5;
        r5 = 1;
        r5 = r14[r5];
        r8 = 2;
        r24[r8] = r5;
        goto L_0x023a;
    L_0x02f9:
        r7 = r25;
        goto L_0x017b;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.util.BasicTimeZone.getSimpleTimeZoneRulesNear(long):android.icu.util.TimeZoneRule[]");
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        throw new IllegalStateException("Not implemented");
    }

    protected BasicTimeZone() {
    }

    @Deprecated
    protected BasicTimeZone(String ID) {
        super(ID);
    }
}
