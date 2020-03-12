package com.masel.almightyvolumekeys;

class AudioStreamState {
    private int stream;
    private int volume;

    AudioStreamState(int stream, int volume) {
        this.stream = stream;
        this.volume = volume;
    }

    AudioStreamState(VolumeUtils volumeUtils, int stream) {
        this(stream, volumeUtils.get(stream));
    }

    int getStream() {
        return stream;
    }

    int getVolume() {
        return volume;
    }
}
