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

    public class DefaultVolume_Up implements Action {
        String name = "Increase default volume";
        String description = null;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public void run() {

        }
    }

    public class AudioRecording_Start implements Action {
        String name = "Start recording audio";
        String description = null;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public void run() {

            audioRecorder.start();
        }
    }

    public class AudioRecording_StopAndSave implements Action {
        String name = "Stop recording audio and save";
        String description = null;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public void run() {

            audioRecorder.stopAndSave();
        }
    }

    public class MediaControl_NextTrack implements Action {
        String name = "Stop recording audio and save";
        String description = null;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public void run() {

        }
    }
}
