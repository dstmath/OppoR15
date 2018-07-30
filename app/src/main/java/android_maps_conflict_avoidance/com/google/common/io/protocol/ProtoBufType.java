package android_maps_conflict_avoidance.com.google.common.io.protocol;

import android_maps_conflict_avoidance.com.google.common.util.IntMap;

public class ProtoBufType {
    private static final TypeInfo[] NULL_DATA_TYPEINFOS = new TypeInfo[168];
    private final String typeName;
    private final IntMap types;

    static class TypeInfo {
        private Object data;
        private int type;

        TypeInfo(int t, Object d) {
            this.type = t;
            this.data = d;
        }

        public int hashCode() {
            return this.type;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof TypeInfo)) {
                return false;
            }
            TypeInfo peerTypeInfo = (TypeInfo) obj;
            if (this.type == peerTypeInfo.type) {
                if (this.data == peerTypeInfo.data) {
                    return true;
                }
                if (this.data != null && this.data.equals(peerTypeInfo.data)) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return "TypeInfo{type=" + this.type + ", data=" + this.data + "}";
        }
    }

    static {
        int index = 0;
        int i = 0;
        while (i <= 7) {
            int index2;
            int j = 16;
            while (true) {
                index2 = index;
                if (j >= 37) {
                    break;
                }
                index = index2 + 1;
                NULL_DATA_TYPEINFOS[index2] = new TypeInfo((i << 8) + j, null);
                j++;
            }
            i++;
            index = index2;
        }
    }

    public ProtoBufType() {
        this.types = new IntMap();
        this.typeName = null;
    }

    public ProtoBufType(String typeName) {
        this.types = new IntMap();
        this.typeName = typeName;
    }

    private static TypeInfo getCacheTypeInfoForNullData(int optionsAndType) {
        return NULL_DATA_TYPEINFOS[(((65280 & optionsAndType) >> 8) * 21) + ((optionsAndType & 255) - 16)];
    }

    public ProtoBufType addElement(int optionsAndType, int tag, Object data) {
        this.types.put(tag, data == null ? getCacheTypeInfoForNullData(optionsAndType) : new TypeInfo(optionsAndType, data));
        return this;
    }

    public int getType(int tag) {
        TypeInfo typeInfo = (TypeInfo) this.types.get(tag);
        return typeInfo == null ? 16 : typeInfo.type & 255;
    }

    public Object getData(int tag) {
        TypeInfo typeInfo = (TypeInfo) this.types.get(tag);
        return typeInfo == null ? typeInfo : typeInfo.data;
    }

    public String toString() {
        return "ProtoBufType Name: " + this.typeName;
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        return this.types.equals(((ProtoBufType) object).types);
    }

    public int hashCode() {
        if (this.types != null) {
            return this.types.hashCode();
        }
        return super.hashCode();
    }
}
