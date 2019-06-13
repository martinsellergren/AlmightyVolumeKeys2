package com.example.almightyvolumekeys;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.media.session.MediaButtonReceiver;

class MyContext {
    final Context context;
    final AudioManager audioManager;
    final MediaSessionCompat mediaSession;
    final AudioRecorder audioRecorder = new AudioRecorder();

    MyContext(Context c) {
        context = c.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mediaSession = new MediaSessionCompat(context, "TAG", new ComponentName(context, MediaButtonReceiver.class), null);
    }

    void destroy() {
        mediaSession.setActive(false);
        mediaSession.release();
        audioRecorder.stopAndSave();
    }
}
