package com.android.server.net;

import android.content.Context;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.face.health.HealthState;
import java.io.PrintWriter;

class NetworkPolicyManagerShellCommand extends ShellCommand {
    private final INetworkPolicyManager mInterface;
    private final WifiManager mWifiManager;

    NetworkPolicyManagerShellCommand(Context context, INetworkPolicyManager service) {
        this.mInterface = service;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            if (cmd.equals(HealthState.GET)) {
                return runGet();
            }
            if (cmd.equals("set")) {
                return runSet();
            }
            if (cmd.equals("list")) {
                return runList();
            }
            if (cmd.equals("add")) {
                return runAdd();
            }
            if (cmd.equals("remove")) {
                return runRemove();
            }
            return handleDefaultCommands(cmd);
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Network policy manager (netpolicy) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  add restrict-background-whitelist UID");
        pw.println("    Adds a UID to the whitelist for restrict background usage.");
        pw.println("  add restrict-background-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict background usage.");
        pw.println("  get restrict-background");
        pw.println("    Gets the global restrict background usage status.");
        pw.println("  list wifi-networks [true|false]");
        pw.println("    Lists all saved wifi networks and whether they are metered or not.");
        pw.println("    If a boolean argument is passed, filters just the metered (or unmetered)");
        pw.println("    networks.");
        pw.println("  list restrict-background-whitelist");
        pw.println("    Lists UIDs that are whitelisted for restrict background usage.");
        pw.println("  list restrict-background-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict background usage.");
        pw.println("  remove restrict-background-whitelist UID");
        pw.println("    Removes a UID from the whitelist for restrict background usage.");
        pw.println("  remove restrict-background-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict background usage.");
        pw.println("  set metered-network ID [undefined|true|false]");
        pw.println("    Toggles whether the given wi-fi network is metered.");
        pw.println("  set restrict-background BOOLEAN");
        pw.println("    Sets the global restrict background usage status.");
    }

    private int runGet() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to get");
            return -1;
        } else if (type.equals("restrict-background")) {
            return getRestrictBackground();
        } else {
            pw.println("Error: unknown get type '" + type + "'");
            return -1;
        }
    }

    private int runSet() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to set");
            return -1;
        } else if (type.equals("metered-network")) {
            return setMeteredWifiNetwork();
        } else {
            if (type.equals("restrict-background")) {
                return setRestrictBackground();
            }
            pw.println("Error: unknown set type '" + type + "'");
            return -1;
        }
    }

    private int runList() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to list");
            return -1;
        } else if (type.equals("wifi-networks")) {
            return listWifiNetworks();
        } else {
            if (type.equals("restrict-background-whitelist")) {
                return listRestrictBackgroundWhitelist();
            }
            if (type.equals("restrict-background-blacklist")) {
                return listRestrictBackgroundBlacklist();
            }
            pw.println("Error: unknown list type '" + type + "'");
            return -1;
        }
    }

    private int runAdd() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to add");
            return -1;
        } else if (type.equals("restrict-background-whitelist")) {
            return addRestrictBackgroundWhitelist();
        } else {
            if (type.equals("restrict-background-blacklist")) {
                return addRestrictBackgroundBlacklist();
            }
            pw.println("Error: unknown add type '" + type + "'");
            return -1;
        }
    }

    private int runRemove() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to remove");
            return -1;
        } else if (type.equals("restrict-background-whitelist")) {
            return removeRestrictBackgroundWhitelist();
        } else {
            if (type.equals("restrict-background-blacklist")) {
                return removeRestrictBackgroundBlacklist();
            }
            pw.println("Error: unknown remove type '" + type + "'");
            return -1;
        }
    }

    private int listUidPolicies(String msg, int policy) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int[] uids = this.mInterface.getUidsWithPolicy(policy);
        pw.print(msg);
        pw.print(": ");
        if (uids.length == 0) {
            pw.println("none");
        } else {
            for (int uid : uids) {
                pw.print(uid);
                pw.print(' ');
            }
        }
        pw.println();
        return 0;
    }

    private int listRestrictBackgroundWhitelist() throws RemoteException {
        return listUidPolicies("Restrict background whitelisted UIDs", 4);
    }

    private int listRestrictBackgroundBlacklist() throws RemoteException {
        return listUidPolicies("Restrict background blacklisted UIDs", 1);
    }

    private int getRestrictBackground() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        pw.print("Restrict background status: ");
        pw.println(this.mInterface.getRestrictBackground() ? "enabled" : "disabled");
        return 0;
    }

    private int setRestrictBackground() throws RemoteException {
        int enabled = getNextBooleanArg();
        if (enabled < 0) {
            return enabled;
        }
        boolean z;
        INetworkPolicyManager iNetworkPolicyManager = this.mInterface;
        if (enabled > 0) {
            z = true;
        } else {
            z = false;
        }
        iNetworkPolicyManager.setRestrictBackground(z);
        return 0;
    }

    private int setUidPolicy(int policy) throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.setUidPolicy(uid, policy);
        return 0;
    }

    private int resetUidPolicy(String errorMessage, int expectedPolicy) throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        if (this.mInterface.getUidPolicy(uid) != expectedPolicy) {
            PrintWriter pw = getOutPrintWriter();
            pw.print("Error: UID ");
            pw.print(uid);
            pw.print(' ');
            pw.println(errorMessage);
            return -1;
        }
        this.mInterface.setUidPolicy(uid, 0);
        return 0;
    }

    private int addRestrictBackgroundWhitelist() throws RemoteException {
        return setUidPolicy(4);
    }

    private int removeRestrictBackgroundWhitelist() throws RemoteException {
        return resetUidPolicy("not whitelisted", 4);
    }

    private int addRestrictBackgroundBlacklist() throws RemoteException {
        return setUidPolicy(1);
    }

    private int removeRestrictBackgroundBlacklist() throws RemoteException {
        return resetUidPolicy("not blacklisted", 1);
    }

    private int listWifiNetworks() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        int match;
        if (arg == null) {
            match = 0;
        } else if (Boolean.parseBoolean(arg)) {
            match = 1;
        } else {
            match = 2;
        }
        for (WifiConfiguration config : this.mWifiManager.getConfiguredNetworks()) {
            if (arg == null || config.meteredOverride == match) {
                pw.print(NetworkPolicyManager.resolveNetworkId(config));
                pw.print(';');
                pw.println(overrideToString(config.meteredOverride));
            }
        }
        return 0;
    }

    private int setMeteredWifiNetwork() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String networkId = getNextArg();
        if (networkId == null) {
            pw.println("Error: didn't specify networkId");
            return -1;
        }
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify meteredOverride");
            return -1;
        }
        this.mInterface.setWifiMeteredOverride(NetworkPolicyManager.resolveNetworkId(networkId), stringToOverride(arg));
        return -1;
    }

    private static String overrideToString(int override) {
        switch (override) {
            case 1:
                return "true";
            case 2:
                return "false";
            default:
                return "none";
        }
    }

    private static int stringToOverride(String override) {
        if (override.equals("true")) {
            return 1;
        }
        if (override.equals("false")) {
            return 2;
        }
        return 0;
    }

    private int getNextBooleanArg() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify BOOLEAN");
            return -1;
        }
        return Boolean.valueOf(arg).booleanValue() ? 1 : 0;
    }

    private int getUidFromNextArg() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify UID");
            return -1;
        }
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            pw.println("Error: UID (" + arg + ") should be a number");
            return -2;
        }
    }
}
