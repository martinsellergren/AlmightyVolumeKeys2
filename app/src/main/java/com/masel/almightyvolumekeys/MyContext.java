package com.masel.almightyvolumekeys;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

import com.masel.rec_utils.Utils;

class MyContext {
    final Context context;
    final AudioManager audioManager;
    final PowerManager powerManager;
    final MediaSessionCompat mediaSession;
    final NotificationManager notificationManager;
    final AudioRecorderDeligator audioRecorder;
    final Notifier notifier;
    final Voice voice;
    final SharedPreferences sharedPreferences;
    final Flashlight flashlight;
    final WakeLock wakeLock;

    MyContext(Context c) {
        context = c.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mediaSession = new MediaSessionCompat(context, "TAG", new ComponentName(context, MediaButtonReceiver.class), null);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifier = new Notifier(context);
        audioRecorder = new AudioRecorderDeligator(context);
        voice = new Voice(context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        flashlight = new Flashlight(context);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.masel.almightyvolumekeys::WakeLock");
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
        voice.destroy();
        flashlight.destroy();
        if (wakeLock.isHeld()) wakeLock.release();
    }
}
