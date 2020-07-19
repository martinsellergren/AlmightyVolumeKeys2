package com.masel.almightyvolumekeys;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;

import androidx.preference.PreferenceManager;

import com.masel.rec_utils.RecUtils;

import java.util.ArrayList;
import java.util.List;

class AppLifecycle {

    private Context context;
    private PowerManager.WakeLock wakeLock;
    private DeviceState deviceState;

    AppLifecycle(Context context, DeviceState deviceState) {
        this.context = context;
        this.deviceState = deviceState;

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, RecUtils.isHuawei(context) ? "LocationManagerService" : "com.masel.almightyvolumekeys::WakeLock");
        }
        else {
            RecUtils.log("No power manager");
            return;
        }

        deviceState.addScreenOnCallback(this::evaluate);
        deviceState.addScreenOffCallback(this::evaluate);
        deviceState.addMediaStartCallback(this::evaluate);
        deviceState.addMediaStopCallback(this::evaluate);
        deviceState.addFlashlightOnCallback(this::evaluate);
        deviceState.addFlashlightOffCallback(this::evaluate);
        deviceState.addSoundRecStartCallback(this::evaluate);
        deviceState.addSoundRecStopCallback(this::evaluate);

        acquireWakeLock();
    }

    void destroy() {
        releaseWakeLock();
        evaluationHandler.removeCallbacksAndMessages(null);
        disableAppHandler.removeCallbacksAndMessages(null);
    }

    private void acquireWakeLock() {
        try {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(12 * 3600 * 1000);
                RecUtils.log("Wakelock acquired");
            }
        }
        catch (Exception e) {
            RecUtils.log("Failed to acquire wakelock");
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                RecUtils.log("Wakelock released");
            }
        }
        catch (Exception e) {
            RecUtils.log("Failed to release wakelock");
        }
    }

    private Handler evaluationHandler = new Handler();
    private static final long EVALUATION_DELAY = 1500;

    private void evaluate() {
        evaluationHandler.removeCallbacksAndMessages(null);
        evaluationHandler.postDelayed(this::evaluate_, EVALUATION_DELAY);
    }

    private Handler disableAppHandler = new Handler();

    private void evaluate_() {
        if (!isDeviceActive()) {
            long disableAppTime = Utils.loadDisableAppTime(PreferenceManager.getDefaultSharedPreferences(context));
            disableAppHandler.removeCallbacksAndMessages(null);
            disableAppHandler.postDelayed(this::disableApp, disableAppTime);
        }
        else {
            disableAppHandler.removeCallbacksAndMessages(null);
            enableApp();
        }
    }

    private List<Runnable> onEnableList = new ArrayList<>();
    private List<Runnable> onDisableList = new ArrayList<>();

    void addEnableAppCallback(Runnable onEnableApp) {
        onEnableList.add(onEnableApp);
    }

    void addDisableAppCallback(Runnable onDisableApp) {
        onDisableList.add(onDisableApp);
    }

    private boolean isEnabled = true;

    private void disableApp() {
        releaseWakeLock();

        if (isEnabled) {
            RecUtils.log("App disabled");
            isEnabled = false;
            for (Runnable onDisable : onDisableList) onDisable.run();
        }
    }

    private void enableApp() {
        acquireWakeLock();

        if (!isEnabled) {
            RecUtils.log("App enabled");
            isEnabled = true;
            for (Runnable onEnable : onEnableList) onEnable.run();
        }
    }

    private boolean isDeviceActive() {
        return deviceState.isScreenOn() ||
                deviceState.isMediaPlaying() ||
                deviceState.isFlashlightOn() ||
                deviceState.isSoundRecOn() ||
                deviceState.isCharging();
    }
}
