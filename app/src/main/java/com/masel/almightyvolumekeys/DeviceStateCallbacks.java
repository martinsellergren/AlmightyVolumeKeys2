package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.masel.rec_utils.RecUtils;

import java.util.ArrayList;
import java.util.List;

class DeviceStateCallbacks {

    private Context context;
    private CameraManager cameraManager;
    private AudioManager audioManager;
    private boolean cameraIsActive;
    private boolean mediaIsPlaying;
    private boolean screenIsOn;

    DeviceStateCallbacks(Context context) {
        this.context = context;
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraIsActive = false;
        setupCameraStateCallbacks();

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mediaIsPlaying = audioManager.isMusicActive();
        setupMediaStateCallbacks();

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        screenIsOn = RecUtils.isScreenOn(powerManager);
        setupScreenStateCallbacks();
    }

    // region Camera

    private Runnable onCameraActive;
    private Runnable onCameraNotActive;

    void setCameraStateCallbacks(Runnable onCameraActive, Runnable onCameraNotActive) {
        this.onCameraActive = onCameraActive;
        this.onCameraNotActive = onCameraNotActive;
    }

    private void setupCameraStateCallbacks() {
        cameraManager.registerAvailabilityCallback(cameraCallback, null);
    }

    private CameraManager.AvailabilityCallback cameraCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            if (cameraIsActive) {
                cameraIsActive = false;
                if (onCameraNotActive != null) onCameraNotActive.run();
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);

            if (!cameraIsActive) {
                cameraIsActive = true;
                if (onCameraActive != null) onCameraActive.run();
            }
        }
    };

    // endregion

    // region Media

    private List<Runnable> onMediaStartList = new ArrayList<>();
    private List<Runnable> onMediaStopList = new ArrayList<>();

    void addMediaStartCallback(Runnable onMediaStart) {
        onMediaStartList.add(onMediaStart);
    }

    void addMediaStopCallback(Runnable onMediaStop) {
        onMediaStopList.add(onMediaStop);
    }

    void removeMediaCallbacks(Runnable onMediaStart, Runnable onMediaStop) {
        onMediaStartList.remove(onMediaStart);
        onMediaStopList.remove(onMediaStop);
    }

    private void setupMediaStateCallbacks() {
//        if (false && Build.VERSION.SDK_INT >= 26) {
//            audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null);
//        }
//        else {
        pollMediaState();
        //}
    }

//    @RequiresApi(api = 26)
//    private AudioManager.AudioPlaybackCallback audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
//        @Override
//        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
//            updateMediaState();
//        }
//    };

    private static final int MEDIA_STATE_POLLING_DELTA = 350;
    private Handler pollMediaStateHandler = new Handler();

    private void pollMediaState() {
        if (!mediaIsPlaying && audioManager.isMusicActive()) {
            mediaIsPlaying = true;
            for (Runnable onMediaStart : onMediaStartList) onMediaStart.run();
        }
        else if (mediaIsPlaying && !audioManager.isMusicActive()) {
            mediaIsPlaying = false;
            for (Runnable onMediaStop : onMediaStopList) onMediaStop.run();
        }

        pollMediaStateHandler.removeCallbacksAndMessages(null);
        pollMediaStateHandler.postDelayed(this::pollMediaState, MEDIA_STATE_POLLING_DELTA);
    }

    // endregion

    // region Screen

    private List<Runnable> onScreenOffList = new ArrayList<>();
    private List<Runnable> onScreenOnList = new ArrayList<>();

    void addScreenOffCallback(Runnable onScreenOff) {
        onScreenOffList.add(onScreenOff);
    }
    void addScreenOnCallback(Runnable onScreenOn) {
        onScreenOnList.add(onScreenOn);
    }

    private void setupScreenStateCallbacks() {
        context.registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        context.registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (screenIsOn) {
                screenIsOn = false;
                for (Runnable onScreenOff : onScreenOffList) onScreenOff.run();
            }
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!screenIsOn) {
                screenIsOn = true;
                for (Runnable onScreenOn : onScreenOnList) onScreenOn.run();
            }
        }
    };

    // endregion

    void destroy() {
        cameraManager.unregisterAvailabilityCallback(cameraCallback);
        pollMediaStateHandler.removeCallbacksAndMessages(null);
        //if (Build.VERSION.SDK_INT >= 26) audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);

        context.unregisterReceiver(screenOffReceiver);
        context.unregisterReceiver(screenOnReceiver);
    }
}
