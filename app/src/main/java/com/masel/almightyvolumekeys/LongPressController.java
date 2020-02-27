//package com.masel.almightyvolumekeys;
//
//import android.media.AudioManager;
//import android.os.Handler;
//
//import com.masel.rec_utils.RecUtils;
//
//class LongPressController {
//
//    private MyContext myContext;
//    private ActionCommand actionCommand;
//    private AudioStreamState resetAudioStreamState = null;
//
//    LongPressController(MyContext myContext, ActionCommand actionCommand) {
//        this.myContext = myContext;
//        this.actionCommand = actionCommand;
//    }
//
//    void destroy() {
//        pairedPressHandler.removeCallbacksAndMessages(null);
//    }
//
//    // region paired click
//
//
//    private static final long LONG_PRESS_TIME = 400;
//    private Handler pairedPressHandler = new Handler();
//    private boolean pairedPressLongPress = false;
//    //private int longPressAudioStream = AudioManager.STREAM_MUSIC;
//
//    boolean pairedClick(boolean volumeUp, boolean keyIn) {
//        if (pairedPressLongPress) return true;
//
//        if (keyIn) {
//            actionCommand.halt();
//            pairedPressHandler.removeCallbacksAndMessages(null);
//            pairedPressHandler.postDelayed(() -> {
//                pairedPressLongPress = true;
//                resetAudioStreamState = new AudioStreamState(myContext.audioManager, getRelevantAudioStream());
//                notifyLongPress();
//                moveVolume(resetAudioStreamState.getStream(), volumeUp);
//            }, LONG_PRESS_TIME);
//            return false;
//        }
//        else {
//            pairedPressHandler.removeCallbacksAndMessages(null);
//
//            if (pairedPressLongPress) {
//                pairedPressLongPress = false;
//                onKeyReleased(volumeUp);
//                return true;
//            }
//            else {
//                return false;
//            }
//        }
//    }
//
//    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 40;
//
//    private void moveVolume(int stream, boolean volumeUp) {
//
//    }
//
//    private void notifyLongPress() {
//        //short vibrate
//    }
//
//    private void onKeyReleased(boolean volumeUp) {
//        int volumePress = volumeUp ? ActionCommand.VOLUME_PRESS_LONG_UP : ActionCommand.VOLUME_PRESS_LONG_DOWN;
//        actionCommand.addBit(volumePress, resetAudioStreamState);
//    }
//
////    private void longPress(boolean volumeUp) {
////        pairedPressLongPress = true;
////        pairedPressHandler.removeCallbacksAndMessages(null);
////
////        int relevantAudioStream = getRelevantAudioStream();
////        Utils.adjustVolume_withFallback(myContext, relevantAudioStream, volumeUp);
////
////        int currentRelevantStreamVolumePercentage = RecUtils.getStreamVolumePercentage(myContext.audioManager, relevantAudioStream);
////        if ((currentRelevantStreamVolumePercentage == 100 && volumeUp) || (currentRelevantStreamVolumePercentage == 0 && !volumeUp)) {
////            return;
////        }
////
////        actionCommand.reset();
////        pairedPressHandler.postDelayed(() -> longPress(volumeUp), LONG_PRESS_VOLUME_CHANGE_TIME);
////    }
//
////    private int getLongPressAudioStream() {
////        return actionCommand.getLength() >= 1 ?
////                Utils.loadVolumeKeysAudioStream(myContext) :
////                Utils.loadVolumeLongPressAudioStream(myContext);
////    }
//
//    // endregion
//
//
//    // region single click
//
//    boolean singleClick(boolean up) {
//        return false;
//    }
//
//    boolean singleClickDetectedFromMusicVolumeChange(boolean up, int musicVolumeBeforeDetected) {
//        return false;
//    }
//
//    /**
//     * @return Audio stream to be adjusted on a volume changing key-event.
//     */
//    private int getRelevantAudioStream() {
//        int activeStream = RecUtils.getActiveAudioStream(myContext.audioManager);
//        if (activeStream != AudioManager.USE_DEFAULT_STREAM_TYPE) {
//            return activeStream;
//        }
//        else {
//            //return longPressAudioStream;
//            return Utils.loadVolumeLongPressAudioStream(myContext);
//        }
//    }
//
//    // endregion
//}
