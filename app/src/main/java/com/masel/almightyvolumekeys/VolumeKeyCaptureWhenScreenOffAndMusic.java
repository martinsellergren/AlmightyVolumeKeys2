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

import com.masel.rec_utils.RecUtils;

import java.util.List;

/**
 * When screen off when music is playing: start polling music-volume, and look for changes.
 * Stops polling when screen turns on, or if music stops.
 */
class VolumeKeyCaptureWhenScreenOffAndMusic {

    private static final int MUSIC_VOLUME_POLLING_DELTA = 100;

    private MyContext myContext;
    private VolumeKeyInputController inputController;
    private BroadcastReceiver screenOffReceiver;

    private int prevMusicVolume;
    private Handler pollingHandler = new Handler();

    VolumeKeyCaptureWhenScreenOffAndMusic(MyContext myContext, VolumeKeyInputController inputController) {
        this.myContext = myContext;
        this.inputController = inputController;

        setupStartPollingWhenScreenOff();
        setupStartPollingWhenMusicStarted();
    }

    void destroy(MyContext myContext) {
        pollingHandler.removeCallbacksAndMessages(null);
        startPollingMethod2Handler.removeCallbacksAndMessages(null);

        try {
            if (Build.VERSION.SDK_INT >= 26 && audioPlaybackCallback != null) myContext.audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
        } catch (Exception e) {}

        try {
            if (startPollingMethod2BroadcastReceiver != null) myContext.context.unregisterReceiver(startPollingMethod2BroadcastReceiver);
        } catch (Exception e) {}

        try {
            myContext.context.unregisterReceiver(screenOffReceiver);
        } catch (Exception e) {}
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

    // region Setup start polling when music started

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
    private Handler startPollingMethod2Handler = new Handler();
    private void setupStartPollingWhenMusicStarted_method2() {
        startPollingMethod2BroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startPollingAttempts();
            }
        };
        myContext.context.registerReceiver(startPollingMethod2BroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void startPollingAttempts() {
        startPollingMethod2Handler.removeCallbacksAndMessages(null);
        startPolling();

        if (RecUtils.isScreenOn(myContext.powerManager)) return;
        try {
            if (myContext.wakeLock.isHeld()) startPollingMethod2Handler.postDelayed(this::startPollingAttempts, ATTEMPT_RATE);
        }
        catch (Exception e) {}
    }

    // endregion


    private void startPolling() {
        RecUtils.log("Start polling music volume");
        prevMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        timeWithoutMusic = 0;
        pollMusicVolume();
    }

    private static final long CONTINUE_POLLING_AFTER_MUSIC_STOP_TIME = 1000;
    private long timeWithoutMusic = 0;

    /**
     * Continues only if music is playing (or just recently stopped) AND screen is off.
     */
    private void pollMusicVolume() {
        pollingHandler.removeCallbacksAndMessages(null);

        if (DeviceState.getCurrent(myContext) == DeviceState.MUSIC) {
            timeWithoutMusic = 0;
        }
        else {
            timeWithoutMusic += MUSIC_VOLUME_POLLING_DELTA;
        }

        if (RecUtils.isScreenOn(myContext.powerManager) || timeWithoutMusic > CONTINUE_POLLING_AFTER_MUSIC_STOP_TIME) {
            RecUtils.log("Stop polling music volume");
            return;
        }

        int currentMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int diff = currentMusicVolume - prevMusicVolume;
        boolean volumeUp = diff > 0;
        AudioStreamState prevMusicStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevMusicVolume);

        for (int i = 0; i < Math.abs(diff); i++) {
            keyPress(volumeUp);
        }

        prevMusicVolume = currentMusicVolume;
        pollingHandler.postDelayed(this::pollMusicVolume, MUSIC_VOLUME_POLLING_DELTA);
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

    static boolean screenOffAndMusic(MyContext myContext) {
        return !RecUtils.isScreenOn(myContext.powerManager) && DeviceState.getCurrent(myContext) == DeviceState.MUSIC;
    }
}
