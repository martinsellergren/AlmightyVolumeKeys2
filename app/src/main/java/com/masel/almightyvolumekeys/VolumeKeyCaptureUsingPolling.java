package com.masel.almightyvolumekeys;

import android.media.AudioManager;
import android.os.Handler;

import com.masel.rec_utils.RecUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Poll device volume (media and ringtone) and look for changes.
 * Device locked and no music: no volume changes occur so can't catch volume keys then.
 * Also holds volume fixed after long press, and prevents volume extremes (unless three individual clicks).
 */
class VolumeKeyCaptureUsingPolling {
    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private final int minMusicVolume;
    private final int maxMusicVolume;

    private boolean isActive = false;

    VolumeKeyCaptureUsingPolling(MyContext myContext, VolumeKeyInputController volumeKeyInputController) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        this.minMusicVolume = myContext.volumeUtils.getMin(AudioManager.STREAM_MUSIC);
        this.maxMusicVolume = myContext.volumeUtils.getMax(AudioManager.STREAM_MUSIC);

        myContext.deviceState.addMediaStartCallback(() -> {
            preventVolumeExtremes(myContext.volumeUtils.getVolume(AudioManager.STREAM_MUSIC));
            enableOrDisable();
        });
        myContext.deviceState.addMediaStopCallback(this::enableOrDisable);
        myContext.deviceState.addOnAllowSleepCallback(this::stopPolling);
        myContext.deviceState.addScreenOnCallback(this::enableOrDisable);
        //myContext.deviceState.addDeviceUnlockedCallback(this::startPollingAttemptsForAWhile);
        
        if (deviceStateOkForCapture()) startPolling();
    }

    private void enableOrDisable() {
        boolean enable = deviceStateOkForCapture();
        RecUtils.log("Music polling capture active: " + enable);

        if (enable) startOrContinuePolling();
        else stopPolling();
    }

    private boolean deviceStateOkForCapture() {
        //return myContext.deviceState.isDeviceUnlocked() || myContext.deviceState.isMediaPlaying();
        return myContext.deviceState.isMediaPlaying();
    }

    boolean isActive() {
        return isActive;
    }

    void destroy() {
        pollingHandler.removeCallbacksAndMessages(null);
//        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
//        stopPollingAttemptsHandler.removeCallbacksAndMessages(null);
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        volumeSliderDragReleaseHandler.removeCallbacksAndMessages(null);
    }

    // endregion

