package com.example.almightyvolumekeys;

abstract class Action {

    /**
     * [0]=vib-time-ms, [1]=silence-time-ms, ...
     */
    static final int[] VIBRATION_PATTERN_ON = new int[]{300};
    static final int[] VIBRATION_PATTERN_OFF = new int[]{600};
    static final int[] VIBRATION_PATTERN_ERROR = new int[]{300,50,300,50,300};

    abstract String getName();
    abstract void run(MyContext myContext) throws ExecutionException;

    String getDescription() {
        return null;
    }

    /**
     * @return Any extra info notes about this action.
     */
    String[] getNotes() {
        return null;
    }

    static class ExecutionException extends Exception {
        final boolean lacksPermission;

        ExecutionException(String msg, boolean lacksPermission) {
            super(msg);
            this.lacksPermission = lacksPermission;
        }

        ExecutionException(String msg) {
            this(msg, false);
        }
    }
}
