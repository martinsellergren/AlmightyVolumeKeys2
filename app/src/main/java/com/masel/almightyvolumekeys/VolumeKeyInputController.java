package com.masel.almightyvolumekeys;

import android.os.Handler;

import com.masel.rec_utils.RecUtils;

class VolumeKeyInputController {

    private static final long LONG_PRESS_TIME = 400;

    private MyContext myContext;
    private ActionCommand actionCommand;

    private boolean currentLongPress = false;
    private Handler longPressHandler = new Handler();

    VolumeKeyInputController(MyContext myContext) {
        this.myContext = myContext;

        actionCommand = new ActionCommand(myContext);
    }

    void setManualMusicVolumeChanger(VolumeKeyCaptureWhenScreenOffAndMusic.ManualMusicVolumeChanger manualMusicVolumeChanger) {
        actionCommand.setManualMusicVolumeChanger(manualMusicVolumeChanger);
    }

    void destroy() {
        longPressHandler.removeCallbacksAndMessages(null);
        actionCommand.reset();
    }

    /**
     * On volume key down.
     * @param onLongPress Additional action when long-press detected.
     */
    void keyDown(Runnable onLongPress) {
        actionCommand.halt();

        longPressHandler.removeCallbacksAndMessages(null);
        longPressHandler.postDelayed(() -> {
            onLongPress.run();
            longPressDetected(0);
        }, LONG_PRESS_TIME);
    }

    /**
     * On volume key up.
     * @param volumeUp Else down.
     * @param resetAudioStreamState Audio stream state as it was before click happened.
     */
    void keyUp(boolean volumeUp, AudioStreamState resetAudioStreamState) {
        int press = volumeUp ? ActionCommand.VOLUME_PRESS_UP : ActionCommand.VOLUME_PRESS_DOWN;
        if (currentLongPress) press = volumeUp ? ActionCommand.VOLUME_PRESS_LONG_UP : ActionCommand.VOLUME_PRESS_LONG_DOWN;

        DeviceState state = DeviceState.getCurrent(myContext);
        if (state == DeviceState.IDLE || state == DeviceState.MUSIC || state == DeviceState.SOUNDREC) {
            actionCommand.addBit(press, resetAudioStreamState);
        }

        longPressHandler.removeCallbacksAndMessages(null);
        currentLongPress = false;
    }

    void longPressDetected(int noRollbackPresses) {
        actionCommand.halt();
        currentLongPress = true;
        myContext.vibrator.vibrate();
        for (int i = 0; i < noRollbackPresses; i++) actionCommand.removeLastBit();
    }

    void adjustVolumeIfAppropriate(int audioStream, boolean volumeUp) {
        DeviceState state = DeviceState.getCurrent(myContext);
        boolean appropriate = false;

        if (state.equals(DeviceState.IDLE) || state.equals(DeviceState.SOUNDREC)) {
            if (RecUtils.isScreenOn(myContext.powerManager) && (!Utils.loadFiveClicksBeforeVolumeChange(myContext) || actionCommand.getLength() >= 5)) {
                appropriate = true;
            }
        }
        else {
            appropriate = true;
        }

        if (appropriate) {
            Utils.adjustVolume_withFallback(myContext.audioManager, audioStream, volumeUp, true);
        }
    }
}
