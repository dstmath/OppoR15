package com.android.commands.requestsync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;
import java.io.PrintStream;
import java.net.URISyntaxException;

public class RequestSync {
    private Account mAccount;
    private String mAccountName;
    private String mAccountType;
    private String[] mArgs;
    private String mAuthority;
    private String mCurArgData;
    private Bundle mExtras = new Bundle();
    private int mNextArg;
    private Operation mOperation;
    private int mPeriodicIntervalSeconds;

    enum Operation {
        REQUEST_SYNC {
            void invoke(RequestSync caller) {
                ContentResolver.requestSync(caller.mAccount, caller.mAuthority, caller.mExtras);
            }
        },
        ADD_PERIODIC_SYNC {
            void invoke(RequestSync caller) {
                ContentResolver.addPeriodicSync(caller.mAccount, caller.mAuthority, caller.mExtras, (long) caller.mPeriodicIntervalSeconds);
            }
        },
        REMOVE_PERIODIC_SYNC {
            void invoke(RequestSync caller) {
                ContentResolver.removePeriodicSync(caller.mAccount, caller.mAuthority, caller.mExtras);
            }
        };

        abstract void invoke(RequestSync requestSync);
    }

    public static void main(String[] args) {
        try {
            new RequestSync().run(args);
        } catch (IllegalArgumentException e) {
            showUsage();
            System.err.println("Error: " + e);
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        this.mArgs = args;
        this.mNextArg = 0;
        if (parseArgs()) {
            Account account = (this.mAccountName == null || this.mAccountType == null) ? null : new Account(this.mAccountName, this.mAccountType);
            System.out.printf("Requesting sync for: \n", new Object[0]);
            if (account != null) {
                System.out.printf("  Account: %s (%s)\n", new Object[]{account.name, account.type});
            } else {
                System.out.printf("  Account: all\n", new Object[0]);
            }
            PrintStream printStream = System.out;
            String str = "  Authority: %s\n";
            Object[] objArr = new Object[1];
            objArr[0] = this.mAuthority != null ? this.mAuthority : "All";
            printStream.printf(str, objArr);
            if (this.mExtras.size() > 0) {
                System.out.printf("  Extras:\n", new Object[0]);
                for (String key : this.mExtras.keySet()) {
                    System.out.printf("    %s: %s\n", new Object[]{key, this.mExtras.get(key)});
                }
            }
            this.mAccount = account;
            this.mOperation.invoke(this);
        }
    }

    private boolean parseArgs() throws URISyntaxException {
        this.mOperation = Operation.REQUEST_SYNC;
        if (this.mArgs.length > 0) {
            String str = this.mArgs[0];
            if (str.equals("add-periodic")) {
                this.mNextArg++;
                this.mOperation = Operation.ADD_PERIODIC_SYNC;
                this.mPeriodicIntervalSeconds = Integer.parseInt(nextArgRequired());
            } else if (str.equals("remove-periodic")) {
                this.mNextArg++;
                this.mOperation = Operation.REMOVE_PERIODIC_SYNC;
            }
        }
        while (true) {
            String opt = nextOption();
            if (opt != null) {
                if (opt.equals("-h") || opt.equals("--help")) {
                    showUsage();
                } else if (opt.equals("-n") || opt.equals("--account-name")) {
                    this.mAccountName = nextArgRequired();
                } else if (opt.equals("-t") || opt.equals("--account-type")) {
                    this.mAccountType = nextArgRequired();
                } else if (opt.equals("-a") || opt.equals("--authority")) {
                    this.mAuthority = nextArgRequired();
                } else if (opt.equals("--is") || opt.equals("--ignore-settings")) {
                    this.mExtras.putBoolean("ignore_settings", true);
                } else if (opt.equals("--ib") || opt.equals("--ignore-backoff")) {
                    this.mExtras.putBoolean("ignore_backoff", true);
                } else if (opt.equals("--dd") || opt.equals("--discard-deletions")) {
                    this.mExtras.putBoolean("discard_deletions", true);
                } else if (opt.equals("--nr") || opt.equals("--no-retry")) {
                    this.mExtras.putBoolean("do_not_retry", true);
                } else if (opt.equals("--ex") || opt.equals("--expedited")) {
                    this.mExtras.putBoolean("expedited", true);
                } else if (opt.equals("-i") || opt.equals("--initialize")) {
                    this.mExtras.putBoolean("initialize", true);
                } else if (opt.equals("-m") || opt.equals("--manual")) {
                    this.mExtras.putBoolean("force", true);
                } else if (opt.equals("--od") || opt.equals("--override-deletions")) {
                    this.mExtras.putBoolean("deletions_override", true);
                } else if (opt.equals("-u") || opt.equals("--upload-only")) {
                    this.mExtras.putBoolean("upload", true);
                } else if (opt.equals("--rc") || opt.equals("--require-charging")) {
                    this.mExtras.putBoolean("require_charging", true);
                } else if (opt.equals("-e") || opt.equals("--es") || opt.equals("--extra-string")) {
                    this.mExtras.putString(nextArgRequired(), nextArgRequired());
                } else if (opt.equals("--esn") || opt.equals("--extra-string-null")) {
                    this.mExtras.putString(nextArgRequired(), null);
                } else if (opt.equals("--ei") || opt.equals("--extra-int")) {
                    this.mExtras.putInt(nextArgRequired(), Integer.valueOf(nextArgRequired()).intValue());
                } else if (opt.equals("--el") || opt.equals("--extra-long")) {
                    this.mExtras.putLong(nextArgRequired(), Long.parseLong(nextArgRequired()));
                } else if (opt.equals("--ef") || opt.equals("--extra-float")) {
                    this.mExtras.putFloat(nextArgRequired(), (float) Long.parseLong(nextArgRequired()));
                } else if (opt.equals("--ed") || opt.equals("--extra-double")) {
                    this.mExtras.putFloat(nextArgRequired(), (float) Long.parseLong(nextArgRequired()));
                } else if (opt.equals("--ez") || opt.equals("--extra-bool")) {
                    this.mExtras.putBoolean(nextArgRequired(), Boolean.valueOf(nextArgRequired()).booleanValue());
                } else {
                    System.err.println("Error: Unknown option: " + opt);
                    showUsage();
                    return false;
                }
            } else if (this.mNextArg >= this.mArgs.length) {
                return true;
            } else {
                showUsage();
                return false;
            }
        }
        showUsage();
        return false;
    }

    private String nextOption() {
        if (this.mCurArgData != null) {
            throw new IllegalArgumentException("No argument expected after \"" + this.mArgs[this.mNextArg - 1] + "\"");
        } else if (this.mNextArg >= this.mArgs.length) {
            return null;
        } else {
            String arg = this.mArgs[this.mNextArg];
            if (!arg.startsWith("-")) {
                return null;
            }
            this.mNextArg++;
            if (arg.equals("--")) {
                return null;
            }
            if (arg.length() <= 1 || arg.charAt(1) == '-') {
                this.mCurArgData = null;
                return arg;
            } else if (arg.length() > 2) {
                this.mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            } else {
                this.mCurArgData = null;
                return arg;
            }
        }
    }

    private String nextArg() {
        if (this.mCurArgData != null) {
            String arg = this.mCurArgData;
            this.mCurArgData = null;
            return arg;
        } else if (this.mNextArg >= this.mArgs.length) {
            return null;
        } else {
            String[] strArr = this.mArgs;
            int i = this.mNextArg;
            this.mNextArg = i + 1;
            return strArr[i];
        }
    }

    private String nextArgRequired() {
        String arg = nextArg();
        if (arg != null) {
            return arg;
        }
        throw new IllegalArgumentException("Argument expected after \"" + this.mArgs[this.mNextArg - 1] + "\"");
    }

    private static void showUsage() {
        System.err.println("Usage:\n\n  requestsync [options]\n    With no options, a sync will be requested for all account and all sync\n    authorities with no extras.\n    Basic options:\n       -h|--help: Display this message\n       -n|--account-name <ACCOUNT-NAME>\n       -t|--account-type <ACCOUNT-TYPE>\n       -a|--authority <AUTHORITY>\n    ContentResolver extra options:\n      --is|--ignore-settings: Add SYNC_EXTRAS_IGNORE_SETTINGS\n      --ib|--ignore-backoff: Add SYNC_EXTRAS_IGNORE_BACKOFF\n      --dd|--discard-deletions: Add SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS\n      --nr|--no-retry: Add SYNC_EXTRAS_DO_NOT_RETRY\n      --ex|--expedited: Add SYNC_EXTRAS_EXPEDITED\n      -i|--initialize: Add SYNC_EXTRAS_INITIALIZE\n      --m|--manual: Add SYNC_EXTRAS_MANUAL\n      --od|--override-deletions: Add SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS\n      -u|--upload-only: Add SYNC_EXTRAS_UPLOAD\n      --rc|--require-charging: Add SYNC_EXTRAS_REQUIRE_CHARGING\n    Custom extra options:\n      -e|--es|--extra-string <KEY> <VALUE>\n      --esn|--extra-string-null <KEY>\n      --ei|--extra-int <KEY> <VALUE>\n      --el|--extra-long <KEY> <VALUE>\n      --ef|--extra-float <KEY> <VALUE>\n      --ed|--extra-double <KEY> <VALUE>\n      --ez|--extra-bool <KEY> <VALUE>\n\n  requestsync add-periodic INTERVAL-SECOND [options]\n  requestsync remove-periodic [options]\n    Mandatory options:\n      -n|--account-name <ACCOUNT-NAME>\n      -t|--account-type <ACCOUNT-TYPE>\n      -a|--authority <AUTHORITY>\n    Also takes the above extra options.\n");
    }
}
