package com.example.almightyvolumekeys;

import android.content.Context;
import android.util.Log;

/**
 * Executes the actions.
 */
public class Actions {

    private Context context;
    private AudioRecorder audioRecorder;

    public Actions(Context context) {
        this.context = context;
        this.audioRecorder = new AudioRecorder(context);
    }

    public class DefaultVolume_Up extends Action {
        @Override
        public String getName() {
            return "Increase default volume";
        }

        @Override
        public void run() {

        }
    }

    public class AudioRecording_Start extends Action {
        @Override
        public String getName() {
            return "Start recording audio";
        }

        @Override
        public void run() {
            audioRecorder.start();
        }
    }

    public class AudioRecording_StopAndSave extends Action {
        @Override
        public String getName() {
            return "Stop recording audio and save";
        }

        @Override
        public void run() {
            audioRecorder.stopAndSave();
        }
    }

    public class MediaControl_NextTrack extends Action {
        @Override
        public String getName() {
            return "Play next media track";
        }

        @Override
        public void run() {

        }
    }
}
