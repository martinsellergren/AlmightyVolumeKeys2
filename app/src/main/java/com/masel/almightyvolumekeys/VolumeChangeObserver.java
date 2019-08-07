package com.masel.almightyvolumekeys;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;

public class VolumeChangeObserver extends ContentObserver {

    private AudioManager audioManager;
    private ActionCommand actionCommand;
    private int prevMusicVolume;

    /**
     * @param audioManager
     * @param actionCommand
     */
    VolumeChangeObserver(AudioManager audioManager, ActionCommand actionCommand) {
        super(new Handler());

        this.audioManager = audioManager;
        this.actionCommand = actionCommand;
        this.prevMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * @param context Application-context
     */
    public void start(Context context) {
        context.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this);
    }

    /**
     * @param context Application-context
     */
    void stop(Context context) {
        context.getContentResolver().unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        int currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int delta = currentMusicVolume - prevMusicVolume;
        prevMusicVolume = currentMusicVolume;
        //Log.i("<ME>", "DELTA:" + delta);

        if (delta != 0) {
            actionCommand.addBit(delta > 0, ActionCommand.DELTA_PRESS_TIME_SLOW);
        }
    }
}
