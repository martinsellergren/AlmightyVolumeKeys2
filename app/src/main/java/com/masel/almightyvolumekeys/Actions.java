package com.masel.almightyvolumekeys;

import android.Manifest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.RemoteException;

import java.util.List;

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


    static class MediaControl_NextTrack extends Action {
        @Override
        public String getName() {
            return "Play next media track";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {

        }
    }

    // region SoundMode

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
