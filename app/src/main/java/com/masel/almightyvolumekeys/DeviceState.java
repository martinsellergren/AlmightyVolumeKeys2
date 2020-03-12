package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masel.rec_utils.RecUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines device states. States are mutually exclusive.
 */
class DeviceState {
    static final int IDLE = 0;
    static final int MUSIC = 1;
    static final int SOUNDREC = 2;
    static final int OTHER = 3;

    private MyContext myContext;
    private CameraManager cameraManager;
    private MediaSessionManager mediaSessionManager;

    private boolean isMediaPlaying;

    DeviceState(MyContext myContext) {
        this.myContext = myContext;
        cameraManager = (CameraManager) myContext.context.getSystemService(Context.CAMERA_SERVICE);
        mediaSessionManager = (MediaSessionManager) myContext.context.getSystemService(Context.MEDIA_SESSION_SERVICE);

        setupCameraStateCallbacks();
        setupMediaStateCallbacks();
        setupScreenStateCallbacks();
        setupDeviceUnlockedCallback();

        isMediaPlaying = isAnyonePlaying();
    }

    /**
     * @return Current device state. If API<26, may return IDLE when should return OTHER (e.g when timer sounding).
     */
    int getCurrent() {
        AudioManager manager = myContext.audioManager;
        int activeAudioStream = RecUtils.getActiveAudioStream(manager);

        if (isMediaPlaying()) return MUSIC;
        if (myContext.audioRecorder.isRecording()) return SOUNDREC;
        if (manager.getMode() == AudioManager.MODE_RINGTONE) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_CALL) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) return OTHER;
        if (activeAudioStream != AudioManager.USE_DEFAULT_STREAM_TYPE) return OTHER;
        else return IDLE;
    }

    static String str(int state) {
        switch (state) {
            case IDLE: return "IDLE";
            case MUSIC: return "MUSIC";
            case SOUNDREC: return "SOUNDREC";
            default: return "OTHER";
        }
    }

    // region Get more states

    boolean isMediaPlaying() {
        return isMediaPlaying;
    }

    boolean isScreenOn() {
        return myContext.powerManager.isInteractive();
    }

    boolean isDeviceUnlocked() {
        return !myContext.keyguardManager.isKeyguardLocked();
    }

    // endregion

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
        mediaSessionManager.addOnActiveSessionsChangedListener(controllers -> {
            if (controllers == null) return;

            for (MediaController controller : controllers) {
                controller.unregisterCallback(mediaControllerCallback);
                controller.registerCallback(mediaControllerCallback);
            }

        }, new ComponentName(myContext.context, MonitorService.class));
    }

    private static final int MEDIA_CHANGE_DELAY = 500;
    private Handler mediaChangeDelayHandler = new Handler();

    private MediaController.Callback mediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            mediaChangeDelayHandler.removeCallbacksAndMessages(null);
            mediaChangeDelayHandler.postDelayed(() -> executeMediaActionIfStateChange(), MEDIA_CHANGE_DELAY);
        }

        @Override
        public void onSessionDestroyed() {
            mediaChangeDelayHandler.removeCallbacksAndMessages(null);
            mediaChangeDelayHandler.postDelayed(() -> executeMediaActionIfStateChange(), MEDIA_CHANGE_DELAY);
        }
    };

    private void executeMediaActionIfStateChange() {
        boolean someonePlaying = isAnyonePlaying();
        if (someonePlaying && !isMediaPlaying) {
            isMediaPlaying = true;
            for (Runnable onMediaStart : onMediaStartList) onMediaStart.run();
        }
        else if (!someonePlaying && isMediaPlaying) {
            isMediaPlaying = false;
            for (Runnable onMediaStop : onMediaStopList) onMediaStop.run();
        }
    }

    private boolean isAnyonePlaying() {
        List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(myContext.context, MonitorService.class));

        for (MediaController controller : controllers) {
            if (controller.getPackageName().equals(myContext.context.getPackageName())) continue;

            PlaybackState state = controller.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING && state.getState() != PlaybackState.STATE_PAUSED) {
                return true;
            }
        }

        return false;
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
        myContext.context.registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        myContext.context.registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
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
        myContext.context.registerReceiver(deviceUnlockedReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
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
        mediaChangeDelayHandler.removeCallbacksAndMessages(null);
        myContext.context.unregisterReceiver(screenOffReceiver);
        myContext.context.unregisterReceiver(screenOnReceiver);
        myContext.context.unregisterReceiver(deviceUnlockedReceiver);
    }
}
