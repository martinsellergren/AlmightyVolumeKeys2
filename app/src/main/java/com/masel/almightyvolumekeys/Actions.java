package com.masel.almightyvolumekeys;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.masel.rec_utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Defines actions.
 */
class Actions {

    private Actions() {}

    static class No_action extends Action {
        @Override
        String getName() {
            return "No action";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {}

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.NEVER;
        }
    }

    // region Audio recording

    static class Sound_recorder_start extends Action {
        @Override
        String getName() {
            return "Sound recorder: start";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            if (TheSoundRecorderConnection.appIsInstalled(myContext.context)) {
                myContext.audioRecorder.start();
            }
            else {
                Utils.openAppOnPlayStore(myContext.context, "com.masel.thesoundrecorder");
            }
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (TheSoundRecorderConnection.appIsInstalled(context)) {
                return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            }
            else {
                return new String[]{};
            }
        }
    }

    static class Sound_recorder_stop_and_save extends Action {
        @Override
        String getName() {
            return "Sound recorder: stop and save";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.stopAndSave();
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    static class Sound_recorder_stop_and_trash extends Action {
        @Override
        String getName() {
            return "Sound recorder: stop and trash";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.stopAndTrash();
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    // endregion

    // region Media control

    static class Media_next extends Action {
        @Override
        String getName() {
            return "Media: next";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            if (Build.VERSION.SDK_INT < 19) return;
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_NEXT);
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_previous extends Action {
        @Override
        String getName() {
            return "Media: previous";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            if (Build.VERSION.SDK_INT < 23) return;
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_play extends Action {
        @Override
        String getName() {
            return "Media: play";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            if (Build.VERSION.SDK_INT < 19) return;
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PLAY);
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class Media_pause extends Action {
        @Override
        String getName() {
            return "Media: pause";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            if (Build.VERSION.SDK_INT < 19) return;
            mediaClick(myContext, KeyEvent.KEYCODE_MEDIA_PAUSE);
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    private static void mediaClick(MyContext myContext, int keycode) {
        if (Build.VERSION.SDK_INT < 19) return;
        myContext.audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
        myContext.audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keycode));
    }

    // endregion

    // region Sound mode

    static class Sound_mode_sound extends Action {
        @Override
        String getName() {
            return "Sound mode: sound";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }
    }

    static class Sound_mode_vibrate extends Action {
        @Override
        String getName() {
            return "Sound mode: vibrate";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }
    }

    static class Sound_mode_silent extends Action {
        @Override
        String getName() {
            return "Sound mode: silent";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }
    }

    static class Sound_mode_sound_silent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new Sound_mode_sound();
                case AudioManager.RINGER_MODE_NORMAL: return new Sound_mode_silent();
                default: return new Sound_mode_sound();
            }
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }
    }

    static class Sound_mode_vibrate_silent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new Sound_mode_vibrate();
                case AudioManager.RINGER_MODE_VIBRATE: return new Sound_mode_silent();
                default: return new Sound_mode_vibrate();
            }
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }
    }

    // endregion

    // region DND mode

    static class Do_not_disturb_on extends Action {

        @Override
        String getName() {
            return "Do not disturb: on";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            setDnd(myContext.notificationManager, true);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.ON;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    static class Do_not_disturb_off extends Action {
        @Override
        String getName() {
            return "Do not disturb: off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            setDnd(myContext.notificationManager, false);
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.AFTER_WAIT_ON_DND;
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    static class Do_not_disturb extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            if (dndIsOn(myContext.notificationManager)) return new Do_not_disturb_off();
            else return new Do_not_disturb_on();
        }

        @Override
        String[] getNeededPermissions(Context context) {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(Context context) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    private static void setDnd(NotificationManager notificationManager, boolean on) {
        if (Build.VERSION.SDK_INT < 23) return;

        if (on) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        }
        else {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }

    private static boolean dndIsOn(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < 23) return false;
        return notificationManager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    // endregion

    // region MyFlashlight

    static class Flashlight_on extends Action {
        @Override
        String getName() {
            return "MyFlashlight: on";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean success = myContext.flashlight.turnOn();
            if (!success) throw new ExecutionException("MyFlashlight error");
        }

        @Override
        boolean isAvailable(Context context) {
            return new MyFlashlight(context).isAvailable();
        }
    }

    static class Flashlight_off extends Action {
        @Override
        String getName() {
            return "MyFlashlight: off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean success = myContext.flashlight.turnOff();
            if (!success) throw new ExecutionException("MyFlashlight error");
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        boolean isAvailable(Context context) {
            return new MyFlashlight(context).isAvailable();
        }
    }

    static class Flashlight extends MultiAction {

        @Override
        Action pickAction(MyContext myContext) {
            return myContext.flashlight.isOn() ? new Flashlight_off() : new Flashlight_on();
        }

        @Override
        boolean isAvailable(Context context) {
            return new MyFlashlight(context).isAvailable();
        }
    }

    // endregion

    // region Tell time

    static class Tell_time extends Action {

        @Override
        String getName() {
            return "Tell time";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            boolean res = myContext.voice.speak(currentTime);

            if (!res) throw new ExecutionException("Unknown tts-error");
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.NEVER;
        }

        @Override
        boolean isAvailable(Context context) {
            return true; // todo
        }
    }

    // endregion

    static class Beep extends Action {
        @Override
        String getName() {
            return "Beep";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        }
    }

    /**
     *
     * @param name As defined in res/values/array.xml, as returned by action.getName().
     * @return Action with specified ~class-name
     */
    @NonNull
    static Action getActionFromName(String name) {
        name = name.replaceAll("[^a-zA-Z]+", "_");
        name = "com.masel.almightyvolumekeys.Actions$" + name;
        try {
            return (Action)Class.forName(name).newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
