package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;
import com.masel.rec_utils.Utils;

class MyContext {
    final Context context;
    final AudioManager audioManager;
    final MediaSessionCompat mediaSession;
    final AudioRecorderDeligator audioRecorder;
    final Notifier notifier;

    MyContext(Context c) {
        context = c.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mediaSession = new MediaSessionCompat(context, "TAG", new ComponentName(context, MediaButtonReceiver.class), null);
        notifier = new Notifier(context);
        audioRecorder = new AudioRecorderDeligator(context);
    }

    void destroy() {
        mediaSession.setActive(false);
        mediaSession.release();
        if (audioRecorder.isRecording()) {
            audioRecorder.stopAndSave();
            Utils.toast(context, "Recording stopped unexpectedly");
        }
        audioRecorder.destroy();
        notifier.cancel();
    }
}
