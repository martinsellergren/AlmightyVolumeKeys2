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

        myContext.deviceState.addMediaStartCallback(this::enableOrDisable);
        myContext.deviceState.addMediaStopCallback(this::enableOrDisable);
        myContext.deviceState.addOnAllowSleepCallback(this::stopPolling);
        myContext.deviceState.addScreenOnCallback(this::enableOrDisable);

        if (deviceStateOkForCapture()) startPolling();
    }

    private void enableOrDisable() {
        boolean enable = deviceStateOkForCapture();
        RecUtils.log("Music polling capture active: " + enable);

        if (enable) startOrContinuePolling();
        else stopPolling();
    }

    private boolean deviceStateOkForCapture() {
        return myContext.deviceState.isMediaPlaying();
    }

    boolean isActive() {
        return isActive;
    }

    void destroy() {
        pollingHandler.removeCallbacksAndMessages(null);
    }

    /**
     * If already polling, discards any uncaught volume changes and restarts.
     */
    private void startPolling() {
        isActive = true;
        prevMusicVolume = myContext.volumeUtils.getVolume(AudioManager.STREAM_MUSIC);
        currentlyAllowVolumeExtremes = prevMusicVolume == maxMusicVolume || prevMusicVolume == minMusicVolume;
        pollMusicVolume();
    }

    private void startOrContinuePolling() {
        if (!isActive) startPolling();
    }

    private static final int MUSIC_VOLUME_POLLING_DELTA = 80;
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

        evaluateWaitingForInactivity();
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
            myContext.volumeUtils.setVolume(holdVolume.getStream(), holdVolume.getVolume(), false, false);
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
            myContext.volumeUtils.setVolume(AudioManager.STREAM_MUSIC, maxMusicVolume - 1, false, false);
            return maxMusicVolume - 1;
        }
        else if (currentVolume == minMusicVolume) {
            myContext.volumeUtils.setVolume(AudioManager.STREAM_MUSIC, minMusicVolume + 1, false, false);
            return minMusicVolume + 1;
        }

        return currentVolume;
    }

    // region Interpret volume change

    /**
     * Time between two presses more than this indicates individual presses. */
    private static final long MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS = 125;

    private List<Boolean> volumeChangesLast3 = new LinkedList<>();
    private List<Long> volumeChangeTimesLast3 = new LinkedList<>();
    private List<Integer> prevVolumesLast3 = new LinkedList<>();

    private void volumeChange(boolean volumeUp, int prevMusicVolume) {
        if (isWaitingForInactivity) {
            lastActivityTime = System.currentTimeMillis();
            return;
        }

        updateHistory(volumeUp, prevMusicVolume);

        if (detectLongPress()) {
            RecUtils.log("Music volume polling caught long-press");
            volumeKeyInputController.undoPresses(2);
            waitForInactivity(MAX_DELTA_PRESS_TIME_FOR_LONG_PRESS, this::completeLongPressDetected);

            boolean expected = volumeKeyInputController.longPressDetected(volumeUp);
            if (expected) {
                holdVolume = new AudioStreamState(AudioManager.STREAM_MUSIC, prevVolumesLast3.get(0));
            }
        }
        else if (detectVolumeSliderDragged()) {
            RecUtils.log("Music volume polling caught volume-drag");
            volumeKeyInputController.discardPresses();
            waitForInactivity(750, this::completeVolumeSliderDragDetected);
        }
        else {
            RecUtils.log("Music volume polling caught click");
            AudioStreamState resetAudioStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevMusicVolume);
            volumeKeyInputController.completePressDetected(volumeUp, resetAudioStreamState);
        }
    }

    private boolean isWaitingForInactivity = false;
    private long lastActivityTime;
    private long minWaitTime;
    private Runnable onWaitDone;

    private void waitForInactivity(long minWaitTime, Runnable onWaitDone) {
        this.minWaitTime = minWaitTime;
        this.onWaitDone = onWaitDone;
        isWaitingForInactivity = true;
        lastActivityTime = System.currentTimeMillis();
    }

    private void evaluateWaitingForInactivity() {
        if (isWaitingForInactivity && System.currentTimeMillis() >= lastActivityTime + minWaitTime) {
            waitDone();
        }
    }

    private void waitDone() {
        if (onWaitDone != null) onWaitDone.run();
        isWaitingForInactivity = false;
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

    private void completeLongPressDetected() {
        RecUtils.log("Complete long press");
        holdVolume = null;
        AudioStreamState resetAudioStreamState = new AudioStreamState(AudioManager.STREAM_MUSIC, prevVolumesLast3.get(0));
        volumeKeyInputController.completePressDetected(volumeChangesLast3.get(0), resetAudioStreamState);
    }

    private void completeVolumeSliderDragDetected() {
        RecUtils.log("Complete slider drag");
        volumeKeyInputController.discardPresses();
    }

    // endregion

    Runnable getResetAction() {
        return this::startPolling;
    }

    Utils.Question isPrev3volumeOneStepFromMax() {
        return () -> isActive() && prevVolumesLast3.size() == 3 && prevVolumesLast3.get(0) == maxMusicVolume - 1;
    }

    Utils.Question isPrev3volumeOneStepFromMin() {
        return () -> isActive() && prevVolumesLast3.size() == 3 && prevVolumesLast3.get(0) == minMusicVolume + 1;
    }
}
