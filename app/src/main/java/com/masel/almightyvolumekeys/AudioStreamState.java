package com.masel.almightyvolumekeys;

import android.media.AudioManager;

class AudioStreamState {

    private int stream;
    private int volume;

    AudioStreamState(int stream, int index) {
        this.stream = stream;
        this.volume = index;
    }

    AudioStreamState(AudioManager audioManager, int stream) {
        this(stream, audioManager.getStreamVolume(stream));
    }


    private void commit(MyContext myContext, int flag) {
        myContext.audioManager.setStreamVolume(stream, volume, flag);
    }

    void commit_noUi(MyContext myContext) {
        commit(myContext, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    void commit_Ui(MyContext myContext) {
        commit(myContext, AudioManager.FLAG_SHOW_UI);
    }

    int getStream() {
        return stream;
    }

    int getVolume() {
        return volume;
    }
}
