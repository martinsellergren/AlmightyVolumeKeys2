package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Handler;

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
    //private CameraManager cameraManager;
    private MediaSessionManager mediaSessionManager;

    private boolean isMediaPlaying;

    DeviceState(MyContext myContext) {
        this.myContext = myContext;
        //cameraManager = (CameraManager) myContext.context.getSystemService(Context.CAMERA_SERVICE);
        mediaSessionManager = (MediaSessionManager) myContext.context.getSystemService(Context.MEDIA_SESSION_SERVICE);

        //setupCameraStateCallbacks();
        setupMediaStateCallbacks();
        setupScreenStateCallbacks();
        setupDeviceUnlockedCallback();

        isMediaPlaying = isMediaPlaying();
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
        if (activeAudioStream == AudioManager.STREAM_MUSIC) return IDLE;
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

    // region Camera callbacks

//    private List<Runnable> onCameraActiveList = new ArrayList<>();
//    private List<Runnable> onCameraNotActiveList = new ArrayList<>();
//
//    void addCameraStateCallbacks(Runnable onCameraActive, Runnable onCameraNotActive) {
//        onCameraActiveList.add(onCameraActive);
//        onCameraNotActiveList.add(onCameraNotActive);
//    }
//
//    private void setupCameraStateCallbacks() {
//        cameraManager.registerAvailabilityCallback(cameraCallback, null);
//    }
//
//    private CameraManager.AvailabilityCallback cameraCallback = new CameraManager.AvailabilityCallback() {
//        @Override
//        public void onCameraAvailable(@NonNull String cameraId) {
//            super.onCameraAvailable(cameraId);
//            for (Runnable onCameraActive : onCameraActiveList) onCameraActive.run();
//        }
//
//        @Override
//        public void onCameraUnavailable(@NonNull String cameraId) {
//            super.onCameraUnavailable(cameraId);
//            for (Runnable onCameraNotActive : onCameraNotActiveList) onCameraNotActive.run();
//        }
//    };

    // endregion

    // region Media callbacks

    private List<Runnable> onMediaStartList = new ArrayList<>();
    private List<Runnable> onMediaStopList = new ArrayList<>();

    /**
     * Note: these callbacks might be called even when state not changed.
     */
    void addMediaStartCallback(Runnable onMediaStart) {
        onMediaStartList.add(onMediaStart);
    }

    void addMediaStopCallback(Runnable onMediaStop) {
        onMediaStopList.add(onMediaStop);
    }


    private long MEDIA_STATE_POLLING_RATE = 1000;
    private Handler mediaStatePollingHandler = new Handler();

    private void setupMediaStateCallbacks() {
        pollMediaState();

        //mediaSessionManager.addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, new ComponentName(myContext.context, MonitorService.class));
    }

    private void pollMediaState() {
        boolean isMediaCurrentlyPlaying = myContext.audioManager.isMusicActive();

        if (isMediaCurrentlyPlaying && !isMediaPlaying) {
            for (Runnable onMediaStart : onMediaStartList) onMediaStart.run();
        }
        else if (!isMediaCurrentlyPlaying && isMediaPlaying) {
            for (Runnable onMediaStop : onMediaStopList) onMediaStop.run();
        }

        isMediaPlaying = isMediaCurrentlyPlaying;

        mediaStatePollingHandler.removeCallbacksAndMessages(null);
        mediaStatePollingHandler.postDelayed(this::pollMediaState, MEDIA_STATE_POLLING_RATE);
    }

    private void stopPollingMediaState() {
        mediaStatePollingHandler.removeCallbacksAndMessages(null);
    }



//
//
//    private Map<MediaSession.Token, MediaControllerCallback> mediaSessionCallbacks = new HashMap<>();
//    private Map<MediaSession.Token, Boolean> mediaSessionIsPlayingStates = new HashMap<>();
//
//    private MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
//        @Override
//        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
//            executeMediaActions_withDelay();
//
//            if (controllers == null) return;
//            for (MediaController controller : controllers) {
//                if (controller.getPackageName().equals(myContext.context.getPackageName())) continue;
//                if (controller.getPlaybackState() == null) continue;
//                if (mediaSessionCallbacks.containsKey(controller.getSessionToken())) continue;
//
//                MediaControllerCallback callback = new MediaControllerCallback(controller.getSessionToken());
//                controller.registerCallback(callback);
//                mediaSessionCallbacks.put(controller.getSessionToken(), callback);
//                mediaSessionIsPlayingStates.put(controller.getSessionToken(), controller.getPlaybackState().getState() == PlaybackState.STATE_PLAYING);
//            }
//            executeMediaActions_withDelay();
//        }
//    };
//
//    private class MediaControllerCallback extends MediaController.Callback {
//        private MediaSession.Token token;
//
//        private MediaControllerCallback(MediaSession.Token token) {
//            this.token = token;
//        }
//
//        @Override
//        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
//            if (state == null) return;
//            mediaSessionIsPlayingStates.put(token, state.getState() == PlaybackState.STATE_PLAYING);
//            executeMediaActions_withDelay();
//        }
//
//        @Override
//        public void onSessionDestroyed() {
//            mediaSessionCallbacks.remove(token);
//            executeMediaActions_withDelay();
//        }
//    }
//
//    private static final int MEDIA_CHANGE_DELAY = 500;
//    private Handler mediaChangeDelayHandler = new Handler();
//
//    private void executeMediaActions_withDelay() {
//        mediaChangeDelayHandler.removeCallbacksAndMessages(null);
//        mediaChangeDelayHandler.postDelayed(this::executeMediaActions, MEDIA_CHANGE_DELAY);
//    }
//
//    private void executeMediaActions() {
//        if (isMediaPlaying()) {
//            for (Runnable onMediaStart : onMediaStartList) onMediaStart.run();
//            RecUtils.log("Execute media start actions");
//        }
//        else {
//            for (Runnable onMediaStop : onMediaStopList) onMediaStop.run();
//            RecUtils.log("Execute media stop actions");
//        }
//    }
//
//    boolean isMediaPlaying() {
//        for (Boolean isPlaying : mediaSessionIsPlayingStates.values()) {
//            if (isPlaying) return true;
//        }
//        return false;
//    }

//    private boolean isAnyonePlaying(List<MediaController> controllers) {
//        for (MediaController controller : controllers) {
//            if (controller.getPackageName().equals(myContext.context.getPackageName())) continue;
//
//            PlaybackState state = controller.getPlaybackState();
//            if (state != null && state.getState() == PlaybackState.STATE_PLAYING && state.getState() != PlaybackState.STATE_PAUSED) {
//                return true;
//            }
//        }
//
//        return false;
//    }

//    private List<MediaController> getMediaControllers() {
//        return mediaSessionManager.getActiveSessions(new ComponentName(myContext.context, MonitorService.class));
//    }

    // endregion

    // region Screen callbacks

    private List<Runnable> onScreenOffList = new ArrayList<>();
    private List<Runnable> onScreenOnList = new ArrayList<>();

    void addScreenOffCallback(Runnable onScreenOff) {
        onScreenOffList.add(onScreenOff);
    }
    void addScreenOnCallback(Runnable onScreenOn) {
        onScreenOnList.add(onScreenOn);
    }

    void removeScreenCallback(Runnable callback) {
        onScreenOffList.remove(callback);
        onScreenOnList.remove(callback);
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

    // region Device unlocked callback

    private List<Runnable> onDeviceUnlockedList = new ArrayList<>();

    void addDeviceUnlockedCallback(Runnable onDeviceUnlocked) {
        onDeviceUnlockedList.add(onDeviceUnlocked);
    }

    void removeDeviceUnlockedCallback(Runnable callback) {
        onDeviceUnlockedList.remove(callback);
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
        //cameraManager.unregisterAvailabilityCallback(cameraCallback);
        myContext.context.unregisterReceiver(screenOffReceiver);
        myContext.context.unregisterReceiver(screenOnReceiver);
        myContext.context.unregisterReceiver(deviceUnlockedReceiver);

        stopPollingMediaState();

//        mediaChangeDelayHandler.removeCallbacksAndMessages(null);
//        mediaSessionManager.removeOnActiveSessionsChangedListener(onActiveSessionsChangedListener);
//        for (MediaController mediaController : getMediaControllers()) {
//            MediaControllerCallback callback = mediaSessionCallbacks.get(mediaController.getSessionToken());
//            if (callback != null) mediaController.unregisterCallback(callback);
//        }
    }
}
