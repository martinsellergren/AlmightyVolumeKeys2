package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.media.session.MediaButtonReceiver;

class MyContext {
    final Context context;
    final AudioManager audioManager;
    final MediaSessionCompat mediaSession;
    final AudioRecorderConnection audioRecorder;

    MyContext(Context c) {
        context = c.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mediaSession = new MediaSessionCompat(context, "TAG", new ComponentName(context, MediaButtonReceiver.class), null);
        audioRecorder = new AudioRecorderConnection(context);
    }

    void destroy() {
        mediaSession.setActive(false);
        mediaSession.release();
        audioRecorder.unbind();
    }
}
