package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RequiresApi;
import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;

import com.masel.rec_utils.RecUtils;

import java.util.List;

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

    private Runnable resetAction;

    VolumeKeyCapture(MyContext myContext, VolumeKeyInputController volumeKeyInputController, Runnable resetAction) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;
        this.resetAction = resetAction;
        init();
    }

    private void init() {
        mediaSession = new MediaSessionCompat(myContext.context, "AVK MEDIA SESSION", new ComponentName(myContext.context, MediaButtonReceiver.class), null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        updateVolumeProvider();
        mediaSession.setActive(true);

        myContext.cameraState.setCallbacks(() -> mediaSession.setActive(false), () -> mediaSession.setActive(true));
        setupResetWhenMusicStarted();
    }

    private void reset() {
        destroy();
        init();
    }

    void destroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }

        try {
            if ( Build.VERSION.SDK_INT >= 26) myContext.audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
        } catch (Exception e) {}
    }

    private AudioManager.AudioPlaybackCallback audioPlaybackCallback;

    private void setupResetWhenMusicStarted() {
        if (Build.VERSION.SDK_INT >= 26) {
            setupResetWhenMusicStarted_method1();
        }
        else {
            setupResetWhenMusicStarted_method2();
        }
    }

    private Handler handler = new Handler();
    @RequiresApi(api = 26)
    private void setupResetWhenMusicStarted_method1() {
        audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> resetAction.run(), 100);
            }
        };
        myContext.audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null);
    }

    private void setupResetWhenMusicStarted_method2() {
        // todo
    }

    private VolumeProviderCompat createRelevantVolumeProvider() {
        int stream = Utils.getRelevantAudioStream(myContext);
        int steps = RecUtils.getAudioStreamSteps(myContext.audioManager, stream);
        int volume = myContext.audioManager.getStreamVolume(stream);

        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, steps, volume) {
            @Override
            public void onAdjustVolume(int direction) {
                handleVolumeKeyPress(direction);
                updateVolumeProvider();

                boolean screenIsOn = RecUtils.isScreenOn(myContext.powerManager);
                if (screenIsOn || myContext.audioManager.isMusicActive()) {
                    updateVolume(direction);
                    Utils.setVolume_withFallback(myContext, mirroredAudioStream, getCurrentVolume(), false);
                }
                if (screenIsOn) {
                    myContext.accessibilityServiceFailing = true;
                }
            }
        };
    }

    private void updateVolumeProvider() {
        int audioStreamToMirror = Utils.getRelevantAudioStream(myContext);
        if (mirroredAudioStream != audioStreamToMirror) {
            volumeProvider = createRelevantVolumeProvider();
            mirroredAudioStream = audioStreamToMirror;
            mediaSession.setPlaybackToRemote(volumeProvider);
        }
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
            resetAudioStreamState = new AudioStreamState(myContext.audioManager, mirroredAudioStream);
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