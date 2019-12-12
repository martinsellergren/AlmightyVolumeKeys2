package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.media.VolumeProviderCompat;

import com.masel.rec_utils.Utils;

/**
 * Enables user interaction through volume key presses.
 */
class UserInteraction {

    static final int PREVENT_SLEEP_TIMEOUT_MINUTES = 60 * 24; // todo: read from settings

    private MyContext myContext;
    private ActionCommand actionCommand;

    private UserInteractionWhenScreenOffAndMusic userInteractionWhenScreenOffAndMusic;

    UserInteraction(Context context) {
        this.myContext = new MyContext(context);
        actionCommand = new ActionCommand(myContext);
        setupMediaSessionForScreenOffCallbacks();
        userInteractionWhenScreenOffAndMusic = new UserInteractionWhenScreenOffAndMusic(myContext, actionCommand);
        actionCommand.setManualMusicVolumeChanger(userInteractionWhenScreenOffAndMusic.getManualMusicVolumeChanger());

        setupWakeLockWhenScreenOff();
    }

    void release() {
        userInteractionWhenScreenOffAndMusic.release(myContext);
        myContext.destroy();
    }

    void onVolumeKeyEvent(KeyEvent event) {
        boolean volumeUp = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            longPressHandler.postDelayed(() -> longPress(volumeUp), LONG_PRESS_TIME);
        }
        else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (!currentlyVolumeLongPress) handleVolumeKeyPress(volumeUp);
            longPressHandler.removeCallbacksAndMessages(null);
            currentlyVolumeLongPress = false;
        }
        else if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
            Utils.log("MULTIPLE KEY PRESS");
        }
        else throw new RuntimeException("Dead end");
    }

    // region volume long-press

    private static final long LONG_PRESS_TIME = 400;
    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 40;
    private Handler longPressHandler = new Handler();
    private boolean currentlyVolumeLongPress = false;

    private void longPress(boolean volumeUp) {
        //Utils.log("LONG PRESS");
        currentlyVolumeLongPress = true;
        adjustRelevantStreamVolume(volumeUp);
        longPressHandler.postDelayed(() -> longPress(volumeUp), LONG_PRESS_VOLUME_CHANGE_TIME);
        actionCommand.reset();
    }

    // endregion

    /**
     * Adds press to action command if appropriate. Else changes volume as normal.
     * Default volume change if more than 4 volume presses.
     * @param up True if volume up pressed, false if down.
     */
    private void handleVolumeKeyPress(boolean up) {
        DeviceState state = DeviceState.getCurrent(myContext);
        if (state.equals(DeviceState.IDLE) || state.equals(DeviceState.RECORDING_AUDIO)) {
            if (actionCommand.getLength() >= 4) {
                adjustRelevantStreamVolume(up);
            }
            actionCommand.addBit(up);
        }
        else if (state.equals(DeviceState.MUSIC)) {
            adjustRelevantStreamVolume(up);
            actionCommand.addBit(up);
        }
        else {
            adjustRelevantStreamVolume(up);
        }
    }

    /**
     * Finds relevant stream and changes its volume.
     * @param up else down
     *
     * todo: investigate AudioManager.adjustVolume(), USE_DEFAULT_STREAM_TYPE -constant
     * todo: when alarm..
     */
    private void adjustRelevantStreamVolume(boolean up) {
        int dir = up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        int volumeChangeFlag = AudioManager.FLAG_SHOW_UI;
        int activeStream = Utils.getActiveAudioStream(myContext.audioManager);

        if (activeStream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
            activeStream = AudioManager.STREAM_MUSIC; //todo: getCurrent from user settings
        }

        if (activeStream == AudioManager.STREAM_SYSTEM) {
            myContext.notifier.notify("SYSTEM VOLUME CHANGE!!!", Notifier.VibrationPattern.ERROR, false);
        }
        myContext.audioManager.adjustStreamVolume(activeStream, dir, volumeChangeFlag);
    }

    private void setupMediaSessionForScreenOffCallbacks() {
        MediaSessionCompat mediaSession = myContext.mediaSession;
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder(); stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        VolumeProviderCompat volumeProvider = screenOffCallback();
        mediaSession.setPlaybackToRemote(volumeProvider);
        mediaSession.setActive(true);
    }

    /**
     * Fallback when onKeyEvent doesn't catch the event (i.e when screen is off).
     * Music active, in call, phone ringing etc "should" take control over the volume-keys and
     * therefor suppress this callback. To be safe, check if to add press to action-command OR
     * change volume of appropriate stream.
     */
    private VolumeProviderCompat screenOffCallback() {
        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 2, 1) {
            @Override
            public void onAdjustVolume(int direction) {
                if (direction != 0) {
                    boolean up = direction > 0;
                    handleVolumeKeyPress(up);
                }
            }
        };
    }

    /**
     * Keep cpu (and volume keys) on after screen off, for minimum time defined in user-settings.
     */
    private void setupWakeLockWhenScreenOff() {
        myContext.context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long timeout = PREVENT_SLEEP_TIMEOUT_MINUTES * 60000;
                myContext.wakeLock.acquire(timeout);
                Utils.log("Wake lock acquired");
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }
}


