package com.masel.almightyvolumekeys;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

/**
 * Defines actions.
 */
class Actions {

    private Actions() {}

    static class AudioRecording_Start extends Action {
        @Override
        public String getName() {
            return "Start recording audio";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {
            myContext.audioRecorder.start();
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
    }

    static class MediaControl_NextTrack extends Action {
        @Override
        public String getName() {
            return "Play next media track";
        }

        @Override
        public void run(MyContext myContext) throws Action.ExecutionException {

        }
    }

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
