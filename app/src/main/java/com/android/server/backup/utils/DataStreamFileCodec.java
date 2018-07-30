package com.android.server.backup.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class DataStreamFileCodec<T> {
    private final DataStreamCodec<T> mCodec;
    private final File mFile;

    public DataStreamFileCodec(File file, DataStreamCodec<T> codec) {
        this.mFile = file;
        this.mCodec = codec;
    }

    public T deserialize() throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        FileInputStream fileInputStream = null;
        DataInputStream dataInputStream = null;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(this.mFile);
            try {
                DataInputStream dataInputStream2 = new DataInputStream(fileInputStream2);
                try {
                    T deserialize = this.mCodec.deserialize(dataInputStream2);
                    if (dataInputStream2 != null) {
                        try {
                            dataInputStream2.close();
                        } catch (Throwable th4) {
                            th3 = th4;
                        }
                    }
                    if (fileInputStream2 != null) {
                        try {
                            fileInputStream2.close();
                        } catch (Throwable th5) {
                            th = th5;
                            if (th3 != null) {
                                if (th3 != th) {
                                    th3.addSuppressed(th);
                                    th = th3;
                                }
                            }
                        }
                    }
                    th = th3;
                    if (th == null) {
                        return deserialize;
                    }
                    throw th;
                } catch (Throwable th6) {
                    th = th6;
                    dataInputStream = dataInputStream2;
                    fileInputStream = fileInputStream2;
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (Throwable th7) {
                            th2 = th7;
                            if (th3 != null) {
                                if (th3 != th2) {
                                    th3.addSuppressed(th2);
                                    th2 = th3;
                                }
                            }
                        }
                    }
                    th2 = th3;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th8) {
                            th3 = th8;
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
            } catch (Throwable th9) {
                th = th9;
                fileInputStream = fileInputStream2;
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th72) {
                        th2 = th72;
                        if (th3 != null) {
                            if (th3 != th2) {
                                th3.addSuppressed(th2);
                                th2 = th3;
                            }
                        }
                    }
                }
                th2 = th3;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th82) {
                        th3 = th82;
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
                } catch (Throwable th722) {
                    th2 = th722;
                    if (th3 != null) {
                        if (th3 != th2) {
                            th3.addSuppressed(th2);
                            th2 = th3;
                        }
                    }
                }
            }
            th2 = th3;
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable th822) {
                    th3 = th822;
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

    public void serialize(T t) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        DataOutputStream dataOutputStream = null;
        try {
            BufferedOutputStream bufferedOutputStream2;
            FileOutputStream fileOutputStream2 = new FileOutputStream(this.mFile);
            try {
                bufferedOutputStream2 = new BufferedOutputStream(fileOutputStream2);
            } catch (Throwable th4) {
                th = th4;
                fileOutputStream = fileOutputStream2;
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (Throwable th22) {
                        if (th3 == null) {
                            th3 = th22;
                        } else if (th3 != th22) {
                            th3.addSuppressed(th22);
                        }
                    }
                }
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (Throwable th5) {
                        th22 = th5;
                        if (th3 != null) {
                            if (th3 != th22) {
                                th3.addSuppressed(th22);
                                th22 = th3;
                            }
                        }
                    }
                }
                th22 = th3;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable th6) {
                        th3 = th6;
                        if (th22 != null) {
                            if (th22 != th3) {
                                th22.addSuppressed(th3);
                                th3 = th22;
                            }
                        }
                    }
                }
                th3 = th22;
                if (th3 == null) {
                    throw th;
                }
                throw th3;
            }
            try {
                DataOutputStream dataOutputStream2 = new DataOutputStream(bufferedOutputStream2);
                try {
                    this.mCodec.serialize(t, dataOutputStream2);
                    dataOutputStream2.flush();
                    if (dataOutputStream2 != null) {
                        try {
                            dataOutputStream2.close();
                        } catch (Throwable th7) {
                            th = th7;
                        }
                    }
                    th = null;
                    if (bufferedOutputStream2 != null) {
                        try {
                            bufferedOutputStream2.close();
                        } catch (Throwable th8) {
                            th3 = th8;
                            if (th != null) {
                                if (th != th3) {
                                    th.addSuppressed(th3);
                                    th3 = th;
                                }
                            }
                        }
                    }
                    th3 = th;
                    if (fileOutputStream2 != null) {
                        try {
                            fileOutputStream2.close();
                        } catch (Throwable th9) {
                            th = th9;
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
                } catch (Throwable th10) {
                    th = th10;
                    dataOutputStream = dataOutputStream2;
                    bufferedOutputStream = bufferedOutputStream2;
                    fileOutputStream = fileOutputStream2;
                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (Throwable th222) {
                            if (th3 == null) {
                                th3 = th222;
                            } else if (th3 != th222) {
                                th3.addSuppressed(th222);
                            }
                        }
                    }
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (Throwable th52) {
                            th222 = th52;
                            if (th3 != null) {
                                if (th3 != th222) {
                                    th3.addSuppressed(th222);
                                    th222 = th3;
                                }
                            }
                        }
                    }
                    th222 = th3;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (Throwable th62) {
                            th3 = th62;
                            if (th222 != null) {
                                if (th222 != th3) {
                                    th222.addSuppressed(th3);
                                    th3 = th222;
                                }
                            }
                        }
                    }
                    th3 = th222;
                    if (th3 == null) {
                        throw th;
                    }
                    throw th3;
                }
            } catch (Throwable th11) {
                th = th11;
                bufferedOutputStream = bufferedOutputStream2;
                fileOutputStream = fileOutputStream2;
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (Throwable th2222) {
                        if (th3 == null) {
                            th3 = th2222;
                        } else if (th3 != th2222) {
                            th3.addSuppressed(th2222);
                        }
                    }
                }
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (Throwable th522) {
                        th2222 = th522;
                        if (th3 != null) {
                            if (th3 != th2222) {
                                th3.addSuppressed(th2222);
                                th2222 = th3;
                            }
                        }
                    }
                }
                th2222 = th3;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable th622) {
                        th3 = th622;
                        if (th2222 != null) {
                            if (th2222 != th3) {
                                th2222.addSuppressed(th3);
                                th3 = th2222;
                            }
                        }
                    }
                }
                th3 = th2222;
                if (th3 == null) {
                    throw th3;
                }
                throw th;
            }
        } catch (Throwable th12) {
            th = th12;
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (Throwable th22222) {
                    if (th3 == null) {
                        th3 = th22222;
                    } else if (th3 != th22222) {
                        th3.addSuppressed(th22222);
                    }
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Throwable th5222) {
                    th22222 = th5222;
                    if (th3 != null) {
                        if (th3 != th22222) {
                            th3.addSuppressed(th22222);
                            th22222 = th3;
                        }
                    }
                }
            }
            th22222 = th3;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th6222) {
                    th3 = th6222;
                    if (th22222 != null) {
                        if (th22222 != th3) {
                            th22222.addSuppressed(th3);
                            th3 = th22222;
                        }
                    }
                }
            }
            th3 = th22222;
            if (th3 == null) {
                throw th3;
            }
            throw th;
        }
    }
}
