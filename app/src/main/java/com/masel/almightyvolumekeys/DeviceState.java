package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;

import com.masel.rec_utils.RecUtils;

import java.util.ArrayList;
import java.util.Calendar;
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

    private boolean isMediaPlaying;

    DeviceState(MyContext myContext) {
        this.myContext = myContext;

        //cameraManager = (CameraManager) myContext.context.getSystemService(Context.CAMERA_SERVICE);
        isMediaPlaying = myContext.audioManager.isMusicActive();

        //setupCameraStateCallbacks();
        setupMediaStateCallbacks();
        setupScreenStateCallbacks();
        setupDeviceUnlockedCallbacks();
        setupOnAllowSleepCallbacks();
        setupOnSettingsChangeCallbacks();
        setupOnRingerModeChangeCallbacks();
    }

    /**
     * @return Current device state. If API<26, may return IDLE when should return OTHER (e.g when timer sounding).
     */
    int getCurrent() {
        if (isMediaPlaying()) return MUSIC;
        if (myContext.audioRecorder.isRecording()) return SOUNDREC;

        AudioManager manager = myContext.audioManager;
        if (manager.getMode() == AudioManager.MODE_RINGTONE) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_CALL) return OTHER;
        if (manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) return OTHER;

        int activeAudioStream = RecUtils.getActiveAudioStream(manager);
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

    // region More states

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

    void addMediaStartCallback(Runnable onMediaStart) {
        onMediaStartList.add(onMediaStart);

        if (isMediaPlaying()) onMediaStart.run();
    }

    void addMediaStopCallback(Runnable onMediaStop) {
        onMediaStopList.add(onMediaStop);

        if (!isMediaPlaying()) onMediaStop.run();
    }

    private void setupMediaStateCallbacks() {
        if (Build.VERSION.SDK_INT >= 26) {
            setupMediaStateCallbacks_method1();
        }
        else {
            setupMediaStateCallbacks_method2();
        }
    }

    private static final long EVALUATE_MEDIA_STATE_DELAY = 1000;
    private Handler evaluateMediaStateDelayHandler = new Handler();
    private AudioManager.AudioPlaybackCallback audioPlaybackCallback = null;

    @RequiresApi(api = 26)
    private void setupMediaStateCallbacks_method1() {
        audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                evaluateMediaStateDelayHandler.removeCallbacksAndMessages(null);
                evaluateMediaStateDelayHandler.postDelayed(DeviceState.this::executeMediaCallbacksIfStateChange, EVALUATE_MEDIA_STATE_DELAY);
            }
        };
        myContext.audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null);
    }

    private void setupMediaStateCallbacks_method2() {
        addOnAllowSleepCallback(this::stopPollingMediaState);
        addScreenOnCallback(this::startPollingMediaState);
        startPollingMediaState();
    }

    private long MEDIA_STATE_POLLING_RATE = 1000;
    private Handler mediaStatePollingHandler = new Handler();
    private boolean isPollingMediaState = false;

    private void startPollingMediaState() {
        if (!isPollingMediaState) {
            RecUtils.log("Start polling media state");
            isPollingMediaState = true;
            isMediaPlaying = myContext.audioManager.isMusicActive();
            pollMediaState();
        }
    }

    private void stopPollingMediaState() {
        RecUtils.log("Stop polling media state");
        isPollingMediaState = false;
        mediaStatePollingHandler.removeCallbacksAndMessages(null);
    }

    private void pollMediaState() {
        executeMediaCallbacksIfStateChange();
        mediaStatePollingHandler.removeCallbacksAndMessages(null);
        mediaStatePollingHandler.postDelayed(this::pollMediaState, MEDIA_STATE_POLLING_RATE);
    }

    private void executeMediaCallbacksIfStateChange() {
        boolean isMediaCurrentlyPlaying = myContext.audioManager.isMusicActive();

        if (isMediaCurrentlyPlaying && !isMediaPlaying) {
            RecUtils.log("Media state change: on");
            isMediaPlaying = true;
            for (Runnable onMediaStart : onMediaStartList) onMediaStart.run();
        }
        else if (!isMediaCurrentlyPlaying && isMediaPlaying) {
            RecUtils.log("Media state change: off");
            isMediaPlaying = false;
            for (Runnable onMediaStop : onMediaStopList) onMediaStop.run();
        }
    }

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

    private BroadcastReceiver screenOffReceiver = null;
    private BroadcastReceiver screenOnReceiver = null;

    private void setupScreenStateCallbacks() {
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (Runnable onScreenOff : onScreenOffList) onScreenOff.run();
            }
        };

        screenOnReceiver= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (Runnable onScreenOn : onScreenOnList) onScreenOn.run();
            }
        };

        myContext.context.registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        myContext.context.registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    // endregion

    // region Device unlocked callback

    private List<Runnable> onDeviceUnlockedList = new ArrayList<>();

    void addDeviceUnlockedCallback(Runnable onDeviceUnlocked) {
        onDeviceUnlockedList.add(onDeviceUnlocked);
    }

    void removeDeviceUnlockedCallback(Runnable callback) {
        onDeviceUnlockedList.remove(callback);
    }

    private BroadcastReceiver deviceUnlockedReceiver = null;

    private void setupDeviceUnlockedCallbacks() {
        deviceUnlockedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (Runnable onDeviceUnlocked : onDeviceUnlockedList) onDeviceUnlocked.run();
            }
        };

        myContext.context.registerReceiver(deviceUnlockedReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
    }


    // endregion

    // region Allow sleep callback

    private List<Runnable> onAllowSleepList = new ArrayList<>();

    /**
     * Executed 'user defined delay' after screen AND music off.
     */
    void addOnAllowSleepCallback(Runnable onAllowSleep) {
        onAllowSleepList.add(onAllowSleep);
    }

    private void setupOnAllowSleepCallbacks() {
        addScreenOffCallback(() -> {
            if (!isMediaPlaying()) allowSleepAfterDelay();
        });

        addMediaStopCallback(() -> {
            if (!isScreenOn()) allowSleepAfterDelay();
        });

        addScreenOnCallback(() -> onAllowSleepHandler.removeCallbacksAndMessages(null));
        addMediaStartCallback(() -> onAllowSleepHandler.removeCallbacksAndMessages(null));
    }

    private Handler onAllowSleepHandler = new Handler();

    private void allowSleepAfterDelay() {
        int disableAppMinutes = myContext.sharedPreferences.getInt("SeekBarPreference_disableAppTimeout", 60);
//        boolean allowSleepSwitch = myContext.sharedPreferences.getBoolean("SwitchPreferenceCompat_allowSleep", false);
//        int allowSleepStartHour = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepStart", 0);
//        int allowSleepEndHour = myContext.sharedPreferences.getInt("SeekBarPreference_allowSleepEnd", 0);
//
//        boolean allowSleep = allowSleepSwitch && currentlyAllowSleep(allowSleepStartHour, allowSleepEndHour);
//        if (disableAppMinutes == 0 || allowSleep) disableAppMinutes = 1;
        if (disableAppMinutes == 0) disableAppMinutes = 1;
        long timeout = disableAppMinutes * 60000;

        onAllowSleepHandler.removeCallbacksAndMessages(null);
        onAllowSleepHandler.postDelayed(() -> {
            for (Runnable onAllowSleep : onAllowSleepList) onAllowSleep.run();

        }, timeout);
    }

    private static boolean currentlyAllowSleep(int allowStartHour, int allowStopHour) {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hourInterval(allowStartHour, allowStopHour) > hourInterval(allowStartHour, currentHour);
    }

    private static int hourInterval(int from, int to) {
        return from <= to ? to - from : (to + 24) - from;
    }

    // endregion

    // region Settings change callbacks

    private List<Runnable> onSettingsChangeList = new ArrayList<>();

    void addOnSettingsChangeCallback(Runnable onSettingsChange) {
        onSettingsChangeList.add(onSettingsChange);
    }

    void removeOnSettingsChangeCallback(Runnable onSettingsChange) {
        onSettingsChangeList.remove(onSettingsChange);
    }

    private ContentObserver settingsObserver = null;

    private void setupOnSettingsChangeCallbacks() {
        settingsObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);

                //RecUtils.log("Settings change");
                for (Runnable onSettingsChange : onSettingsChangeList) onSettingsChange.run();
            }
        };

        myContext.context.getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                settingsObserver);
    }

    // endregion

    // region Ringer mode change

    private List<Runnable> onRingerModeChangeList = new ArrayList<>();

    void addOnRingerModeChangeCallback(Runnable onRingerModeChange) {
        onRingerModeChangeList.add(onRingerModeChange);
    }

    void removeOnRingerModeChangeCallback(Runnable onRingerModeChange) {
        onRingerModeChangeList.remove(onRingerModeChange);
    }

    private BroadcastReceiver onRingerModeChangedReceiver = null;

    private void setupOnRingerModeChangeCallbacks() {
        onRingerModeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                RecUtils.log("DeviceState: Ringer mode changed");
                for (Runnable onRingerModeChange : onRingerModeChangeList) onRingerModeChange.run();
            }
        };

        myContext.context.registerReceiver(onRingerModeChangedReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
    }

    // endregion

    void destroy() {
        //cameraManager.unregisterAvailabilityCallback(cameraCallback);

        stopPollingMediaState();
        if (Build.VERSION.SDK_INT >= 26 && audioPlaybackCallback != null) myContext.audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
        if (screenOffReceiver != null) myContext.context.unregisterReceiver(screenOffReceiver);
        if (screenOnReceiver != null) myContext.context.unregisterReceiver(screenOnReceiver);
        if (deviceUnlockedReceiver != null) myContext.context.unregisterReceiver(deviceUnlockedReceiver);
        if (settingsObserver != null) myContext.context.getContentResolver().unregisterContentObserver(settingsObserver);
        if (onRingerModeChangedReceiver != null) myContext.context.unregisterReceiver(onRingerModeChangedReceiver);

        evaluateMediaStateDelayHandler.removeCallbacksAndMessages(null);
        mediaStatePollingHandler.removeCallbacksAndMessages(null);
        onAllowSleepHandler.removeCallbacksAndMessages(null);
    }
}
