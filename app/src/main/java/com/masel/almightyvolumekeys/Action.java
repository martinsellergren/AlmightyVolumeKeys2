package com.masel.almightyvolumekeys;

import android.os.Handler;

abstract class Action {

    enum NotifyOrder { BEFORE, AFTER, ANY }

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

    NotifyOrder getNotifyOrder() {
        return NotifyOrder.ANY;
    }

    /**
     * @return Vibration pattern for action-notifier.
     */
    Notifier.VibrationPattern getVibrationPattern() {
        //myContext.notifier.notify(getName(), Notifier.VibrationPattern.ON, true);
        return Notifier.VibrationPattern.ON;
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

    /**
     * Execute an action and show corresponding notifier.
     * @param action
     */
    static void execute(MyContext myContext, Action action) throws ExecutionException {
        switch (action.getNotifyOrder()) {
            case ANY:
                myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
                action.run(myContext);
            case BEFORE:
                myContext.notifier.notify(action.getName(), action.getVibrationPattern(), true);
                action.run(myContext);
            case AFTER:
                action.run(myContext);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
                    }
                }, 10);
        }
    }
}
