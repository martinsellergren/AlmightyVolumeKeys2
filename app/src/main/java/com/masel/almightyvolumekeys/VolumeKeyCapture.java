package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;

import com.masel.rec_utils.RecUtils;

/**
 * Key press goes here when not caught by AccessibilityService. This happens when screen off, or service fail.
 * Key press not caught if media-session used is being overshadowed (likely when music).
 * Disable media session when camera is active.
 */
class VolumeKeyCapture {
    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private MediaSessionCompat mediaSession;

    private VolumeProviderCompat volumeProvider;
    private int mirroredAudioStream = -1;

    VolumeKeyCapture(MyContext myContext, VolumeKeyInputController volumeKeyInputController) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        init();
    }

    private Runnable resetVolumeKeyCaptureWhenMusicAndScreenOff = null;

    void setResetVolumeKeyCaptureWhenMusicAndScreenOff(Runnable resetVolumeKeyCaptureWhenMusicAndScreenOff) {
        this.resetVolumeKeyCaptureWhenMusicAndScreenOff = resetVolumeKeyCaptureWhenMusicAndScreenOff;
    }

    private void init() {
        mediaSession = new MediaSessionCompat(myContext.context, "AVK MEDIA SESSION", new ComponentName(myContext.context, MediaButtonReceiver.class), null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        updateVolumeProvider();
        updateActiveStatus();

        //myContext.deviceStateCallbacks.setCameraStateCallbacks(() -> mediaSession.setActive(false), () -> mediaSession.setActive(true));
        myContext.deviceStateCallbacks.addMediaStartCallback(this::updateActiveStatus);
        myContext.deviceStateCallbacks.addMediaStopCallback(this::updateActiveStatus);
        myContext.deviceStateCallbacks.addDeviceUnlockedCallback(this::updateActiveStatus);
        myContext.deviceStateCallbacks.addScreenOffCallback(this::updateActiveStatus);
    }

    private void updateActiveStatus() {
        boolean active = !myContext.deviceState.isDeviceUnlocked() && !myContext.deviceState.isMediaPlaying();
        mediaSession.setActive(active);
        RecUtils.log("Media session active: " + active);
    }

    void destroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }

    private void updateVolumeProvider() {
        int audioStreamToMirror = Utils.getRelevantAudioStream(myContext);
        if (mirroredAudioStream != audioStreamToMirror) {
            volumeProvider = createRelevantVolumeProvider();
            mirroredAudioStream = audioStreamToMirror;
            mediaSession.setPlaybackToRemote(volumeProvider);
        }
    }

    private VolumeProviderCompat createRelevantVolumeProvider() {
        int stream = Utils.getRelevantAudioStream(myContext);
        int steps = RecUtils.getAudioStreamSteps(myContext.audioManager, stream);
        int volume = myContext.volumeUtils.get(stream);

        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, steps, volume) {
            @Override
            public void onAdjustVolume(int direction) {
                handleVolumeKeyPress(direction);
                updateVolumeProvider();

                boolean screenIsOn = myContext.deviceState.isScreenOn();
                if (screenIsOn || myContext.deviceState.isMediaPlaying()) {
                    updateVolume(direction);
                    //myContext.volumeUtils.set(mirroredAudioStream, getCurrentVolume(), false);
                    //if (resetVolumeKeyCaptureWhenMusicAndScreenOff != null) resetVolumeKeyCaptureWhenMusicAndScreenOff.run();
                }
            }
        };
    }

    private void updateVolume(int direction) {
        int volume = volumeProvider.getCurrentVolume();
        volume += direction;
        if (volume > volumeProvider.getMaxVolume()) volume = volumeProvider.getMaxVolume();
        if (volume < 0) volume = 0;
        volumeProvider.setCurrentVolume(volume);
    }

    private AudioStreamState resetAudioStreamState;
    private int prevDirection = 0;

    private void handleVolumeKeyPress(int direction) {
        if (direction != 0 && prevDirection == 0) {
            RecUtils.log("Media session caught press");
            resetAudioStreamState = new AudioStreamState(myContext.volumeUtils, mirroredAudioStream);
        }
        else if (direction != 0 && direction == prevDirection) {
            volumeKeyInputController.longPressDetected();
        }
        else if (direction == 0 && prevDirection != 0) {
            volumeKeyInputController.keyUp(prevDirection > 0, resetAudioStreamState);
        }

        prevDirection = direction;
    };
}