package com.masel.almightyvolumekeys;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.view.KeyEvent;

import com.masel.rec_utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Defines actions.
 */
class Actions {

    private Actions() {}

    // region Audio recording

    static class Sound_recorder__start extends Action {
        @Override
        String getName() {
            return "Sound recorder: start";
        }

        @Override
        void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.start();
        }

        @Override
        Action.NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions() {
            return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    static class Sound_recorder__stop_and_save extends Action {
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
        String[] getNeededPermissions() {
            return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    static class Sound_recorder__stop_and_trash extends Action {
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
        String[] getNeededPermissions() {
            return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    // endregion

    // region Media control

    static class Media__next extends Action {
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

    static class Media__previous extends Action {
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

    static class Media__play extends Action {
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

    static class Media__pause extends Action {
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

    static class Sound_mode__sound extends Action {
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
    }

    static class Sound_mode__vibrate extends Action {
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
    }

    static class Sound_mode__silent extends Action {
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
            return Notifier.VibrationPattern.SILENCE;
        }

        @Override
        NotifyOrder getNotifyOrder() {
            return NotifyOrder.BEFORE;
        }

        @Override
        String[] getNeededPermissions() {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }
    }

    static class Sound_mode__toggle_sound_silent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new Sound_mode__sound();
                case AudioManager.RINGER_MODE_NORMAL: return new Sound_mode__silent();
                default: return new Sound_mode__sound();
            }
        }

        @Override
        String[] getNeededPermissions() {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }
    }

    static class Sound_mode__toggle_vibrate_silent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new Sound_mode__vibrate();
                case AudioManager.RINGER_MODE_VIBRATE: return new Sound_mode__silent();
                default: return new Sound_mode__vibrate();
            }
        }

        @Override
        String[] getNeededPermissions() {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }
    }

    // endregion

    // region DND mode

    static class Do_not_disturb__on extends Action {

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
        String[] getNeededPermissions() {
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

    static class Do_not_disturb__off extends Action {
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
        String[] getNeededPermissions() {
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

    static class Do_not_disturb__toggle extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            if (dndIsOn(myContext.notificationManager)) return new Do_not_disturb__off();
            else return new Do_not_disturb__on();
        }

        @Override
        String[] getNeededPermissions() {
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

    // region Flashlight

    static class Flashlight__on extends Action {
        @Override
        String getName() {
            return "Flashlight: on";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean success = myContext.flashlight.turnOn();
            if (!success) throw new ExecutionException("Flashlight error");
        }

        @Override
        boolean isAvailable(Context context) {
            return new Flashlight(context).isAvailable();
        }
    }

    static class Flashlight__off extends Action {
        @Override
        String getName() {
            return "Flashlight: off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            boolean success = myContext.flashlight.turnOff();
            if (!success) throw new ExecutionException("Flashlight error");
        }

        @Override
        Notifier.VibrationPattern getVibrationPattern() {
            return Notifier.VibrationPattern.OFF;
        }

        @Override
        boolean isAvailable(Context context) {
            return new Flashlight(context).isAvailable();
        }
    }

    static class Flashlight__toggle extends MultiAction {

        @Override
        Action pickAction(MyContext myContext) {
            return myContext.flashlight.isOn() ? new Flashlight__off() : new Flashlight__on();
        }

        @Override
        boolean isAvailable(Context context) {
            return new Flashlight(context).isAvailable();
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
     * @param name As defined in res/values/array.xml
     * @return Action with specified ~class-name
     */
    static Action getActionFromName(String name) {
        if (name.equals("No action")) return null;

        name = name.replace(": ", "__");
        name = name.replace(" ", "_");
        name = "com.masel.almightyvolumekeys.Actions$" + name;
        try {
            return (Action)Class.forName(name).newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
