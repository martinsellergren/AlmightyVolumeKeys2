package com.masel.almightyvolumekeys;

import android.media.AudioManager;

import com.masel.rec_utils.RecUtils;

class VolumeKeyInputController {

    private MyContext myContext;
    private ActionCommand actionCommand;
    private LongPressController longPressController;

    VolumeKeyInputController(MyContext myContext) {
        this.myContext = myContext;

        actionCommand = new ActionCommand(myContext);
        longPressController = new LongPressController(myContext, actionCommand);
    }

    void setManualMusicVolumeChanger(VolumeKeyCaptureWhenScreenOffAndMusic.ManualMusicVolumeChanger manualMusicVolumeChanger) {
        actionCommand.setManualMusicVolumeChanger(manualMusicVolumeChanger);
    }

    void destroy() {
        actionCommand.reset();
        longPressController.destroy();
    }

    void pairedClick(boolean volumeUp, boolean keyIn) {
        boolean consumed = longPressController.pairedClick(volumeUp, keyIn);
        if (!consumed && !keyIn) {
            handleClick(volumeUp, true);
        }
    }

    void singleClick(boolean volumeUp, boolean volumeChanged) {
        boolean consumed = longPressController.singleClick(volumeUp, !volumeChanged);
        if (!consumed) {
            handleClick(volumeUp, !volumeChanged);
        }
    }

    /**
     * @param volumeUp
     * @param changeVolume True if to change volume (if appropriate) in addition to register click.
     */
    private void handleClick(boolean volumeUp, boolean changeVolume) {
        DeviceState state = DeviceState.getCurrent(myContext);

        if (state.equals(DeviceState.IDLE) || state.equals(DeviceState.SOUNDREC)) {
            actionCommand.addBit(volumeUp);
            if (changeVolume && (!Utils.loadFiveClicksBeforeVolumeChange(myContext) || actionCommand.getLength() >= 5)) {
                Utils.adjustStreamVolume_noUI(myContext, getRelevantAudioStream(), volumeUp);
            }
        }
        else if (state.equals(DeviceState.MUSIC)) {
            actionCommand.addBit(volumeUp);
            if (changeVolume) Utils.adjustStreamVolume_noUI(myContext, getRelevantAudioStream(), volumeUp);
        }
        else if (changeVolume) {
            Utils.adjustStreamVolume_noUI(myContext, getRelevantAudioStream(), volumeUp);
        }
    }

    /**
     * @return Audio stream to be adjusted on a volume changing key-event.
     */
    private int getRelevantAudioStream() {
        int activeStream = RecUtils.getActiveAudioStream(myContext.audioManager);
        if (activeStream != AudioManager.USE_DEFAULT_STREAM_TYPE) {
            return activeStream;
        }
        else {
            return Utils.loadVolumeClickAudioStream(myContext);
        }
    }
}
