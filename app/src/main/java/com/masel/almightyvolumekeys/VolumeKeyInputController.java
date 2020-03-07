package com.masel.almightyvolumekeys;

import android.os.Handler;

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

    void setResetActionForVolumeKeyCaptureWhenMusic(Runnable resetVolumeKeyCaptureWhenMusic) {
        actionCommand.setResetActionForVolumeKeyCaptureWhenMusic(resetVolumeKeyCaptureWhenMusic);
    }

    void destroy() {
        longPressHandler.removeCallbacksAndMessages(null);
        actionCommand.reset();
    }

//    /**
//     * On volume key down.
//     * @param onLongPress Additional action when long-press detected.
//     */
//    void keyDown(Runnable onLongPress) {
//        actionCommand.halt();
//
//        longPressHandler.removeCallbacksAndMessages(null);
//        longPressHandler.postDelayed(() -> {
//            onLongPress.run();
//            longPressDetected();
//        }, LONG_PRESS_TIME);
//    }

    /**
     * On volume key up.
     * @param volumeUp Else down.
     * @param resetAudioStreamState Audio stream state as it was before click happened.
     */
    void keyUp(boolean volumeUp, AudioStreamState resetAudioStreamState) {
        int press = volumeUp ? ActionCommand.VOLUME_PRESS_UP : ActionCommand.VOLUME_PRESS_DOWN;
        if (currentLongPress) press = volumeUp ? ActionCommand.VOLUME_PRESS_LONG_UP : ActionCommand.VOLUME_PRESS_LONG_DOWN;

        int state = myContext.deviceState.getCurrent();
        if (state == DeviceState.IDLE || state == DeviceState.MUSIC || state == DeviceState.SOUNDREC) {
            actionCommand.addBit(press, resetAudioStreamState);
        }

        longPressHandler.removeCallbacksAndMessages(null);
        currentLongPress = false;
    }

    boolean longPressDetected(boolean volumeUp) {
        actionCommand.addBit(volumeUp ? ActionCommand.VOLUME_PRESS_LONG_UP : ActionCommand.VOLUME_PRESS_LONG_DOWN, null);
        boolean expected = actionCommand.isMappedCommandStart();
        actionCommand.removeLastBit();

        if (expected && !currentLongPress) {
            myContext.vibrator.vibrate();
        }

        actionCommand.halt();
        currentLongPress = true;

        return expected;
    }

    void undoPresses(int count) {
        for (int i = 0; i < count; i++) actionCommand.removeLastBit();
    }

    void adjustVolumeIfAppropriate(int audioStream, boolean volumeUp, boolean showUi) {
        int state = myContext.deviceState.getCurrent();
        boolean appropriate = false;

        if (state == DeviceState.IDLE || state == DeviceState.SOUNDREC) {
            if (myContext.deviceState.isScreenOn() && (!Utils.loadFiveClicksBeforeVolumeChange(myContext) || actionCommand.getLength() >= 5)) {
                appropriate = true;
            }
        }
        else {
            appropriate = true;
        }

        if (appropriate) {
            myContext.volumeUtils.adjust(audioStream, volumeUp, showUi);
        }
    }
}
