package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;

import com.masel.rec_utils.RecUtils;

/**
 * Key press goes here when not caught by AccessibilityService. This happens when screen off, or service fail.
 * Key press not caught if media-session used is betting overshadowed (likely when music, ringing, in call..).
 * Disable media session when camera is active.
 */
class VolumeKeyCaptureWhenScreenOff {
    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private VolumeKeyCaptureWhenScreenOffAndMusic volumeKeyCaptureWhenScreenOffAndMusic;
    private MediaSessionCompat mediaSession;

    VolumeKeyCaptureWhenScreenOff(MyContext myContext, VolumeKeyInputController volumeKeyInputController, VolumeKeyCaptureWhenScreenOffAndMusic volumeKeyCaptureWhenScreenOffAndMusic) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        this.volumeKeyCaptureWhenScreenOffAndMusic = volumeKeyCaptureWhenScreenOffAndMusic;

        mediaSession = new MediaSessionCompat(myContext.context, "AVK MEDIA SESSION", new ComponentName(myContext.context, MediaButtonReceiver.class), null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setPlaybackToRemote(new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 0, 0) {
            @Override
            public void onAdjustVolume(int direction) {
                onVolumeKeyPress(direction);
            }
        });
        mediaSession.setActive(true);
        myContext.cameraState.setCallbacks(() -> mediaSession.setActive(false), () -> mediaSession.setActive(true));
    }

    void destroy() {
        mediaSession.setActive(false);
        mediaSession.release();
    }

    boolean isActive() {
        return mediaSession.isActive();
    }

    private enum Status {noHeld, volumeDownHeld, volumeUpHeld };
    private Status status = Status.noHeld;
    private AudioStreamState resetAudioStreamState;


    /**
     * Music active, in call, phone ringing etc "should" take control over the volume-keys and
     * therefor suppress this callback. To be safe, check if to add press to action-command and/or
     * change volume of appropriate stream.
     *
     * Screen on and active camera: media session should be disabled. If for some reason not
     * yet disabled and volume press passes through here, do nothing.
     */
    private void onVolumeKeyPress(int direction) {
        if (myContext.cameraState.isCameraActive()) return;

        if (direction != 0) {
            if (status == Status.noHeld) {
                keyDown(direction > 0);
                status = direction > 0 ? Status.volumeUpHeld : Status.volumeDownHeld;
            }
            else {
                // ignore
            }
        }
        else {
            if (status == Status.volumeUpHeld) keyUp(true);
            else if (status == Status.volumeDownHeld) keyUp(false);

            status = Status.noHeld;
        }
    };

    private void keyDown(boolean volumeUp) {
        RecUtils.log("Media session caught press");

        volumeKeyCaptureWhenScreenOffAndMusic.setDisabled(true);
        int relevantAudioStream = Utils.getRelevantAudioStream(myContext);
        volumeKeyInputController.keyDown(() -> myContext.volumeMovement.start(relevantAudioStream, volumeUp));
        resetAudioStreamState = new AudioStreamState(myContext.audioManager, Utils.getRelevantAudioStream(myContext));
    }

    private void keyUp(boolean volumeUp) {
        volumeKeyInputController.keyUp(volumeUp, resetAudioStreamState);
        myContext.volumeMovement.stop();
        volumeKeyInputController.adjustVolumeIfAppropriate(resetAudioStreamState.getStream(), volumeUp);
        volumeKeyCaptureWhenScreenOffAndMusic.setDisabled(false);
    }
}
