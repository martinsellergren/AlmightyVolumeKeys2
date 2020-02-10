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

import com.masel.rec_utils.RecUtils;

import java.util.Calendar;

/**
 * Enables user interaction through volume key presses.
 */
class UserInteraction {
    private MyContext myContext;
    private ActionCommand actionCommand;
    private Runnable onAccessibilityServiceFail;

    /**
     * Audio stream to be changed by a long press. Updated at start of each volume longpress. */
    private int longPressAudioStream = AudioManager.STREAM_MUSIC;

    private UserInteractionWhenScreenOffAndMusic userInteractionWhenScreenOffAndMusic;

    UserInteraction(MyContext myContext, Runnable onAccessibilityServiceFail) {
        this.myContext = myContext;
        this.onAccessibilityServiceFail = onAccessibilityServiceFail;

        actionCommand = new ActionCommand(myContext);
        setupMediaSessionForScreenOffCallbacks();
        userInteractionWhenScreenOffAndMusic = new UserInteractionWhenScreenOffAndMusic(myContext, actionCommand);
        actionCommand.setManualMusicVolumeChanger(userInteractionWhenScreenOffAndMusic.getManualMusicVolumeChanger());

        setupWakeLockWhenScreenOff();
    }

    void destroy() {
        longPressHandler.removeCallbacksAndMessages(null);
        failCountHandler.removeCallbacksAndMessages(null);
        userInteractionWhenScreenOffAndMusic.destroy(myContext);
        myContext.destroy();
    }

    void onVolumeKeyEvent(KeyEvent event) {
        boolean volumeUp = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            longPressHandler.removeCallbacksAndMessages(null);
            longPressHandler.postDelayed(() -> {
                longPressAudioStream = getLongPressAudioStream();
                longPress(volumeUp);
            }, LONG_PRESS_TIME);
        }
        else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (!currentlyVolumeLongPress) {
                handleVolumeKeyPress(volumeUp);
            }
            longPressHandler.removeCallbacksAndMessages(null);
            currentlyVolumeLongPress = false;
        }
        else if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
            RecUtils.log("MULTIPLE KEY PRESS");
        }
        else throw new RuntimeException("Dead end");
    }

    // region volume long-press

    private static final long LONG_PRESS_TIME = 400;
    private static final long LONG_PRESS_VOLUME_CHANGE_TIME = 40;
    private Handler longPressHandler = new Handler();
    private boolean currentlyVolumeLongPress = false;

    private void longPress(boolean volumeUp) {
        currentlyVolumeLongPress = true;
        longPressHandler.removeCallbacksAndMessages(null);
        int currentRelevantStreamVolumePercentage = RecUtils.getStreamVolumePercentage(myContext.audioManager, getRelevantAudioStream());
        if ((currentRelevantStreamVolumePercentage == 100 && volumeUp) ||
                (currentRelevantStreamVolumePercentage == 0 && !volumeUp)) {
            adjustRelevantStreamVolume(volumeUp);
            return;
        }

        adjustRelevantStreamVolume(volumeUp);
        actionCommand.reset();
        longPressHandler.postDelayed(() -> longPress(volumeUp), LONG_PRESS_VOLUME_CHANGE_TIME);
    }

    // endregion

    /**
     * Adds press to action command if appropriate. Else changes volume as normal.
     * Default volume change if more than 4 volume presses.
     * @param up True if volume up pressed, false if down.
     */
    private void handleVolumeKeyPress(boolean up) {
        DeviceState state = DeviceState.getCurrent(myContext);

        if (state.equals(DeviceState.IDLE) || state.equals(DeviceState.SOUNDREC) || state.equals(DeviceState.CAMERA)) {
            actionCommand.addBit(up);
            if (!Utils.loadFiveClicksBeforeVolumeChange(myContext) || actionCommand.getLength() >= 5) {
                adjustRelevantStreamVolume(up);
            }
        }
        else if (state.equals(DeviceState.MUSIC)) {
            actionCommand.addBit(up);
            adjustRelevantStreamVolume(up);
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
        int relevantStream = getRelevantAudioStream();

        try {
            myContext.audioManager.adjustStreamVolume(relevantStream, dir, volumeChangeFlag);
        }
        catch (SecurityException e) {
            myContext.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, volumeChangeFlag);
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
            if (currentlyVolumeLongPress) return longPressAudioStream;
            else return Utils.loadVolumeClicksAudioStream(myContext);
        }
    }

    private int getLongPressAudioStream() {
        return actionCommand.getLength() >= 1 ?
                Utils.loadVolumeClicksAudioStream(myContext) :
                Utils.loadVolumeLongPressAudioStream(myContext);
    }

    /**
     * Disable media session when camera is active.
     */
    private void setupMediaSessionForScreenOffCallbacks() {
        MediaSessionCompat mediaSession = myContext.mediaSession;
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setPlaybackToRemote(screenOffCallback());
        mediaSession.setActive(true);
        myContext.setCameraStateCallbacks(() -> mediaSession.setActive(false), () -> mediaSession.setActive(true));
    }

    /**
     * Fallback when onKeyEvent doesn't catch the event (happens when screen is off).
     * Music active, in call, phone ringing etc "should" take control over the volume-keys and
     * therefor suppress this callback. To be safe, check if to add press to action-command OR
     * change volume of appropriate stream.
     *
     * Screen on and active camera: media session should be disabled. If for some reason not
     * yet disabled and volume press passes through here, do nothing.
     *
     * Else if happens when screen on: accessibility service fail to catch volume press.
     * Run fail-action, if happens 3 times within 10 seconds.
     */
    private int failCount = 0;
    private Handler failCountHandler = new Handler();
    private VolumeProviderCompat screenOffCallback() {
        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 2, 1) {
            @Override
            public void onAdjustVolume(int direction) {
                boolean up = direction > 0;

                if (myContext.isCameraActive()) {
                    // noop
                }
                else if (RecUtils.isScreenOn(myContext.powerManager)) {
                    failCountHandler.removeCallbacksAndMessages(null);
                    if (failCount == 0) failCountHandler.postDelayed(() -> failCount = 0, 10000);
                    failCount += 1;
                    if (failCount > 3) onAccessibilityServiceFail.run();
                }
                else if (direction != 0) {
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
                int preventSleepMinutes = myContext.sharedPreferences.getInt("SeekBarPreference_preventSleepTimeout", 60);
                boolean allowSleepSwitch = myContext.sharedPreferences.getBoolean("SwitchPreferenceCompat_allowSleep", false);
                int allowSleepStartHour = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepStart", 0);
                int allowSleepEndHour = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepEnd", 0);

                long timeout = preventSleepMinutes * 60000;
                boolean allowSleep = allowSleepSwitch && currentlyAllowSleep(allowSleepStartHour, allowSleepEndHour);

                if (!allowSleep) {
                    myContext.wakeLock.acquire(timeout);
                    RecUtils.log("Wake lock acquired for (minutes): " + preventSleepMinutes);
                }
                else {
                    RecUtils.log("Wake lock not acquired (prevention bypassed by user settings)");
                }
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private boolean currentlyAllowSleep(int allowStartHour, int allowStopHour) {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hourInterval(allowStartHour, allowStopHour) > hourInterval(allowStartHour, currentHour);
    }

    private int hourInterval(int from, int to) {
        return from <= to ? to - from : (to + 24) - from;
    }
}


