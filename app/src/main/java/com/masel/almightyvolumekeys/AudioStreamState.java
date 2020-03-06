package com.masel.almightyvolumekeys;

import android.media.AudioManager;

class AudioStreamState {

    private VolumeUtils volumeUtils;
    private int stream;
    private int volume;

    AudioStreamState(VolumeUtils volumeUtils, int stream, int volume) {
        this.volumeUtils = volumeUtils;
        this.stream = stream;
        this.volume = volume;
    }

    AudioStreamState(VolumeUtils volumeUtils, int stream) {
        this(volumeUtils, stream, volumeUtils.get(stream));
    }

    void commit(boolean showUi) {
        int flag = showUi ? AudioManager.FLAG_SHOW_UI : AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;
        volumeUtils.set(stream, volume, showUi);
    }

    int getStream() {
        return stream;
    }

    int getVolume() {
        return volume;
    }
}
