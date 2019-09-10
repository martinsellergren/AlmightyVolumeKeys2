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
     * @return Vibration pattern for notifier.
     */
    Notifier.VibrationPattern getVibrationPattern() {
        return Notifier.VibrationPattern.ON;
    }

    static class ExecutionException extends Exception {
        //final boolean lacksPermission;
//
//        ExecutionException(String msg, boolean lacksPermission) {
//            super(msg);
//            this.lacksPermission = lacksPermission;
//        }

        ExecutionException(String msg) {
            super(msg);
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
                break;
            case BEFORE:
                myContext.notifier.notify(action.getName(), action.getVibrationPattern(), true);
                action.run(myContext);
                break;
            case AFTER:
                action.run(myContext);
                new Handler().postDelayed(() -> myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false), 10);
        }
    }
}
