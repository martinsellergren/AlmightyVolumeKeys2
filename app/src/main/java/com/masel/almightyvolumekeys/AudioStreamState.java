package com.masel.almightyvolumekeys;

import androidx.annotation.NonNull;

class AudioStreamState {
    private int stream;
    private int volume;

    private int ringerMuteFlag = 0;

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

    /**
     * Also specify that ring-stream is muted. Not set means ignore.
     */
    void setRingerMuteFlag() {
        ringerMuteFlag = 1;
    }
    int getRingerMuteFlag() {
        return ringerMuteFlag;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Stream %s, Volume %s", stream, volume);
    }
}
