package com.example.almightyvolumekeys;

import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class VolumeChangeObserver extends ContentObserver {

    private AudioManager audioManager;
    private ActionCommand actionCommand;
    private int prevVolume = 0;

    VolumeChangeObserver(AudioManager audioManager, ActionCommand actionCommand) {
        super(new Handler());

        this.audioManager = audioManager;
        this.actionCommand = actionCommand;
    }

    @Override
    public void onChange(boolean selfChange) {
        //super.onChange(selfChange);

        onChange(selfChange, null);
        Log.i("<ME>", "Change!");
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        // super.onChange(selfChange, uri);

//        Log.i("<ME>", String.format("%s : %s", selfChange, uri));
//
//        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        int delta = currentVolume - prevVolume;
//        prevVolume = currentVolume;
//
//        if (delta != 0) {
//            Log.i("<ME>", "v:" + delta);
//        }
    }
}
