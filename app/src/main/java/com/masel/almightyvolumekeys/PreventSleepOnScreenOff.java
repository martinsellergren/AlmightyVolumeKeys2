package com.masel.almightyvolumekeys;

import com.masel.rec_utils.RecUtils;

/**
 * Keep cpu (and volume keys) on after screen off, for minimum time defined in user-settings.
 */
class PreventSleepOnScreenOff {

    static void init(MyContext myContext) {
        myContext.deviceState.addScreenOnCallback(() -> acquireWakeLock(myContext));
        myContext.deviceState.addMediaStartCallback(() -> acquireWakeLock(myContext));
        myContext.deviceState.addOnAllowSleepCallback(() -> releaseWakeLock(myContext));

        acquireWakeLock(myContext);
    }

    private static void acquireWakeLock(MyContext myContext) {
        try {
            if (!myContext.wakeLock.isHeld()) {
                RecUtils.log("Wakelock acquired");
                myContext.wakeLock.acquire();
            }
        }
        catch (Exception e) {
            RecUtils.log("Failed to acquire wakelock");
        }
    }
    private static void releaseWakeLock(MyContext myContext) {
        try {
            if (myContext.wakeLock.isHeld()) {
                RecUtils.log("Wakelock released");
                myContext.wakeLock.release();
            }
        }
        catch (Exception e) {
            RecUtils.log("Failed to release wakelock");
        }
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
