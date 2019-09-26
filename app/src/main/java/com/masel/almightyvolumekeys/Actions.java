package com.masel.almightyvolumekeys;

import android.Manifest;
import android.app.NotificationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;

/**
 * Defines actions.
 */
class Actions {

    private Actions() {}

    // region Audio recording

    static class AudioRecording_Start extends Action {
        @Override
        public String getName() {
            return "Start recording audio";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {
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
        public String getName() {
            return "Stop recording audio and save";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {
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

    static class AudioRecording_StopAndDiscard extends Action {
        @Override
        public String getName() {
            return "Stop recording audio and discard";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.stopAndDiscard();
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
        public String getName() {
            return "Play next media track";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {

        }
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
            return NotifyOrder.BEFORE;
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
            return NotifyOrder.BEFORE;
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

    // region DND mode (Problem: can't seem to use the user-defined dnd-settings. Better with audio-ringer-mode silent.)

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
            return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
        }

        @Override
        int getMinApiLevel() {
            return 23;
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
            return NotifyOrder.AFTER;
        }

        @Override
        String[] getNeededPermissions() {
            return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
        }

        @Override
        int getMinApiLevel() {
            return 23;
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
            return new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
        }

        @Override
        int getMinApiLevel() {
            return 23;
        }
    }

    private static void setDnd(NotificationManager notificationManager, boolean on) {
        if (Build.VERSION.SDK_INT < 23) return;

        if (on) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
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
