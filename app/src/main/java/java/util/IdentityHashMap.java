package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable {
    private static final int DEFAULT_CAPACITY = 32;
    private static final int MAXIMUM_CAPACITY = 536870912;
    private static final int MINIMUM_CAPACITY = 4;
    static final Object NULL_KEY = new Object();
    private static final long serialVersionUID = 8188218128353913216L;
    private transient Set<Entry<K, V>> entrySet;
    transient int modCount;
    int size;
    transient Object[] table;

    private abstract class IdentityHashMapIterator<T> implements Iterator<T> {
        int expectedModCount;
        int index;
        boolean indexValid;
        int lastReturnedIndex;
        final /* synthetic */ IdentityHashMap this$0;
        Object[] traversalTable;

        /* synthetic */ IdentityHashMapIterator(IdentityHashMap this$0, IdentityHashMapIterator -this1) {
            this(this$0);
        }

        private IdentityHashMapIterator(IdentityHashMap this$0) {
            int i = 0;
            this.this$0 = this$0;
            if (this.this$0.size == 0) {
                i = this.this$0.table.length;
            }
            this.index = i;
            this.expectedModCount = this.this$0.modCount;
            this.lastReturnedIndex = -1;
            this.traversalTable = this.this$0.table;
        }

        public boolean hasNext() {
            Object[] tab = this.traversalTable;
            for (int i = this.index; i < tab.length; i += 2) {
                if (tab[i] != null) {
                    this.index = i;
                    this.indexValid = true;
                    return true;
                }
            }
            this.index = tab.length;
            return false;
        }

        protected int nextIndex() {
            if (this.this$0.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            } else if (this.indexValid || (hasNext() ^ 1) == 0) {
                this.indexValid = false;
                this.lastReturnedIndex = this.index;
                this.index += 2;
                return this.lastReturnedIndex;
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (this.lastReturnedIndex == -1) {
                throw new IllegalStateException();
            } else if (this.this$0.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            } else {
                IdentityHashMap identityHashMap = this.this$0;
                int i = identityHashMap.modCount + 1;
                identityHashMap.modCount = i;
                this.expectedModCount = i;
                int deletedSlot = this.lastReturnedIndex;
                this.lastReturnedIndex = -1;
                this.index = deletedSlot;
                this.indexValid = false;
                Object tab = this.traversalTable;
                int len = tab.length;
                int d = deletedSlot;
                Object key = tab[deletedSlot];
                tab[deletedSlot] = null;
                tab[deletedSlot + 1] = null;
                if (tab != this.this$0.table) {
                    this.this$0.remove(key);
                    this.expectedModCount = this.this$0.modCount;
                    return;
                }
                identityHashMap = this.this$0;
                identityHashMap.size--;
                int i2 = IdentityHashMap.nextKeyIndex(deletedSlot, len);
                while (true) {
                    Object item = tab[i2];
                    if (item != null) {
                        int r = IdentityHashMap.hash(item, len);
                        if ((i2 < r && (r <= d || d <= i2)) || (r <= d && d <= i2)) {
                            if (i2 < deletedSlot && d >= deletedSlot && this.traversalTable == this.this$0.table) {
                                int remaining = len - deletedSlot;
                                Object newTable = new Object[remaining];
                                System.arraycopy(tab, deletedSlot, newTable, 0, remaining);
                                this.traversalTable = newTable;
                                this.index = 0;
                            }
                            tab[d] = item;
                            tab[d + 1] = tab[i2 + 1];
                            tab[i2] = null;
                            tab[i2 + 1] = null;
                            d = i2;
                        }
                        i2 = IdentityHashMap.nextKeyIndex(i2, len);
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private class EntryIterator extends IdentityHashMapIterator<java.util.Map.Entry<K, V>> {
        private java.util.IdentityHashMap$EntryIterator.Entry lastReturnedEntry;

        private class Entry implements java.util.Map.Entry<K, V> {
            private int index;

            /* synthetic */ Entry(EntryIterator this$1, int index, Entry -this2) {
                this(index);
            }

            private Entry(int index) {
                this.index = index;
            }

            public K getKey() {
                checkIndexForEntryUse();
                return IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index]);
            }

            public V getValue() {
                checkIndexForEntryUse();
                return EntryIterator.this.traversalTable[this.index + 1];
            }

            public V setValue(V value) {
                checkIndexForEntryUse();
                V oldValue = EntryIterator.this.traversalTable[this.index + 1];
                EntryIterator.this.traversalTable[this.index + 1] = value;
                if (EntryIterator.this.traversalTable != IdentityHashMap.this.table) {
                    IdentityHashMap.this.put(EntryIterator.this.traversalTable[this.index], value);
                }
                return oldValue;
            }

            public boolean equals(Object o) {
                boolean z = false;
                if (this.index < 0) {
                    return super.equals(o);
                }
                if (!(o instanceof java.util.Map.Entry)) {
                    return false;
                }
                java.util.Map.Entry<?, ?> e = (java.util.Map.Entry) o;
                if (e.getKey() == IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index]) && e.getValue() == EntryIterator.this.traversalTable[this.index + 1]) {
                    z = true;
                }
                return z;
            }

            public int hashCode() {
                if (EntryIterator.this.lastReturnedIndex < 0) {
                    return super.hashCode();
                }
                return System.identityHashCode(IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index])) ^ System.identityHashCode(EntryIterator.this.traversalTable[this.index + 1]);
            }

            public String toString() {
                if (this.index < 0) {
                    return super.toString();
                }
                return IdentityHashMap.unmaskNull(EntryIterator.this.traversalTable[this.index]) + "=" + EntryIterator.this.traversalTable[this.index + 1];
            }

            private void checkIndexForEntryUse() {
                if (this.index < 0) {
                    throw new IllegalStateException("Entry was removed");
                }
            }
        }

        /* synthetic */ EntryIterator(IdentityHashMap this$0, EntryIterator -this1) {
            this();
        }

        private EntryIterator() {
            super(IdentityHashMap.this, null);
        }

        public java.util.Map.Entry<K, V> next() {
            this.lastReturnedEntry = new Entry(this, nextIndex(), null);
            return this.lastReturnedEntry;
        }

        public void remove() {
            this.lastReturnedIndex = this.lastReturnedEntry == null ? -1 : this.lastReturnedEntry.index;
            super.remove();
            this.lastReturnedEntry.index = this.lastReturnedIndex;
            this.lastReturnedEntry = null;
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
        /* synthetic */ EntrySet(IdentityHashMap this$0, EntrySet -this1) {
            this();
        }

        private EntrySet() {
        }

        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator(IdentityHashMap.this, null);
        }

        public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> entry = (Entry) o;
            return IdentityHashMap.this.containsMapping(entry.getKey(), entry.getValue());
        }

        public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> entry = (Entry) o;
            return IdentityHashMap.this.removeMapping(entry.getKey(), entry.getValue());
        }

        public int size() {
            return IdentityHashMap.this.size;
        }

        public void clear() {
            IdentityHashMap.this.clear();
        }

        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            Iterator<Entry<K, V>> i = iterator();
            while (i.hasNext()) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        public <T> T[] toArray(T[] a) {
            int expectedModCount = IdentityHashMap.this.modCount;
            int size = size();
            if (a.length < size) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            Object[] tab = IdentityHashMap.this.table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                Object key = tab[si];
                if (key != null) {
                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    int ti2 = ti + 1;
                    a[ti] = new SimpleEntry(IdentityHashMap.unmaskNull(key), tab[si + 1]);
                    ti = ti2;
                }
            }
            if (ti < size || expectedModCount != IdentityHashMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (ti < a.length) {
                a[ti] = null;
            }
            return a;
        }

        public Spliterator<Entry<K, V>> spliterator() {
            return new EntrySpliterator(IdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    static class IdentityHashMapSpliterator<K, V> {
        int est;
        int expectedModCount;
        int fence;
        int index;
        final IdentityHashMap<K, V> map;

        IdentityHashMapSpliterator(IdentityHashMap<K, V> map, int origin, int fence, int est, int expectedModCount) {
            this.map = map;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() {
            int hi = this.fence;
            if (hi >= 0) {
                return hi;
            }
            this.est = this.map.size;
            this.expectedModCount = this.map.modCount;
            hi = this.map.table.length;
            this.fence = hi;
            return hi;
        }

        public final long estimateSize() {
            getFence();
            return (long) this.est;
        }
    }

    static final class EntrySpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<Entry<K, V>> {
        EntrySpliterator(IdentityHashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K, V> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = ((lo + hi) >>> 1) & -2;
            if (lo >= mid) {
                return null;
            }
            IdentityHashMap identityHashMap = this.map;
            this.index = mid;
            int i = this.est >>> 1;
            this.est = i;
            return new EntrySpliterator(identityHashMap, lo, mid, i, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            IdentityHashMap<K, V> m = this.map;
            if (m != null) {
                Object[] a = m.table;
                if (a != null) {
                    int i = this.index;
                    if (i >= 0) {
                        int hi = getFence();
                        this.index = hi;
                        if (hi <= a.length) {
                            while (i < hi) {
                                Object key = a[i];
                                if (key != null) {
                                    action.accept(new SimpleImmutableEntry(IdentityHashMap.unmaskNull(key), a[i + 1]));
                                }
                                i += 2;
                            }
                            if (m.modCount == this.expectedModCount) {
                                return;
                            }
                        }
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = this.map.table;
            int hi = getFence();
            while (this.index < hi) {
                Object key = a[this.index];
                V v = a[this.index + 1];
                this.index += 2;
                if (key != null) {
                    action.accept(new SimpleImmutableEntry(IdentityHashMap.unmaskNull(key), v));
                    if (this.map.modCount == this.expectedModCount) {
                        return true;
                    }
                    throw new ConcurrentModificationException();
                }
            }
            return false;
        }

        public int characteristics() {
            int i = 0;
            if (this.fence < 0 || this.est == this.map.size) {
                i = 64;
            }
            return i | 1;
        }
    }

    private class KeyIterator extends IdentityHashMapIterator<K> {
        /* synthetic */ KeyIterator(IdentityHashMap this$0, KeyIterator -this1) {
            this();
        }

        private KeyIterator() {
            super(IdentityHashMap.this, null);
        }

        public K next() {
            return IdentityHashMap.unmaskNull(this.traversalTable[nextIndex()]);
        }
    }

    private class KeySet extends AbstractSet<K> {
        /* synthetic */ KeySet(IdentityHashMap this$0, KeySet -this1) {
            this();
        }

        private KeySet() {
        }

        public Iterator<K> iterator() {
            return new KeyIterator(IdentityHashMap.this, null);
        }

        public int size() {
            return IdentityHashMap.this.size;
        }

        public boolean contains(Object o) {
            return IdentityHashMap.this.containsKey(o);
        }

        public boolean remove(Object o) {
            int oldSize = IdentityHashMap.this.size;
            IdentityHashMap.this.remove(o);
            return IdentityHashMap.this.size != oldSize;
        }

        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            Iterator<K> i = iterator();
            while (i.hasNext()) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public void clear() {
            IdentityHashMap.this.clear();
        }

        public int hashCode() {
            int result = 0;
            for (K key : this) {
                result += System.identityHashCode(key);
            }
            return result;
        }

        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        public <T> T[] toArray(T[] a) {
            int expectedModCount = IdentityHashMap.this.modCount;
            int size = size();
            if (a.length < size) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            Object[] tab = IdentityHashMap.this.table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                Object key = tab[si];
                if (key != null) {
                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    int ti2 = ti + 1;
                    a[ti] = IdentityHashMap.unmaskNull(key);
                    ti = ti2;
                }
            }
            if (ti < size || expectedModCount != IdentityHashMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (ti < a.length) {
                a[ti] = null;
            }
            return a;
        }

        public Spliterator<K> spliterator() {
            return new KeySpliterator(IdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    static final class KeySpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<K> {
        KeySpliterator(IdentityHashMap<K, V> map, int origin, int fence, int est, int expectedModCount) {
            super(map, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = ((lo + hi) >>> 1) & -2;
            if (lo >= mid) {
                return null;
            }
            IdentityHashMap identityHashMap = this.map;
            this.index = mid;
            int i = this.est >>> 1;
            this.est = i;
            return new KeySpliterator(identityHashMap, lo, mid, i, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            IdentityHashMap<K, V> m = this.map;
            if (m != null) {
                Object[] a = m.table;
                if (a != null) {
                    int i = this.index;
                    if (i >= 0) {
                        int hi = getFence();
                        this.index = hi;
                        if (hi <= a.length) {
                            while (i < hi) {
                                Object key = a[i];
                                if (key != null) {
                                    action.accept(IdentityHashMap.unmaskNull(key));
                                }
                                i += 2;
                            }
                            if (m.modCount == this.expectedModCount) {
                                return;
                            }
                        }
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = this.map.table;
            int hi = getFence();
            while (this.index < hi) {
                Object key = a[this.index];
                this.index += 2;
                if (key != null) {
                    action.accept(IdentityHashMap.unmaskNull(key));
                    if (this.map.modCount == this.expectedModCount) {
                        return true;
                    }
                    throw new ConcurrentModificationException();
                }
            }
            return false;
        }

        public int characteristics() {
            int i = 0;
            if (this.fence < 0 || this.est == this.map.size) {
                i = 64;
            }
            return i | 1;
        }
    }

    private class ValueIterator extends IdentityHashMapIterator<V> {
        /* synthetic */ ValueIterator(IdentityHashMap this$0, ValueIterator -this1) {
            this();
        }

        private ValueIterator() {
            super(IdentityHashMap.this, null);
        }

        public V next() {
            return this.traversalTable[nextIndex() + 1];
        }
    }

    static final class ValueSpliterator<K, V> extends IdentityHashMapSpliterator<K, V> implements Spliterator<V> {
        ValueSpliterator(IdentityHashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = ((lo + hi) >>> 1) & -2;
            if (lo >= mid) {
                return null;
            }
            IdentityHashMap identityHashMap = this.map;
            this.index = mid;
            int i = this.est >>> 1;
            this.est = i;
            return new ValueSpliterator(identityHashMap, lo, mid, i, this.expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            IdentityHashMap<K, V> m = this.map;
            if (m != null) {
                Object[] a = m.table;
                if (a != null) {
                    int i = this.index;
                    if (i >= 0) {
                        int hi = getFence();
                        this.index = hi;
                        if (hi <= a.length) {
                            while (i < hi) {
                                if (a[i] != null) {
                                    action.accept(a[i + 1]);
                                }
                                i += 2;
                            }
                            if (m.modCount == this.expectedModCount) {
                                return;
                            }
                        }
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = this.map.table;
            int hi = getFence();
            while (this.index < hi) {
                Object key = a[this.index];
                V v = a[this.index + 1];
                this.index += 2;
                if (key != null) {
                    action.accept(v);
                    if (this.map.modCount == this.expectedModCount) {
                        return true;
                    }
                    throw new ConcurrentModificationException();
                }
            }
            return false;
        }

        public int characteristics() {
            return (this.fence < 0 || this.est == this.map.size) ? 64 : 0;
        }
    }

    private class Values extends AbstractCollection<V> {
        /* synthetic */ Values(IdentityHashMap this$0, Values -this1) {
            this();
        }

        private Values() {
        }

        public Iterator<V> iterator() {
            return new ValueIterator(IdentityHashMap.this, null);
        }

        public int size() {
            return IdentityHashMap.this.size;
        }

        public boolean contains(Object o) {
            return IdentityHashMap.this.containsValue(o);
        }

        public boolean remove(Object o) {
            Iterator<V> i = iterator();
            while (i.hasNext()) {
                if (i.next() == o) {
                    i.remove();
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            IdentityHashMap.this.clear();
        }

        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        public <T> T[] toArray(T[] a) {
            int expectedModCount = IdentityHashMap.this.modCount;
            int size = size();
            if (a.length < size) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            Object[] tab = IdentityHashMap.this.table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                if (tab[si] != null) {
                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    int ti2 = ti + 1;
                    a[ti] = tab[si + 1];
                    ti = ti2;
                }
            }
            if (ti < size || expectedModCount != IdentityHashMap.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (ti < a.length) {
                a[ti] = null;
            }
            return a;
        }

        public Spliterator<V> spliterator() {
            return new ValueSpliterator(IdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    private static Object maskNull(Object key) {
        return key == null ? NULL_KEY : key;
    }

    static final Object unmaskNull(Object key) {
        return key == NULL_KEY ? null : key;
    }

    public IdentityHashMap() {
        init(32);
    }

    public IdentityHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0) {
            throw new IllegalArgumentException("expectedMaxSize is negative: " + expectedMaxSize);
        }
        init(capacity(expectedMaxSize));
    }

    private static int capacity(int expectedMaxSize) {
        if (expectedMaxSize > 178956970) {
            return MAXIMUM_CAPACITY;
        }
        if (expectedMaxSize <= 2) {
            return 4;
        }
        return Integer.highestOneBit((expectedMaxSize << 1) + expectedMaxSize);
    }

    private void init(int initCapacity) {
        this.table = new Object[(initCapacity * 2)];
    }

    public IdentityHashMap(Map<? extends K, ? extends V> m) {
        this((int) (((double) (m.size() + 1)) * 1.1d));
        putAll(m);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    private static int hash(Object x, int length) {
        int h = System.identityHashCode(x);
        return ((h << 1) - (h << 8)) & (length - 1);
    }

    private static int nextKeyIndex(int i, int len) {
        return i + 2 < len ? i + 2 : 0;
    }

    public V get(Object key) {
        Object k = maskNull(key);
        Object[] tab = this.table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                return tab[i + 1];
            }
            if (item == null) {
                return null;
            }
            i = nextKeyIndex(i, len);
        }
    }

    public boolean containsKey(Object key) {
        Object k = maskNull(key);
        Object[] tab = this.table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                return true;
            }
            if (item == null) {
                return false;
            }
            i = nextKeyIndex(i, len);
        }
    }

    public boolean containsValue(Object value) {
        Object[] tab = this.table;
        int i = 1;
        while (i < tab.length) {
            if (tab[i] == value && tab[i - 1] != null) {
                return true;
            }
            i += 2;
        }
        return false;
    }

    private boolean containsMapping(Object key, Object value) {
        boolean z = false;
        Object k = maskNull(key);
        Object[] tab = this.table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                if (tab[i + 1] == value) {
                    z = true;
                }
                return z;
            } else if (item == null) {
                return false;
            } else {
                i = nextKeyIndex(i, len);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public V put(K key, V value) {
        Object[] tab;
        int i;
        Object k = maskNull(key);
        int len;
        do {
            tab = this.table;
            len = tab.length;
            i = hash(k, len);
            while (true) {
                Object item = tab[i];
                if (item == null) {
                    break;
                } else if (item == k) {
                    V oldValue = tab[i + 1];
                    tab[i + 1] = value;
                    return oldValue;
                } else {
                    i = nextKeyIndex(i, len);
                }
            }
        } while (resize(len));
        this.modCount++;
        tab[i] = k;
        tab[i + 1] = value;
        this.size = s;
        return null;
    }

    private boolean resize(int newCapacity) {
        int newLength = newCapacity * 2;
        Object[] oldTable = this.table;
        int oldLength = oldTable.length;
        if (oldLength == 1073741824) {
            if (this.size != 536870911) {
                return false;
            }
            throw new IllegalStateException("Capacity exhausted.");
        } else if (oldLength >= newLength) {
            return false;
        } else {
            Object[] newTable = new Object[newLength];
            for (int j = 0; j < oldLength; j += 2) {
                Object key = oldTable[j];
                if (key != null) {
                    Object value = oldTable[j + 1];
                    oldTable[j] = null;
                    oldTable[j + 1] = null;
                    int i = hash(key, newLength);
                    while (newTable[i] != null) {
                        i = nextKeyIndex(i, newLength);
                    }
                    newTable[i] = key;
                    newTable[i + 1] = value;
                }
            }
            this.table = newTable;
            return true;
        }
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        int n = m.size();
        if (n != 0) {
            if (n > this.size) {
                resize(capacity(n));
            }
            for (Entry<? extends K, ? extends V> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }
    }

    public V remove(Object key) {
        Object k = maskNull(key);
        Object[] tab = this.table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                this.modCount++;
                this.size--;
                V oldValue = tab[i + 1];
                tab[i + 1] = null;
                tab[i] = null;
                closeDeletion(i);
                return oldValue;
            } else if (item == null) {
                return null;
            } else {
                i = nextKeyIndex(i, len);
            }
        }
    }

    private boolean removeMapping(Object key, Object value) {
        Object k = maskNull(key);
        Object[] tab = this.table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                if (tab[i + 1] != value) {
                    return false;
                }
                this.modCount++;
                this.size--;
                tab[i] = null;
                tab[i + 1] = null;
                closeDeletion(i);
                return true;
            } else if (item == null) {
                return false;
            } else {
                i = nextKeyIndex(i, len);
            }
        }
    }

    private void closeDeletion(int d) {
        Object[] tab = this.table;
        int len = tab.length;
        int i = nextKeyIndex(d, len);
        while (true) {
            Object item = tab[i];
            if (item != null) {
                int r = hash(item, len);
                if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
                    tab[d] = item;
                    tab[d + 1] = tab[i + 1];
                    tab[i] = null;
                    tab[i + 1] = null;
                    d = i;
                }
                i = nextKeyIndex(i, len);
            } else {
                return;
            }
        }
    }

    public void clear() {
        this.modCount++;
        Object[] tab = this.table;
        for (int i = 0; i < tab.length; i++) {
            tab[i] = null;
        }
        this.size = 0;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof IdentityHashMap) {
            IdentityHashMap<?, ?> m = (IdentityHashMap) o;
            if (m.size() != this.size) {
                return false;
            }
            Object[] tab = m.table;
            int i = 0;
            while (i < tab.length) {
                Object k = tab[i];
                if (k != null && (containsMapping(k, tab[i + 1]) ^ 1) != 0) {
                    return false;
                }
                i += 2;
            }
            return true;
        } else if (!(o instanceof Map)) {
            return false;
        } else {
            return entrySet().equals(((Map) o).entrySet());
        }
    }

    public int hashCode() {
        int result = 0;
        Object[] tab = this.table;
        for (int i = 0; i < tab.length; i += 2) {
            Object key = tab[i];
            if (key != null) {
                result += System.identityHashCode(unmaskNull(key)) ^ System.identityHashCode(tab[i + 1]);
            }
        }
        return result;
    }

    public Object clone() {
        try {
            IdentityHashMap<?, ?> m = (IdentityHashMap) super.clone();
            m.entrySet = null;
            m.table = (Object[]) this.table.clone();
            return m;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    public Set<K> keySet() {
        Set<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        ks = new KeySet();
        this.keySet = ks;
        return ks;
    }

    public Collection<V> values() {
        Collection<V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        vs = new Values();
        this.values = vs;
        return vs;
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = this.entrySet;
        if (es != null) {
            return es;
        }
        Set<Entry<K, V>> entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(this.size);
        Object[] tab = this.table;
        for (int i = 0; i < tab.length; i += 2) {
            Object key = tab[i];
            if (key != null) {
                s.writeObject(unmaskNull(key));
                s.writeObject(tab[i + 1]);
            }
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        int size = s.readInt();
        if (size < 0) {
            throw new StreamCorruptedException("Illegal mappings count: " + size);
        }
        init(capacity(size));
        for (int i = 0; i < size; i++) {
            putForCreate(s.readObject(), s.readObject());
        }
    }

    private void putForCreate(K key, V value) throws StreamCorruptedException {
        Object k = maskNull(key);
        Object[] tab = this.table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == null) {
                tab[i] = k;
                tab[i + 1] = value;
                return;
            } else if (item == k) {
                throw new StreamCorruptedException();
            } else {
                i = nextKeyIndex(i, len);
            }
        }
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = this.modCount;
        Object[] t = this.table;
        for (int index = 0; index < t.length; index += 2) {
            Object k = t[index];
            if (k != null) {
                action.accept(unmaskNull(k), t[index + 1]);
            }
            if (this.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = this.modCount;
        Object[] t = this.table;
        for (int index = 0; index < t.length; index += 2) {
            Object k = t[index];
            if (k != null) {
                t[index + 1] = function.apply(unmaskNull(k), t[index + 1]);
            }
            if (this.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
