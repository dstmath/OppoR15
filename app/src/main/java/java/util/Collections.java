package java.util;

import dalvik.system.VMRuntime;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis.AnonymousClass4;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Collections {
    private static final int BINARYSEARCH_THRESHOLD = 5000;
    private static final int COPY_THRESHOLD = 10;
    public static final List EMPTY_LIST = new EmptyList();
    public static final Map EMPTY_MAP = new EmptyMap();
    public static final Set EMPTY_SET = new EmptySet();
    private static final int FILL_THRESHOLD = 25;
    private static final int INDEXOFSUBLIST_THRESHOLD = 35;
    private static final int REPLACEALL_THRESHOLD = 11;
    private static final int REVERSE_THRESHOLD = 18;
    private static final int ROTATE_THRESHOLD = 100;
    private static final int SHUFFLE_THRESHOLD = 5;
    private static Random r;

    static class AsLIFOQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable {
        private static final long serialVersionUID = 1802017725587941708L;
        private final Deque<E> q;

        AsLIFOQueue(Deque<E> q) {
            this.q = q;
        }

        public boolean add(E e) {
            this.q.addFirst(e);
            return true;
        }

        public boolean offer(E e) {
            return this.q.offerFirst(e);
        }

        public E poll() {
            return this.q.pollFirst();
        }

        public E remove() {
            return this.q.removeFirst();
        }

        public E peek() {
            return this.q.peekFirst();
        }

        public E element() {
            return this.q.getFirst();
        }

        public void clear() {
            this.q.clear();
        }

        public int size() {
            return this.q.size();
        }

        public boolean isEmpty() {
            return this.q.isEmpty();
        }

        public boolean contains(Object o) {
            return this.q.contains(o);
        }

        public boolean remove(Object o) {
            return this.q.remove(o);
        }

        public Iterator<E> iterator() {
            return this.q.iterator();
        }

        public Object[] toArray() {
            return this.q.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return this.q.toArray(a);
        }

        public String toString() {
            return this.q.toString();
        }

        public boolean containsAll(Collection<?> c) {
            return this.q.containsAll(c);
        }

        public boolean removeAll(Collection<?> c) {
            return this.q.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return this.q.retainAll(c);
        }

        public void forEach(Consumer<? super E> action) {
            this.q.forEach(action);
        }

        public boolean removeIf(Predicate<? super E> filter) {
            return this.q.removeIf(filter);
        }

        public Spliterator<E> spliterator() {
            return this.q.spliterator();
        }

        public Stream<E> stream() {
            return this.q.stream();
        }

        public Stream<E> parallelStream() {
            return this.q.parallelStream();
        }
    }

    static class CheckedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1578914078182001775L;
        final Collection<E> c;
        final Class<E> type;
        private E[] zeroLengthElementArray;

        E typeCheck(Object o) {
            if (o == null || (this.type.isInstance(o) ^ 1) == 0) {
                return o;
            }
            throw new ClassCastException(badElementMsg(o));
        }

        private String badElementMsg(Object o) {
            return "Attempt to insert " + o.getClass() + " element into collection with element type " + this.type;
        }

        CheckedCollection(Collection<E> c, Class<E> type) {
            this.c = (Collection) Objects.requireNonNull((Object) c, "c");
            this.type = (Class) Objects.requireNonNull((Object) type, "type");
        }

        public int size() {
            return this.c.size();
        }

        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        public boolean contains(Object o) {
            return this.c.contains(o);
        }

        public Object[] toArray() {
            return this.c.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return this.c.toArray(a);
        }

        public String toString() {
            return this.c.toString();
        }

        public boolean remove(Object o) {
            return this.c.remove(o);
        }

        public void clear() {
            this.c.clear();
        }

        public boolean containsAll(Collection<?> coll) {
            return this.c.containsAll(coll);
        }

        public boolean removeAll(Collection<?> coll) {
            return this.c.removeAll(coll);
        }

        public boolean retainAll(Collection<?> coll) {
            return this.c.retainAll(coll);
        }

        public Iterator<E> iterator() {
            final Iterator<E> it = this.c.iterator();
            return new Iterator<E>() {
                public boolean hasNext() {
                    return it.hasNext();
                }

                public E next() {
                    return it.next();
                }

                public void remove() {
                    it.remove();
                }
            };
        }

        public boolean add(E e) {
            return this.c.add(typeCheck(e));
        }

        private E[] zeroLengthElementArray() {
            if (this.zeroLengthElementArray != null) {
                return this.zeroLengthElementArray;
            }
            E[] zeroLengthArray = Collections.zeroLengthArray(this.type);
            this.zeroLengthElementArray = zeroLengthArray;
            return zeroLengthArray;
        }

        Collection<E> checkedCopyOf(Collection<? extends E> coll) {
            Object[] a;
            try {
                E[] z = zeroLengthElementArray();
                a = coll.toArray(z);
                if (a.getClass() != z.getClass()) {
                    a = Arrays.copyOf(a, a.length, z.getClass());
                }
            } catch (ArrayStoreException e) {
                a = (Object[]) coll.toArray().clone();
                for (Object o : a) {
                    typeCheck(o);
                }
            }
            return Arrays.asList(a);
        }

        public boolean addAll(Collection<? extends E> coll) {
            return this.c.addAll(checkedCopyOf(coll));
        }

        public void forEach(Consumer<? super E> action) {
            this.c.forEach(action);
        }

        public boolean removeIf(Predicate<? super E> filter) {
            return this.c.removeIf(filter);
        }

        public Spliterator<E> spliterator() {
            return this.c.spliterator();
        }

        public Stream<E> stream() {
            return this.c.stream();
        }

        public Stream<E> parallelStream() {
            return this.c.parallelStream();
        }
    }

    static class CheckedList<E> extends CheckedCollection<E> implements List<E> {
        private static final long serialVersionUID = 65247728283967356L;
        final List<E> list;

        CheckedList(List<E> list, Class<E> type) {
            super(list, type);
            this.list = list;
        }

        public boolean equals(Object o) {
            return o != this ? this.list.equals(o) : true;
        }

        public int hashCode() {
            return this.list.hashCode();
        }

        public E get(int index) {
            return this.list.get(index);
        }

        public E remove(int index) {
            return this.list.remove(index);
        }

        public int indexOf(Object o) {
            return this.list.indexOf(o);
        }

        public int lastIndexOf(Object o) {
            return this.list.lastIndexOf(o);
        }

        public E set(int index, E element) {
            return this.list.set(index, typeCheck(element));
        }

        public void add(int index, E element) {
            this.list.add(index, typeCheck(element));
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            return this.list.addAll(index, checkedCopyOf(c));
        }

        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        public ListIterator<E> listIterator(int index) {
            final ListIterator<E> i = this.list.listIterator(index);
            return new ListIterator<E>() {
                public boolean hasNext() {
                    return i.hasNext();
                }

                public E next() {
                    return i.next();
                }

                public boolean hasPrevious() {
                    return i.hasPrevious();
                }

                public E previous() {
                    return i.previous();
                }

                public int nextIndex() {
                    return i.nextIndex();
                }

                public int previousIndex() {
                    return i.previousIndex();
                }

                public void remove() {
                    i.remove();
                }

                public void set(E e) {
                    i.set(CheckedList.this.typeCheck(e));
                }

                public void add(E e) {
                    i.add(CheckedList.this.typeCheck(e));
                }

                public void forEachRemaining(Consumer<? super E> action) {
                    i.forEachRemaining(action);
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new CheckedList(this.list.subList(fromIndex, toIndex), this.type);
        }

        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
            this.list.replaceAll(new AnonymousClass4(this, operator));
        }

        /* synthetic */ Object lambda$-java_util_Collections$CheckedList_142882(UnaryOperator operator, Object e) {
            return typeCheck(operator.apply(e));
        }

        public void sort(Comparator<? super E> c) {
            this.list.sort(c);
        }
    }

    private static class CheckedMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = 5742860141034234728L;
        private transient Set<Entry<K, V>> entrySet;
        final Class<K> keyType;
        private final Map<K, V> m;
        final Class<V> valueType;

        static class CheckedEntrySet<K, V> implements Set<Entry<K, V>> {
            private final Set<Entry<K, V>> s;
            private final Class<V> valueType;

            private static class CheckedEntry<K, V, T> implements Entry<K, V> {
                private final Entry<K, V> e;
                private final Class<T> valueType;

                CheckedEntry(Entry<K, V> e, Class<T> valueType) {
                    this.e = (Entry) Objects.requireNonNull(e);
                    this.valueType = (Class) Objects.requireNonNull(valueType);
                }

                public K getKey() {
                    return this.e.getKey();
                }

                public V getValue() {
                    return this.e.getValue();
                }

                public int hashCode() {
                    return this.e.hashCode();
                }

                public String toString() {
                    return this.e.toString();
                }

                public V setValue(V value) {
                    if (value == null || (this.valueType.isInstance(value) ^ 1) == 0) {
                        return this.e.setValue(value);
                    }
                    throw new ClassCastException(badValueMsg(value));
                }

                private String badValueMsg(Object value) {
                    return "Attempt to insert " + value.getClass() + " value into map with value type " + this.valueType;
                }

                public boolean equals(Object o) {
                    if (o == this) {
                        return true;
                    }
                    if (o instanceof Entry) {
                        return this.e.equals(new SimpleImmutableEntry((Entry) o));
                    }
                    return false;
                }
            }

            CheckedEntrySet(Set<Entry<K, V>> s, Class<V> valueType) {
                this.s = s;
                this.valueType = valueType;
            }

            public int size() {
                return this.s.size();
            }

            public boolean isEmpty() {
                return this.s.isEmpty();
            }

            public String toString() {
                return this.s.toString();
            }

            public int hashCode() {
                return this.s.hashCode();
            }

            public void clear() {
                this.s.clear();
            }

            public boolean add(Entry<K, V> entry) {
                throw new UnsupportedOperationException();
            }

            public boolean addAll(Collection<? extends Entry<K, V>> collection) {
                throw new UnsupportedOperationException();
            }

            public Iterator<Entry<K, V>> iterator() {
                final Iterator<Entry<K, V>> i = this.s.iterator();
                final Class<V> valueType = this.valueType;
                return new Iterator<Entry<K, V>>() {
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public void remove() {
                        i.remove();
                    }

                    public Entry<K, V> next() {
                        return CheckedEntrySet.checkedEntry((Entry) i.next(), valueType);
                    }
                };
            }

            public Object[] toArray() {
                Object[] dest;
                Object[] source = this.s.toArray();
                if (CheckedEntry.class.isInstance(source.getClass().getComponentType())) {
                    dest = source;
                } else {
                    dest = new Object[source.length];
                }
                for (int i = 0; i < source.length; i++) {
                    dest[i] = checkedEntry((Entry) source[i], this.valueType);
                }
                return dest;
            }

            public <T> T[] toArray(T[] a) {
                Object arr = this.s.toArray(a.length == 0 ? a : Arrays.copyOf((Object[]) a, 0));
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = checkedEntry((Entry) arr[i], this.valueType);
                }
                if (arr.length > a.length) {
                    return arr;
                }
                System.arraycopy(arr, 0, (Object) a, 0, arr.length);
                if (a.length > arr.length) {
                    a[arr.length] = null;
                }
                return a;
            }

            public boolean contains(Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                Entry<?, ?> e = (Entry) o;
                Set set = this.s;
                if (!(e instanceof CheckedEntry)) {
                    e = checkedEntry(e, this.valueType);
                }
                return set.contains(e);
            }

            public boolean containsAll(Collection<?> c) {
                for (Object o : c) {
                    if (!contains(o)) {
                        return false;
                    }
                }
                return true;
            }

            public boolean remove(Object o) {
                if (o instanceof Entry) {
                    return this.s.remove(new SimpleImmutableEntry((Entry) o));
                }
                return false;
            }

            public boolean removeAll(Collection<?> c) {
                return batchRemove(c, false);
            }

            public boolean retainAll(Collection<?> c) {
                return batchRemove(c, true);
            }

            private boolean batchRemove(Collection<?> c, boolean complement) {
                Objects.requireNonNull(c);
                boolean modified = false;
                Iterator<Entry<K, V>> it = iterator();
                while (it.hasNext()) {
                    if (c.contains(it.next()) != complement) {
                        it.remove();
                        modified = true;
                    }
                }
                return modified;
            }

            public boolean equals(Object o) {
                boolean z = false;
                if (o == this) {
                    return true;
                }
                if (!(o instanceof Set)) {
                    return false;
                }
                Set<?> that = (Set) o;
                if (that.size() == this.s.size()) {
                    z = containsAll(that);
                }
                return z;
            }

            static <K, V, T> CheckedEntry<K, V, T> checkedEntry(Entry<K, V> e, Class<T> valueType) {
                return new CheckedEntry(e, valueType);
            }
        }

        private void typeCheck(Object key, Object value) {
            if (key != null && (this.keyType.isInstance(key) ^ 1) != 0) {
                throw new ClassCastException(badKeyMsg(key));
            } else if (value != null && (this.valueType.isInstance(value) ^ 1) != 0) {
                throw new ClassCastException(badValueMsg(value));
            }
        }

        private BiFunction<? super K, ? super V, ? extends V> typeCheck(BiFunction<? super K, ? super V, ? extends V> func) {
            Objects.requireNonNull(func);
            return new java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis.AnonymousClass2((byte) 1, this, func);
        }

        /* synthetic */ Object lambda$-java_util_Collections$CheckedMap_146492(BiFunction func, Object k, Object v) {
            V newValue = func.apply(k, v);
            typeCheck(k, newValue);
            return newValue;
        }

        private String badKeyMsg(Object key) {
            return "Attempt to insert " + key.getClass() + " key into map with key type " + this.keyType;
        }

        private String badValueMsg(Object value) {
            return "Attempt to insert " + value.getClass() + " value into map with value type " + this.valueType;
        }

        CheckedMap(Map<K, V> m, Class<K> keyType, Class<V> valueType) {
            this.m = (Map) Objects.requireNonNull(m);
            this.keyType = (Class) Objects.requireNonNull(keyType);
            this.valueType = (Class) Objects.requireNonNull(valueType);
        }

        public int size() {
            return this.m.size();
        }

        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        public boolean containsKey(Object key) {
            return this.m.containsKey(key);
        }

        public boolean containsValue(Object v) {
            return this.m.containsValue(v);
        }

        public V get(Object key) {
            return this.m.get(key);
        }

        public V remove(Object key) {
            return this.m.remove(key);
        }

        public void clear() {
            this.m.clear();
        }

        public Set<K> keySet() {
            return this.m.keySet();
        }

        public Collection<V> values() {
            return this.m.values();
        }

        public boolean equals(Object o) {
            return o != this ? this.m.equals(o) : true;
        }

        public int hashCode() {
            return this.m.hashCode();
        }

        public String toString() {
            return this.m.toString();
        }

        public V put(K key, V value) {
            typeCheck(key, value);
            return this.m.put(key, value);
        }

        public void putAll(Map<? extends K, ? extends V> t) {
            Object[] entries = t.entrySet().toArray();
            List<Entry<K, V>> checked = new ArrayList(entries.length);
            for (Entry<?, ?> e : entries) {
                Object k = e.getKey();
                Object v = e.getValue();
                typeCheck(k, v);
                checked.add(new SimpleImmutableEntry(k, v));
            }
            for (Entry<K, V> e2 : checked) {
                this.m.put(e2.getKey(), e2.getValue());
            }
        }

        public Set<Entry<K, V>> entrySet() {
            if (this.entrySet == null) {
                this.entrySet = new CheckedEntrySet(this.m.entrySet(), this.valueType);
            }
            return this.entrySet;
        }

        public void forEach(BiConsumer<? super K, ? super V> action) {
            this.m.forEach(action);
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            this.m.replaceAll(typeCheck(function));
        }

        public V putIfAbsent(K key, V value) {
            typeCheck(key, value);
            return this.m.putIfAbsent(key, value);
        }

        public boolean remove(Object key, Object value) {
            return this.m.remove(key, value);
        }

        public boolean replace(K key, V oldValue, V newValue) {
            typeCheck(key, newValue);
            return this.m.replace(key, oldValue, newValue);
        }

        public V replace(K key, V value) {
            typeCheck(key, value);
            return this.m.replace(key, value);
        }

        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            Objects.requireNonNull(mappingFunction);
            return this.m.computeIfAbsent(key, new java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis.AnonymousClass3(this, mappingFunction));
        }

        /* synthetic */ Object lambda$-java_util_Collections$CheckedMap_150612(Function mappingFunction, Object k) {
            V value = mappingFunction.apply(k);
            typeCheck(k, value);
            return value;
        }

        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return this.m.computeIfPresent(key, typeCheck(remappingFunction));
        }

        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return this.m.compute(key, typeCheck(remappingFunction));
        }

        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            Objects.requireNonNull(remappingFunction);
            return this.m.merge(key, value, new java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis.AnonymousClass2((byte) 0, this, remappingFunction));
        }

        /* synthetic */ Object lambda$-java_util_Collections$CheckedMap_151435(BiFunction remappingFunction, Object v1, Object v2) {
            V newValue = remappingFunction.apply(v1, v2);
            typeCheck(null, newValue);
            return newValue;
        }
    }

    static class CheckedSortedMap<K, V> extends CheckedMap<K, V> implements SortedMap<K, V>, Serializable {
        private static final long serialVersionUID = 1599671320688067438L;
        private final SortedMap<K, V> sm;

        CheckedSortedMap(SortedMap<K, V> m, Class<K> keyType, Class<V> valueType) {
            super(m, keyType, valueType);
            this.sm = m;
        }

        public Comparator<? super K> comparator() {
            return this.sm.comparator();
        }

        public K firstKey() {
            return this.sm.firstKey();
        }

        public K lastKey() {
            return this.sm.lastKey();
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return Collections.checkedSortedMap(this.sm.subMap(fromKey, toKey), this.keyType, this.valueType);
        }

        public SortedMap<K, V> headMap(K toKey) {
            return Collections.checkedSortedMap(this.sm.headMap(toKey), this.keyType, this.valueType);
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            return Collections.checkedSortedMap(this.sm.tailMap(fromKey), this.keyType, this.valueType);
        }
    }

    static class CheckedNavigableMap<K, V> extends CheckedSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
        private static final long serialVersionUID = -4852462692372534096L;
        private final NavigableMap<K, V> nm;

        CheckedNavigableMap(NavigableMap<K, V> m, Class<K> keyType, Class<V> valueType) {
            super(m, keyType, valueType);
            this.nm = m;
        }

        public Comparator<? super K> comparator() {
            return this.nm.comparator();
        }

        public K firstKey() {
            return this.nm.firstKey();
        }

        public K lastKey() {
            return this.nm.lastKey();
        }

        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> lower = this.nm.lowerEntry(key);
            if (lower != null) {
                return new CheckedEntry(lower, this.valueType);
            }
            return null;
        }

        public K lowerKey(K key) {
            return this.nm.lowerKey(key);
        }

        public Entry<K, V> floorEntry(K key) {
            Entry<K, V> floor = this.nm.floorEntry(key);
            if (floor != null) {
                return new CheckedEntry(floor, this.valueType);
            }
            return null;
        }

        public K floorKey(K key) {
            return this.nm.floorKey(key);
        }

        public Entry<K, V> ceilingEntry(K key) {
            Entry<K, V> ceiling = this.nm.ceilingEntry(key);
            if (ceiling != null) {
                return new CheckedEntry(ceiling, this.valueType);
            }
            return null;
        }

        public K ceilingKey(K key) {
            return this.nm.ceilingKey(key);
        }

        public Entry<K, V> higherEntry(K key) {
            Entry<K, V> higher = this.nm.higherEntry(key);
            if (higher != null) {
                return new CheckedEntry(higher, this.valueType);
            }
            return null;
        }

        public K higherKey(K key) {
            return this.nm.higherKey(key);
        }

        public Entry<K, V> firstEntry() {
            Entry<K, V> first = this.nm.firstEntry();
            if (first != null) {
                return new CheckedEntry(first, this.valueType);
            }
            return null;
        }

        public Entry<K, V> lastEntry() {
            Entry<K, V> last = this.nm.lastEntry();
            if (last != null) {
                return new CheckedEntry(last, this.valueType);
            }
            return null;
        }

        public Entry<K, V> pollFirstEntry() {
            Entry<K, V> entry = this.nm.pollFirstEntry();
            if (entry == null) {
                return null;
            }
            return new CheckedEntry(entry, this.valueType);
        }

        public Entry<K, V> pollLastEntry() {
            Entry<K, V> entry = this.nm.pollLastEntry();
            if (entry == null) {
                return null;
            }
            return new CheckedEntry(entry, this.valueType);
        }

        public NavigableMap<K, V> descendingMap() {
            return Collections.checkedNavigableMap(this.nm.descendingMap(), this.keyType, this.valueType);
        }

        public NavigableSet<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> navigableKeySet() {
            return Collections.checkedNavigableSet(this.nm.navigableKeySet(), this.keyType);
        }

        public NavigableSet<K> descendingKeySet() {
            return Collections.checkedNavigableSet(this.nm.descendingKeySet(), this.keyType);
        }

        public NavigableMap<K, V> subMap(K fromKey, K toKey) {
            return Collections.checkedNavigableMap(this.nm.subMap(fromKey, true, toKey, false), this.keyType, this.valueType);
        }

        public NavigableMap<K, V> headMap(K toKey) {
            return Collections.checkedNavigableMap(this.nm.headMap(toKey, false), this.keyType, this.valueType);
        }

        public NavigableMap<K, V> tailMap(K fromKey) {
            return Collections.checkedNavigableMap(this.nm.tailMap(fromKey, true), this.keyType, this.valueType);
        }

        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return Collections.checkedNavigableMap(this.nm.subMap(fromKey, fromInclusive, toKey, toInclusive), this.keyType, this.valueType);
        }

        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return Collections.checkedNavigableMap(this.nm.headMap(toKey, inclusive), this.keyType, this.valueType);
        }

        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return Collections.checkedNavigableMap(this.nm.tailMap(fromKey, inclusive), this.keyType, this.valueType);
        }
    }

    static class CheckedSet<E> extends CheckedCollection<E> implements Set<E>, Serializable {
        private static final long serialVersionUID = 4694047833775013803L;

        CheckedSet(Set<E> s, Class<E> elementType) {
            super(s, elementType);
        }

        public boolean equals(Object o) {
            return o != this ? this.c.equals(o) : true;
        }

        public int hashCode() {
            return this.c.hashCode();
        }
    }

    static class CheckedSortedSet<E> extends CheckedSet<E> implements SortedSet<E>, Serializable {
        private static final long serialVersionUID = 1599911165492914959L;
        private final SortedSet<E> ss;

        CheckedSortedSet(SortedSet<E> s, Class<E> type) {
            super(s, type);
            this.ss = s;
        }

        public Comparator<? super E> comparator() {
            return this.ss.comparator();
        }

        public E first() {
            return this.ss.first();
        }

        public E last() {
            return this.ss.last();
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            return Collections.checkedSortedSet(this.ss.subSet(fromElement, toElement), this.type);
        }

        public SortedSet<E> headSet(E toElement) {
            return Collections.checkedSortedSet(this.ss.headSet(toElement), this.type);
        }

        public SortedSet<E> tailSet(E fromElement) {
            return Collections.checkedSortedSet(this.ss.tailSet(fromElement), this.type);
        }
    }

    static class CheckedNavigableSet<E> extends CheckedSortedSet<E> implements NavigableSet<E>, Serializable {
        private static final long serialVersionUID = -5429120189805438922L;
        private final NavigableSet<E> ns;

        CheckedNavigableSet(NavigableSet<E> s, Class<E> type) {
            super(s, type);
            this.ns = s;
        }

        public E lower(E e) {
            return this.ns.lower(e);
        }

        public E floor(E e) {
            return this.ns.floor(e);
        }

        public E ceiling(E e) {
            return this.ns.ceiling(e);
        }

        public E higher(E e) {
            return this.ns.higher(e);
        }

        public E pollFirst() {
            return this.ns.pollFirst();
        }

        public E pollLast() {
            return this.ns.pollLast();
        }

        public NavigableSet<E> descendingSet() {
            return Collections.checkedNavigableSet(this.ns.descendingSet(), this.type);
        }

        public Iterator<E> descendingIterator() {
            return Collections.checkedNavigableSet(this.ns.descendingSet(), this.type).iterator();
        }

        public NavigableSet<E> subSet(E fromElement, E toElement) {
            return Collections.checkedNavigableSet(this.ns.subSet(fromElement, true, toElement, false), this.type);
        }

        public NavigableSet<E> headSet(E toElement) {
            return Collections.checkedNavigableSet(this.ns.headSet(toElement, false), this.type);
        }

        public NavigableSet<E> tailSet(E fromElement) {
            return Collections.checkedNavigableSet(this.ns.tailSet(fromElement, true), this.type);
        }

        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return Collections.checkedNavigableSet(this.ns.subSet(fromElement, fromInclusive, toElement, toInclusive), this.type);
        }

        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return Collections.checkedNavigableSet(this.ns.headSet(toElement, inclusive), this.type);
        }

        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return Collections.checkedNavigableSet(this.ns.tailSet(fromElement, inclusive), this.type);
        }
    }

    static class CheckedQueue<E> extends CheckedCollection<E> implements Queue<E>, Serializable {
        private static final long serialVersionUID = 1433151992604707767L;
        final Queue<E> queue;

        CheckedQueue(Queue<E> queue, Class<E> elementType) {
            super(queue, elementType);
            this.queue = queue;
        }

        public E element() {
            return this.queue.element();
        }

        public boolean equals(Object o) {
            return o != this ? this.c.equals(o) : true;
        }

        public int hashCode() {
            return this.c.hashCode();
        }

        public E peek() {
            return this.queue.peek();
        }

        public E poll() {
            return this.queue.poll();
        }

        public E remove() {
            return this.queue.remove();
        }

        public boolean offer(E e) {
            return this.queue.offer(typeCheck(e));
        }
    }

    static class CheckedRandomAccessList<E> extends CheckedList<E> implements RandomAccess {
        private static final long serialVersionUID = 1638200125423088369L;

        CheckedRandomAccessList(List<E> list, Class<E> type) {
            super(list, type);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new CheckedRandomAccessList(this.list.subList(fromIndex, toIndex), this.type);
        }
    }

    private static class CopiesList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        static final /* synthetic */ boolean -assertionsDisabled = (CopiesList.class.desiredAssertionStatus() ^ 1);
        private static final long serialVersionUID = 2739099268398711800L;
        final E element;
        final int n;

        CopiesList(int n, E e) {
            if (-assertionsDisabled || n >= 0) {
                this.n = n;
                this.element = e;
                return;
            }
            throw new AssertionError();
        }

        public int size() {
            return this.n;
        }

        public boolean contains(Object obj) {
            return this.n != 0 ? Collections.eq(obj, this.element) : -assertionsDisabled;
        }

        public int indexOf(Object o) {
            return contains(o) ? 0 : -1;
        }

        public int lastIndexOf(Object o) {
            return contains(o) ? this.n - 1 : -1;
        }

        public E get(int index) {
            if (index >= 0 && index < this.n) {
                return this.element;
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.n);
        }

        public Object[] toArray() {
            Object[] a = new Object[this.n];
            if (this.element != null) {
                Arrays.fill(a, 0, this.n, this.element);
            }
            return a;
        }

        public <T> T[] toArray(T[] a) {
            int n = this.n;
            if (a.length < n) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), n);
                if (this.element != null) {
                    Arrays.fill((Object[]) a, 0, n, this.element);
                }
            } else {
                Arrays.fill((Object[]) a, 0, n, this.element);
                if (a.length > n) {
                    a[n] = null;
                }
            }
            return a;
        }

        public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            } else if (toIndex > this.n) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            } else if (fromIndex <= toIndex) {
                return new CopiesList(toIndex - fromIndex, this.element);
            } else {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
        }

        /* synthetic */ Object lambda$-java_util_Collections$CopiesList_199111(int i) {
            return this.element;
        }

        public Stream<E> stream() {
            return IntStream.range(0, this.n).mapToObj(new java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis.AnonymousClass1((byte) 1, this));
        }

        /* synthetic */ Object lambda$-java_util_Collections$CopiesList_199260(int i) {
            return this.element;
        }

        public Stream<E> parallelStream() {
            return IntStream.range(0, this.n).parallel().mapToObj(new java.util.-$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis.AnonymousClass1((byte) 0, this));
        }

        public Spliterator<E> spliterator() {
            return stream().spliterator();
        }
    }

    private static class EmptyEnumeration<E> implements Enumeration<E> {
        static final EmptyEnumeration<Object> EMPTY_ENUMERATION = new EmptyEnumeration();

        private EmptyEnumeration() {
        }

        public boolean hasMoreElements() {
            return false;
        }

        public E nextElement() {
            throw new NoSuchElementException();
        }
    }

    private static class EmptyIterator<E> implements Iterator<E> {
        static final EmptyIterator<Object> EMPTY_ITERATOR = new EmptyIterator();

        /* synthetic */ EmptyIterator(EmptyIterator -this0) {
            this();
        }

        private EmptyIterator() {
        }

        public boolean hasNext() {
            return false;
        }

        public E next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new IllegalStateException();
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }
    }

    private static class EmptyList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        private static final long serialVersionUID = 8842843931221139166L;

        /* synthetic */ EmptyList(EmptyList -this0) {
            this();
        }

        private EmptyList() {
        }

        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        public ListIterator<E> listIterator() {
            return Collections.emptyListIterator();
        }

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean contains(Object obj) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        public Object[] toArray() {
            return new Object[0];
        }

        public <T> T[] toArray(T[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        public E get(int index) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        public boolean equals(Object o) {
            return o instanceof List ? ((List) o).isEmpty() : false;
        }

        public int hashCode() {
            return 1;
        }

        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            return false;
        }

        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
        }

        public void sort(Comparator<? super E> comparator) {
        }

        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }

        public Spliterator<E> spliterator() {
            return Spliterators.emptySpliterator();
        }

        private Object readResolve() {
            return Collections.EMPTY_LIST;
        }
    }

    private static class EmptyListIterator<E> extends EmptyIterator<E> implements ListIterator<E> {
        static final EmptyListIterator<Object> EMPTY_ITERATOR = new EmptyListIterator();

        private EmptyListIterator() {
            super();
        }

        public boolean hasPrevious() {
            return false;
        }

        public E previous() {
            throw new NoSuchElementException();
        }

        public int nextIndex() {
            return 0;
        }

        public int previousIndex() {
            return -1;
        }

        public void set(E e) {
            throw new IllegalStateException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    private static class EmptyMap<K, V> extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = 6428348081105594320L;

        /* synthetic */ EmptyMap(EmptyMap -this0) {
            this();
        }

        private EmptyMap() {
        }

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }

        public V get(Object key) {
            return null;
        }

        public Set<K> keySet() {
            return Collections.emptySet();
        }

        public Collection<V> values() {
            return Collections.emptySet();
        }

        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        public boolean equals(Object o) {
            return o instanceof Map ? ((Map) o).isEmpty() : false;
        }

        public int hashCode() {
            return 0;
        }

        public V getOrDefault(Object k, V defaultValue) {
            return defaultValue;
        }

        public void forEach(BiConsumer<? super K, ? super V> action) {
            Objects.requireNonNull(action);
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            Objects.requireNonNull(function);
        }

        public V putIfAbsent(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean replace(K k, V v, V v2) {
            throw new UnsupportedOperationException();
        }

        public V replace(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        private Object readResolve() {
            return Collections.EMPTY_MAP;
        }
    }

    private static class EmptySet<E> extends AbstractSet<E> implements Serializable {
        private static final long serialVersionUID = 1582296315990362920L;

        /* synthetic */ EmptySet(EmptySet -this0) {
            this();
        }

        private EmptySet() {
        }

        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean contains(Object obj) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        public Object[] toArray() {
            return new Object[0];
        }

        public <T> T[] toArray(T[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }

        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            return false;
        }

        public Spliterator<E> spliterator() {
            return Spliterators.emptySpliterator();
        }

        private Object readResolve() {
            return Collections.EMPTY_SET;
        }
    }

    private static class ReverseComparator2<T> implements Comparator<T>, Serializable {
        static final /* synthetic */ boolean -assertionsDisabled = (ReverseComparator2.class.desiredAssertionStatus() ^ 1);
        private static final long serialVersionUID = 4374092139857L;
        final Comparator<T> cmp;

        ReverseComparator2(Comparator<T> cmp) {
            if (-assertionsDisabled || cmp != null) {
                this.cmp = cmp;
                return;
            }
            throw new AssertionError();
        }

        public int compare(T t1, T t2) {
            return this.cmp.compare(t2, t1);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof ReverseComparator2) {
                return this.cmp.equals(((ReverseComparator2) o).cmp);
            }
            return -assertionsDisabled;
        }

        public int hashCode() {
            return this.cmp.hashCode() ^ Integer.MIN_VALUE;
        }

        public Comparator<T> reversed() {
            return this.cmp;
        }
    }

    private static class ReverseComparator implements Comparator<Comparable<Object>>, Serializable {
        static final ReverseComparator REVERSE_ORDER = new ReverseComparator();
        private static final long serialVersionUID = 7207038068494060240L;

        private ReverseComparator() {
        }

        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c2.compareTo(c1);
        }

        private Object readResolve() {
            return Collections.reverseOrder();
        }

        public Comparator<Comparable<Object>> reversed() {
            return Comparator.naturalOrder();
        }
    }

    private static class SetFromMap<E> extends AbstractSet<E> implements Set<E>, Serializable {
        private static final long serialVersionUID = 2454657854757543876L;
        private final Map<E, Boolean> m;
        private transient Set<E> s;

        SetFromMap(Map<E, Boolean> map) {
            if (map.isEmpty()) {
                this.m = map;
                this.s = map.keySet();
                return;
            }
            throw new IllegalArgumentException("Map is non-empty");
        }

        public void clear() {
            this.m.clear();
        }

        public int size() {
            return this.m.size();
        }

        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        public boolean contains(Object o) {
            return this.m.containsKey(o);
        }

        public boolean remove(Object o) {
            return this.m.remove(o) != null;
        }

        public boolean add(E e) {
            return this.m.put(e, Boolean.TRUE) == null;
        }

        public Iterator<E> iterator() {
            return this.s.iterator();
        }

        public Object[] toArray() {
            return this.s.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return this.s.toArray(a);
        }

        public String toString() {
            return this.s.toString();
        }

        public int hashCode() {
            return this.s.hashCode();
        }

        public boolean equals(Object o) {
            return o != this ? this.s.equals(o) : true;
        }

        public boolean containsAll(Collection<?> c) {
            return this.s.containsAll(c);
        }

        public boolean removeAll(Collection<?> c) {
            return this.s.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return this.s.retainAll(c);
        }

        public void forEach(Consumer<? super E> action) {
            this.s.forEach(action);
        }

        public boolean removeIf(Predicate<? super E> filter) {
            return this.s.removeIf(filter);
        }

        public Spliterator<E> spliterator() {
            return this.s.spliterator();
        }

        public Stream<E> stream() {
            return this.s.stream();
        }

        public Stream<E> parallelStream() {
            return this.s.parallelStream();
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            this.s = this.m.keySet();
        }
    }

    private static class SingletonList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        private static final long serialVersionUID = 3093736618740652951L;
        private final E element;

        SingletonList(E obj) {
            this.element = obj;
        }

        public Iterator<E> iterator() {
            return Collections.singletonIterator(this.element);
        }

        public int size() {
            return 1;
        }

        public boolean contains(Object obj) {
            return Collections.eq(obj, this.element);
        }

        public E get(int index) {
            if (index == 0) {
                return this.element;
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
        }

        public void forEach(Consumer<? super E> action) {
            action.accept(this.element);
        }

        public boolean removeIf(Predicate<? super E> predicate) {
            throw new UnsupportedOperationException();
        }

        public void replaceAll(UnaryOperator<E> unaryOperator) {
            throw new UnsupportedOperationException();
        }

        public void sort(Comparator<? super E> comparator) {
        }

        public Spliterator<E> spliterator() {
            return Collections.singletonSpliterator(this.element);
        }
    }

    private static class SingletonMap<K, V> extends AbstractMap<K, V> implements Serializable {
        private static final long serialVersionUID = -6979724477215052911L;
        private transient Set<Entry<K, V>> entrySet;
        private final K k;
        private transient Set<K> keySet;
        private final V v;
        private transient Collection<V> values;

        SingletonMap(K key, V value) {
            this.k = key;
            this.v = value;
        }

        public int size() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object key) {
            return Collections.eq(key, this.k);
        }

        public boolean containsValue(Object value) {
            return Collections.eq(value, this.v);
        }

        public V get(Object key) {
            return Collections.eq(key, this.k) ? this.v : null;
        }

        public Set<K> keySet() {
            if (this.keySet == null) {
                this.keySet = Collections.singleton(this.k);
            }
            return this.keySet;
        }

        public Set<Entry<K, V>> entrySet() {
            if (this.entrySet == null) {
                this.entrySet = Collections.singleton(new SimpleImmutableEntry(this.k, this.v));
            }
            return this.entrySet;
        }

        public Collection<V> values() {
            if (this.values == null) {
                this.values = Collections.singleton(this.v);
            }
            return this.values;
        }

        public V getOrDefault(Object key, V defaultValue) {
            return Collections.eq(key, this.k) ? this.v : defaultValue;
        }

        public void forEach(BiConsumer<? super K, ? super V> action) {
            action.accept(this.k, this.v);
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V putIfAbsent(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean replace(K k, V v, V v2) {
            throw new UnsupportedOperationException();
        }

        public V replace(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SingletonSet<E> extends AbstractSet<E> implements Serializable {
        private static final long serialVersionUID = 3193687207550431679L;
        private final E element;

        SingletonSet(E e) {
            this.element = e;
        }

        public Iterator<E> iterator() {
            return Collections.singletonIterator(this.element);
        }

        public int size() {
            return 1;
        }

        public boolean contains(Object o) {
            return Collections.eq(o, this.element);
        }

        public void forEach(Consumer<? super E> action) {
            action.accept(this.element);
        }

        public Spliterator<E> spliterator() {
            return Collections.singletonSpliterator(this.element);
        }

        public boolean removeIf(Predicate<? super E> predicate) {
            throw new UnsupportedOperationException();
        }
    }

    static class SynchronizedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 3053995032091335093L;
        final Collection<E> c;
        final Object mutex;

        SynchronizedCollection(Collection<E> c) {
            this.c = (Collection) Objects.requireNonNull(c);
            this.mutex = this;
        }

        SynchronizedCollection(Collection<E> c, Object mutex) {
            this.c = (Collection) Objects.requireNonNull(c);
            this.mutex = Objects.requireNonNull(mutex);
        }

        public int size() {
            int size;
            synchronized (this.mutex) {
                size = this.c.size();
            }
            return size;
        }

        public boolean isEmpty() {
            boolean isEmpty;
            synchronized (this.mutex) {
                isEmpty = this.c.isEmpty();
            }
            return isEmpty;
        }

        public boolean contains(Object o) {
            boolean contains;
            synchronized (this.mutex) {
                contains = this.c.contains(o);
            }
            return contains;
        }

        public Object[] toArray() {
            Object[] toArray;
            synchronized (this.mutex) {
                toArray = this.c.toArray();
            }
            return toArray;
        }

        public <T> T[] toArray(T[] a) {
            T[] toArray;
            synchronized (this.mutex) {
                toArray = this.c.toArray(a);
            }
            return toArray;
        }

        public Iterator<E> iterator() {
            return this.c.iterator();
        }

        public boolean add(E e) {
            boolean add;
            synchronized (this.mutex) {
                add = this.c.add(e);
            }
            return add;
        }

        public boolean remove(Object o) {
            boolean remove;
            synchronized (this.mutex) {
                remove = this.c.remove(o);
            }
            return remove;
        }

        public boolean containsAll(Collection<?> coll) {
            boolean containsAll;
            synchronized (this.mutex) {
                containsAll = this.c.containsAll(coll);
            }
            return containsAll;
        }

        public boolean addAll(Collection<? extends E> coll) {
            boolean addAll;
            synchronized (this.mutex) {
                addAll = this.c.addAll(coll);
            }
            return addAll;
        }

        public boolean removeAll(Collection<?> coll) {
            boolean removeAll;
            synchronized (this.mutex) {
                removeAll = this.c.removeAll(coll);
            }
            return removeAll;
        }

        public boolean retainAll(Collection<?> coll) {
            boolean retainAll;
            synchronized (this.mutex) {
                retainAll = this.c.retainAll(coll);
            }
            return retainAll;
        }

        public void clear() {
            synchronized (this.mutex) {
                this.c.clear();
            }
        }

        public String toString() {
            String obj;
            synchronized (this.mutex) {
                obj = this.c.toString();
            }
            return obj;
        }

        public void forEach(Consumer<? super E> consumer) {
            synchronized (this.mutex) {
                this.c.forEach(consumer);
            }
        }

        public boolean removeIf(Predicate<? super E> filter) {
            boolean removeIf;
            synchronized (this.mutex) {
                removeIf = this.c.removeIf(filter);
            }
            return removeIf;
        }

        public Spliterator<E> spliterator() {
            return this.c.spliterator();
        }

        public Stream<E> stream() {
            return this.c.stream();
        }

        public Stream<E> parallelStream() {
            return this.c.parallelStream();
        }

        private void writeObject(ObjectOutputStream s) throws IOException {
            synchronized (this.mutex) {
                s.defaultWriteObject();
            }
        }
    }

    static class SynchronizedList<E> extends SynchronizedCollection<E> implements List<E> {
        private static final long serialVersionUID = -7754090372962971524L;
        final List<E> list;

        SynchronizedList(List<E> list) {
            super(list);
            this.list = list;
        }

        SynchronizedList(List<E> list, Object mutex) {
            super(list, mutex);
            this.list = list;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = this.list.equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = this.list.hashCode();
            }
            return hashCode;
        }

        public E get(int index) {
            E e;
            synchronized (this.mutex) {
                e = this.list.get(index);
            }
            return e;
        }

        public E set(int index, E element) {
            E e;
            synchronized (this.mutex) {
                e = this.list.set(index, element);
            }
            return e;
        }

        public void add(int index, E element) {
            synchronized (this.mutex) {
                this.list.add(index, element);
            }
        }

        public E remove(int index) {
            E remove;
            synchronized (this.mutex) {
                remove = this.list.remove(index);
            }
            return remove;
        }

        public int indexOf(Object o) {
            int indexOf;
            synchronized (this.mutex) {
                indexOf = this.list.indexOf(o);
            }
            return indexOf;
        }

        public int lastIndexOf(Object o) {
            int lastIndexOf;
            synchronized (this.mutex) {
                lastIndexOf = this.list.lastIndexOf(o);
            }
            return lastIndexOf;
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            boolean addAll;
            synchronized (this.mutex) {
                addAll = this.list.addAll(index, c);
            }
            return addAll;
        }

        public ListIterator<E> listIterator() {
            return this.list.listIterator();
        }

        public ListIterator<E> listIterator(int index) {
            return this.list.listIterator(index);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            List synchronizedList;
            synchronized (this.mutex) {
                synchronizedList = new SynchronizedList(this.list.subList(fromIndex, toIndex), this.mutex);
            }
            return synchronizedList;
        }

        public void replaceAll(UnaryOperator<E> operator) {
            synchronized (this.mutex) {
                this.list.replaceAll(operator);
            }
        }

        public void sort(Comparator<? super E> c) {
            synchronized (this.mutex) {
                this.list.sort(c);
            }
        }

        private Object readResolve() {
            if (this.list instanceof RandomAccess) {
                return new SynchronizedRandomAccessList(this.list);
            }
            return this;
        }
    }

    private static class SynchronizedMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = 1978198479659022715L;
        private transient Set<Entry<K, V>> entrySet;
        private transient Set<K> keySet;
        private final Map<K, V> m;
        final Object mutex;
        private transient Collection<V> values;

        SynchronizedMap(Map<K, V> m) {
            this.m = (Map) Objects.requireNonNull(m);
            this.mutex = this;
        }

        SynchronizedMap(Map<K, V> m, Object mutex) {
            this.m = m;
            this.mutex = mutex;
        }

        public int size() {
            int size;
            synchronized (this.mutex) {
                size = this.m.size();
            }
            return size;
        }

        public boolean isEmpty() {
            boolean isEmpty;
            synchronized (this.mutex) {
                isEmpty = this.m.isEmpty();
            }
            return isEmpty;
        }

        public boolean containsKey(Object key) {
            boolean containsKey;
            synchronized (this.mutex) {
                containsKey = this.m.containsKey(key);
            }
            return containsKey;
        }

        public boolean containsValue(Object value) {
            boolean containsValue;
            synchronized (this.mutex) {
                containsValue = this.m.containsValue(value);
            }
            return containsValue;
        }

        public V get(Object key) {
            V v;
            synchronized (this.mutex) {
                v = this.m.get(key);
            }
            return v;
        }

        public V put(K key, V value) {
            V put;
            synchronized (this.mutex) {
                put = this.m.put(key, value);
            }
            return put;
        }

        public V remove(Object key) {
            V remove;
            synchronized (this.mutex) {
                remove = this.m.remove(key);
            }
            return remove;
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            synchronized (this.mutex) {
                this.m.putAll(map);
            }
        }

        public void clear() {
            synchronized (this.mutex) {
                this.m.clear();
            }
        }

        public Set<K> keySet() {
            Set<K> set;
            synchronized (this.mutex) {
                if (this.keySet == null) {
                    this.keySet = new SynchronizedSet(this.m.keySet(), this.mutex);
                }
                set = this.keySet;
            }
            return set;
        }

        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> set;
            synchronized (this.mutex) {
                if (this.entrySet == null) {
                    this.entrySet = new SynchronizedSet(this.m.entrySet(), this.mutex);
                }
                set = this.entrySet;
            }
            return set;
        }

        public Collection<V> values() {
            Collection<V> collection;
            synchronized (this.mutex) {
                if (this.values == null) {
                    this.values = new SynchronizedCollection(this.m.values(), this.mutex);
                }
                collection = this.values;
            }
            return collection;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = this.m.equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = this.m.hashCode();
            }
            return hashCode;
        }

        public String toString() {
            String obj;
            synchronized (this.mutex) {
                obj = this.m.toString();
            }
            return obj;
        }

        public V getOrDefault(Object k, V defaultValue) {
            V orDefault;
            synchronized (this.mutex) {
                orDefault = this.m.getOrDefault(k, defaultValue);
            }
            return orDefault;
        }

        public void forEach(BiConsumer<? super K, ? super V> action) {
            synchronized (this.mutex) {
                this.m.forEach(action);
            }
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            synchronized (this.mutex) {
                this.m.replaceAll(function);
            }
        }

        public V putIfAbsent(K key, V value) {
            V putIfAbsent;
            synchronized (this.mutex) {
                putIfAbsent = this.m.putIfAbsent(key, value);
            }
            return putIfAbsent;
        }

        public boolean remove(Object key, Object value) {
            boolean remove;
            synchronized (this.mutex) {
                remove = this.m.remove(key, value);
            }
            return remove;
        }

        public boolean replace(K key, V oldValue, V newValue) {
            boolean replace;
            synchronized (this.mutex) {
                replace = this.m.replace(key, oldValue, newValue);
            }
            return replace;
        }

        public V replace(K key, V value) {
            V replace;
            synchronized (this.mutex) {
                replace = this.m.replace(key, value);
            }
            return replace;
        }

        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            V computeIfAbsent;
            synchronized (this.mutex) {
                computeIfAbsent = this.m.computeIfAbsent(key, mappingFunction);
            }
            return computeIfAbsent;
        }

        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            V computeIfPresent;
            synchronized (this.mutex) {
                computeIfPresent = this.m.computeIfPresent(key, remappingFunction);
            }
            return computeIfPresent;
        }

        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            V compute;
            synchronized (this.mutex) {
                compute = this.m.compute(key, remappingFunction);
            }
            return compute;
        }

        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            V merge;
            synchronized (this.mutex) {
                merge = this.m.merge(key, value, remappingFunction);
            }
            return merge;
        }

        private void writeObject(ObjectOutputStream s) throws IOException {
            synchronized (this.mutex) {
                s.defaultWriteObject();
            }
        }
    }

    static class SynchronizedSortedMap<K, V> extends SynchronizedMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = -8798146769416483793L;
        private final SortedMap<K, V> sm;

        SynchronizedSortedMap(SortedMap<K, V> m) {
            super(m);
            this.sm = m;
        }

        SynchronizedSortedMap(SortedMap<K, V> m, Object mutex) {
            super(m, mutex);
            this.sm = m;
        }

        public Comparator<? super K> comparator() {
            Comparator<? super K> comparator;
            synchronized (this.mutex) {
                comparator = this.sm.comparator();
            }
            return comparator;
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            SortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.subMap(fromKey, toKey), this.mutex);
            }
            return synchronizedSortedMap;
        }

        public SortedMap<K, V> headMap(K toKey) {
            SortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.headMap(toKey), this.mutex);
            }
            return synchronizedSortedMap;
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            SortedMap synchronizedSortedMap;
            synchronized (this.mutex) {
                synchronizedSortedMap = new SynchronizedSortedMap(this.sm.tailMap(fromKey), this.mutex);
            }
            return synchronizedSortedMap;
        }

        public K firstKey() {
            K firstKey;
            synchronized (this.mutex) {
                firstKey = this.sm.firstKey();
            }
            return firstKey;
        }

        public K lastKey() {
            K lastKey;
            synchronized (this.mutex) {
                lastKey = this.sm.lastKey();
            }
            return lastKey;
        }
    }

    static class SynchronizedNavigableMap<K, V> extends SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {
        private static final long serialVersionUID = 699392247599746807L;
        private final NavigableMap<K, V> nm;

        SynchronizedNavigableMap(NavigableMap<K, V> m) {
            super(m);
            this.nm = m;
        }

        SynchronizedNavigableMap(NavigableMap<K, V> m, Object mutex) {
            super(m, mutex);
            this.nm = m;
        }

        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> lowerEntry;
            synchronized (this.mutex) {
                lowerEntry = this.nm.lowerEntry(key);
            }
            return lowerEntry;
        }

        public K lowerKey(K key) {
            K lowerKey;
            synchronized (this.mutex) {
                lowerKey = this.nm.lowerKey(key);
            }
            return lowerKey;
        }

        public Entry<K, V> floorEntry(K key) {
            Entry<K, V> floorEntry;
            synchronized (this.mutex) {
                floorEntry = this.nm.floorEntry(key);
            }
            return floorEntry;
        }

        public K floorKey(K key) {
            K floorKey;
            synchronized (this.mutex) {
                floorKey = this.nm.floorKey(key);
            }
            return floorKey;
        }

        public Entry<K, V> ceilingEntry(K key) {
            Entry<K, V> ceilingEntry;
            synchronized (this.mutex) {
                ceilingEntry = this.nm.ceilingEntry(key);
            }
            return ceilingEntry;
        }

        public K ceilingKey(K key) {
            K ceilingKey;
            synchronized (this.mutex) {
                ceilingKey = this.nm.ceilingKey(key);
            }
            return ceilingKey;
        }

        public Entry<K, V> higherEntry(K key) {
            Entry<K, V> higherEntry;
            synchronized (this.mutex) {
                higherEntry = this.nm.higherEntry(key);
            }
            return higherEntry;
        }

        public K higherKey(K key) {
            K higherKey;
            synchronized (this.mutex) {
                higherKey = this.nm.higherKey(key);
            }
            return higherKey;
        }

        public Entry<K, V> firstEntry() {
            Entry<K, V> firstEntry;
            synchronized (this.mutex) {
                firstEntry = this.nm.firstEntry();
            }
            return firstEntry;
        }

        public Entry<K, V> lastEntry() {
            Entry<K, V> lastEntry;
            synchronized (this.mutex) {
                lastEntry = this.nm.lastEntry();
            }
            return lastEntry;
        }

        public Entry<K, V> pollFirstEntry() {
            Entry<K, V> pollFirstEntry;
            synchronized (this.mutex) {
                pollFirstEntry = this.nm.pollFirstEntry();
            }
            return pollFirstEntry;
        }

        public Entry<K, V> pollLastEntry() {
            Entry<K, V> pollLastEntry;
            synchronized (this.mutex) {
                pollLastEntry = this.nm.pollLastEntry();
            }
            return pollLastEntry;
        }

        public NavigableMap<K, V> descendingMap() {
            NavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.descendingMap(), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        public NavigableSet<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> navigableKeySet() {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.nm.navigableKeySet(), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public NavigableSet<K> descendingKeySet() {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.nm.descendingKeySet(), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            SortedMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.subMap(fromKey, true, toKey, false), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        public SortedMap<K, V> headMap(K toKey) {
            SortedMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.headMap(toKey, false), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            SortedMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.tailMap(fromKey, true), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            NavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.subMap(fromKey, fromInclusive, toKey, toInclusive), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            NavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.headMap(toKey, inclusive), this.mutex);
            }
            return synchronizedNavigableMap;
        }

        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            NavigableMap synchronizedNavigableMap;
            synchronized (this.mutex) {
                synchronizedNavigableMap = new SynchronizedNavigableMap(this.nm.tailMap(fromKey, inclusive), this.mutex);
            }
            return synchronizedNavigableMap;
        }
    }

    static class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {
        private static final long serialVersionUID = 487447009682186044L;

        SynchronizedSet(Set<E> s) {
            super(s);
        }

        SynchronizedSet(Set<E> s, Object mutex) {
            super(s, mutex);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = this.c.equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = this.c.hashCode();
            }
            return hashCode;
        }
    }

    static class SynchronizedSortedSet<E> extends SynchronizedSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = 8695801310862127406L;
        private final SortedSet<E> ss;

        SynchronizedSortedSet(SortedSet<E> s) {
            super(s);
            this.ss = s;
        }

        SynchronizedSortedSet(SortedSet<E> s, Object mutex) {
            super(s, mutex);
            this.ss = s;
        }

        public Comparator<? super E> comparator() {
            Comparator<? super E> comparator;
            synchronized (this.mutex) {
                comparator = this.ss.comparator();
            }
            return comparator;
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            SortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.subSet(fromElement, toElement), this.mutex);
            }
            return synchronizedSortedSet;
        }

        public SortedSet<E> headSet(E toElement) {
            SortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.headSet(toElement), this.mutex);
            }
            return synchronizedSortedSet;
        }

        public SortedSet<E> tailSet(E fromElement) {
            SortedSet synchronizedSortedSet;
            synchronized (this.mutex) {
                synchronizedSortedSet = new SynchronizedSortedSet(this.ss.tailSet(fromElement), this.mutex);
            }
            return synchronizedSortedSet;
        }

        public E first() {
            E first;
            synchronized (this.mutex) {
                first = this.ss.first();
            }
            return first;
        }

        public E last() {
            E last;
            synchronized (this.mutex) {
                last = this.ss.last();
            }
            return last;
        }
    }

    static class SynchronizedNavigableSet<E> extends SynchronizedSortedSet<E> implements NavigableSet<E> {
        private static final long serialVersionUID = -5505529816273629798L;
        private final NavigableSet<E> ns;

        SynchronizedNavigableSet(NavigableSet<E> s) {
            super(s);
            this.ns = s;
        }

        SynchronizedNavigableSet(NavigableSet<E> s, Object mutex) {
            super(s, mutex);
            this.ns = s;
        }

        public E lower(E e) {
            E lower;
            synchronized (this.mutex) {
                lower = this.ns.lower(e);
            }
            return lower;
        }

        public E floor(E e) {
            E floor;
            synchronized (this.mutex) {
                floor = this.ns.floor(e);
            }
            return floor;
        }

        public E ceiling(E e) {
            E ceiling;
            synchronized (this.mutex) {
                ceiling = this.ns.ceiling(e);
            }
            return ceiling;
        }

        public E higher(E e) {
            E higher;
            synchronized (this.mutex) {
                higher = this.ns.higher(e);
            }
            return higher;
        }

        public E pollFirst() {
            E pollFirst;
            synchronized (this.mutex) {
                pollFirst = this.ns.pollFirst();
            }
            return pollFirst;
        }

        public E pollLast() {
            E pollLast;
            synchronized (this.mutex) {
                pollLast = this.ns.pollLast();
            }
            return pollLast;
        }

        public NavigableSet<E> descendingSet() {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.descendingSet(), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public Iterator<E> descendingIterator() {
            Iterator<E> it;
            synchronized (this.mutex) {
                it = descendingSet().iterator();
            }
            return it;
        }

        public NavigableSet<E> subSet(E fromElement, E toElement) {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.subSet(fromElement, true, toElement, false), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public NavigableSet<E> headSet(E toElement) {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.headSet(toElement, false), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public NavigableSet<E> tailSet(E fromElement) {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.tailSet(fromElement, true), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.subSet(fromElement, fromInclusive, toElement, toInclusive), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.headSet(toElement, inclusive), this.mutex);
            }
            return synchronizedNavigableSet;
        }

        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            NavigableSet synchronizedNavigableSet;
            synchronized (this.mutex) {
                synchronizedNavigableSet = new SynchronizedNavigableSet(this.ns.tailSet(fromElement, inclusive), this.mutex);
            }
            return synchronizedNavigableSet;
        }
    }

    static class SynchronizedRandomAccessList<E> extends SynchronizedList<E> implements RandomAccess {
        private static final long serialVersionUID = 1530674583602358482L;

        SynchronizedRandomAccessList(List<E> list) {
            super(list);
        }

        SynchronizedRandomAccessList(List<E> list, Object mutex) {
            super(list, mutex);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            List synchronizedRandomAccessList;
            synchronized (this.mutex) {
                synchronizedRandomAccessList = new SynchronizedRandomAccessList(this.list.subList(fromIndex, toIndex), this.mutex);
            }
            return synchronizedRandomAccessList;
        }

        private Object writeReplace() {
            return new SynchronizedList(this.list);
        }
    }

    static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1820017752578914078L;
        final Collection<? extends E> c;

        UnmodifiableCollection(Collection<? extends E> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            this.c = c;
        }

        public int size() {
            return this.c.size();
        }

        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        public boolean contains(Object o) {
            return this.c.contains(o);
        }

        public Object[] toArray() {
            return this.c.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return this.c.toArray(a);
        }

        public String toString() {
            return this.c.toString();
        }

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i = UnmodifiableCollection.this.c.iterator();

                public boolean hasNext() {
                    return this.i.hasNext();
                }

                public E next() {
                    return this.i.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                public void forEachRemaining(Consumer<? super E> action) {
                    this.i.forEachRemaining(action);
                }
            };
        }

        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> coll) {
            return this.c.containsAll(coll);
        }

        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public void forEach(Consumer<? super E> action) {
            this.c.forEach(action);
        }

        public boolean removeIf(Predicate<? super E> predicate) {
            throw new UnsupportedOperationException();
        }

        public Spliterator<E> spliterator() {
            return this.c.spliterator();
        }

        public Stream<E> stream() {
            return this.c.stream();
        }

        public Stream<E> parallelStream() {
            return this.c.parallelStream();
        }
    }

    static class UnmodifiableList<E> extends UnmodifiableCollection<E> implements List<E> {
        private static final long serialVersionUID = -283967356065247728L;
        final List<? extends E> list;

        UnmodifiableList(List<? extends E> list) {
            super(list);
            this.list = list;
        }

        public boolean equals(Object o) {
            return o != this ? this.list.equals(o) : true;
        }

        public int hashCode() {
            return this.list.hashCode();
        }

        public E get(int index) {
            return this.list.get(index);
        }

        public E set(int index, E e) {
            throw new UnsupportedOperationException();
        }

        public void add(int index, E e) {
            throw new UnsupportedOperationException();
        }

        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        public int indexOf(Object o) {
            return this.list.indexOf(o);
        }

        public int lastIndexOf(Object o) {
            return this.list.lastIndexOf(o);
        }

        public boolean addAll(int index, Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        public void replaceAll(UnaryOperator<E> unaryOperator) {
            throw new UnsupportedOperationException();
        }

        public void sort(Comparator<? super E> comparator) {
            throw new UnsupportedOperationException();
        }

        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        public ListIterator<E> listIterator(final int index) {
            return new ListIterator<E>() {
                private final ListIterator<? extends E> i = UnmodifiableList.this.list.listIterator(index);

                public boolean hasNext() {
                    return this.i.hasNext();
                }

                public E next() {
                    return this.i.next();
                }

                public boolean hasPrevious() {
                    return this.i.hasPrevious();
                }

                public E previous() {
                    return this.i.previous();
                }

                public int nextIndex() {
                    return this.i.nextIndex();
                }

                public int previousIndex() {
                    return this.i.previousIndex();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                public void set(E e) {
                    throw new UnsupportedOperationException();
                }

                public void add(E e) {
                    throw new UnsupportedOperationException();
                }

                public void forEachRemaining(Consumer<? super E> action) {
                    this.i.forEachRemaining(action);
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableList(this.list.subList(fromIndex, toIndex));
        }

        private Object readResolve() {
            if (this.list instanceof RandomAccess) {
                return new UnmodifiableRandomAccessList(this.list);
            }
            return this;
        }
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E>, Serializable {
        private static final long serialVersionUID = -9215047833775013803L;

        UnmodifiableSet(Set<? extends E> s) {
            super(s);
        }

        public boolean equals(Object o) {
            return o != this ? this.c.equals(o) : true;
        }

        public int hashCode() {
            return this.c.hashCode();
        }
    }

    private static class UnmodifiableMap<K, V> implements Map<K, V>, Serializable {
        private static final long serialVersionUID = -1034234728574286014L;
        private transient Set<Entry<K, V>> entrySet;
        private transient Set<K> keySet;
        private final Map<? extends K, ? extends V> m;
        private transient Collection<V> values;

        static class UnmodifiableEntrySet<K, V> extends UnmodifiableSet<Entry<K, V>> {
            private static final long serialVersionUID = 7854390611657943733L;

            private static class UnmodifiableEntry<K, V> implements Entry<K, V> {
                private Entry<? extends K, ? extends V> e;

                UnmodifiableEntry(Entry<? extends K, ? extends V> e) {
                    this.e = (Entry) Objects.requireNonNull(e);
                }

                public K getKey() {
                    return this.e.getKey();
                }

                public V getValue() {
                    return this.e.getValue();
                }

                public V setValue(V v) {
                    throw new UnsupportedOperationException();
                }

                public int hashCode() {
                    return this.e.hashCode();
                }

                public boolean equals(Object o) {
                    boolean z = false;
                    if (this == o) {
                        return true;
                    }
                    if (!(o instanceof Entry)) {
                        return false;
                    }
                    Entry<?, ?> t = (Entry) o;
                    if (Collections.eq(this.e.getKey(), t.getKey())) {
                        z = Collections.eq(this.e.getValue(), t.getValue());
                    }
                    return z;
                }

                public String toString() {
                    return this.e.toString();
                }
            }

            static final class UnmodifiableEntrySetSpliterator<K, V> implements Spliterator<Entry<K, V>> {
                final Spliterator<Entry<K, V>> s;

                UnmodifiableEntrySetSpliterator(Spliterator<Entry<K, V>> s) {
                    this.s = s;
                }

                public boolean tryAdvance(Consumer<? super Entry<K, V>> action) {
                    Objects.requireNonNull(action);
                    return this.s.tryAdvance(UnmodifiableEntrySet.entryConsumer(action));
                }

                public void forEachRemaining(Consumer<? super Entry<K, V>> action) {
                    Objects.requireNonNull(action);
                    this.s.forEachRemaining(UnmodifiableEntrySet.entryConsumer(action));
                }

                public Spliterator<Entry<K, V>> trySplit() {
                    Spliterator<Entry<K, V>> split = this.s.trySplit();
                    if (split == null) {
                        return null;
                    }
                    return new UnmodifiableEntrySetSpliterator(split);
                }

                public long estimateSize() {
                    return this.s.estimateSize();
                }

                public long getExactSizeIfKnown() {
                    return this.s.getExactSizeIfKnown();
                }

                public int characteristics() {
                    return this.s.characteristics();
                }

                public boolean hasCharacteristics(int characteristics) {
                    return this.s.hasCharacteristics(characteristics);
                }

                public Comparator<? super Entry<K, V>> getComparator() {
                    return this.s.getComparator();
                }
            }

            UnmodifiableEntrySet(Set<? extends Entry<? extends K, ? extends V>> s) {
                super(s);
            }

            static <K, V> Consumer<Entry<K, V>> entryConsumer(Consumer<? super Entry<K, V>> action) {
                return new -$Lambda$i2v6-5RQNI3YGcqf1AP8d7D8zis(action);
            }

            public void forEach(Consumer<? super Entry<K, V>> action) {
                Objects.requireNonNull(action);
                this.c.forEach(entryConsumer(action));
            }

            public Spliterator<Entry<K, V>> spliterator() {
                return new UnmodifiableEntrySetSpliterator(this.c.spliterator());
            }

            public Stream<Entry<K, V>> stream() {
                return StreamSupport.stream(spliterator(), false);
            }

            public Stream<Entry<K, V>> parallelStream() {
                return StreamSupport.stream(spliterator(), true);
            }

            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    private final Iterator<? extends Entry<? extends K, ? extends V>> i = UnmodifiableEntrySet.this.c.iterator();

                    public boolean hasNext() {
                        return this.i.hasNext();
                    }

                    public Entry<K, V> next() {
                        return new UnmodifiableEntry((Entry) this.i.next());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            public Object[] toArray() {
                Object[] a = this.c.toArray();
                for (int i = 0; i < a.length; i++) {
                    a[i] = new UnmodifiableEntry((Entry) a[i]);
                }
                return a;
            }

            public <T> T[] toArray(T[] a) {
                Object arr = this.c.toArray(a.length == 0 ? a : Arrays.copyOf((Object[]) a, 0));
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = new UnmodifiableEntry((Entry) arr[i]);
                }
                if (arr.length > a.length) {
                    return arr;
                }
                System.arraycopy(arr, 0, (Object) a, 0, arr.length);
                if (a.length > arr.length) {
                    a[arr.length] = null;
                }
                return a;
            }

            public boolean contains(Object o) {
                if (o instanceof Entry) {
                    return this.c.contains(new UnmodifiableEntry((Entry) o));
                }
                return false;
            }

            public boolean containsAll(Collection<?> coll) {
                for (Object e : coll) {
                    if (!contains(e)) {
                        return false;
                    }
                }
                return true;
            }

            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (!(o instanceof Set)) {
                    return false;
                }
                Set<?> s = (Set) o;
                if (s.size() != this.c.size()) {
                    return false;
                }
                return containsAll(s);
            }
        }

        UnmodifiableMap(Map<? extends K, ? extends V> m) {
            if (m == null) {
                throw new NullPointerException();
            }
            this.m = m;
        }

        public int size() {
            return this.m.size();
        }

        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        public boolean containsKey(Object key) {
            return this.m.containsKey(key);
        }

        public boolean containsValue(Object val) {
            return this.m.containsValue(val);
        }

        public V get(Object key) {
            return this.m.get(key);
        }

        public V put(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public Set<K> keySet() {
            if (this.keySet == null) {
                this.keySet = Collections.unmodifiableSet(this.m.keySet());
            }
            return this.keySet;
        }

        public Set<Entry<K, V>> entrySet() {
            if (this.entrySet == null) {
                this.entrySet = new UnmodifiableEntrySet(this.m.entrySet());
            }
            return this.entrySet;
        }

        public Collection<V> values() {
            if (this.values == null) {
                this.values = Collections.unmodifiableCollection(this.m.values());
            }
            return this.values;
        }

        public boolean equals(Object o) {
            return o != this ? this.m.equals(o) : true;
        }

        public int hashCode() {
            return this.m.hashCode();
        }

        public String toString() {
            return this.m.toString();
        }

        public V getOrDefault(Object k, V defaultValue) {
            return this.m.getOrDefault(k, defaultValue);
        }

        public void forEach(BiConsumer<? super K, ? super V> action) {
            this.m.forEach(action);
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V putIfAbsent(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean replace(K k, V v, V v2) {
            throw new UnsupportedOperationException();
        }

        public V replace(K k, V v) {
            throw new UnsupportedOperationException();
        }

        public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }

        public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
            throw new UnsupportedOperationException();
        }
    }

    static class UnmodifiableSortedMap<K, V> extends UnmodifiableMap<K, V> implements SortedMap<K, V>, Serializable {
        private static final long serialVersionUID = -8806743815996713206L;
        private final SortedMap<K, ? extends V> sm;

        UnmodifiableSortedMap(SortedMap<K, ? extends V> m) {
            super(m);
            this.sm = m;
        }

        public Comparator<? super K> comparator() {
            return this.sm.comparator();
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return new UnmodifiableSortedMap(this.sm.subMap(fromKey, toKey));
        }

        public SortedMap<K, V> headMap(K toKey) {
            return new UnmodifiableSortedMap(this.sm.headMap(toKey));
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            return new UnmodifiableSortedMap(this.sm.tailMap(fromKey));
        }

        public K firstKey() {
            return this.sm.firstKey();
        }

        public K lastKey() {
            return this.sm.lastKey();
        }
    }

    static class UnmodifiableNavigableMap<K, V> extends UnmodifiableSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
        private static final EmptyNavigableMap<?, ?> EMPTY_NAVIGABLE_MAP = new EmptyNavigableMap();
        private static final long serialVersionUID = -4858195264774772197L;
        private final NavigableMap<K, ? extends V> nm;

        private static class EmptyNavigableMap<K, V> extends UnmodifiableNavigableMap<K, V> implements Serializable {
            private static final long serialVersionUID = -2239321462712562324L;

            EmptyNavigableMap() {
                super(new TreeMap());
            }

            public NavigableSet<K> navigableKeySet() {
                return Collections.emptyNavigableSet();
            }

            private Object readResolve() {
                return UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
            }
        }

        UnmodifiableNavigableMap(NavigableMap<K, ? extends V> m) {
            super(m);
            this.nm = m;
        }

        public K lowerKey(K key) {
            return this.nm.lowerKey(key);
        }

        public K floorKey(K key) {
            return this.nm.floorKey(key);
        }

        public K ceilingKey(K key) {
            return this.nm.ceilingKey(key);
        }

        public K higherKey(K key) {
            return this.nm.higherKey(key);
        }

        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> lower = this.nm.lowerEntry(key);
            if (lower != null) {
                return new UnmodifiableEntry(lower);
            }
            return null;
        }

        public Entry<K, V> floorEntry(K key) {
            Entry<K, V> floor = this.nm.floorEntry(key);
            if (floor != null) {
                return new UnmodifiableEntry(floor);
            }
            return null;
        }

        public Entry<K, V> ceilingEntry(K key) {
            Entry<K, V> ceiling = this.nm.ceilingEntry(key);
            if (ceiling != null) {
                return new UnmodifiableEntry(ceiling);
            }
            return null;
        }

        public Entry<K, V> higherEntry(K key) {
            Entry<K, V> higher = this.nm.higherEntry(key);
            if (higher != null) {
                return new UnmodifiableEntry(higher);
            }
            return null;
        }

        public Entry<K, V> firstEntry() {
            Entry<K, V> first = this.nm.firstEntry();
            if (first != null) {
                return new UnmodifiableEntry(first);
            }
            return null;
        }

        public Entry<K, V> lastEntry() {
            Entry<K, V> last = this.nm.lastEntry();
            if (last != null) {
                return new UnmodifiableEntry(last);
            }
            return null;
        }

        public Entry<K, V> pollFirstEntry() {
            throw new UnsupportedOperationException();
        }

        public Entry<K, V> pollLastEntry() {
            throw new UnsupportedOperationException();
        }

        public NavigableMap<K, V> descendingMap() {
            return Collections.unmodifiableNavigableMap(this.nm.descendingMap());
        }

        public NavigableSet<K> navigableKeySet() {
            return Collections.unmodifiableNavigableSet(this.nm.navigableKeySet());
        }

        public NavigableSet<K> descendingKeySet() {
            return Collections.unmodifiableNavigableSet(this.nm.descendingKeySet());
        }

        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return Collections.unmodifiableNavigableMap(this.nm.subMap(fromKey, fromInclusive, toKey, toInclusive));
        }

        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return Collections.unmodifiableNavigableMap(this.nm.headMap(toKey, inclusive));
        }

        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return Collections.unmodifiableNavigableMap(this.nm.tailMap(fromKey, inclusive));
        }
    }

    static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> implements SortedSet<E>, Serializable {
        private static final long serialVersionUID = -4929149591599911165L;
        private final SortedSet<E> ss;

        UnmodifiableSortedSet(SortedSet<E> s) {
            super(s);
            this.ss = s;
        }

        public Comparator<? super E> comparator() {
            return this.ss.comparator();
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            return new UnmodifiableSortedSet(this.ss.subSet(fromElement, toElement));
        }

        public SortedSet<E> headSet(E toElement) {
            return new UnmodifiableSortedSet(this.ss.headSet(toElement));
        }

        public SortedSet<E> tailSet(E fromElement) {
            return new UnmodifiableSortedSet(this.ss.tailSet(fromElement));
        }

        public E first() {
            return this.ss.first();
        }

        public E last() {
            return this.ss.last();
        }
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements NavigableSet<E>, Serializable {
        private static final NavigableSet<?> EMPTY_NAVIGABLE_SET = new EmptyNavigableSet();
        private static final long serialVersionUID = -6027448201786391929L;
        private final NavigableSet<E> ns;

        private static class EmptyNavigableSet<E> extends UnmodifiableNavigableSet<E> implements Serializable {
            private static final long serialVersionUID = -6291252904449939134L;

            public EmptyNavigableSet() {
                super(new TreeSet());
            }

            private Object readResolve() {
                return UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
            }
        }

        UnmodifiableNavigableSet(NavigableSet<E> s) {
            super(s);
            this.ns = s;
        }

        public E lower(E e) {
            return this.ns.lower(e);
        }

        public E floor(E e) {
            return this.ns.floor(e);
        }

        public E ceiling(E e) {
            return this.ns.ceiling(e);
        }

        public E higher(E e) {
            return this.ns.higher(e);
        }

        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        public NavigableSet<E> descendingSet() {
            return new UnmodifiableNavigableSet(this.ns.descendingSet());
        }

        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return new UnmodifiableNavigableSet(this.ns.subSet(fromElement, fromInclusive, toElement, toInclusive));
        }

        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new UnmodifiableNavigableSet(this.ns.headSet(toElement, inclusive));
        }

        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new UnmodifiableNavigableSet(this.ns.tailSet(fromElement, inclusive));
        }
    }

    static class UnmodifiableRandomAccessList<E> extends UnmodifiableList<E> implements RandomAccess {
        private static final long serialVersionUID = -2542308836966382001L;

        UnmodifiableRandomAccessList(List<? extends E> list) {
            super(list);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableRandomAccessList(this.list.subList(fromIndex, toIndex));
        }

        private Object writeReplace() {
            return new UnmodifiableList(this.list);
        }
    }

    private Collections() {
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        sort(list, null);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        if (VMRuntime.getRuntime().getTargetSdkVersion() > FILL_THRESHOLD) {
            list.sort(c);
        } else if (list.getClass() == ArrayList.class) {
            Arrays.sort(((ArrayList) list).elementData, 0, list.size(), c);
        } else {
            Object[] a = list.toArray();
            Arrays.sort(a, c);
            ListIterator<T> i = list.listIterator();
            for (Object obj : a) {
                i.next();
                i.set(obj);
            }
        }
    }

    public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
        if ((list instanceof RandomAccess) || list.size() < BINARYSEARCH_THRESHOLD) {
            return indexedBinarySearch(list, key);
        }
        return iteratorBinarySearch(list, key);
    }

    private static <T> int indexedBinarySearch(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = ((Comparable) list.get(mid)).compareTo(key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp <= 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    private static <T> int iteratorBinarySearch(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        ListIterator<? extends Comparable<? super T>> i = list.listIterator();
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = ((Comparable) get(i, mid)).compareTo(key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp <= 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    private static <T> T get(ListIterator<? extends T> i, int index) {
        T obj;
        int pos = i.nextIndex();
        if (pos <= index) {
            while (true) {
                obj = i.next();
                int pos2 = pos + 1;
                if (pos >= index) {
                    break;
                }
                pos = pos2;
            }
        } else {
            do {
                obj = i.previous();
                pos--;
            } while (pos > index);
        }
        return obj;
    }

    public static <T> int binarySearch(List<? extends T> list, T key, Comparator<? super T> c) {
        if (c == null) {
            return binarySearch(list, key);
        }
        if ((list instanceof RandomAccess) || list.size() < BINARYSEARCH_THRESHOLD) {
            return indexedBinarySearch(list, key, c);
        }
        return iteratorBinarySearch(list, key, c);
    }

    private static <T> int indexedBinarySearch(List<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = c.compare(l.get(mid), key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp <= 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    private static <T> int iteratorBinarySearch(List<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size() - 1;
        ListIterator<? extends T> i = l.listIterator();
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = c.compare(get(i, mid), key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp <= 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static void reverse(List<?> list) {
        int size = list.size();
        int i;
        int mid;
        if (size < 18 || (list instanceof RandomAccess)) {
            i = 0;
            mid = size >> 1;
            int j = size - 1;
            while (i < mid) {
                swap((List) list, i, j);
                i++;
                j--;
            }
            return;
        }
        ListIterator fwd = list.listIterator();
        ListIterator rev = list.listIterator(size);
        mid = list.size() >> 1;
        for (i = 0; i < mid; i++) {
            Object tmp = fwd.next();
            fwd.set(rev.previous());
            rev.set(tmp);
        }
    }

    public static void shuffle(List<?> list) {
        Random rnd = r;
        if (rnd == null) {
            rnd = new Random();
            r = rnd;
        }
        shuffle(list, rnd);
    }

    public static void shuffle(List<?> list, Random rnd) {
        int size = list.size();
        int i;
        if (size < 5 || (list instanceof RandomAccess)) {
            for (i = size; i > 1; i--) {
                swap((List) list, i - 1, rnd.nextInt(i));
            }
            return;
        }
        Object[] arr = list.toArray();
        for (i = size; i > 1; i--) {
            swap(arr, i - 1, rnd.nextInt(i));
        }
        ListIterator it = list.listIterator();
        for (Object obj : arr) {
            it.next();
            it.set(obj);
        }
    }

    public static void swap(List<?> list, int i, int j) {
        List<?> l = list;
        list.set(i, list.set(j, list.get(i)));
    }

    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    public static <T> void fill(List<? super T> list, T obj) {
        int size = list.size();
        int i;
        if (size < FILL_THRESHOLD || (list instanceof RandomAccess)) {
            for (i = 0; i < size; i++) {
                list.set(i, obj);
            }
            return;
        }
        ListIterator<? super T> itr = list.listIterator();
        for (i = 0; i < size; i++) {
            itr.next();
            itr.set(obj);
        }
    }

    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        int srcSize = src.size();
        int i;
        if (srcSize > dest.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        } else if (srcSize < 10 || ((src instanceof RandomAccess) && (dest instanceof RandomAccess))) {
            for (i = 0; i < srcSize; i++) {
                dest.set(i, src.get(i));
            }
        } else {
            ListIterator<? super T> di = dest.listIterator();
            ListIterator<? extends T> si = src.listIterator();
            for (i = 0; i < srcSize; i++) {
                di.next();
                di.set(si.next());
            }
        }
    }

    public static <T extends Comparable<? super T>> T min(Collection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (((Comparable) next).compareTo(candidate) < 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
        if (comp == null) {
            return min(coll);
        }
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) < 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    public static <T extends Comparable<? super T>> T max(Collection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (((Comparable) next).compareTo(candidate) > 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
        if (comp == null) {
            return max(coll);
        }
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) > 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    public static void rotate(List<?> list, int distance) {
        if ((list instanceof RandomAccess) || list.size() < ROTATE_THRESHOLD) {
            rotate1(list, distance);
        } else {
            rotate2(list, distance);
        }
    }

    private static <T> void rotate1(List<T> list, int distance) {
        int size = list.size();
        if (size != 0) {
            distance %= size;
            if (distance < 0) {
                distance += size;
            }
            if (distance != 0) {
                int cycleStart = 0;
                int nMoved = 0;
                while (nMoved != size) {
                    T displaced = list.get(cycleStart);
                    int i = cycleStart;
                    do {
                        i += distance;
                        if (i >= size) {
                            i -= size;
                        }
                        displaced = list.set(i, displaced);
                        nMoved++;
                    } while (i != cycleStart);
                    cycleStart++;
                }
            }
        }
    }

    private static void rotate2(List<?> list, int distance) {
        int size = list.size();
        if (size != 0) {
            int mid = (-distance) % size;
            if (mid < 0) {
                mid += size;
            }
            if (mid != 0) {
                reverse(list.subList(0, mid));
                reverse(list.subList(mid, size));
                reverse(list);
            }
        }
    }

    public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
        boolean result = false;
        int size = list.size();
        int i;
        if (size >= 11 && !(list instanceof RandomAccess)) {
            ListIterator<T> itr = list.listIterator();
            if (oldVal == null) {
                for (i = 0; i < size; i++) {
                    if (itr.next() == null) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            } else {
                for (i = 0; i < size; i++) {
                    if (oldVal.equals(itr.next())) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            }
        } else if (oldVal == null) {
            for (i = 0; i < size; i++) {
                if (list.get(i) == null) {
                    list.set(i, newVal);
                    result = true;
                }
            }
        } else {
            for (i = 0; i < size; i++) {
                if (oldVal.equals(list.get(i))) {
                    list.set(i, newVal);
                    result = true;
                }
            }
        }
        return result;
    }

    public static int indexOfSubList(List<?> source, List<?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        int maxCandidate = sourceSize - targetSize;
        int candidate;
        int i;
        int j;
        if (sourceSize < INDEXOFSUBLIST_THRESHOLD || ((source instanceof RandomAccess) && (target instanceof RandomAccess))) {
            candidate = 0;
            while (candidate <= maxCandidate) {
                i = 0;
                j = candidate;
                while (i < targetSize) {
                    if (eq(target.get(i), source.get(j))) {
                        i++;
                        j++;
                    } else {
                        candidate++;
                    }
                }
                return candidate;
            }
        }
        ListIterator<?> si = source.listIterator();
        candidate = 0;
        while (candidate <= maxCandidate) {
            ListIterator<?> ti = target.listIterator();
            i = 0;
            while (i < targetSize) {
                if (eq(ti.next(), si.next())) {
                    i++;
                } else {
                    for (j = 0; j < i; j++) {
                        si.previous();
                    }
                    candidate++;
                }
            }
            return candidate;
        }
        return -1;
    }

    public static int lastIndexOfSubList(List<?> source, List<?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        int maxCandidate = sourceSize - targetSize;
        int candidate;
        int i;
        int j;
        if (sourceSize < INDEXOFSUBLIST_THRESHOLD || (source instanceof RandomAccess)) {
            candidate = maxCandidate;
            while (candidate >= 0) {
                i = 0;
                j = candidate;
                while (i < targetSize) {
                    if (eq(target.get(i), source.get(j))) {
                        i++;
                        j++;
                    } else {
                        candidate--;
                    }
                }
                return candidate;
            }
        } else if (maxCandidate < 0) {
            return -1;
        } else {
            ListIterator<?> si = source.listIterator(maxCandidate);
            candidate = maxCandidate;
            while (candidate >= 0) {
                ListIterator<?> ti = target.listIterator();
                i = 0;
                while (i < targetSize) {
                    if (eq(ti.next(), si.next())) {
                        i++;
                    } else {
                        if (candidate != 0) {
                            for (j = 0; j <= i + 1; j++) {
                                si.previous();
                            }
                        }
                        candidate--;
                    }
                }
                return candidate;
            }
        }
        return -1;
    }

    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
        return new UnmodifiableCollection(c);
    }

    public static <T> Set<T> unmodifiableSet(Set<? extends T> s) {
        return new UnmodifiableSet(s);
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> s) {
        return new UnmodifiableSortedSet(s);
    }

    public static <T> NavigableSet<T> unmodifiableNavigableSet(NavigableSet<T> s) {
        return new UnmodifiableNavigableSet(s);
    }

    public static <T> List<T> unmodifiableList(List<? extends T> list) {
        if (list instanceof RandomAccess) {
            return new UnmodifiableRandomAccessList(list);
        }
        return new UnmodifiableList(list);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> m) {
        return new UnmodifiableMap(m);
    }

    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> m) {
        return new UnmodifiableSortedMap(m);
    }

    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> m) {
        return new UnmodifiableNavigableMap(m);
    }

    public static <T> Collection<T> synchronizedCollection(Collection<T> c) {
        return new SynchronizedCollection(c);
    }

    static <T> Collection<T> synchronizedCollection(Collection<T> c, Object mutex) {
        return new SynchronizedCollection(c, mutex);
    }

    public static <T> Set<T> synchronizedSet(Set<T> s) {
        return new SynchronizedSet(s);
    }

    static <T> Set<T> synchronizedSet(Set<T> s, Object mutex) {
        return new SynchronizedSet(s, mutex);
    }

    public static <T> SortedSet<T> synchronizedSortedSet(SortedSet<T> s) {
        return new SynchronizedSortedSet(s);
    }

    public static <T> NavigableSet<T> synchronizedNavigableSet(NavigableSet<T> s) {
        return new SynchronizedNavigableSet(s);
    }

    public static <T> List<T> synchronizedList(List<T> list) {
        if (list instanceof RandomAccess) {
            return new SynchronizedRandomAccessList(list);
        }
        return new SynchronizedList(list);
    }

    static <T> List<T> synchronizedList(List<T> list, Object mutex) {
        if (list instanceof RandomAccess) {
            return new SynchronizedRandomAccessList(list, mutex);
        }
        return new SynchronizedList(list, mutex);
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> m) {
        return new SynchronizedMap(m);
    }

    public static <K, V> SortedMap<K, V> synchronizedSortedMap(SortedMap<K, V> m) {
        return new SynchronizedSortedMap(m);
    }

    public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(NavigableMap<K, V> m) {
        return new SynchronizedNavigableMap(m);
    }

    public static <E> Collection<E> checkedCollection(Collection<E> c, Class<E> type) {
        return new CheckedCollection(c, type);
    }

    static <T> T[] zeroLengthArray(Class<T> type) {
        return (Object[]) Array.newInstance((Class) type, 0);
    }

    public static <E> Queue<E> checkedQueue(Queue<E> queue, Class<E> type) {
        return new CheckedQueue(queue, type);
    }

    public static <E> Set<E> checkedSet(Set<E> s, Class<E> type) {
        return new CheckedSet(s, type);
    }

    public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> s, Class<E> type) {
        return new CheckedSortedSet(s, type);
    }

    public static <E> NavigableSet<E> checkedNavigableSet(NavigableSet<E> s, Class<E> type) {
        return new CheckedNavigableSet(s, type);
    }

    public static <E> List<E> checkedList(List<E> list, Class<E> type) {
        if (list instanceof RandomAccess) {
            return new CheckedRandomAccessList(list, type);
        }
        return new CheckedList(list, type);
    }

    public static <K, V> Map<K, V> checkedMap(Map<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new CheckedMap(m, keyType, valueType);
    }

    public static <K, V> SortedMap<K, V> checkedSortedMap(SortedMap<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new CheckedSortedMap(m, keyType, valueType);
    }

    public static <K, V> NavigableMap<K, V> checkedNavigableMap(NavigableMap<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new CheckedNavigableMap(m, keyType, valueType);
    }

    public static <T> Iterator<T> emptyIterator() {
        return EmptyIterator.EMPTY_ITERATOR;
    }

    public static <T> ListIterator<T> emptyListIterator() {
        return EmptyListIterator.EMPTY_ITERATOR;
    }

    public static <T> Enumeration<T> emptyEnumeration() {
        return EmptyEnumeration.EMPTY_ENUMERATION;
    }

    public static final <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    public static <E> SortedSet<E> emptySortedSet() {
        return UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }

    public static <E> NavigableSet<E> emptyNavigableSet() {
        return UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }

    public static final <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    public static final <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    public static final <K, V> SortedMap<K, V> emptySortedMap() {
        return UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
    }

    public static final <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
    }

    public static <T> Set<T> singleton(T o) {
        return new SingletonSet(o);
    }

    static <E> Iterator<E> singletonIterator(final E e) {
        return new Iterator<E>() {
            private boolean hasNext = true;

            public boolean hasNext() {
                return this.hasNext;
            }

            public E next() {
                if (this.hasNext) {
                    this.hasNext = false;
                    return e;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void forEachRemaining(Consumer<? super E> action) {
                Objects.requireNonNull(action);
                if (this.hasNext) {
                    action.accept(e);
                    this.hasNext = false;
                }
            }
        };
    }

    static <T> Spliterator<T> singletonSpliterator(final T element) {
        return new Spliterator<T>() {
            long est = 1;

            public Spliterator<T> trySplit() {
                return null;
            }

            public boolean tryAdvance(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                if (this.est <= 0) {
                    return false;
                }
                this.est--;
                consumer.accept(element);
                return true;
            }

            public void forEachRemaining(Consumer<? super T> consumer) {
                tryAdvance(consumer);
            }

            public long estimateSize() {
                return this.est;
            }

            public int characteristics() {
                return (((((element != null ? 256 : 0) | 64) | 16384) | 1024) | 1) | 16;
            }
        };
    }

    public static <T> List<T> singletonList(T o) {
        return new SingletonList(o);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        return new SingletonMap(key, value);
    }

    public static <T> List<T> nCopies(int n, T o) {
        if (n >= 0) {
            return new CopiesList(n, o);
        }
        throw new IllegalArgumentException("List length = " + n);
    }

    public static <T> Comparator<T> reverseOrder() {
        return ReverseComparator.REVERSE_ORDER;
    }

    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
        if (cmp == null) {
            return reverseOrder();
        }
        if (cmp instanceof ReverseComparator2) {
            return ((ReverseComparator2) cmp).cmp;
        }
        return new ReverseComparator2(cmp);
    }

    public static <T> Enumeration<T> enumeration(final Collection<T> c) {
        return new Enumeration<T>() {
            private final Iterator<T> i = c.iterator();

            public boolean hasMoreElements() {
                return this.i.hasNext();
            }

            public T nextElement() {
                return this.i.next();
            }
        };
    }

    public static <T> ArrayList<T> list(Enumeration<T> e) {
        ArrayList<T> l = new ArrayList();
        while (e.hasMoreElements()) {
            l.add(e.nextElement());
        }
        return l;
    }

    static boolean eq(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }

    public static int frequency(Collection<?> c, Object o) {
        int result = 0;
        if (o == null) {
            for (Object e : c) {
                if (e == null) {
                    result++;
                }
            }
        } else {
            for (Object e2 : c) {
                if (o.equals(e2)) {
                    result++;
                }
            }
        }
        return result;
    }

    public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
        Collection<?> contains = c2;
        Collection<?> iterate = c1;
        if (c1 instanceof Set) {
            iterate = c2;
            contains = c1;
        } else if (!(c2 instanceof Set)) {
            int c1size = c1.size();
            int c2size = c2.size();
            if (c1size == 0 || c2size == 0) {
                return true;
            }
            if (c1size > c2size) {
                iterate = c2;
                contains = c1;
            }
        }
        for (Object e : iterate) {
            if (contains.contains(e)) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        boolean result = false;
        for (T element : elements) {
            result |= c.add(element);
        }
        return result;
    }

    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        return new SetFromMap(map);
    }

    public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
        return new AsLIFOQueue(deque);
    }
}
