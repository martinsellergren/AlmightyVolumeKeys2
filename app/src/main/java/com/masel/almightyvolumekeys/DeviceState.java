package com.masel.almightyvolumekeys;

import android.media.AudioManager;

import com.masel.rec_utils.RecUtils;

/**
 * Defines device states. States are mutually exclusive.
 */
enum DeviceState {
    IDLE,
    MUSIC,
//    RINGING,
//    IN_CALL,
    SOUNDREC,
    CAMERA,
    OTHER;

    /**
     * @return Current device state. If API<26, may return IDLE when should return OTHER (e.g when timer sounding).
     */
    static DeviceState getCurrent(MyContext myContext) {
        AudioManager manager = myContext.audioManager;
        int activeAudioStream = RecUtils.getActiveAudioStream(manager);

        if (myContext.isCameraActive()) return CAMERA;
        if (manager.isMusicActive()) return MUSIC;
        if (myContext.audioRecorder.isRecording()) return SOUNDREC;
        if (manager.getMode() == AudioManager.MODE_RINGTONE) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_CALL) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) return OTHER;
        if (activeAudioStream != AudioManager.USE_DEFAULT_STREAM_TYPE) return OTHER;
        else return IDLE;
    }
}
