package com.example.almightyvolumekeys;

abstract class Action {

    int[] DEFAULT_VIBRATION_PATTERN = new int[]{300};

    abstract String getName();
    abstract void run();

    String getDescription() {
        return null;
    }

    /**
     * @return Any extra info notes about this action.
     */
    String[] getNotes() {
        return null;
    }

    /**
     * @return Vibration pattern when this action executes. [0]=vib-time-ms, [1]=silence-time-ms, ...
     */
    int[] getVibrationPattern() {
        return DEFAULT_VIBRATION_PATTERN;
    }
}
