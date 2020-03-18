package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
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

    private VolumeProviderCompat volumeProvider;
    private int controlledAudioStream;

    VolumeKeyCaptureUsingMediaSession(MyContext myContext, VolumeKeyInputController volumeKeyInputController) {
        this.myContext = myContext;
        this.volumeKeyInputController = volumeKeyInputController;

        mediaSession = new MediaSessionCompat(myContext.context, "AVK MEDIA SESSION", new ComponentName(myContext.context, MediaButtonReceiver.class), null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        setupControlledAudioStream();

        myContext.deviceState.addMediaStartCallback(this::enableOrDisable);
        myContext.deviceState.addMediaStopCallback(this::enableOrDisable);
        myContext.deviceState.addOnAllowSleepCallback(() -> mediaSession.setActive(false));
        myContext.deviceState.addScreenOnCallback(this::enableOrDisable);
        myContext.deviceState.addOnRingerModeChangeCallback(this::onRingerModeChange);
        //myContext.deviceState.addOnSettingsChangeCallback(this::syncMediaSessionVolume);

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
        try {
            myContext.sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        } catch (Exception e) {}
    }

    private void setupControlledAudioStream() {
        updateVolumeProvider();
        myContext.sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        myContext.volumeUtils.addOnVolumeSetCallback((stream, volume) -> {
            if (stream == controlledAudioStream) volumeProvider.setCurrentVolume(volume);
        });
    }

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = ((sharedPreferences, key) -> {
        if (key.equals("ListPreference_VolumeKeysChange")) {
            updateVolumeProvider();
        }
    });

    private void onRingerModeChange() {
        if (controlledAudioStream != AudioManager.STREAM_RING) return;

        int ringerMode = myContext.audioManager.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            if (volumeProvider.getCurrentVolume() != 0) {
                myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_RING, 0, false);
                volumeProvider.setCurrentVolume(0);
            }
        } else if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            if (volumeProvider.getCurrentVolume() == 0) {
                myContext.volumeUtils.setVolumePercentage(AudioManager.STREAM_RING, 75, false);
                syncMediaSessionVolume();
            }
        }
    }

    private void updateVolumeProvider() {
        RecUtils.log("Update volume provider");
        controlledAudioStream = Utils.loadVolumeKeysAudioStream(myContext.sharedPreferences);
        int minVolume = myContext.volumeUtils.getMin(controlledAudioStream);
        int maxVolume = myContext.volumeUtils.getMax(controlledAudioStream);

        volumeProvider = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE,
                myContext.volumeUtils.getSteps(controlledAudioStream),
                myContext.volumeUtils.getVolume(controlledAudioStream)) {
            @Override
            public void onAdjustVolume(int direction) {
                handleVolumeKeyPress(direction);

                int volume = getCurrentVolume();
                if (direction == AudioManager.ADJUST_RAISE) volume += 1;
                else if (direction == AudioManager.ADJUST_LOWER) volume -= 1;
                if (volume > maxVolume) volume = maxVolume;
                else if (volume < minVolume) volume = minVolume;

                setCurrentVolume(volume);
                syncAudioStreamVolume();
            }
        };

        mediaSession.setPlaybackToRemote(volumeProvider);
    }

    private void syncAudioStreamVolume() {
        int volumePercentage = (int)Math.round((double)volumeProvider.getCurrentVolume() / volumeProvider.getMaxVolume() * 100);
        myContext.volumeUtils.setVolumePercentage(controlledAudioStream, volumePercentage, false);
    }

    private void syncMediaSessionVolume() {
        int volumePercentage = myContext.volumeUtils.getVolumePercentage(controlledAudioStream);
        int mediaSessionVolume = (int)Math.round((double)volumePercentage / 100 * volumeProvider.getMaxVolume());
        volumeProvider.setCurrentVolume(mediaSessionVolume);
    }

    private int prevDirection = 0;
    private AudioStreamState resetAudioStreamState = null;

    private void handleVolumeKeyPress(int direction) {
        if (direction != 0 && prevDirection == 0) {
            RecUtils.log("Media session caught press");
            resetAudioStreamState = new AudioStreamState(myContext.volumeUtils, controlledAudioStream);
            if (controlledAudioStream == AudioManager.STREAM_RING && myContext.audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) resetAudioStreamState.setRingerMuteFlag();
        }
        else if (direction != 0 && direction == prevDirection) {
            volumeKeyInputController.longPressDetected(direction > 0);
        }
        else if (direction == 0 && prevDirection != 0) {
            volumeKeyInputController.completePressDetected(prevDirection > 0, resetAudioStreamState);
        }

        prevDirection = direction;
    };
}