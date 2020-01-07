package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;

import com.masel.rec_utils.Utils;

import java.util.List;

/**
 * When screen off when music is playing: start polling music-volume, and look for changes.
 * Stops polling when screen turns on, or if music stops.
 */
class UserInteractionWhenScreenOffAndMusic {

    private static final int MUSIC_VOLUME_POLLING_DELTA = 100;

    private MyContext myContext;
    private ActionCommand actionCommand;
    private BroadcastReceiver screenOffReceiver;

    private int prevMusicVolume;
    private Handler pollingHandler = new Handler();

    UserInteractionWhenScreenOffAndMusic(MyContext myContext, ActionCommand actionCommand) {
        this.myContext = myContext;
        this.actionCommand = actionCommand;

        setupStartPollingWhenScreenOff();
        setupStartPollingWhenMusicStarted();
    }

    void destroy(MyContext myContext) {
        try {
            pollingHandler.removeCallbacksAndMessages(null);
            myContext.context.unregisterReceiver(screenOffReceiver);
            if (Build.VERSION.SDK_INT >= 26 && audioPlaybackCallback != null) myContext.audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
            if (startPollingMethod2BroadcastReceiver != null) myContext.context.unregisterReceiver(startPollingMethod2BroadcastReceiver);
        }
        catch (Exception e) {}
    }

    private void setupStartPollingWhenScreenOff() {
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startPolling();
            }
        };
        myContext.context.registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void setupStartPollingWhenMusicStarted() {
        if (Build.VERSION.SDK_INT >= 26) {
            setupStartPollingWhenMusicStarted_method1();
        }
        else {
            setupStartPollingWhenMusicStarted_method2();
        }
    }

    private AudioManager.AudioPlaybackCallback audioPlaybackCallback = null;
    @RequiresApi(api = 26)
    private void setupStartPollingWhenMusicStarted_method1() {
        audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    startPolling();
                }
        };
        myContext.audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null);
    }

    /**
     * Attempt to start polling, at a fixed rate. (Polling only starts if conditions met).
     */
    private static final long ATTEMPT_RATE = 750;
    private BroadcastReceiver startPollingMethod2BroadcastReceiver = null;
    private long sleepAllowedTime;
    private void setupStartPollingWhenMusicStarted_method2() {
        Handler handler = new Handler();
        startPollingMethod2BroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int preventSleepMinutes = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepStart", 10);
                sleepAllowedTime = System.currentTimeMillis() + preventSleepMinutes * 60000;
                startPollingAttempts(handler);
            }
        };
        myContext.context.registerReceiver(startPollingMethod2BroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void startPollingAttempts(Handler handler) {
        handler.removeCallbacksAndMessages(null);
        startPolling();
        if (System.currentTimeMillis() < sleepAllowedTime) {
            handler.postDelayed(() -> startPollingAttempts(handler), ATTEMPT_RATE);
        }
    }


    private void startPolling() {
        Utils.log("Start polling music volume");
        prevMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        time_without_music = 0;
        pollMusicVolume();
    }

    private static final long CONTINUE_POLLING_AFTER_MUSIC_STOP_TIME = 1000;
    private long time_without_music = 0;

    /**
     * Continues only if music is playing AND screen is off.
     */
    private void pollMusicVolume() {
        pollingHandler.removeCallbacksAndMessages(null);

        if (DeviceState.getCurrent(myContext) == DeviceState.MUSIC) {
            time_without_music = 0;
        }
        else {
            time_without_music += MUSIC_VOLUME_POLLING_DELTA;
        }

        if (isScreenOn() || time_without_music > CONTINUE_POLLING_AFTER_MUSIC_STOP_TIME) {
            Utils.log("Stop polling music volume");
            return;
        }

//        if (isScreenOn() || DeviceState.getCurrent(myContext) != DeviceState.MUSIC) {
//            Utils.log("Stop polling music volume");
//            return;
//        }

        int currentMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (currentMusicVolume == prevMusicVolume + 1) {
            actionCommand.addBit(true);
        }
        else if (currentMusicVolume == prevMusicVolume - 1) {
            actionCommand.addBit(false);
        }

        prevMusicVolume = currentMusicVolume;
        pollingHandler.postDelayed(this::pollMusicVolume, MUSIC_VOLUME_POLLING_DELTA);
    }

    private boolean isScreenOn() {
        return Build.VERSION.SDK_INT >= 20 ?
                myContext.powerManager.isInteractive() :
                myContext.powerManager.isScreenOn();
    }


    interface ManualMusicVolumeChanger {
        void setVolume(int volume);
    }
    ManualMusicVolumeChanger getManualMusicVolumeChanger() {
        return volume -> {
            pollingHandler.removeCallbacksAndMessages(null);
            myContext.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            startPolling();
        };
    }
}
