package com.masel.almightyvolumekeys;

import android.media.AudioManager;

class Utils {

    static int loadVolumeClicksAudioStream(MyContext myContext) {
        String value = myContext.sharedPreferences.getString("ListPreference_VolumeClicksChange", null);
        int backupStream = AudioManager.STREAM_MUSIC;
        if (value == null) return backupStream;

        if (value.equals("Ringtone volume")) return AudioManager.STREAM_RING;
        if (value.equals("Media volume after 5 clicks")) return AudioManager.STREAM_MUSIC;
        if (value.equals("Ringtone volume after 5 clicks")) return AudioManager.STREAM_RING;
        else return AudioManager.STREAM_MUSIC;
    }


    static boolean loadFiveClicksBeforeVolumeChange(MyContext myContext) {
        String value = myContext.sharedPreferences.getString("ListPreference_VolumeClicksChange", null);
        if (value == null) return false;
        return value.equals("Media volume after 5 clicks") ||
                value.equals("Ringtone volume after 5 clicks");
    }

    static int loadVolumeLongPressAudioStream(MyContext myContext) {
        String value = myContext.sharedPreferences.getString("ListPreference_LongVolumePressChanges", null);
        if (value == null || value.equals("Ringtone volume")) return AudioManager.STREAM_RING;
        else return AudioManager.STREAM_MUSIC;
    }

    static boolean loadDefaultVolumeKeyActionWhenCameraActive(MyContext myContext) {
        return myContext.sharedPreferences.getBoolean("SwitchPreferenceCompat_defaultVolumeKeyActionWhenCameraActive", true);
    }
}
