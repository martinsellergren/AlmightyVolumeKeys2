package com.masel.almightyvolumekeys;

import androidx.annotation.NonNull;

class AudioStreamState {
    private int stream;
    private int volume;

    AudioStreamState(int stream, int volume) {
        this.stream = stream;
        this.volume = volume;
    }

    AudioStreamState(VolumeUtils volumeUtils, int stream) {
        this(stream, volumeUtils.getVolume(stream));
    }

    int getStream() {
        return stream;
    }

    int getVolume() {
        return volume;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Stream %s, Volume %s", stream, volume);
    }
}
