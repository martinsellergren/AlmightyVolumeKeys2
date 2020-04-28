package com.masel.almightyvolumekeys;

import android.content.Context;
import android.content.SharedPreferences;
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

    private boolean isEnabled = true;

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

        deviceState.addScreenOffCallback(this::acquireWakeLock);
        deviceState.addScreenOnCallback(this::releaseWakeLock);

        deviceState.addScreenOnCallback(this::evaluate);
        deviceState.addScreenOffCallback(this::evaluate);
        deviceState.addMediaStartCallback(this::evaluate);
        deviceState.addMediaStopCallback(this::evaluate);
        deviceState.addFlashlightOnCallback(this::evaluate);
        deviceState.addFlashlightOffCallback(this::evaluate);
        deviceState.addSoundRecStartCallback(this::evaluate);
        deviceState.addSoundRecStopCallback(this::evaluate);
    }

    void destroy() {
        releaseWakeLock();
    }

    private void acquireWakeLock() {
        try {
            if (!wakeLock.isHeld()) {
                RecUtils.log("Wakelock acquired");
                wakeLock.acquire(12 * 60 * 60 * 1000);
            }
        }
        catch (Exception e) {
            RecUtils.log("Failed to acquire wakelock");
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock.isHeld()) {
                RecUtils.log("Wakelock released");
                wakeLock.release();
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

    private void evaluate_() {
        boolean shouldBeEnabled = shouldBeEnabled();

        if (isEnabled && !shouldBeEnabled) {
            onDisable();
        }
        else if (!isEnabled && shouldBeEnabled) {
            onEnable();
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

    private void onDisable() {
        isEnabled = false;
        for (Runnable onDisable : onDisableList) onDisable.run();
        releaseWakeLock();
        RecUtils.log("App disabled");
    }

    private void onEnable() {
        isEnabled = true;
        if (!deviceState.isScreenOn()) acquireWakeLock();
        for (Runnable onEnable : onEnableList) onEnable.run();
        RecUtils.log("App enabled");
    }

    private long previousActivityTime = System.currentTimeMillis();

    private boolean shouldBeEnabled() {
        if (deviceState.isScreenOn() ||
                deviceState.isMediaPlaying() ||
                deviceState.isFlashlightOn() ||
                deviceState.isSoundRecOn()) {
            previousActivityTime = System.currentTimeMillis();
            return true;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return System.currentTimeMillis() < previousActivityTime + Utils.loadDisableAppTime(sharedPreferences);
    }
}
