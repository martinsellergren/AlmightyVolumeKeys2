package com.masel.almightyvolumekeys;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

abstract class Action {

    enum NotifyOrder { BEFORE, AFTER, ANY, NEVER }

    static class ExecutionException extends Exception {
        ExecutionException(String msg) {
            super(msg);
        }
    }

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
     * @return Vibration pattern for notifier.
     */
    Notifier.VibrationPattern getVibrationPattern() {
        return Notifier.VibrationPattern.ON;
    }

    /**
     * @return Permissions necessary for this action. Default: none.
     */
    String[] getNeededPermissions() {
        return new String[]{};
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
                break;
            case BEFORE:
                myContext.notifier.notify(action.getName(), action.getVibrationPattern(), true);
                action.run(myContext);
                break;
            case AFTER:
                action.run(myContext);

                final long WAIT_BEFORE_NOTIFY = 10;
                new Handler().postDelayed(() -> myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false), WAIT_BEFORE_NOTIFY);
                break;
            case NEVER:
                action.run(myContext);
        }
    }
}
