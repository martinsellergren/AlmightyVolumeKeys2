package com.masel.almightyvolumekeys;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

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
            case AFTER_WAIT_ON_DND:
                action.run(myContext);
                final long MIN_WAIT_BEFORE_NOTIFY = 10;
                new Handler().postDelayed(() -> vibrateAfterWaitOnDnd(myContext, action), MIN_WAIT_BEFORE_NOTIFY);
                break;
            case NEVER:
                action.run(myContext);
        }
    }

    // region Wait on silent device

    private static void vibrateAfterWaitOnDnd(MyContext myContext, Action action) {
        if (!currentlySilent(myContext)) {
            myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
        }
        else {
            BroadcastReceiver stateChangeListener = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!isInitialStickyBroadcast() && !currentlySilent(myContext)) {
                        myContext.context.unregisterReceiver(this);
                        myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= 23) {
                myContext.context.registerReceiver(stateChangeListener, new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED));
            }
            myContext.context.registerReceiver(stateChangeListener, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));

            new Handler().postDelayed(() -> {
                try {
                    myContext.context.unregisterReceiver(stateChangeListener);
                }
                catch (Exception e) {}
            }, MAX_WAIT_ON_DND);
        }
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

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
