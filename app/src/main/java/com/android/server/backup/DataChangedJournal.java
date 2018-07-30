package com.android.server.backup;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public final class DataChangedJournal {
    private static final int BUFFER_SIZE_BYTES = 8192;
    private static final String FILE_NAME_PREFIX = "journal";
    private final File mFile;

    @FunctionalInterface
    public interface Consumer {
        void accept(String str);
    }

    DataChangedJournal(File file) {
        this.mFile = file;
    }

    public void addPackage(String packageName) throws IOException {
        Throwable th;
        Throwable th2 = null;
        RandomAccessFile out = null;
        try {
            RandomAccessFile out2 = new RandomAccessFile(this.mFile, "rws");
            try {
                out2.seek(out2.length());
                out2.writeUTF(packageName);
                if (out2 != null) {
                    try {
                        out2.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 != null) {
                    throw th2;
                }
            } catch (Throwable th4) {
                th = th4;
                out = out2;
                if (out != null) {
                    try {
                        out.close();
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
            if (out != null) {
                try {
                    out.close();
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

    public void forEach(Consumer consumer) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        BufferedInputStream bufferedInputStream = null;
        DataInputStream dataInputStream = null;
        try {
            BufferedInputStream bufferedInputStream2 = new BufferedInputStream(new FileInputStream(this.mFile), 8192);
            try {
                DataInputStream dataInputStream2 = new DataInputStream(bufferedInputStream2);
                while (dataInputStream2.available() > 0) {
                    try {
                        consumer.accept(dataInputStream2.readUTF());
                    } catch (Throwable th4) {
                        th = th4;
                        dataInputStream = dataInputStream2;
                        bufferedInputStream = bufferedInputStream2;
                        if (dataInputStream != null) {
                            try {
                                dataInputStream.close();
                            } catch (Throwable th5) {
                                th2 = th5;
                                if (th3 != null) {
                                    if (th3 != th2) {
                                        th3.addSuppressed(th2);
                                        th2 = th3;
                                    }
                                }
                            }
                        }
                        th2 = th3;
                        if (bufferedInputStream != null) {
                            try {
                                bufferedInputStream.close();
                            } catch (Throwable th6) {
                                th3 = th6;
                                if (th2 != null) {
                                    if (th2 != th3) {
                                        th2.addSuppressed(th3);
                                        th3 = th2;
                                    }
                                }
                            }
                        }
                        th3 = th2;
                        if (th3 != null) {
                            throw th3;
                        }
                        throw th;
                    }
                }
                if (dataInputStream2 != null) {
                    try {
                        dataInputStream2.close();
                    } catch (Throwable th7) {
                        th3 = th7;
                    }
                }
                if (bufferedInputStream2 != null) {
                    try {
                        bufferedInputStream2.close();
                    } catch (Throwable th8) {
                        th = th8;
                        if (th3 != null) {
                            if (th3 != th) {
                                th3.addSuppressed(th);
                                th = th3;
                            }
                        }
                    }
                }
                th = th3;
                if (th != null) {
                    throw th;
                }
            } catch (Throwable th9) {
                th = th9;
                bufferedInputStream = bufferedInputStream2;
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th52) {
                        th2 = th52;
                        if (th3 != null) {
                            if (th3 != th2) {
                                th3.addSuppressed(th2);
                                th2 = th3;
                            }
                        }
                    }
                }
                th2 = th3;
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (Throwable th62) {
                        th3 = th62;
                        if (th2 != null) {
                            if (th2 != th3) {
                                th2.addSuppressed(th3);
                                th3 = th2;
                            }
                        }
                    }
                }
                th3 = th2;
                if (th3 != null) {
                    throw th3;
                }
                throw th;
            }
        } catch (Throwable th10) {
            th = th10;
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Throwable th522) {
                    th2 = th522;
                    if (th3 != null) {
                        if (th3 != th2) {
                            th3.addSuppressed(th2);
                            th2 = th3;
                        }
                    }
                }
            }
            th2 = th3;
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (Throwable th622) {
                    th3 = th622;
                    if (th2 != null) {
                        if (th2 != th3) {
                            th2.addSuppressed(th3);
                            th3 = th2;
                        }
                    }
                }
            }
            th3 = th2;
            if (th3 != null) {
                throw th;
            }
            throw th3;
        }
    }

    public boolean delete() {
        return this.mFile.delete();
    }

    public boolean equals(Object object) {
        if (!(object instanceof DataChangedJournal)) {
            return false;
        }
        try {
            return this.mFile.getCanonicalPath().equals(((DataChangedJournal) object).mFile.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    public String toString() {
        return this.mFile.toString();
    }

    static DataChangedJournal newJournal(File journalDirectory) throws IOException {
        return new DataChangedJournal(File.createTempFile(FILE_NAME_PREFIX, null, journalDirectory));
    }

    static ArrayList<DataChangedJournal> listJournals(File journalDirectory) {
        ArrayList<DataChangedJournal> journals = new ArrayList();
        for (File file : journalDirectory.listFiles()) {
            journals.add(new DataChangedJournal(file));
        }
        return journals;
    }
}
