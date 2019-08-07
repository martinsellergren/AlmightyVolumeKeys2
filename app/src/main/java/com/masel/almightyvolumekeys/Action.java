package com.masel.almightyvolumekeys;

abstract class Action {

    static final int[] VIBRATION_PATTERN_ON = new int[]{100};
    static final int[] VIBRATION_PATTERN_OFF = new int[]{400};
    static final int[] VIBRATION_PATTERN_ERROR = new int[]{100,10,100,10,100};

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

    /**
     * Default implementation provides async on-vibration. Override otherwise.
     */
    MyVibrator getVibration() {
        return new MyVibrator(VIBRATION_PATTERN_ON, false);
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
