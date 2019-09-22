package com.masel.almightyvolumekeys;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

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
            case NEVER:
                action.run(myContext);
        }
    }

    // region Wait on DND

    private static void vibrateAfterWaitOnDnd(MyContext myContext, Action action) {
        if (Build.VERSION.SDK_INT >= 23) {
            interruptionFilterMethod(myContext, action);
        }
        else {
            ringerModeMethod(myContext, action);
        }
    }

    private static void interruptionFilterMethod(MyContext myContext, Action action) {
        if (Build.VERSION.SDK_INT < 23) return;

        final NotificationManager notificationManager = (NotificationManager) myContext.context.getSystemService(Context.NOTIFICATION_SERVICE);
        int currentFilter = notificationManager.getCurrentInterruptionFilter();
        if (currentFilter != NotificationManager.INTERRUPTION_FILTER_NONE) {
            myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
        }
        else {
            BroadcastReceiver interruptionFilterChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Build.VERSION.SDK_INT < 23) return;
                    if (!isInitialStickyBroadcast() && notificationManager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_NONE) {
                        myContext.context.unregisterReceiver(this);
                        myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
                    }
                }
            };

            myContext.context.registerReceiver(interruptionFilterChangedReceiver, new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED));
            new Handler().postDelayed(() -> {
                try {
                    myContext.context.unregisterReceiver(interruptionFilterChangedReceiver);
                }
                catch (Exception e) {}
            }, MAX_WAIT_ON_DND);
        }
    }

    private static void ringerModeMethod(MyContext myContext, Action action) {
        if (Build.VERSION.SDK_INT >= 23) return;

        final AudioManager audioManager = (AudioManager) myContext.context.getSystemService(Context.AUDIO_SERVICE);
        int currentRingerMode = audioManager.getRingerMode();
        if (currentRingerMode != AudioManager.RINGER_MODE_SILENT) {
            myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
        }
        else {
            BroadcastReceiver ringerModeChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!isInitialStickyBroadcast() && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        myContext.context.unregisterReceiver(this);
                        myContext.notifier.notify(action.getName(), action.getVibrationPattern(), false);
                    }
                }
            };

            myContext.context.registerReceiver(ringerModeChangedReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
            new Handler().postDelayed(() -> {
                try {
                    myContext.context.unregisterReceiver(ringerModeChangedReceiver);
                }
                catch (Exception e) {}
            }, MAX_WAIT_ON_DND);
        }
    }

    // endregion
}
