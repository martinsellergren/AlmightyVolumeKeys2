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

    void commit_noUI(MyContext myContext) {
        commit(myContext, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    void commit_UI(MyContext myContext) {
        commit(myContext, AudioManager.FLAG_SHOW_UI);
    }

    int getStream() {
        return stream;
    }

    void setStream(int stream) {
        this.stream = stream;
    }

    int getVolume() {
        return volume;
    }

    void setVolume(int volume) {
        this.volume = volume;
    }
}
