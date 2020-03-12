package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Build;

import com.masel.rec_utils.RecUtils;

class VolumeUtils {
    private MyContext myContext;
    private Runnable onVolumeSet = null;

    VolumeUtils(MyContext myContext) {
        this.myContext = myContext;
    }

    void setOnVolumeSet(Runnable onVolumeSet) {
        this.onVolumeSet = onVolumeSet;
    }

    void set(int stream, int volume, boolean showUi, boolean runOnVolumeSetAction) {
        int volumeChangeFlag = showUi ? AudioManager.FLAG_SHOW_UI : AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;

        try {
            myContext.audioManager.setStreamVolume(stream, volume, volumeChangeFlag);
        }
        catch (SecurityException e) {
            RecUtils.requestPermissionToSilenceDevice(myContext.context);
        }

        if (runOnVolumeSetAction && onVolumeSet != null) onVolumeSet.run();
    }
    void set(int stream, int volume, boolean showUi) {
        set(stream, volume, showUi, true);
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
}
