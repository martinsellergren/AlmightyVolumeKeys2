package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Handler;

import com.masel.rec_utils.RecUtils;

class VolumeMovement {

    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 50;

    private AudioManager audioManager;

    private Handler handler = new Handler();

    VolumeMovement(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    void start(int audioStream, boolean volumeUp) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> stepVolume(audioStream, volumeUp), 0);
    }

    private void stepVolume(int audioStream, boolean volumeUp) {
        handler.removeCallbacksAndMessages(null);

        int volumePercentage = RecUtils.getStreamVolumePercentage(audioManager, audioStream);
        if ((volumePercentage == 100 && volumeUp) || (volumePercentage == 0 && !volumeUp)) {
            return;
        }

        Utils.adjustVolume_withFallback(audioManager, audioStream, volumeUp, true);
        handler.postDelayed(() -> stepVolume(audioStream, volumeUp), LONG_PRESS_VOLUME_CHANGE_TIME);
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
    }
}
