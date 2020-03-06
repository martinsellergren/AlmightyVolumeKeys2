package com.masel.almightyvolumekeys;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import androidx.preference.PreferenceManager;

class MyContext {
    final Context context;
    final AudioManager audioManager;
    final PowerManager powerManager;
    final NotificationManager notificationManager;
    final AudioRecorderDeligator audioRecorder;
    final Notifier notifier;
    final MyVibrator vibrator;
    final Voice voice;
    final SharedPreferences sharedPreferences;
    final MyFlashlight flashlight;
    final DeviceStateCallbacks deviceStateCallbacks;
    final VolumeUtils volumeUtils;
    final WakeLock wakeLock;

    MyContext(Context c) {
        context = c.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifier = new Notifier(context);
        vibrator = new MyVibrator(context);
        audioRecorder = new AudioRecorderDeligator(context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        flashlight = new MyFlashlight(context);
        deviceStateCallbacks = new DeviceStateCallbacks(context);
        volumeUtils = new VolumeUtils(this);
        voice = new Voice(context, volumeUtils);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.masel.almightyvolumekeys::WakeLock");
    }

    void destroy() {
        audioRecorder.destroy();
        notifier.cancel();
        vibrator.cancel();
        voice.destroy();
        flashlight.destroy();
        deviceStateCallbacks.destroy();
        //volume.stopMovement();
        if (wakeLock.isHeld()) wakeLock.release();
    }
}
