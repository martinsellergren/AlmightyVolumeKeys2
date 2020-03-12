package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;

import com.masel.rec_utils.RecUtils;

/**
 * Captures keys when polling volume doesn't work (device locked and no music).
 */
class VolumeKeyCaptureUsingMediaSession {
    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private MediaSessionCompat mediaSession;

    VolumeKeyCaptureUsingMediaSession(MyContext myContext, VolumeKeyInputController volumeKeyInputController) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        init();
    }

    private void init() {
        mediaSession = new MediaSessionCompat(myContext.context, "AVK MEDIA SESSION", new ComponentName(myContext.context, MediaButtonReceiver.class), null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setPlaybackToRemote(new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 0, 0) {
            @Override
            public void onAdjustVolume(int direction) {
                handleVolumeKeyPress(direction);
            }
        });
        updateActiveStatus();

        myContext.deviceState.addMediaStartCallback(this::updateActiveStatus);
        myContext.deviceState.addMediaStopCallback(this::updateActiveStatus);
        myContext.deviceState.addDeviceUnlockedCallback(this::updateActiveStatus);
        myContext.deviceState.addScreenOffCallback(this::updateActiveStatus);
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

    private int prevDirection = 0;

    private void handleVolumeKeyPress(int direction) {
        if (direction != 0 && prevDirection == 0) {
            RecUtils.log("Media session caught press");
        }
        else if (direction != 0 && direction == prevDirection) {
            volumeKeyInputController.longPressDetected(direction > 0);
        }
        else if (direction == 0 && prevDirection != 0) {
            volumeKeyInputController.completePressDetected(prevDirection > 0, null);
        }

        prevDirection = direction;
    };
}