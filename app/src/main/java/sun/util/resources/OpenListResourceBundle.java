package sun.util.resources;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import sun.util.ResourceBundleEnumeration;

public abstract class OpenListResourceBundle extends ResourceBundle {
    private volatile Set<String> keyset;
    private volatile Map<String, Object> lookup = null;

    protected abstract Object[][] getContents();

    protected OpenListResourceBundle() {
    }

    protected Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        loadLookupTablesIfNecessary();
        return this.lookup.get(key);
    }

    public Enumeration<String> getKeys() {
        Enumeration enumeration = null;
        ResourceBundle parentBundle = this.parent;
        Set handleKeySet = handleKeySet();
        if (parentBundle != null) {
            enumeration = parentBundle.getKeys();
        }
        return new ResourceBundleEnumeration(handleKeySet, enumeration);
    }

    protected Set<String> handleKeySet() {
        loadLookupTablesIfNecessary();
        return this.lookup.keySet();
    }

    public Set<String> keySet() {
        if (this.keyset != null) {
            return this.keyset;
        }
        Set<String> ks = createSet();
        ks.addAll(handleKeySet());
        if (this.parent != null) {
            ks.addAll(this.parent.keySet());
        }
        synchronized (this) {
            if (this.keyset == null) {
                this.keyset = ks;
            }
        }
        return this.keyset;
    }

    void loadLookupTablesIfNecessary() {
        if (this.lookup == null) {
            loadLookup();
        }
    }

    private void loadLookup() {
        Object[][] contents = getContents();
        Map<String, Object> temp = createMap(contents.length);
        for (int i = 0; i < contents.length; i++) {
            String key = contents[i][0];
            Object value = contents[i][1];
            if (key == null || value == null) {
                throw new NullPointerException();
            }
            temp.put(key, value);
        }
        synchronized (this) {
            if (this.lookup == null) {
                this.lookup = temp;
            }
        }
    }

    protected <K, V> Map<K, V> createMap(int size) {
        return new HashMap(size);
    }

    protected <E> Set<E> createSet() {
        return new HashSet();
    }
}
