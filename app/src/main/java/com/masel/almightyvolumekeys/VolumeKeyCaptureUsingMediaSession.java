package com.masel.almightyvolumekeys;

import android.content.ComponentName;
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

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        setupControlledAudioStream();

        myContext.deviceState.addMediaStartCallback(this::enableOrDisable);
        myContext.deviceState.addMediaStopCallback(this::enableOrDisable);
//        myContext.deviceState.addOnAllowSleepCallback(() -> new Handler().postDelayed(() -> mediaSession.setActive(false), 1000));
//        myContext.deviceState.addScreenOnCallback(this::enableOrDisable);
        myContext.deviceState.appLifecycle.addDisableAppCallback(() -> mediaSession.setActive(false));
        myContext.deviceState.appLifecycle.addEnableAppCallback(this::enableOrDisable);
        myContext.deviceState.addOnSystemSettingsChangeCallback(this::enableOrDisable);
        myContext.deviceState.addOnSecureSettingsChangeCallback(this::enableOrDisable);
        myContext.deviceState.addCameraStateCallbacks(this::enableOrDisable, this::enableOrDisable);
        myContext.deviceState.addOnRingerModeChangeCallback(this::fineTuningsIfControllingRingerVolume);

        enableOrDisable();
    }

    private void enableOrDisable() {
        boolean active = deviceStateOkForCapture();
        if (active != mediaSession.isActive()) {
            RecUtils.log("Volume key capture: media session: " + active);
            syncMediaSessionVolume();
            mediaSession.setActive(active);
        }
    }

    private boolean deviceStateOkForCapture() {
        int state = myContext.deviceState.getCurrent();
        return (state == DeviceState.IDLE || state == DeviceState.SOUNDREC) && !myContext.deviceState.isCameraActive();
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

    private void fineTuningsIfControllingRingerVolume() {
        if (!mediaSession.isActive() || controlledAudioStream != AudioManager.STREAM_RING) return;

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

        volumeProvider = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE,
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
                boolean res = syncAudioStreamVolume();
                if (!res) syncMediaSessionVolume();
            }

            @Override
            public void onSetVolumeTo(int volume) {
                setCurrentVolume(volume);
                syncAudioStreamVolume();
            }
        };

        mediaSession.setPlaybackToRemote(volumeProvider);
    }

    private boolean syncAudioStreamVolume() {
        int volumePercentage = (int)Math.round((double)volumeProvider.getCurrentVolume() / volumeProvider.getMaxVolume() * 100);
        myContext.volumeUtils.setVolumePercentage(controlledAudioStream, volumePercentage, false);
        return myContext.volumeUtils.getVolumePercentage(controlledAudioStream) == volumePercentage;
    }

    private void syncMediaSessionVolume() {
        if (!mediaSession.isActive()) return;
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