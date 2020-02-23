package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;

import com.masel.rec_utils.RecUtils;

/**
 * Key press goes here, OR slips through and changes volume directly (likely when music).
 * Disable media session when camera is active.
 */
class VolumeKeyCaptureWhenScreenOff {
    private MyContext myContext;
    private VolumeKeyInputController inputController;
    private Runnable onAccessibilityServiceFail;
    private MediaSessionCompat mediaSession;

    VolumeKeyCaptureWhenScreenOff(MyContext myContext, VolumeKeyInputController inputController, Runnable onAccessibilityServiceFail) {
        this.myContext = myContext;
        this.inputController = inputController;
        this.onAccessibilityServiceFail = onAccessibilityServiceFail;

        mediaSession = new MediaSessionCompat(myContext.context, "AVK MEDIA SESSION", new ComponentName(myContext.context, MediaButtonReceiver.class), null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setPlaybackToRemote(screenOffCallback);
        mediaSession.setActive(true);
        myContext.cameraState.setCallbacks(() -> mediaSession.setActive(false), () -> mediaSession.setActive(true));
    }

    void destroy() {
        failCountHandler.removeCallbacksAndMessages(null);
        mediaSession.setActive(false);
        mediaSession.release();
    }

    /**
     * Fallback when onKeyEvent doesn't catch the event (happens when screen is off).
     * Music active, in call, phone ringing etc "should" take control over the volume-keys and
     * therefor suppress this callback. To be safe, check if to add press to action-command and/or
     * change volume of appropriate stream.
     *
     * Screen on and active camera: media session should be disabled. If for some reason not
     * yet disabled and volume press passes through here, do nothing.
     *
     * Else if happens when screen on: accessibility service fail to catch volume press.
     * Run fail-action, if happens 3 times within 5 seconds.
     */
    private int failCount = 0;
    private Handler failCountHandler = new Handler();
    private VolumeProviderCompat screenOffCallback = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 2, 1) {
        @Override
        public void onAdjustVolume(int direction) {
            if (myContext.cameraState.isCameraActive()) {
                // noop
            }
            else if (RecUtils.isScreenOn(myContext.powerManager)) {
                failCountHandler.removeCallbacksAndMessages(null);
                if (failCount == 0) failCountHandler.postDelayed(() -> failCount = 0, 5000);
                failCount += 1;
                if (failCount >= 3) onAccessibilityServiceFail.run();
            }
            else if (direction != 0) {
                RecUtils.log("Media session caught press");
                inputController.singleClick(direction > 0);
            }
        }
    };

}
