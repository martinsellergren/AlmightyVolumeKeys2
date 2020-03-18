package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Build;

import com.masel.rec_utils.RecUtils;

import java.util.ArrayList;
import java.util.List;

class VolumeUtils {
    private MyContext myContext;

    VolumeUtils(MyContext myContext) {
        this.myContext = myContext;
    }

    void setVolume(int stream, int volume, boolean showUi, boolean executeCallbacks) {
        int volumeChangeFlag = showUi ? AudioManager.FLAG_SHOW_UI : AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;

        try {
            myContext.audioManager.setStreamVolume(stream, volume, volumeChangeFlag);
        }
        catch (SecurityException e) {
            RecUtils.requestPermissionToSilenceDevice(myContext.context);
        }

        if (executeCallbacks) {
            for (OnVolumeSetCallback onVolumeSetCallback : onVolumeSetCallbackList) onVolumeSetCallback.onVolumeSet(stream, volume);
        }
    }
    void setVolume(int stream, int volume, boolean showUi) {
        setVolume(stream, volume, showUi, true);
    }

    int getVolume(int stream) {
        return myContext.audioManager.getStreamVolume(stream);
    }

    void setVolumePercentage(int stream, int volumePercentage, boolean showUi) {
        int minVolume = getMin(stream);
        int maxVolume = getMax(stream);
        int targetVolume = (int)Math.round(minVolume + (maxVolume - minVolume) * ((double)volumePercentage / 100d));
        targetVolume = Math.min(targetVolume, maxVolume);
        targetVolume = Math.max(targetVolume, minVolume);
        setVolume(stream, targetVolume, showUi);
    }

    int getVolumePercentage(int stream) {
        float minVolume = getMin(stream);
        float maxVolume = getMax(stream);
        int percentage = Math.round((getVolume(stream) - minVolume) / (maxVolume - minVolume) * 100);
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

    // region Callbacks on volume set

    interface OnVolumeSetCallback { void onVolumeSet(int stream, int volume); }

    private List<OnVolumeSetCallback> onVolumeSetCallbackList = new ArrayList<>();

    void addOnVolumeSetCallback(OnVolumeSetCallback onVolumeSet) {
        onVolumeSetCallbackList.add(onVolumeSet);
    }

    void removeOnVolumeSetCallback(OnVolumeSetCallback onVolumeSet) {
        onVolumeSetCallbackList.remove(onVolumeSet);
    }

    // endregion
}
