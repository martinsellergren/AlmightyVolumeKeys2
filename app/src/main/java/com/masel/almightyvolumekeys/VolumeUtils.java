package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

import com.masel.rec_utils.RecUtils;

class VolumeUtils {

    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 50;

    private MyContext myContext;
    private Runnable onVolumeSet = null;

    VolumeUtils(MyContext myContext) {
        this.myContext = myContext;
    }

    void setOnVolumeSet(Runnable onVolumeSet) {
        this.onVolumeSet = onVolumeSet;
    }

    void set(int stream, int volume, boolean showUi) {
        int volumeChangeFlag = showUi ? AudioManager.FLAG_SHOW_UI : AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;

        try {
            myContext.audioManager.setStreamVolume(stream, volume, volumeChangeFlag);
        }
        catch (SecurityException e) {
            RecUtils.requestPermissionToSilenceDevice(myContext.context);
        }

        if (onVolumeSet != null) onVolumeSet.run();
    }

    void adjust(int stream, boolean volumeUp, boolean showUi) {
        int dir = volumeUp ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        int volumeChangeFlag = showUi ? AudioManager.FLAG_SHOW_UI : AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;

        try {
            myContext.audioManager.adjustStreamVolume(stream, dir, volumeChangeFlag);
        }
        catch (SecurityException e) {
            RecUtils.requestPermissionToSilenceDevice(myContext.context);
        }

        if (onVolumeSet != null) onVolumeSet.run();
    }

    int get(int stream) {
        return myContext.audioManager.getStreamVolume(stream);
    }

    void setPercentage(int stream, int volumePercentage, boolean showUi) {
        int minVolume = getMin(stream);
        int maxVolume = getMax(stream);
        int targetVolume = (int)Math.round(minVolume + (maxVolume - minVolume) * ((double)volumePercentage / 100d));
        targetVolume = Math.min(targetVolume, maxVolume);
        targetVolume = Math.max(targetVolume, minVolume);
        set(stream, targetVolume, showUi);
    }

    int getPercentage(int stream) {
        float minVolume = getMin(stream);
        float maxVolume = getMax(stream);
        int percentage = Math.round((get(stream) - minVolume) / (maxVolume - minVolume) * 100);
        percentage = Math.min(percentage, 100);
        percentage = Math.max(percentage, 0);
        return percentage;
    }

    int getSteps(int stream) {
        return getMax(stream) - getMin(stream);
    }

    int getMin(int stream) {
        return Build.VERSION.SDK_INT >= 28 ? myContext.audioManager.getStreamMinVolume(stream) : 0;
    }

    int getMax(int stream) {
        return myContext.audioManager.getStreamMaxVolume(stream);
    }

//    // region Volume movement
//
//    private Handler movementHandler = new Handler();
//
//    void startMovement(int audioStream, boolean volumeUp, boolean showUi) {
//        movementHandler.removeCallbacksAndMessages(null);
//        movementHandler.postDelayed(() -> stepVolume(audioStream, volumeUp, showUi), 0);
//    }
//
//    private void stepVolume(int audioStream, boolean volumeUp, boolean showUi) {
//        movementHandler.removeCallbacksAndMessages(null);
//
//        int volumePercentage = RecUtils.getStreamVolumePercentage(myContext.audioManager, audioStream);
//        if ((volumePercentage == 100 && volumeUp) || (volumePercentage == 0 && !volumeUp)) {
//            return;
//        }
//
//        Utils.adjustVolume_withFallback(myContext, audioStream, volumeUp, showUi);
//        movementHandler.postDelayed(() -> stepVolume(audioStream, volumeUp, showUi), LONG_PRESS_VOLUME_CHANGE_TIME);
//    }
//
//    void stopMovement() {
//        movementHandler.removeCallbacksAndMessages(null);
//    }
//
//    // endregion
}
