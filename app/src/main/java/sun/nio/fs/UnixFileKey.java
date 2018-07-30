package sun.nio.fs;

class UnixFileKey {
    private final long st_dev;
    private final long st_ino;

    UnixFileKey(long st_dev, long st_ino) {
        this.st_dev = st_dev;
        this.st_ino = st_ino;
    }

    public int hashCode() {
        return ((int) (this.st_dev ^ (this.st_dev >>> 32))) + ((int) (this.st_ino ^ (this.st_ino >>> 32)));
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UnixFileKey)) {
            return false;
        }
        UnixFileKey other = (UnixFileKey) obj;
        if (!(this.st_dev == other.st_dev && this.st_ino == other.st_ino)) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(dev=").append(Long.toHexString(this.st_dev)).append(",ino=").append(this.st_ino).append(')');
        return sb.toString();
    }
}
