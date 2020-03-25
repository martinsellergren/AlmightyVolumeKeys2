package com.masel.almightyvolumekeys;

class VolumeKeyInputController {
    private MyContext myContext;
    private ActionCommand actionCommand;

    private boolean currentLongPress = false;

    VolumeKeyInputController(MyContext myContext) {
        this.myContext = myContext;
        actionCommand = new ActionCommand(myContext);
    }

    void destroy() {
        actionCommand.reset();
    }

    /**
     * @param volumeUp Else down.
     * @param resetAudioStreamState Audio stream state as it was before click happened.
     */
    void completePressDetected(boolean volumeUp, AudioStreamState resetAudioStreamState) {
        int press = volumeUp ? ActionCommand.VOLUME_PRESS_UP : ActionCommand.VOLUME_PRESS_DOWN;
        if (currentLongPress) press = volumeUp ? ActionCommand.VOLUME_PRESS_LONG_UP : ActionCommand.VOLUME_PRESS_LONG_DOWN;

        actionCommand.addBit(press, resetAudioStreamState);
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

    void undoPresses(int n) {
        for (int i = 0; i < n; i++) actionCommand.removeLastBit();
    }

    void discardPresses() {
        actionCommand.reset();
    }

    void setAllowCurrentCommandToSetExtremeVolumeQuestions(Utils.Question allowCurrentCommandToSetMaxVolumeQuestion, Utils.Question allowCurrentCommandToSetMinVolumeQuestion) {
        actionCommand.setAllowCurrentCommandToSetExtremeVolumeQuestions(allowCurrentCommandToSetMaxVolumeQuestion, allowCurrentCommandToSetMinVolumeQuestion);
    }
}
