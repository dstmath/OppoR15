package com.android.server.timezone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class CheckToken {
    final int mOptimisticLockId;
    final PackageVersions mPackageVersions;

    CheckToken(int optimisticLockId, PackageVersions packageVersions) {
        this.mOptimisticLockId = optimisticLockId;
        if (packageVersions == null) {
            throw new NullPointerException("packageVersions == null");
        }
        this.mPackageVersions = packageVersions;
    }

    byte[] toByteArray() {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(12);
        DataOutputStream dos = null;
        try {
            DataOutputStream dos2 = new DataOutputStream(baos);
            try {
                dos2.writeInt(this.mOptimisticLockId);
                dos2.writeInt(this.mPackageVersions.mUpdateAppVersion);
                dos2.writeInt(this.mPackageVersions.mDataAppVersion);
                if (dos2 != null) {
                    try {
                        dos2.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return baos.toByteArray();
                }
                try {
                    throw th2;
                } catch (IOException e2) {
                    e = e2;
                    dos = dos2;
                }
            } catch (Throwable th4) {
                th = th4;
                dos = dos2;
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    try {
                        throw th2;
                    } catch (IOException e3) {
                        e = e3;
                        throw new RuntimeException("Unable to write into a ByteArrayOutputStream", e);
                    }
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (dos != null) {
                try {
                    dos.close();
                } catch (Throwable th52) {
                    if (th2 == null) {
                        th2 = th52;
                    } else if (th2 != th52) {
                        th2.addSuppressed(th52);
                    }
                }
            }
            if (th2 == null) {
                throw th;
            }
            try {
                throw th2;
            } catch (IOException e32) {
                e = e32;
                throw new RuntimeException("Unable to write into a ByteArrayOutputStream", e);
            }
        }
    }

    static CheckToken fromByteArray(byte[] tokenBytes) throws IOException {
        Throwable th;
        Throwable th2 = null;
        DataInputStream dis = null;
        try {
            DataInputStream dis2 = new DataInputStream(new ByteArrayInputStream(tokenBytes));
            try {
                CheckToken checkToken = new CheckToken(dis2.readInt(), new PackageVersions(dis2.readInt(), dis2.readInt()));
                if (dis2 != null) {
                    try {
                        dis2.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return checkToken;
                }
                throw th2;
            } catch (Throwable th4) {
                th = th4;
                dis = dis2;
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (dis != null) {
                try {
                    dis.close();
                } catch (Throwable th52) {
                    if (th2 == null) {
                        th2 = th52;
                    } else if (th2 != th52) {
                        th2.addSuppressed(th52);
                    }
                }
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckToken checkToken = (CheckToken) o;
        if (this.mOptimisticLockId != checkToken.mOptimisticLockId) {
            return false;
        }
        return this.mPackageVersions.equals(checkToken.mPackageVersions);
    }

    public int hashCode() {
        return (this.mOptimisticLockId * 31) + this.mPackageVersions.hashCode();
    }

    public String toString() {
        return "Token{mOptimisticLockId=" + this.mOptimisticLockId + ", mPackageVersions=" + this.mPackageVersions + '}';
    }
}
