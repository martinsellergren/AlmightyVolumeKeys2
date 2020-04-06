package com.masel.almightyvolumekeys;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;

abstract class Action {

    /**
     * Wait this long for do-not-disturb no be disabled, then skip vibration.
     */
    private static final long MAX_WAIT_ON_DND = 1000;

    /**
     * Notify before/ after the action-execution.
     * AFTER_WAIT_ON_DND: Vibrate after execution, but also wait for do-not-disturb-mode to be disabled.
     */
    enum NotifyOrder { BEFORE, AFTER, ANY, NEVER, AFTER_WAIT_ON_DND }


    static class ExecutionException extends Exception {
        ExecutionException(String msg) {
            super(msg);
        }
    }
    abstract String getName();
    abstract void run(MyContext myContext) throws ExecutionException;

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
    String[] getNeededPermissions(Context context) {
        return new String[]{};
    }

//    /**
//     * Action only available if device-api >= this.
//     */
//    int getMinApiLevel() {
//        return 0;
//    }

    /**
     * Action only available on systems where this returns true.
     */
    boolean isAvailable(Context context) {
        return true;
    }

    /**
     * Execute an action and show corresponding notifier.
     * @param action
     */
    static void execute(MyContext myContext, Action action) throws ExecutionException {
        switch (action.getNotifyOrder()) {
            case ANY:
                action.run(myContext);
                myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
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
            case AFTER_WAIT_ON_DND:
                action.run(myContext);
                final long MIN_WAIT_BEFORE_NOTIFY = 10;
                new Handler().postDelayed(() -> notifyAfterWaitOnDnd(myContext, action), MIN_WAIT_BEFORE_NOTIFY);
                break;
            case NEVER:
                action.run(myContext);
        }
    }

    // region Wait on silent device

    private static void notifyAfterWaitOnDnd(MyContext myContext, Action action) {
        if (!currentlySilent(myContext)) {
            myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
        }
        else {
            Handler onTimeOut = new Handler();
            BroadcastReceiver stateChangeListener = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!isInitialStickyBroadcast() && !currentlySilent(myContext)) {
                        onTimeOut.removeCallbacksAndMessages(null);
                        unregisterReceiverAndNotify(myContext, this, action);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= 23) {
                myContext.context.registerReceiver(stateChangeListener, new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED));
            }
            else {
                myContext.context.registerReceiver(stateChangeListener, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
            }

            onTimeOut.postDelayed(() -> unregisterReceiverAndNotify(myContext, stateChangeListener, action), MAX_WAIT_ON_DND);
        }
    }

    private static void unregisterReceiverAndNotify(MyContext myContext, BroadcastReceiver receiver, Action action) {
        try {
            myContext.context.unregisterReceiver(receiver);
        }
        catch (Exception e) {}
        myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
    }

    private static boolean currentlySilent(MyContext myContext) {
        if (Build.VERSION.SDK_INT >= 23) {
            return myContext.notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    myContext.audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
        }
        else {
            return myContext.audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
        }
    }

    // endregion
}
