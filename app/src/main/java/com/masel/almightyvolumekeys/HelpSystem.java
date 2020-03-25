package com.masel.almightyvolumekeys;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class HelpSystem {
    private HelpSystem(){}

    // region Long press heads-up

    private static final String LONG_PRESS_HEADS_UP_SHOWN = "LONG_PRESS_HEADS_UP_SHOWN";

    static boolean isLongPressHeadsUpAppropriate(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return !sharedPreferences.getBoolean(LONG_PRESS_HEADS_UP_SHOWN, false);
    }

    static void longPressHeadsUpShown(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(LONG_PRESS_HEADS_UP_SHOWN, true).apply();
    }

    // endregion
}