//
//    private static final long ATTEMPT_RATE = 750;
//    private Handler startPollingAttemptsHandler = new Handler();
//
//    /**
//     * Attempt to start polling, at a fixed rate.
//     * Stop attempting if already polling.
//     */
//    private void startPollingAttempts() {
//        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
//        if (isPolling) return;
//
//        startOrContinuePolling();
//        startPollingAttemptsHandler.postDelayed(this::startPollingAttempts, ATTEMPT_RATE);
//    }
//
//    private void stopPollingAttempts() {
//        startPollingAttemptsHandler.removeCallbacksAndMessages(null);
//    }
//
//    private static final long STOP_POLLING_ATTEMPTS_TIME = 3000;
//    private Handler stopPollingAttemptsHandler = new Handler();
//    private void startPollingAttemptsForAWhile() {
//        startPollingAttempts();
//
//        stopPollingAttemptsHandler.removeCallbacksAndMessages(null);
//        stopPollingAttemptsHandler.postDelayed(this::stopPollingAttempts, STOP_POLLING_ATTEMPTS_TIME);
//    }

    /**
     * If already polling, discards any uncaught volume changes and restarts.
     */
    private void startPolling() {
//        if (!deviceStateOkForCapture()) {
//            return;
//        }

        isActive = true;
        prevMusicVolume = myContext.volumeUtils.getVolume(AudioManager.STREAM_MUSIC);
        currentlyAllowVolumeExtremes = prevMusicVolume == maxMusicVolume || prevMusicVolume == minMusicVolume;
        pollMusicVolume();
    }

    private void startOrContinuePolling() {
        if (!isActive) startPolling();
    }

    private static final int MUSIC_VOLUME_POLLING_DELTA = 100;
    private int prevMusicVolume;
    private Handler pollingHandler = new Handler();
    private AudioStreamState holdVolume = null;

    /**
     * Way in through startPolling(), way out through stopPolling().
     */
    private void pollMusicVolume() {
        pollingHandler.removeCallbacksAndMessages(null);

        int currentMusicVolume = myContext.volumeUtils.getVolume(AudioManager.STREAM_MUSIC);
        int diff = currentMusicVolume - prevMusicVolume;
        boolean volumeUp = diff > 0;

        for (int i = 0; i < Math.abs(diff); i++) {
            volumeChange(volumeUp, prevMusicVolume);
        }

        currentMusicVolume = modifyVolume(currentMusicVolume);

        prevMusicVolume = currentMusicVolume;
        pollingHandler.postDelayed(this::pollMusicVolume, MUSIC_VOLUME_POLLING_DELTA);
    }

    /**
     * Attempt to start polling for a while. Track skips means short period with music not playing, still might miss
     * music start since such a short break.
     */
    private void stopPolling() {
        isActive = false;
        pollingHandler.removeCallbacksAndMessages(null);
    }

    /**
     * If expected long-press: hold volume on long-press start.
     * If max/ min volume: prevent extreme unless conditions met.
     */
    private int modifyVolume(int currentMusicVolume) {
        if (holdVolume != null) {
            myContext.volumeUtils.setVolume(holdVolume.getStream(), holdVolume.getVolume(), true, false);
            return holdVolume.getVolume();
        }

        if (!allowVolumeExtremes(currentMusicVolume)) {
            return preventVolumeExtremes(currentMusicVolume);
        }

        return currentMusicVolume;
    }

    private boolean currentlyAllowVolumeExtremes = false;
    private boolean allowVolumeExtremes(int currentMusicVolume) {
        currentlyAllowVolumeExtremes = currentlyAllowVolumeExtremes &&
                (currentMusicVolume == maxMusicVolume || currentMusicVolume == minMusicVolume);
        return currentlyAllowVolumeExtremes;
    }

    private int preventVolumeExtremes(int currentVolume) {
        if (currentVolume == maxMusicVolume) {
            myContext.volumeUtils.setVolume(AudioManager.STREAM_MUSIC, maxMusicVolume - 1, true, false);
            return maxMusicVolume - 1;
        }
        else if (currentVolume == minMusicVolume) {
            myContext.volumeUtils.setVolume(AudioManager.STREAM_MUSIC, minMusicVolume + 1, true, false);
            return minMusicVolume + 1;
        }

        return currentVolume;
    }

    // region Interpret volume change

    /**
     * Time between two presses more than this indicates individual presses. */
    private static final long MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS = 125;

    private boolean waitingForKeyRelease = false;
    private boolean waitingForDragRelease = false;

    private List<Boolean> volumeChangesLast3 = new LinkedList<>();
    private List<Long> volumeChangeTimesLast3 = new LinkedList<>();
    private List<Integer> prevVolumesLast3 = new LinkedList<>();

    private void volumeChange(boolean volumeUp, int prevMusicVolume) {
        if (waitingForKeyRelease) {
            initLongPressKeyReleaseHandler();
            return;
        }
        if (waitingForDragRelease) {
            volumeKeyInputController.discardPresses();
            initVolumeSliderDragReleaseHandler();
            return;
        }

        updateHistory(volumeUp, prevMusicVolume);

        if (detectLongPress()) {
            RecUtils.log("Music volume polling caught long-press");
            volumeKeyInputController.undoPresses(2);
            waitingForKeyRelease = true;
            initLongPressKeyReleaseHandler();

            boolean expected = volumeKeyInputController.longPressDetected(volumeUp);
            if (expected) {
                holdVolume = new AudioStreamState(AudioManager.STREAM_MUSIC, prevVolumesLast3.get(0));
            }
        }
        else if (detectVolumeSliderDragged()) {
            RecUtils.log("Music volume polling caught volume-drag");
            waitingForDragRelease = true;
            initVolumeSliderDragReleaseHandler();
        }
        else {
            RecUtils.log("Music volume polling caught click");
            AudioStreamState resetAudioStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevMusicVolume);
            volumeKeyInputController.completePressDetected(volumeUp, resetAudioStreamState);
        }
    }

    /**
     * Long press if 3 up or down presses, where last two are quickly subsequent, and second is within 1 second after first.
     */
    private boolean detectLongPress() {
        if (volumeChangesLast3.size() < 3) return false;

        boolean first = volumeChangesLast3.get(0);
        for (boolean volumeChange : volumeChangesLast3) {
            if (volumeChange != first) return false;
        }

        long delta1 = volumeChangeTimesLast3.get(1) - volumeChangeTimesLast3.get(0);
        long delta2 = volumeChangeTimesLast3.get(2) - volumeChangeTimesLast3.get(1);
        return delta1 < 1000 && delta1 > MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS && delta2 < MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS;
    }

    /**
     * Pre: Long-press not detected, or waiting for key release.
     */
    private boolean detectVolumeSliderDragged() {
        if (volumeChangeTimesLast3.size() < 3) return false;
        return volumeChangeTimesLast3.get(2) - volumeChangeTimesLast3.get(1) < MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS;
    }

    private void updateHistory(boolean volumeUp, int prevMusicVolume) {
        if (volumeChangesLast3.size() == 3) {
            volumeChangesLast3.remove(0);
            volumeChangeTimesLast3.remove(0);
            prevVolumesLast3.remove(0);
        }

        volumeChangesLast3.add(volumeUp);
        volumeChangeTimesLast3.add(System.currentTimeMillis());
        prevVolumesLast3.add(prevMusicVolume);
    }

    Utils.Question isPrev3volumeOneStepFromMax() {
        return () -> isActive() && prevVolumesLast3.size() == 3 && prevVolumesLast3.get(0) == maxMusicVolume - 1;
    }

    Utils.Question isPrev3volumeOneStepFromMin() {
        return () -> isActive() && prevVolumesLast3.size() == 3 && prevVolumesLast3.get(0) == minMusicVolume + 1;
    }

    private static final long TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED = Math.max(MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS, MUSIC_VOLUME_POLLING_DELTA * 2);
    private Handler longPressKeyReleaseHandler = new Handler();
    private void initLongPressKeyReleaseHandler() {
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        longPressKeyReleaseHandler.postDelayed(this::completeLongPressDetected, TIME_INACTIVE_BEFORE_KEY_RELEASE_ASSUMED);
    }

    private void completeLongPressDetected() {
        RecUtils.log("Complete long press");
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        waitingForKeyRelease = false;
        holdVolume = null;
        AudioStreamState resetAudioStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevVolumesLast3.get(0));
        volumeKeyInputController.completePressDetected(volumeChangesLast3.get(0), resetAudioStreamState);
    }

    private static final long TIME_INACTIVE_BEFORE_DRAG_RELEASE_ASSUMED = 1000;
    private Handler volumeSliderDragReleaseHandler = new Handler();
    private void initVolumeSliderDragReleaseHandler() {
        volumeSliderDragReleaseHandler.removeCallbacksAndMessages(null);
        volumeSliderDragReleaseHandler.postDelayed(this::completeVolumeSliderDragDetected, TIME_INACTIVE_BEFORE_DRAG_RELEASE_ASSUMED);
    }

    private void completeVolumeSliderDragDetected() {
        RecUtils.log("Complete slider drag");
        longPressKeyReleaseHandler.removeCallbacksAndMessages(null);
        waitingForDragRelease = false;
        volumeKeyInputController.discardPresses();
    }

    // endregion

    Runnable getResetAction() {
        return this::startPolling;
    }
}
