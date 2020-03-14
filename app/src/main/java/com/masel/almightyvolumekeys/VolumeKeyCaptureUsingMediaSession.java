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

        myContext.deviceState.addMediaStartCallback(this::enableOrDisable);
        myContext.deviceState.addMediaStopCallback(this::enableOrDisable);
        myContext.deviceState.addOnAllowSleepCallback(() -> mediaSession.setActive(false));
        myContext.deviceState.addScreenOnCallback(this::enableOrDisable);
//        myContext.deviceState.addDeviceUnlockedCallback(this::updateActiveStatus);
//        myContext.deviceState.addScreenOffCallback(this::updateActiveStatus);

        enableOrDisable();
    }

    private void enableOrDisable() {
        boolean active = deviceStateOkForCapture();
        RecUtils.log("Volume key capture: media session: " + active);

        mediaSession.setActive(active);
    }

    private boolean deviceStateOkForCapture() {
        //return !myContext.deviceState.isDeviceUnlocked() && !myContext.deviceState.isMediaPlaying();
        return !myContext.deviceState.isMediaPlaying();
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