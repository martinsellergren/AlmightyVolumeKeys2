package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import com.masel.rec_utils.Utils;

/**
 * Defines device states. States are mutually exclusive. Hijack volume keys only when in IDLE.
 * Else, volume keys change volume as usual (with additional action if registered).
 *
 * IDLE = nothing going on
 * OTHER = undefined sound: timer, rec video..
 */
enum DeviceState {
    IDLE,
    MUSIC,
//    RINGING,
//    IN_CALL,
    RECORDING_AUDIO,
    OTHER;

    /**
     * @return Current device state. If API<26, may return IDLE when should return OTHER (e.g when timer sounding).
     */
    static DeviceState getCurrent(MyContext myContext) {
        AudioManager manager = myContext.audioManager;
        int activeAudioStream = Utils.getActiveAudioStream(manager);

        if (myContext.audioRecorder.isRecording()) return DeviceState.RECORDING_AUDIO;
        if (manager.isMusicActive()) return DeviceState.MUSIC;
        if (manager.getMode() == AudioManager.MODE_RINGTONE) return DeviceState.OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_CALL) return DeviceState.OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) return DeviceState.OTHER;
        if (activeAudioStream != AudioManager.USE_DEFAULT_STREAM_TYPE) return DeviceState.OTHER;
        else return DeviceState.IDLE;
    }
}
