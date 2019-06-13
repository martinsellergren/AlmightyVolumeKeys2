package com.example.almightyvolumekeys;

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
        public void run(MyContext myContext) {
        }
    }

    static class AudioRecording_StopAndSave extends Action {
        @Override
        public String getName() {
            return "Stop recording audio and save";
        }

        @Override
        public void run(MyContext myContext) {
        }
    }

    static class MediaControl_NextTrack extends Action {
        @Override
        public String getName() {
            return "Play next media track";
        }

        @Override
        public void run(MyContext myContext) {

        }
    }
}
