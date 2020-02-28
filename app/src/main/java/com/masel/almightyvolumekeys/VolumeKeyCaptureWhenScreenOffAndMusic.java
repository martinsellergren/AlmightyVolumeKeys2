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

import java.util.LinkedList;
import java.util.List;

/**
 * When screen off when music is playing: start polling music-volume, and look for changes.
 * Stops polling when screen turns on, or if music stops.
 */
class VolumeKeyCaptureWhenScreenOffAndMusic {

    private static final int MUSIC_VOLUME_POLLING_DELTA = 50;

    /**
     * Time between two presses more than this indicates manual presses. */
    private static final long MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS = 100;

    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private BroadcastReceiver screenOffReceiver;
    private final int minMusicVolume;
    private final int maxMusicVolume;

    private int prevMusicVolume;
    private Handler pollingHandler = new Handler();

    private boolean disabled = false;

    VolumeKeyCaptureWhenScreenOffAndMusic(MyContext myContext, VolumeKeyInputController volumeKeyInputController) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        this.minMusicVolume = Build.VERSION.SDK_INT >= 28 ? myContext.audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) : 0;
        this.maxMusicVolume = myContext.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

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

    void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (!disabled) startPolling();
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
        if (disabled) {
            return;
        }

        int currentMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int diff = currentMusicVolume - prevMusicVolume;
        boolean volumeUp = diff > 0;

        for (int i = 0; i < Math.abs(diff); i++) {
            volumeChange(volumeUp, currentMusicVolume, prevMusicVolume);
        }

        prevMusicVolume = currentMusicVolume;
        if (waitingForKeyRelease) {
            if (prevMusicVolume == minMusicVolume) prevMusicVolume = minMusicVolume + 1;
            else if (prevMusicVolume == maxMusicVolume) prevMusicVolume = maxMusicVolume - 1;
        }

        pollingHandler.postDelayed(this::pollMusicVolume, MUSIC_VOLUME_POLLING_DELTA);
    }

    // region Long press

    private final int NO_HISTORY_ENTRIES = 2;
    private List<Boolean> volumeChangesHistory = new LinkedList<>();
    private List<Long> volumeChangeTimesHistory = new LinkedList<>();
    private List<Integer> prevVolumesHistory = new LinkedList<>();

    private boolean waitingForKeyRelease = false;
    private Handler longPressKeyReleaseHandler = new Handler();
    private static final long TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED = Math.max(MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS, MUSIC_VOLUME_POLLING_DELTA * 2);

    /**
     *
     * @param volumeUp
     */
    private void volumeChange(boolean volumeUp, int currentMusicVolume, int prevMusicVolume) {
        if (waitingForKeyRelease) {
            resetLongPressKeyReleaseHandler();
            preventVolumeExtremes(currentMusicVolume);
            return;
        }

        updateHistory(volumeUp, prevMusicVolume);

        if (detectLongPress()) {
            volumeKeyInputController.longPressDetected();
            volumeKeyInputController.undoPresses(NO_HISTORY_ENTRIES);
            waitingForKeyRelease = true;
            resetLongPressKeyReleaseHandler();
        }
        else if (prevMusicVolume != currentMusicVolume) {
            AudioStreamState resetAudioStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevMusicVolume);
            volumeKeyInputController.keyUp(volumeUp, resetAudioStreamState);
        }
    }

    private boolean detectLongPress() {
        if (volumeChangeTimesHistory.size() < NO_HISTORY_ENTRIES) return false;

        boolean first = volumeChangesHistory.get(0);
        for (boolean volumeChange : volumeChangesHistory) {
            if (volumeChange != first) return false;
        }

        long deltaTime = volumeChangeTimesHistory.get(NO_HISTORY_ENTRIES - 1) - volumeChangeTimesHistory.get(0);
        long maxDeltaTime = (NO_HISTORY_ENTRIES - 1) * MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS;
        return deltaTime <= maxDeltaTime;
    }

    private void updateHistory(boolean volumeUp, int prevMusicVolume) {
        if (volumeChangesHistory.size() == NO_HISTORY_ENTRIES) {
            volumeChangesHistory.remove(0);
            volumeChangeTimesHistory.remove(0);
            prevVolumesHistory.remove(0);
        }
        else if (volumeChangesHistory.size() > NO_HISTORY_ENTRIES) {
            throw new RuntimeException("Dead end");
        }

        volumeChangesHistory.add(volumeUp);
        volumeChangeTimesHistory.add(System.currentTimeMillis());
        prevVolumesHistory.add(prevMusicVolume);
    }

    private void preventVolumeExtremes(int currentMusicVolume) {
        if (currentMusicVolume == maxMusicVolume) {
            Utils.adjustVolume_withFallback(myContext.audioManager, AudioManager.STREAM_MUSIC, false, false);
        }
        else if (currentMusicVolume == minMusicVolume) {
            Utils.adjustVolume_withFallback(myContext.audioManager, AudioManager.STREAM_MUSIC, true, false);
            //myContext.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    private void resetLongPressKeyReleaseHandler() {
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        longPressKeyReleaseHandler.postDelayed(this::completeLongPressDetected, TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED);
    }

    private void completeLongPressDetected() {
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        waitingForKeyRelease = false;
        AudioStreamState resetAudioStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevVolumesHistory.get(0));
        volumeKeyInputController.keyUp(volumeChangesHistory.get(0), resetAudioStreamState);
        preventVolumeExtremes(myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    // endregion


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
