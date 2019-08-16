package com.masel.almightyvolumekeys;

abstract class Action {

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
    void notify(MyContext myContext) {
        myContext.notifier.notify(getName(), Notifier.VibrationPattern.ON, false);
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
