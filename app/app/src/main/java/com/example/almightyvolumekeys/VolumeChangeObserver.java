package com.example.almightyvolumekeys;

import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class VolumeChangeObserver extends ContentObserver {

    private AudioManager audioManager;
    private ActionCommand actionCommand;
    private int prevMusicVolume;

    VolumeChangeObserver(AudioManager audioManager, ActionCommand actionCommand) {
        super(new Handler());

        this.audioManager = audioManager;
        this.actionCommand = actionCommand;
        this.prevMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        int currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int delta = currentMusicVolume - prevMusicVolume;
        prevMusicVolume = currentMusicVolume;
        //Log.i("<ME>", "DELTA:" + delta);

        if (delta != 0) {
            actionCommand.addBit(delta > 0, ActionCommand.DELTA_PRESS_TIME_FAST);
        }
    }
}
