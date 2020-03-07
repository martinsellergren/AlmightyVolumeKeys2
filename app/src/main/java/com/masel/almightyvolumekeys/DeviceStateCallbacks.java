package com.masel.almightyvolumekeys;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
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

    private boolean mediaIsPlaying;

    DeviceStateCallbacks(Context context) {
        this.context = context;
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mediaIsPlaying = audioManager.isMusicActive();

        setupCameraStateCallbacks();
        setupMediaStateCallbacks();
        setupScreenStateCallbacks();
        setupDeviceUnlockedCallback();
    }

    // region Camera

    private List<Runnable> onCameraActiveList = new ArrayList<>();
    private List<Runnable> onCameraNotActiveList = new ArrayList<>();

    void addCameraStateCallbacks(Runnable onCameraActive, Runnable onCameraNotActive) {
        onCameraActiveList.add(onCameraActive);
        onCameraNotActiveList.add(onCameraNotActive);
    }

    private void setupCameraStateCallbacks() {
        cameraManager.registerAvailabilityCallback(cameraCallback, null);
    }

    private CameraManager.AvailabilityCallback cameraCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            for (Runnable onCameraActive : onCameraActiveList) onCameraActive.run();
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);
            for (Runnable onCameraNotActive : onCameraNotActiveList) onCameraNotActive.run();
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

    private void setupMediaStateCallbacks() {
        pollMediaState();
    }

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
            for (Runnable onScreenOff : onScreenOffList) onScreenOff.run();
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (Runnable onScreenOn : onScreenOnList) onScreenOn.run();
        }
    };

    // endregion

    // region Device unlocked

    private List<Runnable> onDeviceUnlockedList = new ArrayList<>();

    void addDeviceUnlockedCallback(Runnable onDeviceUnlocked) {
        onDeviceUnlockedList.add(onDeviceUnlocked);
    }

    private void setupDeviceUnlockedCallback() {
        context.registerReceiver(deviceUnlockedReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
    }

    private BroadcastReceiver deviceUnlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (Runnable onDeviceUnlocked : onDeviceUnlockedList) onDeviceUnlocked.run();
        }
    };

    // endregion

    void destroy() {
        cameraManager.unregisterAvailabilityCallback(cameraCallback);
        pollMediaStateHandler.removeCallbacksAndMessages(null);
        context.unregisterReceiver(screenOffReceiver);
        context.unregisterReceiver(screenOnReceiver);
        context.unregisterReceiver(deviceUnlockedReceiver);
    }
}
