package com.masel.almightyvolumekeys;

import android.app.KeyguardManager;
import android.media.AudioManager;
import android.os.PowerManager;

import com.masel.rec_utils.RecUtils;

/**
 * Defines device states. States are mutually exclusive.
 */
class DeviceState {
    static final int IDLE = 0;
    static final int MUSIC = 1;
    static final int SOUNDREC = 2;
    static final int OTHER = 3;

    private MyContext myContext;

    DeviceState(MyContext myContext) {
        this.myContext = myContext;
    }

    /**
     * @return Current device state. If API<26, may return IDLE when should return OTHER (e.g when timer sounding).
     */
    int getCurrent() {
        AudioManager manager = myContext.audioManager;
        int activeAudioStream = RecUtils.getActiveAudioStream(manager);

        if (manager.isMusicActive()) return MUSIC;
        if (myContext.audioRecorder.isRecording()) return SOUNDREC;
        if (manager.getMode() == AudioManager.MODE_RINGTONE) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_CALL) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) return OTHER;
        if (activeAudioStream != AudioManager.USE_DEFAULT_STREAM_TYPE) return OTHER;
        else return IDLE;
    }

    static String str(int state) {
        switch (state) {
            case IDLE: return "IDLE";
            case MUSIC: return "MUSIC";
            case SOUNDREC: return "SOUNDREC";
            default: return "OTHER";
        }
    }

    // region Utils

    boolean isMediaPlaying() {
        return myContext.audioManager.isMusicActive();
    }

    boolean isScreenOn() {
        return myContext.powerManager.isInteractive();
    }

    boolean isDeviceUnlocked() {
        return !myContext.keyguardManager.isKeyguardLocked();
    }

    // endregion
}
