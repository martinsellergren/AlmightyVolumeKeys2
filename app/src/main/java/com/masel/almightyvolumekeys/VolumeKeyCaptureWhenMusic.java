package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Handler;

import com.masel.rec_utils.RecUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * When when music is playing: start polling music-volume, and look for changes.
 * Stops polling when music stops (save battery..).
 */
class VolumeKeyCaptureWhenMusic {
    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private final int minMusicVolume;
    private final int maxMusicVolume;

    VolumeKeyCaptureWhenMusic(MyContext myContext, VolumeKeyInputController volumeKeyInputController) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        this.minMusicVolume = myContext.volumeUtils.getMin(AudioManager.STREAM_MUSIC);
        this.maxMusicVolume = myContext.volumeUtils.getMax(AudioManager.STREAM_MUSIC);

        //myContext.deviceStateCallbacks.addMediaStartCallback(this::startPollingAttemptsForAWhile);
        startPolling();

        myContext.deviceStateCallbacks.addMediaStartCallback(this::startOrContinuePolling);
        myContext.deviceStateCallbacks.addDeviceUnlockedCallback(this::startOrContinuePolling);
    }

    void destroy() {
        pollingHandler.removeCallbacksAndMessages(null);
//        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
//        stopPollingAttemptsHandler.removeCallbacksAndMessages(null);
    }

    // endregion


    private static final long ATTEMPT_RATE = 750;
    private Handler startPollingAttemptsHandler = new Handler();
    /**
     * Attempt to start polling, at a fixed rate.
     * Stop attempting if already polling.
     */
    private void startPollingAttempts() {
        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
        if (isPolling) return;

        startOrContinuePolling();
        startPollingAttemptsHandler.postDelayed(this::startPollingAttempts, ATTEMPT_RATE);
    }

    private void stopPollingAttempts() {
        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
    }

    private static final long STOP_POLLING_ATTEMPTS_TIME = 3000;
    private Handler stopPollingAttemptsHandler = new Handler();
    private void startPollingAttemptsForAWhile() {
        startPollingAttempts();

        stopPollingAttemptsHandler.removeCallbacksAndMessages(null);
        stopPollingAttemptsHandler.postDelayed(this::stopPollingAttempts, STOP_POLLING_ATTEMPTS_TIME);
    }

    /**
     * If already polling, discards any uncaught volume changes and restarts.
     */
    private void startPolling() {
        if (!deviceStateOkForPolling()) {
            return;
        }

        RecUtils.log("Start polling music volume");
        isPolling = true;
        prevMusicVolume = myContext.volumeUtils.get(AudioManager.STREAM_MUSIC);
        pollMusicVolume();
    }

    private void startOrContinuePolling() {
        if (!isPolling) startPolling();
    }

    private static final int MUSIC_VOLUME_POLLING_DELTA = 100;
    private boolean isPolling = false;
    private int prevMusicVolume;
    private Handler pollingHandler = new Handler();

    /**
     * Continues only if music is playing.
     * Way in through startPolling().
     */
    private void pollMusicVolume() {
        pollingHandler.removeCallbacksAndMessages(null);

        if (!deviceStateOkForPolling()) {
            stopPolling();
            return;
        }

        int currentMusicVolume = myContext.volumeUtils.get(AudioManager.STREAM_MUSIC);
        int diff = currentMusicVolume - prevMusicVolume;
        boolean volumeUp = diff > 0;

        for (int i = 0; i < Math.abs(diff); i++) {
            volumeChange(volumeUp, prevMusicVolume);
        }

        currentMusicVolume = preventVolumeExtremes(currentMusicVolume);
        prevMusicVolume = currentMusicVolume;

        pollingHandler.postDelayed(this::pollMusicVolume, MUSIC_VOLUME_POLLING_DELTA);
    }

    private boolean deviceStateOkForPolling() {
        return myContext.deviceState.isDeviceUnlocked() || myContext.deviceState.isMediaPlaying();
    }

    private int preventVolumeExtremes(int currentVolume) {
//        if (currentVolume == maxMusicVolume || currentVolume == minMusicVolume) {
//            new Handler().postDelayed(() -> {
//                int vol = myContext.volumeUtils.get(AudioManager.STREAM_MUSIC);
//                if (vol == maxMusicVolume - 1 || vol == minMusicVolume + 1) {
//                    myContext.volumeUtils.set(AudioManager.STREAM_MUSIC, vol, true);
//                }
//            }, 500);
//        }

        if (currentVolume == maxMusicVolume) {
            myContext.volumeUtils.adjust(AudioManager.STREAM_MUSIC, false, true);
            return currentVolume - 1;
        }
        else if (currentVolume == minMusicVolume) {
            myContext.volumeUtils.adjust(AudioManager.STREAM_MUSIC, true, true);
            return currentVolume + 1;
        }

        return currentVolume;
    }

    /**
     * Attempt to start polling for a while. Track skips means short period with music not playing, still might miss
     * music start since such a short break.
     */
    private void stopPolling() {
        RecUtils.log("Stop polling music volume");
        isPolling = false;
        startPollingAttemptsForAWhile();
    }

    // region Interpret volume change

    /**
     * Time between two presses more than this indicates individual presses. */
    private static final long MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS = 125;

    private static final long TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED = Math.max(MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS, MUSIC_VOLUME_POLLING_DELTA * 2);
    private boolean waitingForKeyRelease = false;
    private Handler longPressKeyReleaseHandler = new Handler();

    private void volumeChange(boolean volumeUp, int prevMusicVolume) {
        if (waitingForKeyRelease) {
            resetLongPressKeyReleaseHandler();
            return;
        }

        if (detectLongPress(volumeUp, prevMusicVolume)) {
            RecUtils.log("Music volume polling caught long-press");
            volumeKeyInputController.longPressDetected();
            volumeKeyInputController.undoPresses(2);
            waitingForKeyRelease = true;
            resetLongPressKeyReleaseHandler();
        }
        else {
            RecUtils.log("Music volume polling caught click");
            AudioStreamState resetAudioStreamState = new AudioStreamState(myContext.volumeUtils, AudioManager.STREAM_MUSIC, prevMusicVolume);
            volumeKeyInputController.keyUp(volumeUp, resetAudioStreamState);
        }
    }

    private List<Boolean> volumeChangesHistory = new LinkedList<>();
    private List<Long> volumeChangeTimesHistory = new LinkedList<>();
    private List<Integer> prevVolumesHistory = new LinkedList<>();

    /**
     * Long press if 3 up or down presses, where last two are quickly subsequent, and second is within 1 second after first.
     */
    private boolean detectLongPress(boolean volumeUp, int prevMusicVolume) {
        updateHistory(volumeUp, prevMusicVolume);

        if (volumeChangesHistory.size() < 3) return false;

        boolean first = volumeChangesHistory.get(0);
        for (boolean volumeChange : volumeChangesHistory) {
            if (volumeChange != first) return false;
        }

        long delta1 = volumeChangeTimesHistory.get(1) - volumeChangeTimesHistory.get(0);
        long delta2 = volumeChangeTimesHistory.get(2) - volumeChangeTimesHistory.get(1);
        return delta1 < 1000 && delta2 < MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS;
    }

    private void updateHistory(boolean volumeUp, int prevMusicVolume) {
        if (volumeChangesHistory.size() == 3) {
            volumeChangesHistory.remove(0);
            volumeChangeTimesHistory.remove(0);
            prevVolumesHistory.remove(0);
        }

        volumeChangesHistory.add(volumeUp);
        volumeChangeTimesHistory.add(System.currentTimeMillis());
        prevVolumesHistory.add(prevMusicVolume);
    }

    private void resetLongPressKeyReleaseHandler() {
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        longPressKeyReleaseHandler.postDelayed(this::completeLongPressDetected, TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED);
    }

    private void completeLongPressDetected() {
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        waitingForKeyRelease = false;
        AudioStreamState resetAudioStreamState = new AudioStreamState(myContext.volumeUtils, AudioManager.STREAM_MUSIC, prevVolumesHistory.get(0));
        volumeKeyInputController.keyUp(volumeChangesHistory.get(0), resetAudioStreamState);
    }

    // endregion

    Runnable getResetAction() {
        return this::startPolling;
    }
}
