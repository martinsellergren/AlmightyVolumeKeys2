//package com.masel.almightyvolumekeys;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.media.AudioManager;
//import android.os.Handler;
//import android.support.v4.media.session.MediaSessionCompat;
//import android.support.v4.media.session.PlaybackStateCompat;
//import android.view.KeyEvent;
//
//import androidx.media.VolumeProviderCompat;
//
//import com.masel.rec_utils.RecUtils;
//
//import java.util.Calendar;
//
///**
// * Enables user interaction through volume key presses.
// */
//class UserInteraction {
//    private MyContext myContext;
//    //private ActionCommand actionCommand;
//    private Runnable onAccessibilityServiceFail;
//
//    /**
//     * Audio stream to be changed by a long press. Updated at start of each volume longpress. */
//    private int longPressAudioStream = AudioManager.STREAM_MUSIC;
//
//
//    UserInteraction(MyContext myContext, Runnable onAccessibilityServiceFail) {
//        this.myContext = myContext;
//        this.onAccessibilityServiceFail = onAccessibilityServiceFail;
//
//        setupMediaSessionForScreenOffCallbacks();
//        setupWakeLockWhenScreenOff();
//    }
//
//    void destroy() {
//        longPressHandler.removeCallbacksAndMessages(null);
//        failCountHandler.removeCallbacksAndMessages(null);
//        userInteractionWhenScreenOffAndMusic.destroy(myContext);
//        myContext.destroy();
//    }
//
//    void onVolumeKeyEvent(KeyEvent event) {
//        boolean volumeUp = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;
//
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            longPressHandler.removeCallbacksAndMessages(null);
//            longPressHandler.postDelayed(() -> {
//                longPressAudioStream = getLongPressAudioStream();
//                longPress(volumeUp);
//            }, LONG_PRESS_TIME);
//        }
//        else if (event.getAction() == KeyEvent.ACTION_UP) {
//            if (!currentlyVolumeLongPress) {
//                handleVolumeKeyPress(volumeUp);
//            }
//            longPressHandler.removeCallbacksAndMessages(null);
//            currentlyVolumeLongPress = false;
//        }
//        else if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
//            RecUtils.log("MULTIPLE KEY PRESS");
//        }
//        else throw new RuntimeException("Dead end");
//    }
//
//    // region volume long-press
//
//    private static final long LONG_PRESS_TIME = 400;
//    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 40;
//    private Handler longPressHandler = new Handler();
//    private boolean currentlyVolumeLongPress = false;
//
//    private void longPress(boolean volumeUp) {
//        currentlyVolumeLongPress = true;
//        longPressHandler.removeCallbacksAndMessages(null);
//        int currentRelevantStreamVolumePercentage = RecUtils.getStreamVolumePercentage(myContext.audioManager, getRelevantAudioStream());
//        if ((currentRelevantStreamVolumePercentage == 100 && volumeUp) ||
//                (currentRelevantStreamVolumePercentage == 0 && !volumeUp)) {
//            adjustRelevantStreamVolume(volumeUp);
//            return;
//        }
//
//        adjustRelevantStreamVolume(volumeUp);
//        actionCommand.reset();
//        longPressHandler.postDelayed(() -> longPress(volumeUp), LONG_PRESS_VOLUME_CHANGE_TIME);
//    }
//
//    // endregion
//
//    /**
//     * Adds press to action command if appropriate. Else changes volume as normal.
//     * Default volume change if more than 4 volume presses.
//     * @param up True if volume up pressed, false if down.
//     */
//    private void handleVolumeKeyPress(boolean up) {
//        DeviceState state = DeviceState.getCurrent(myContext);
//
//        if (state.equals(DeviceState.IDLE) || state.equals(DeviceState.SOUNDREC)) {
//            actionCommand.addBit(up);
//            if (!Utils.loadFiveClicksBeforeVolumeChange(myContext) || actionCommand.getLength() >= 5) {
//                adjustRelevantStreamVolume(up);
//            }
//        }
//        else if (state.equals(DeviceState.MUSIC)) {
//            actionCommand.addBit(up);
//            adjustRelevantStreamVolume(up);
//        }
//        else {
//            adjustRelevantStreamVolume(up);
//        }
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
//            if (currentlyVolumeLongPress) return longPressAudioStream;
//            else return Utils.loadVolumeClickAudioStream(myContext);
//        }
//    }
//
//    private int getLongPressAudioStream() {
//        return actionCommand.getLength() >= 1 ?
//                Utils.loadVolumeClickAudioStream(myContext) :
//                Utils.loadVolumeLongPressAudioStream(myContext);
//    }
//
//}
//
//
