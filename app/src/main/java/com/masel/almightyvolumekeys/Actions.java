package com.masel.almightyvolumekeys;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
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

    static class AudioRecording_Start extends Action {
        @Override
        String getName() {
            return "Start recording audio";
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

    static class AudioRecording_StopAndSave extends Action {
        @Override
        String getName() {
            return "Stop and save recording";
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

    static class AudioRecording_StopAndTrash extends Action {
        @Override
        String getName() {
            return "Stop and trash recording";
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

    static class MediaControl_NextTrack extends Action {
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
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class MediaControl_PrevTrack extends Action {
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
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class MediaControl_Play extends Action {
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
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= 19;
        }
    }

    static class MediaControl_Pause extends Action {
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
        boolean isAvailable(MyContext myContext) {
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

    static class SoundMode_Sound extends Action {
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
            return NotifyOrder.NEVER;
        }
    }

    static class SoundMode_Vibrate extends Action {
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

    static class SoundMode_Silent extends Action {
        @Override
        String getName() {
            return "Sound mode: silent";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            try {
                myContext.audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
            catch (Exception e) {
                throw new ExecutionException("No permission");
            }
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

    static class SoundMode_ToggleVibrateSilent extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            switch (myContext.audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT: return new SoundMode_Vibrate();
                case AudioManager.RINGER_MODE_VIBRATE: return new SoundMode_Silent();
                default: return new SoundMode_Vibrate();
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

    static class DndMode_On extends Action {

        @Override
        String getName() {
            return "Do not disturb: ON";
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
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    static class DndMode_Off extends Action {
        @Override
        String getName() {
            return "Do not disturb: OFF";
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
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= 23;
        }
    }

    static class DndMode_Toggle extends MultiAction {
        @Override
        Action pickAction(MyContext myContext) {
            if (dndIsOn(myContext.notificationManager)) return new Actions.DndMode_Off();
            else return new Actions.DndMode_On();
        }

        @Override
        String[] getNeededPermissions() {
            if (Build.VERSION.SDK_INT >= 23) {
                return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            else return new String[]{};
        }

        @Override
        boolean isAvailable(MyContext myContext) {
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

    static class Flashlight_On extends Action {

        private static final int minApi = 23;

        @Override
        String getName() {
            return "Flashlight: on";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            if (Build.VERSION.SDK_INT < minApi) return;

            try {
                CameraManager camManager = (CameraManager) myContext.context.getSystemService(Context.CAMERA_SERVICE);
                String cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, true);
            }
            catch (CameraAccessException e) {
                Utils.log(e.getMessage());
            }
        }

        @Override
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= minApi &&
                    myContext.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        }
    }

    static class Flashlight_Off extends Action {

        private static final int minApi = 23;

        @Override
        String getName() {
            return "Flashlight: off";
        }

        @Override
        void run(MyContext myContext) throws ExecutionException {
            if (Build.VERSION.SDK_INT < minApi) return;

            try {
                CameraManager camManager = (CameraManager) myContext.context.getSystemService(Context.CAMERA_SERVICE);
                String cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, false);
            }
            catch (CameraAccessException e) {
                Utils.log(e.getMessage());
            }
        }

        @Override
        boolean isAvailable(MyContext myContext) {
            return Build.VERSION.SDK_INT >= minApi &&
                    myContext.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        }
    }

    // endregion

    // region Tell time

    static class TellTime extends Action {

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
}
