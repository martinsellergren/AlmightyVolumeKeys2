package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Handler;

import com.masel.rec_utils.RecUtils;

class VolumeMovement {

    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 50;

    private MyContext myContext;
    private Handler handler = new Handler();

    VolumeMovement(MyContext myContext) {
        this.myContext = myContext;
    }

    void start(int audioStream, boolean volumeUp, boolean showUi) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> stepVolume(audioStream, volumeUp, showUi), 0);
    }

    private void stepVolume(int audioStream, boolean volumeUp, boolean showUi) {
        handler.removeCallbacksAndMessages(null);

        int volumePercentage = RecUtils.getStreamVolumePercentage(myContext.audioManager, audioStream);
        if ((volumePercentage == 100 && volumeUp) || (volumePercentage == 0 && !volumeUp)) {
            return;
        }

        Utils.adjustVolume_withFallback(myContext, audioStream, volumeUp, showUi);
        handler.postDelayed(() -> stepVolume(audioStream, volumeUp, showUi), LONG_PRESS_VOLUME_CHANGE_TIME);
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
    }
}
