package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.masel.rec_utils.RecUtils;

import java.util.Calendar;

/**
 * Keep cpu (and volume keys) on after screen off, for minimum time defined in user-settings.
 */
class PreventSleepOnScreenOff {

    static void init(MyContext myContext) {
        myContext.deviceState.addScreenOffCallback(() -> {
            int preventSleepMinutes = myContext.sharedPreferences.getInt("SeekBarPreference_preventSleepTimeout", 60);
            boolean allowSleepSwitch = myContext.sharedPreferences.getBoolean("SwitchPreferenceCompat_allowSleep", false);
            int allowSleepStartHour = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepStart", 0);
            int allowSleepEndHour = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepEnd", 0);

            long timeout = preventSleepMinutes * 60000;
            boolean allowSleep = allowSleepSwitch && currentlyAllowSleep(allowSleepStartHour, allowSleepEndHour);

            if (!allowSleep) {
                myContext.wakeLock.acquire(timeout);
                RecUtils.log("Wake lock acquired for (minutes): " + preventSleepMinutes);
            }
            else {
                RecUtils.log("Wake lock not acquired (prevention bypassed by user settings)");
            }
        });
    }

    private static boolean currentlyAllowSleep(int allowStartHour, int allowStopHour) {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hourInterval(allowStartHour, allowStopHour) > hourInterval(allowStartHour, currentHour);
    }

    private static int hourInterval(int from, int to) {
        return from <= to ? to - from : (to + 24) - from;
    }


    static boolean sleepCurrentlyPrevented(MyContext myContext) {
        try {
            return myContext.wakeLock.isHeld();
        }
        catch (Exception e) {
            return false;
        }
    }
}
