package com.masel.almightyvolumekeys;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.annotation.NonNull;

class MyFlashlight {
    private boolean isOn = false;
    private boolean isAvailable;

    private CameraManager cameraManager;
    private String cameraId;

    @TargetApi(23)
    private CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            isOn = enabled;
        }
    };

    MyFlashlight(Context context) {
        if (Build.VERSION.SDK_INT < 23 || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            isAvailable = false;
            return;
        }

        try {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            cameraManager.registerTorchCallback(torchCallback, null);
            cameraId = cameraManager.getCameraIdList()[0];
        }
        catch (CameraAccessException e) {
            isAvailable = false;
            return;
        }

        isAvailable = true;
    }

    private boolean set(boolean on) {
        if (!isAvailable || Build.VERSION.SDK_INT < 23) return false;

        try {
            cameraManager.setTorchMode(cameraId, on);
        }
        catch (CameraAccessException e) {
            return false;
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * @return True unless error.
     */
    boolean turnOn() {
        return set(true);
    }

    /**
     * @return True unless error.
     */
    boolean turnOff() {
        return set(false);
    }

    boolean isOn() {
        return isOn;
    }

    boolean isAvailable() {
        return isAvailable;
    }

    @TargetApi(23)
    void destroy() {
        if (isAvailable) {
            cameraManager.unregisterTorchCallback(torchCallback);
        }
    }
}
