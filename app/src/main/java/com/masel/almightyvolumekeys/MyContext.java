package com.masel.almightyvolumekeys;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import androidx.preference.PreferenceManager;

import com.masel.rec_utils.RecUtils;

class MyContext {
    final Context context;
    final AudioManager audioManager;
    final PowerManager powerManager;
    final NotificationManager notificationManager;
    final KeyguardManager keyguardManager;
    final AudioRecorderDeligator audioRecorder;
    final Notifier notifier;
    final MyVibrator vibrator;
    final Voice voice;
    final TuneAnnouncer tuneAnnouncer;
    final SharedPreferences sharedPreferences;
    final MyFlashlight flashlight;
    final VolumeUtils volumeUtils;
    final WakeLock wakeLock;

    final DeviceState deviceState;

    MyContext(Context c) {
        context = c.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        audioRecorder = new AudioRecorderDeligator(context);
        notifier = new Notifier(context);
        vibrator = new MyVibrator(context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        flashlight = new MyFlashlight(context);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, RecUtils.isHuawei(context) ? "LocationManagerService" : "com.masel.almightyvolumekeys::WakeLock");

        deviceState = new DeviceState(this);
        volumeUtils = new VolumeUtils(this);
        voice = new Voice(this, volumeUtils);
        tuneAnnouncer = new TuneAnnouncer(this);
    }

    void destroy() {
        audioRecorder.destroy();
        notifier.cancel();
        vibrator.cancel();
        voice.destroy();
        flashlight.destroy();
        deviceState.destroy();
        if (wakeLock.isHeld()) wakeLock.release();
    }
}
