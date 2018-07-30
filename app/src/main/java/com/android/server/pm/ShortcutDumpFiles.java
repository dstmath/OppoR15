package com.android.server.pm;

import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.-$Lambda$cC5GTLJlwZun1lDliqKZF7MnGwo.AnonymousClass1;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class ShortcutDumpFiles {
    private static final boolean DEBUG = false;
    private static final String TAG = "ShortcutService";
    private final ShortcutService mService;

    public ShortcutDumpFiles(ShortcutService service) {
        this.mService = service;
    }

    public boolean save(String filename, Consumer<PrintWriter> dumper) {
        Throwable th;
        Throwable th2 = null;
        try {
            File directory = this.mService.getDumpPath();
            directory.mkdirs();
            if (directory.exists()) {
                PrintWriter pw = null;
                try {
                    PrintWriter pw2 = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(directory, filename))));
                    try {
                        dumper.accept(pw2);
                        if (pw2 != null) {
                            try {
                                pw2.close();
                            } catch (Throwable th3) {
                                th2 = th3;
                            }
                        }
                        if (th2 == null) {
                            return true;
                        }
                        throw th2;
                    } catch (Throwable th4) {
                        th = th4;
                        pw = pw2;
                        if (pw != null) {
                            try {
                                pw.close();
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
                    if (pw != null) {
                        try {
                            pw.close();
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
            Slog.e(TAG, "Failed to create directory: " + directory);
            return false;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create dump file: " + filename, e);
            return false;
        }
    }

    public boolean save(String filename, byte[] utf8bytes) {
        return save(filename, new -$Lambda$4qJi2sHY5X4ys3rtlAQIsVPSn60((byte) 0, utf8bytes));
    }

    public void dumpAll(PrintWriter pw) {
        Throwable th;
        try {
            File directory = this.mService.getDumpPath();
            File[] files = directory.listFiles(-$Lambda$cC5GTLJlwZun1lDliqKZF7MnGwo.$INST$0);
            if (!directory.exists() || ArrayUtils.isEmpty(files)) {
                pw.print("  No dump files found.");
                return;
            }
            Arrays.sort(files, Comparator.comparing(AnonymousClass1.$INST$0));
            int i = 0;
            int length = files.length;
            while (i < length) {
                File path = files[i];
                pw.print("*** Dumping: ");
                pw.println(path.getName());
                pw.print("mtime: ");
                pw.println(ShortcutService.formatTime(path.lastModified()));
                Throwable th2 = null;
                BufferedReader reader = null;
                try {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
                    while (true) {
                        try {
                            String line = reader2.readLine();
                            if (line == null) {
                                break;
                            }
                            pw.println(line);
                        } catch (Throwable th3) {
                            th = th3;
                            reader = reader2;
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (Throwable th4) {
                                    if (th2 == null) {
                                        th2 = th4;
                                    } else if (th2 != th4) {
                                        th2.addSuppressed(th4);
                                    }
                                }
                            }
                            if (th2 != null) {
                                throw th2;
                            } else {
                                throw th;
                            }
                        }
                    }
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (Throwable th5) {
                            th2 = th5;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                    i++;
                } catch (Throwable th6) {
                    th = th6;
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to print dump files", e);
        }
    }
}
