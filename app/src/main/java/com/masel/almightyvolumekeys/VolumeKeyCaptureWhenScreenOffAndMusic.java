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

    private boolean isPollingMusicVolume = false;

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
        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
        stopPollingAttemptsHandler.removeCallbacksAndMessages(null);

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
                startPollingAttemptsForAWhile();
            }
        };
        myContext.context.registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    // region Setup start polling when music started

    private void setupStartPollingWhenMusicStarted() {
        if (useDefaultMethodForStartPollingOnMusicStart()) {
            setupStartPollingWhenMusicStarted_method1();
        }
        else {
            setupStartPollingWhenMusicStarted_method2();
        }
    }

    private boolean useDefaultMethodForStartPollingOnMusicStart() {
        return Build.VERSION.SDK_INT >= 26;
    }

    private AudioManager.AudioPlaybackCallback audioPlaybackCallback = null;
    @RequiresApi(api = 26)
    private void setupStartPollingWhenMusicStarted_method1() {
        audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    startPollingAttemptsForAWhile();
                }
        };
        myContext.audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null);
    }

    private BroadcastReceiver startPollingMethod2BroadcastReceiver = null;
    private void setupStartPollingWhenMusicStarted_method2() {
        startPollingMethod2BroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startPollingAttempts();
            }
        };
        myContext.context.registerReceiver(startPollingMethod2BroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    // endregion


    private static final long ATTEMPT_RATE = 750;
    private Handler startPollingAttemptsHandler = new Handler();
    /**
     * Attempt to start polling, at a fixed rate.
     * Stop attempting if already polling or if screen on.
     */
    private void startPollingAttempts() {
        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
        if (isPollingMusicVolume) return;
        if (RecUtils.isScreenOn(myContext.powerManager) && !myContext.accessibilityServiceFailing) return;

        initOrContinuePolling();
        startPollingAttemptsHandler.postDelayed(this::startPollingAttempts, ATTEMPT_RATE);
    }

    private void stopPollingAttempts() {
        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
    }

    private static final long STOP_POLLING_ATTEMPTS_TIME = 3000;
    private Handler stopPollingAttemptsHandler = new Handler();
    private void startPollingAttemptsForAWhile() {
        startPollingAttempts();

        if (useDefaultMethodForStartPollingOnMusicStart()) {
            stopPollingAttemptsHandler.removeCallbacksAndMessages(null);
            stopPollingAttemptsHandler.postDelayed(this::stopPollingAttempts, STOP_POLLING_ATTEMPTS_TIME);
        }
    }

    /**
     * Start polling if appropriate.
     * If already polling, discards any uncaught volume changes and restarts.
     */
    private void initPolling() {
        if (!deviceStateOkForPolling()) {
            return;
        }

        RecUtils.log("Start polling music volume");
        isPollingMusicVolume = true;
        prevMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        pollMusicVolume();
    }

    private void initOrContinuePolling() {
        if (!isPollingMusicVolume) initPolling();
    }

    /**
     * Continues only if music is playing (or just recently stopped) AND screen is off.
     * Way in through initPolling(), way out through stopPolling().
     */
    private void pollMusicVolume() {
        pollingHandler.removeCallbacksAndMessages(null);

        if (!deviceStateOkForPolling()) {
            stopPolling();
            return;
        }

        int currentMusicVolume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int diff = currentMusicVolume - prevMusicVolume;
        boolean volumeUp = diff > 0;

        for (int i = 0; i < Math.abs(diff); i++) {
            volumeChange(volumeUp, prevMusicVolume);
        }

        prevMusicVolume = currentMusicVolume;
        if (waitingForKeyRelease) {
            if (prevMusicVolume == minMusicVolume) prevMusicVolume = minMusicVolume + 1;
            else if (prevMusicVolume == maxMusicVolume) prevMusicVolume = maxMusicVolume - 1;
        }

        pollingHandler.postDelayed(this::pollMusicVolume, MUSIC_VOLUME_POLLING_DELTA);
    }


    /**
     * Attempt to start polling for a while. Track skips means short period with music not playing, still might miss
     * music start since such a short break.
     */
    private void stopPolling() {
        RecUtils.log("Stop polling music volume");
        isPollingMusicVolume = false;

        if (useDefaultMethodForStartPollingOnMusicStart()) {
            startPollingAttemptsForAWhile();
        }
        else {
            startPollingAttempts();
        }
    }

    // region Interpret press

    private final int NO_HISTORY_ENTRIES = 2;
    private List<Boolean> volumeChangesHistory = new LinkedList<>();
    private List<Long> volumeChangeTimesHistory = new LinkedList<>();
    private List<Integer> prevVolumesHistory = new LinkedList<>();

    private boolean waitingForKeyRelease = false;
    private Handler longPressKeyReleaseHandler = new Handler();
    private static final long TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED = Math.max(MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS, MUSIC_VOLUME_POLLING_DELTA * 2);

    private void volumeChange(boolean volumeUp, int prevMusicVolume) {
        if (waitingForKeyRelease) {
            resetLongPressKeyReleaseHandler();
            preventVolumeExtremes();
            return;
        }

        updateHistory(volumeUp, prevMusicVolume);

        if (detectLongPress()) {
            RecUtils.log("Music volume polling caught long-press");
            volumeKeyInputController.longPressDetected();
            volumeKeyInputController.undoPresses(NO_HISTORY_ENTRIES);
            waitingForKeyRelease = true;
            resetLongPressKeyReleaseHandler();
        }
        else {
            RecUtils.log("Music volume polling caught click");
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

    private void preventVolumeExtremes() {
        int volume = myContext.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (volume == maxMusicVolume) {
            Utils.adjustVolume_withFallback(myContext, AudioManager.STREAM_MUSIC, false, false);
        }
        else if (volume == minMusicVolume) {
            Utils.adjustVolume_withFallback(myContext, AudioManager.STREAM_MUSIC, true, false);
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
        preventVolumeExtremes();
    }

    // endregion

    Runnable getResetAction() {
        return this::initPolling;
    }

    private boolean deviceStateOkForPolling() {
        return DeviceState.getCurrent(myContext) == DeviceState.MUSIC &&
                (!RecUtils.isScreenOn(myContext.powerManager) || myContext.accessibilityServiceFailing);
    }
}
