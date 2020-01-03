package com.masel.almightyvolumekeys;

import android.bluetooth.BluetoothAdapter;

class MyBluetooth {

    static boolean enable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return false;
        if (adapter.isEnabled()) return true;
        return adapter.enable();
    }

    static boolean disable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return false;
        if (!adapter.isEnabled()) return true;
        return adapter.disable();
    }

    static boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return false;
        return adapter.isEnabled();
    }

    static boolean isAvailable() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }
}
