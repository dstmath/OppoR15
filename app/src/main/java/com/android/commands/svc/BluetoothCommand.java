package com.android.commands.svc;

import android.bluetooth.BluetoothAdapter;
import com.android.commands.svc.Svc.Command;

public class BluetoothCommand extends Command {
    public BluetoothCommand() {
        super("bluetooth");
    }

    public String shortHelp() {
        return "Control Bluetooth service";
    }

    public String longHelp() {
        return shortHelp() + "\n" + "\n" + "usage: svc bluetooth [enable|disable]\n" + "         Turn Bluetooth on or off.\n\n";
    }

    public void run(String[] args) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            System.err.println("Got a null BluetoothAdapter, is the system running?");
            return;
        }
        if (args.length == 2 && "enable".equals(args[1])) {
            adapter.enable();
        } else if (args.length == 2 && "disable".equals(args[1])) {
            adapter.disable();
        } else {
            System.err.println(longHelp());
        }
    }
}
