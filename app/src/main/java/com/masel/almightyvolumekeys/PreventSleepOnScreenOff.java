package com.masel.almightyvolumekeys;

/**
 * Keep cpu (and volume keys) on after screen off, for minimum time defined in user-settings.
 */
class PreventSleepOnScreenOff {

    static void init(MyContext myContext) {
        myContext.deviceState.addScreenOffCallback(() -> acquireWakeLock(myContext));
        myContext.deviceState.addAllowSleepCallback(() -> releaseWakeLock(myContext));
        myContext.deviceState.addScreenOnCallback(() -> releaseWakeLock(myContext));
    }

    private static void acquireWakeLock(MyContext myContext) {
        if (!myContext.wakeLock.isHeld()) myContext.wakeLock.acquire();
    }
    private static void releaseWakeLock(MyContext myContext) {
        if (myContext.wakeLock.isHeld()) myContext.wakeLock.release();
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
