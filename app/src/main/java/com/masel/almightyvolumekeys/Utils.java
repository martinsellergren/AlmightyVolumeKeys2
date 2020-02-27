package com.masel.almightyvolumekeys;

import android.media.AudioManager;

import com.masel.rec_utils.RecUtils;

class Utils {

    static void adjustVolume_withFallback(AudioManager audioManager, int stream, boolean volumeUp, boolean showUi) {
        int dir = volumeUp ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        int volumeChangeFlag = showUi ? AudioManager.FLAG_SHOW_UI : AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;

        try {
            audioManager.adjustStreamVolume(stream, dir, volumeChangeFlag);
        }
        catch (SecurityException e) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, volumeChangeFlag);
        }
    }

    static int loadVolumeKeysAudioStream(MyContext myContext) {
        String value = myContext.sharedPreferences.getString("ListPreference_VolumeKeysChange", null);
        int backupStream = AudioManager.STREAM_MUSIC;
        if (value == null) return backupStream;

        if (value.equals("Ringtone volume")) return AudioManager.STREAM_RING;
        if (value.equals("Media volume after 5 clicks")) return AudioManager.STREAM_MUSIC;
        if (value.equals("Ringtone volume after 5 clicks")) return AudioManager.STREAM_RING;
        else return AudioManager.STREAM_MUSIC;
    }


    static boolean loadFiveClicksBeforeVolumeChange(MyContext myContext) {
        String value = myContext.sharedPreferences.getString("ListPreference_VolumeKeysChange", null);
        if (value == null) return false;
        return value.equals("Media volume after 5 clicks") ||
                value.equals("Ringtone volume after 5 clicks");
    }

    static boolean loadDefaultVolumeKeyActionWhenCameraActive(MyContext myContext) {
        return myContext.sharedPreferences.getBoolean("SwitchPreferenceCompat_defaultVolumeKeyActionWhenCameraActive", true);
    }

    /**
     * @return Audio stream to be adjusted on a volume changing key-event.
     */
    static int getRelevantAudioStream(MyContext myContext) {
        int activeStream = RecUtils.getActiveAudioStream(myContext.audioManager);
        if (activeStream != AudioManager.USE_DEFAULT_STREAM_TYPE) {
            return activeStream;
        }
        else {
            return Utils.loadVolumeKeysAudioStream(myContext);
        }
    }
}
