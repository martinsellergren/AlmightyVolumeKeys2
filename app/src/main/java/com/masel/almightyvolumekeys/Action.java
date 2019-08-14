package com.masel.almightyvolumekeys;

abstract class Action {

    static final long[] VIBRATION_PATTERN_ON = new long[]{0,200};
    static final long[] VIBRATION_PATTERN_OFF = new long[]{0,500,100,500};
    static final long[] VIBRATION_PATTERN_ERROR = new long[]{0,100,10,100,10,100};

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
        return new MyVibrator(getName(), VIBRATION_PATTERN_ON, false);
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
