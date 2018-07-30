package com.oppo.util;

import java.util.ArrayList;

public class FixSizeFIFOList<T> {
    private int mNum = 0;
    private ArrayList<T> mObjects;

    public FixSizeFIFOList(int num) {
        this.mNum = num;
        this.mObjects = new ArrayList(10);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized T pushToHead(T object) {
        T removedObject = null;
        if (!this.mObjects.isEmpty() && this.mObjects.contains(object)) {
            this.mObjects.remove(object);
            this.mObjects.add(0, object);
            return null;
        } else if (this.mNum > 0) {
            int curSize = this.mObjects.size();
            if (curSize >= this.mNum) {
                removedObject = this.mObjects.get(curSize - 1);
                this.mObjects.remove(removedObject);
                this.mObjects.add(0, object);
            } else {
                this.mObjects.add(0, object);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized T pushToTail(T object) {
        T removedObject = null;
        if (!this.mObjects.isEmpty() && this.mObjects.contains(object)) {
            this.mObjects.remove(object);
            this.mObjects.add(object);
            return null;
        } else if (this.mNum > 0) {
            if (this.mObjects.size() >= this.mNum) {
                removedObject = this.mObjects.get(0);
                this.mObjects.remove(removedObject);
                this.mObjects.add(object);
            } else {
                this.mObjects.add(object);
            }
        }
    }

    public synchronized boolean remove(T object) {
        return this.mObjects.remove(object);
    }

    public synchronized boolean contains(T object) {
        if (this.mObjects.isEmpty() || !this.mObjects.contains(object)) {
            return false;
        }
        return true;
    }

    public synchronized T get(int index) {
        if (index >= this.mObjects.size()) {
            return null;
        }
        return this.mObjects.get(index);
    }

    public synchronized T getLatest() {
        if (this.mObjects.size() <= 0) {
            return null;
        }
        return this.mObjects.get(0);
    }

    public synchronized void clear() {
        if (!this.mObjects.isEmpty()) {
            this.mObjects.clear();
        }
    }

    public synchronized ArrayList<T> getAll() {
        return this.mObjects;
    }

    public int getCapacity() {
        return this.mNum;
    }

    public synchronized int getSize() {
        return this.mObjects.size();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("mRecordList: ");
        for (T object : this.mObjects) {
            stringBuffer.append(object + " ");
        }
        return stringBuffer.toString();
    }
}
