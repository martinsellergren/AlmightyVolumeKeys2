package com.masel.almightyvolumekeys;

public class MyVibrator {

    /**
     * [0]=vib-time-ms, [1]=silence-time-ms, ... */
    private int[] pattern;

    /**
     * Sleep thread until vib done. */
    private boolean wait;

    MyVibrator(int[] pattern, boolean wait) {
        this.pattern = pattern;
        this.wait = wait;
    }

    void vibrate() {
        // todo
    }

    void interrupt() {
        // todo
    }
}
