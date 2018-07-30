package com.android.server;

import android.content.Intent;
import android.content.IntentFilter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.FastImmutableArraySet;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class IntentResolver<F extends IntentFilter, R> {
    private static final boolean DEBUG = false;
    private static final String TAG = "IntentResolver";
    private static final boolean localLOGV = false;
    private static final boolean localVerificationLOGV = false;
    private static final Comparator mResolvePrioritySorter = new Comparator() {
        public int compare(Object o1, Object o2) {
            int q1 = ((IntentFilter) o1).getPriority();
            int q2 = ((IntentFilter) o2).getPriority();
            if (q1 > q2) {
                return -1;
            }
            return q1 < q2 ? 1 : 0;
        }
    };
    private final ArrayMap<String, F[]> mActionToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mBaseTypeToFilter = new ArrayMap();
    private final ArraySet<F> mFilters = new ArraySet();
    private final ArrayMap<String, F[]> mSchemeToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mTypeToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mTypedActionToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mWildTypeToFilter = new ArrayMap();

    private class IteratorWrapper implements Iterator<F> {
        private F mCur;
        private final Iterator<F> mI;

        IteratorWrapper(Iterator<F> it) {
            this.mI = it;
        }

        public boolean hasNext() {
            return this.mI.hasNext();
        }

        public F next() {
            IntentFilter intentFilter = (IntentFilter) this.mI.next();
            this.mCur = intentFilter;
            return intentFilter;
        }

        public void remove() {
            if (this.mCur != null) {
                IntentResolver.this.removeFilterInternal(this.mCur);
            }
            this.mI.remove();
        }
    }

    protected abstract boolean isPackageForFilter(String str, F f);

    protected abstract F[] newArray(int i);

    public void addFilter(F f) {
        this.mFilters.add(f);
        int numS = register_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = register_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            register_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            register_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    private boolean filterEquals(IntentFilter f1, IntentFilter f2) {
        int s1 = f1.countActions();
        if (s1 != f2.countActions()) {
            return false;
        }
        int i;
        for (i = 0; i < s1; i++) {
            if (!f2.hasAction(f1.getAction(i))) {
                return false;
            }
        }
        s1 = f1.countCategories();
        if (s1 != f2.countCategories()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasCategory(f1.getCategory(i))) {
                return false;
            }
        }
        s1 = f1.countDataTypes();
        if (s1 != f2.countDataTypes()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasExactDataType(f1.getDataType(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemes();
        if (s1 != f2.countDataSchemes()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataScheme(f1.getDataScheme(i))) {
                return false;
            }
        }
        s1 = f1.countDataAuthorities();
        if (s1 != f2.countDataAuthorities()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataAuthority(f1.getDataAuthority(i))) {
                return false;
            }
        }
        s1 = f1.countDataPaths();
        if (s1 != f2.countDataPaths()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataPath(f1.getDataPath(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemeSpecificParts();
        if (s1 != f2.countDataSchemeSpecificParts()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataSchemeSpecificPart(f1.getDataSchemeSpecificPart(i))) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<F> collectFilters(F[] array, IntentFilter matching) {
        ArrayList<F> arrayList = null;
        if (array != null) {
            for (F cur : array) {
                if (cur == null) {
                    break;
                }
                if (filterEquals(cur, matching)) {
                    ArrayList arrayList2;
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                    }
                    arrayList2.add(cur);
                }
            }
        }
        return arrayList;
    }

    public ArrayList<F> findFilters(IntentFilter matching) {
        if (matching.countDataSchemes() == 1) {
            return collectFilters((IntentFilter[]) this.mSchemeToFilter.get(matching.getDataScheme(0)), matching);
        }
        if (matching.countDataTypes() != 0 && matching.countActions() == 1) {
            return collectFilters((IntentFilter[]) this.mTypedActionToFilter.get(matching.getAction(0)), matching);
        }
        if (matching.countDataTypes() == 0 && matching.countDataSchemes() == 0 && matching.countActions() == 1) {
            return collectFilters((IntentFilter[]) this.mActionToFilter.get(matching.getAction(0)), matching);
        }
        ArrayList<F> res = null;
        for (IntentFilter cur : this.mFilters) {
            if (filterEquals(cur, matching)) {
                if (res == null) {
                    res = new ArrayList();
                }
                res.add(cur);
            }
        }
        return res;
    }

    public void removeFilter(F f) {
        removeFilterInternal(f);
        this.mFilters.remove(f);
    }

    void removeFilterInternal(F f) {
        int numS = unregister_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = unregister_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            unregister_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            unregister_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    boolean dumpMap(java.io.PrintWriter r20, java.lang.String r21, java.lang.String r22, java.lang.String r23, android.util.ArrayMap<java.lang.String, F[]> r24, java.lang.String r25, boolean r26, boolean r27) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r16_4 'printer' android.util.Printer) in PHI: PHI: (r16_5 'printer' android.util.Printer) = (r16_2 'printer' android.util.Printer), (r16_4 'printer' android.util.Printer) binds: {(r16_2 'printer' android.util.Printer)=B:41:0x0142, (r16_4 'printer' android.util.Printer)=B:42:0x0144}
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
        r19 = this;
        r17 = new java.lang.StringBuilder;
        r17.<init>();
        r0 = r17;
        r1 = r23;
        r17 = r0.append(r1);
        r18 = "  ";
        r17 = r17.append(r18);
        r6 = r17.toString();
        r17 = new java.lang.StringBuilder;
        r17.<init>();
        r0 = r17;
        r1 = r23;
        r17 = r0.append(r1);
        r18 = "    ";
        r17 = r17.append(r18);
        r9 = r17.toString();
        r8 = new android.util.ArrayMap;
        r8.<init>();
        r15 = 0;
        r16 = 0;
        r13 = 0;
    L_0x0039:
        r17 = r24.size();
        r0 = r17;
        if (r13 >= r0) goto L_0x016f;
    L_0x0041:
        r0 = r24;
        r5 = r0.valueAt(r13);
        r5 = (android.content.IntentFilter[]) r5;
        r4 = r5.length;
        r14 = 0;
        if (r27 == 0) goto L_0x00f1;
    L_0x004d:
        r17 = r26 ^ 1;
        if (r17 == 0) goto L_0x00f1;
    L_0x0051:
        r8.clear();
        r10 = 0;
    L_0x0055:
        if (r10 >= r4) goto L_0x009a;
    L_0x0057:
        r7 = r5[r10];
        if (r7 == 0) goto L_0x009a;
    L_0x005b:
        if (r25 == 0) goto L_0x006c;
    L_0x005d:
        r0 = r19;
        r1 = r25;
        r17 = r0.isPackageForFilter(r1, r7);
        r17 = r17 ^ 1;
        if (r17 == 0) goto L_0x006c;
    L_0x0069:
        r10 = r10 + 1;
        goto L_0x0055;
    L_0x006c:
        r0 = r19;
        r12 = r0.filterToLabel(r7);
        r11 = r8.indexOfKey(r12);
        if (r11 >= 0) goto L_0x0085;
    L_0x0078:
        r17 = new android.util.MutableInt;
        r18 = 1;
        r17.<init>(r18);
        r0 = r17;
        r8.put(r12, r0);
        goto L_0x0069;
    L_0x0085:
        r17 = r8.valueAt(r11);
        r17 = (android.util.MutableInt) r17;
        r0 = r17;
        r0 = r0.value;
        r18 = r0;
        r18 = r18 + 1;
        r0 = r18;
        r1 = r17;
        r1.value = r0;
        goto L_0x0069;
    L_0x009a:
        r10 = 0;
    L_0x009b:
        r17 = r8.size();
        r0 = r17;
        if (r10 >= r0) goto L_0x016b;
    L_0x00a3:
        if (r22 == 0) goto L_0x00b1;
    L_0x00a5:
        r20.print(r21);
        r0 = r20;
        r1 = r22;
        r0.println(r1);
        r22 = 0;
    L_0x00b1:
        if (r14 != 0) goto L_0x00d2;
    L_0x00b3:
        r0 = r20;
        r0.print(r6);
        r0 = r24;
        r17 = r0.keyAt(r13);
        r17 = (java.lang.String) r17;
        r0 = r20;
        r1 = r17;
        r0.print(r1);
        r17 = ":";
        r0 = r20;
        r1 = r17;
        r0.println(r1);
        r14 = 1;
    L_0x00d2:
        r15 = 1;
        r18 = r8.keyAt(r10);
        r17 = r8.valueAt(r10);
        r17 = (android.util.MutableInt) r17;
        r0 = r17;
        r0 = r0.value;
        r17 = r0;
        r0 = r19;
        r1 = r20;
        r2 = r18;
        r3 = r17;
        r0.dumpFilterLabel(r1, r9, r2, r3);
        r10 = r10 + 1;
        goto L_0x009b;
    L_0x00f1:
        r10 = 0;
    L_0x00f2:
        if (r10 >= r4) goto L_0x016b;
    L_0x00f4:
        r7 = r5[r10];
        if (r7 == 0) goto L_0x016b;
    L_0x00f8:
        if (r25 == 0) goto L_0x0109;
    L_0x00fa:
        r0 = r19;
        r1 = r25;
        r17 = r0.isPackageForFilter(r1, r7);
        r17 = r17 ^ 1;
        if (r17 == 0) goto L_0x0109;
    L_0x0106:
        r10 = r10 + 1;
        goto L_0x00f2;
    L_0x0109:
        if (r22 == 0) goto L_0x0117;
    L_0x010b:
        r20.print(r21);
        r0 = r20;
        r1 = r22;
        r0.println(r1);
        r22 = 0;
    L_0x0117:
        if (r14 != 0) goto L_0x0138;
    L_0x0119:
        r0 = r20;
        r0.print(r6);
        r0 = r24;
        r17 = r0.keyAt(r13);
        r17 = (java.lang.String) r17;
        r0 = r20;
        r1 = r17;
        r0.print(r1);
        r17 = ":";
        r0 = r20;
        r1 = r17;
        r0.println(r1);
        r14 = 1;
    L_0x0138:
        r15 = 1;
        r0 = r19;
        r1 = r20;
        r0.dumpFilter(r1, r9, r7);
        if (r26 == 0) goto L_0x0106;
    L_0x0142:
        if (r16 != 0) goto L_0x014d;
    L_0x0144:
        r16 = new android.util.PrintWriterPrinter;
        r0 = r16;
        r1 = r20;
        r0.<init>(r1);
    L_0x014d:
        r17 = new java.lang.StringBuilder;
        r17.<init>();
        r0 = r17;
        r17 = r0.append(r9);
        r18 = "  ";
        r17 = r17.append(r18);
        r17 = r17.toString();
        r0 = r16;
        r1 = r17;
        r7.dump(r0, r1);
        goto L_0x0106;
    L_0x016b:
        r13 = r13 + 1;
        goto L_0x0039;
    L_0x016f:
        return r15;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.IntentResolver.dumpMap(java.io.PrintWriter, java.lang.String, java.lang.String, java.lang.String, android.util.ArrayMap, java.lang.String, boolean, boolean):boolean");
    }

    public boolean dump(PrintWriter out, String title, String prefix, String packageName, boolean printFilter, boolean collapseDuplicates) {
        String innerPrefix = prefix + "  ";
        String sepPrefix = "\n" + prefix;
        String curPrefix = title + "\n" + prefix;
        if (dumpMap(out, curPrefix, "Full MIME Types:", innerPrefix, this.mTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Base MIME Types:", innerPrefix, this.mBaseTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Wild MIME Types:", innerPrefix, this.mWildTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Schemes:", innerPrefix, this.mSchemeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Non-Data Actions:", innerPrefix, this.mActionToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "MIME Typed Actions:", innerPrefix, this.mTypedActionToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        return curPrefix == sepPrefix;
    }

    public Iterator<F> filterIterator() {
        return new IteratorWrapper(this.mFilters.iterator());
    }

    public Set<F> filterSet() {
        return Collections.unmodifiableSet(this.mFilters);
    }

    public List<R> queryIntentFromList(Intent intent, String resolvedType, boolean defaultOnly, ArrayList<F[]> listCut, int userId) {
        ArrayList<R> resultList = new ArrayList();
        boolean debug = (intent.getFlags() & 8) != 0;
        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        String scheme = intent.getScheme();
        int N = listCut.size();
        for (int i = 0; i < N; i++) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, (IntentFilter[]) listCut.get(i), resultList, userId);
        }
        filterResults(resultList);
        sortResults(resultList);
        return resultList;
    }

    public List<R> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
        String scheme = intent.getScheme();
        ArrayList<R> finalList = new ArrayList();
        boolean debug = (intent.getFlags() & 8) != 0;
        if (debug) {
            Slog.v(TAG, "Resolving type=" + resolvedType + " scheme=" + scheme + " defaultOnly=" + defaultOnly + " userId=" + userId + " of " + intent);
        }
        F[] firstTypeCut = null;
        F[] secondTypeCut = null;
        F[] thirdTypeCut = null;
        F[] schemeCut = null;
        if (resolvedType != null) {
            int slashpos = resolvedType.indexOf(47);
            if (slashpos > 0) {
                String baseType = resolvedType.substring(0, slashpos);
                IntentFilter[] firstTypeCut2;
                if (!baseType.equals("*")) {
                    if (resolvedType.length() == slashpos + 2) {
                        if (resolvedType.charAt(slashpos + 1) == '*') {
                            firstTypeCut2 = (IntentFilter[]) this.mBaseTypeToFilter.get(baseType);
                            if (debug) {
                                Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut2));
                            }
                            IntentFilter[] secondTypeCut2 = (IntentFilter[]) this.mWildTypeToFilter.get(baseType);
                            if (debug) {
                                Slog.v(TAG, "Second type cut: " + Arrays.toString(secondTypeCut2));
                            }
                            thirdTypeCut = (IntentFilter[]) this.mWildTypeToFilter.get("*");
                            if (debug) {
                                Slog.v(TAG, "Third type cut: " + Arrays.toString(thirdTypeCut));
                            }
                        }
                    }
                    firstTypeCut = (IntentFilter[]) this.mTypeToFilter.get(resolvedType);
                    if (debug) {
                        Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut));
                    }
                    secondTypeCut = (IntentFilter[]) this.mWildTypeToFilter.get(baseType);
                    if (debug) {
                        Slog.v(TAG, "Second type cut: " + Arrays.toString(secondTypeCut));
                    }
                    thirdTypeCut = (IntentFilter[]) this.mWildTypeToFilter.get("*");
                    if (debug) {
                        Slog.v(TAG, "Third type cut: " + Arrays.toString(thirdTypeCut));
                    }
                } else if (intent.getAction() != null) {
                    firstTypeCut2 = (IntentFilter[]) this.mTypedActionToFilter.get(intent.getAction());
                    if (debug) {
                        Slog.v(TAG, "Typed Action list: " + Arrays.toString(firstTypeCut2));
                    }
                }
            }
        }
        if (scheme != null) {
            schemeCut = (IntentFilter[]) this.mSchemeToFilter.get(scheme);
            if (debug) {
                Slog.v(TAG, "Scheme list: " + Arrays.toString(schemeCut));
            }
        }
        if (resolvedType == null && scheme == null && intent.getAction() != null) {
            firstTypeCut = (IntentFilter[]) this.mActionToFilter.get(intent.getAction());
            if (debug) {
                Slog.v(TAG, "Action list: " + Arrays.toString(firstTypeCut));
            }
        }
        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        if (firstTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, firstTypeCut, finalList, userId);
        }
        if (secondTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, secondTypeCut, finalList, userId);
        }
        if (thirdTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, thirdTypeCut, finalList, userId);
        }
        if (schemeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, schemeCut, finalList, userId);
        }
        filterResults(finalList);
        sortResults(finalList);
        if (debug) {
            Slog.v(TAG, "Final result list:");
            for (int i = 0; i < finalList.size(); i++) {
                Slog.v(TAG, "  " + finalList.get(i));
            }
        }
        return finalList;
    }

    protected boolean allowFilterResult(F f, List<R> list) {
        return true;
    }

    protected boolean isFilterStopped(F f, int userId) {
        return false;
    }

    protected boolean isFilterVerified(F filter) {
        return filter.isVerified();
    }

    protected R newResult(F filter, int match, int userId) {
        return filter;
    }

    protected void sortResults(List<R> results) {
        Collections.sort(results, mResolvePrioritySorter);
    }

    protected void filterResults(List<R> list) {
    }

    protected void dumpFilter(PrintWriter out, String prefix, F filter) {
        out.print(prefix);
        out.println(filter);
    }

    protected Object filterToLabel(F f) {
        return "IntentFilter";
    }

    protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
        out.print(prefix);
        out.print(label);
        out.print(": ");
        out.println(count);
    }

    private final void addFilter(ArrayMap<String, F[]> map, String name, F filter) {
        IntentFilter[] array = (IntentFilter[]) map.get(name);
        if (array == null) {
            F[] array2 = newArray(2);
            map.put(name, array2);
            array2[0] = filter;
            return;
        }
        int N = array.length;
        int i = N;
        while (i > 0 && array[i - 1] == null) {
            i--;
        }
        if (i < N) {
            array[i] = filter;
            return;
        }
        F[] newa = newArray((N * 3) / 2);
        System.arraycopy(array, 0, newa, 0, N);
        newa[N] = filter;
        map.put(name, newa);
    }

    private final int register_mime_types(F filter, String prefix) {
        Iterator<String> i = filter.typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = (String) i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }
            addFilter(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                addFilter(this.mBaseTypeToFilter, baseName, filter);
            } else {
                addFilter(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int unregister_mime_types(F filter, String prefix) {
        Iterator<String> i = filter.typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = (String) i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }
            remove_all_objects(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                remove_all_objects(this.mBaseTypeToFilter, baseName, filter);
            } else {
                remove_all_objects(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int register_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            num++;
            addFilter(dest, (String) i.next(), filter);
        }
        return num;
    }

    private final int unregister_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            num++;
            remove_all_objects(dest, (String) i.next(), filter);
        }
        return num;
    }

    private final void remove_all_objects(ArrayMap<String, F[]> map, String name, Object object) {
        IntentFilter[] array = (IntentFilter[]) map.get(name);
        if (array != null) {
            int LAST = array.length - 1;
            while (LAST >= 0 && array[LAST] == null) {
                LAST--;
            }
            for (int idx = LAST; idx >= 0; idx--) {
                if (array[idx] == object) {
                    int remain = LAST - idx;
                    if (remain > 0) {
                        System.arraycopy(array, idx + 1, array, idx, remain);
                    }
                    array[LAST] = null;
                    LAST--;
                }
            }
            if (LAST < 0) {
                map.remove(name);
            } else if (LAST < array.length / 2) {
                F[] newa = newArray(LAST + 2);
                System.arraycopy(array, 0, newa, 0, LAST + 1);
                map.put(name, newa);
            }
        }
    }

    private static FastImmutableArraySet<String> getFastIntentCategories(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories == null) {
            return null;
        }
        return new FastImmutableArraySet((String[]) categories.toArray(new String[categories.size()]));
    }

    private void buildResolveList(android.content.Intent r23, android.util.FastImmutableArraySet<java.lang.String> r24, boolean r25, boolean r26, java.lang.String r27, java.lang.String r28, F[] r29, java.util.List<R> r30, int r31) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r16_0 'logPrinter' android.util.Printer) in PHI: PHI: (r16_1 'logPrinter' android.util.Printer) = (r16_0 'logPrinter' android.util.Printer), (r16_2 'logPrinter' android.util.Printer) binds: {(r16_0 'logPrinter' android.util.Printer)=B:2:0x0012, (r16_2 'logPrinter' android.util.Printer)=B:17:0x0066}
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
        r22 = this;
        r4 = r23.getAction();
        r7 = r23.getData();
        r19 = r23.getPackage();
        r12 = r23.isExcludingStopped();
        if (r25 == 0) goto L_0x0066;
    L_0x0012:
        r16 = new android.util.LogPrinter;
        r5 = "IntentResolver";
        r6 = 2;
        r8 = 3;
        r0 = r16;
        r0.<init>(r6, r5, r8);
        r15 = new com.android.internal.util.FastPrintWriter;
        r15.<init>(r16);
    L_0x0023:
        if (r29 == 0) goto L_0x006a;
    L_0x0025:
        r0 = r29;
        r10 = r0.length;
    L_0x0028:
        r13 = 0;
        r14 = 0;
    L_0x002a:
        if (r14 >= r10) goto L_0x01bf;
    L_0x002c:
        r3 = r29[r14];
        if (r3 == 0) goto L_0x01bf;
    L_0x0030:
        if (r25 == 0) goto L_0x004c;
    L_0x0032:
        r5 = "IntentResolver";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "Matching against filter ";
        r6 = r6.append(r8);
        r6 = r6.append(r3);
        r6 = r6.toString();
        android.util.Slog.v(r5, r6);
    L_0x004c:
        if (r12 == 0) goto L_0x006c;
    L_0x004e:
        r0 = r22;
        r1 = r31;
        r5 = r0.isFilterStopped(r3, r1);
        if (r5 == 0) goto L_0x006c;
    L_0x0058:
        if (r25 == 0) goto L_0x0063;
    L_0x005a:
        r5 = "IntentResolver";
        r6 = "  Filter's target is stopped; skipping";
        android.util.Slog.v(r5, r6);
    L_0x0063:
        r14 = r14 + 1;
        goto L_0x002a;
    L_0x0066:
        r16 = 0;
        r15 = 0;
        goto L_0x0023;
    L_0x006a:
        r10 = 0;
        goto L_0x0028;
    L_0x006c:
        if (r19 == 0) goto L_0x00a0;
    L_0x006e:
        r0 = r22;
        r1 = r19;
        r5 = r0.isPackageForFilter(r1, r3);
        r5 = r5 ^ 1;
        if (r5 == 0) goto L_0x00a0;
    L_0x007a:
        if (r25 == 0) goto L_0x0063;
    L_0x007c:
        r5 = "IntentResolver";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "  Filter is not from package ";
        r6 = r6.append(r8);
        r0 = r19;
        r6 = r6.append(r0);
        r8 = "; skipping";
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.v(r5, r6);
        goto L_0x0063;
    L_0x00a0:
        r5 = r3.getAutoVerify();
        if (r5 == 0) goto L_0x00f9;
    L_0x00a6:
        if (r25 == 0) goto L_0x00f9;
    L_0x00a8:
        r5 = "IntentResolver";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "  Filter verified: ";
        r6 = r6.append(r8);
        r0 = r22;
        r8 = r0.isFilterVerified(r3);
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.v(r5, r6);
        r11 = r3.countDataAuthorities();
        r21 = 0;
    L_0x00ce:
        r0 = r21;
        if (r0 >= r11) goto L_0x00f9;
    L_0x00d2:
        r5 = "IntentResolver";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "   ";
        r6 = r6.append(r8);
        r0 = r21;
        r8 = r3.getDataAuthority(r0);
        r8 = r8.getHost();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.v(r5, r6);
        r21 = r21 + 1;
        goto L_0x00ce;
    L_0x00f9:
        r0 = r22;
        r1 = r30;
        r5 = r0.allowFilterResult(r3, r1);
        if (r5 != 0) goto L_0x0110;
    L_0x0103:
        if (r25 == 0) goto L_0x0063;
    L_0x0105:
        r5 = "IntentResolver";
        r6 = "  Filter's target already added";
        android.util.Slog.v(r5, r6);
        goto L_0x0063;
    L_0x0110:
        r9 = "IntentResolver";
        r5 = r27;
        r6 = r28;
        r8 = r24;
        r17 = r3.match(r4, r5, r6, r7, r8, r9);
        if (r17 < 0) goto L_0x0189;
    L_0x011f:
        if (r25 == 0) goto L_0x0151;
    L_0x0121:
        r5 = "IntentResolver";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "  Filter matched!  match=0x";
        r6 = r6.append(r8);
        r8 = java.lang.Integer.toHexString(r17);
        r6 = r6.append(r8);
        r8 = " hasDefault=";
        r6 = r6.append(r8);
        r8 = "android.intent.category.DEFAULT";
        r8 = r3.hasCategory(r8);
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Slog.v(r5, r6);
    L_0x0151:
        if (r26 == 0) goto L_0x015c;
    L_0x0153:
        r5 = "android.intent.category.DEFAULT";
        r5 = r3.hasCategory(r5);
        if (r5 == 0) goto L_0x0186;
    L_0x015c:
        r0 = r22;
        r1 = r17;
        r2 = r31;
        r18 = r0.newResult(r3, r1, r2);
        if (r18 == 0) goto L_0x0063;
    L_0x0168:
        r0 = r30;
        r1 = r18;
        r0.add(r1);
        if (r25 == 0) goto L_0x0063;
    L_0x0171:
        r5 = "    ";
        r0 = r22;
        r0.dumpFilter(r15, r5, r3);
        r15.flush();
        r5 = "    ";
        r0 = r16;
        r3.dump(r0, r5);
        goto L_0x0063;
    L_0x0186:
        r13 = 1;
        goto L_0x0063;
    L_0x0189:
        if (r25 == 0) goto L_0x0063;
    L_0x018b:
        switch(r17) {
            case -4: goto L_0x01b3;
            case -3: goto L_0x01af;
            case -2: goto L_0x01b7;
            case -1: goto L_0x01bb;
            default: goto L_0x018e;
        };
    L_0x018e:
        r20 = "unknown reason";
    L_0x0191:
        r5 = "IntentResolver";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "  Filter did not match: ";
        r6 = r6.append(r8);
        r0 = r20;
        r6 = r6.append(r0);
        r6 = r6.toString();
        android.util.Slog.v(r5, r6);
        goto L_0x0063;
    L_0x01af:
        r20 = "action";
        goto L_0x0191;
    L_0x01b3:
        r20 = "category";
        goto L_0x0191;
    L_0x01b7:
        r20 = "data";
        goto L_0x0191;
    L_0x01bb:
        r20 = "type";
        goto L_0x0191;
    L_0x01bf:
        if (r25 == 0) goto L_0x01d2;
    L_0x01c1:
        if (r13 == 0) goto L_0x01d2;
    L_0x01c3:
        r5 = r30.size();
        if (r5 != 0) goto L_0x01d3;
    L_0x01c9:
        r5 = "IntentResolver";
        r6 = "resolveIntent failed: found match, but none with CATEGORY_DEFAULT";
        android.util.Slog.v(r5, r6);
    L_0x01d2:
        return;
    L_0x01d3:
        r5 = r30.size();
        r6 = 1;
        if (r5 <= r6) goto L_0x01d2;
    L_0x01da:
        r5 = "IntentResolver";
        r6 = "resolveIntent: multiple matches, only some with CATEGORY_DEFAULT";
        android.util.Slog.v(r5, r6);
        goto L_0x01d2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.IntentResolver.buildResolveList(android.content.Intent, android.util.FastImmutableArraySet, boolean, boolean, java.lang.String, java.lang.String, android.content.IntentFilter[], java.util.List, int):void");
    }
}
