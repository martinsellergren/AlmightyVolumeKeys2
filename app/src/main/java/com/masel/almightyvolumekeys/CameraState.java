package com.masel.almightyvolumekeys;

import android.content.Context;
import android.hardware.camera2.CameraManager;

import androidx.annotation.NonNull;

class CameraState {

    private CameraManager cameraManager;
    private boolean isCameraActive;
    private Runnable onCameraActive;
    private Runnable onCameraNotActive;

    private CameraManager.AvailabilityCallback callback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            isCameraActive = false;
            if (onCameraNotActive != null) onCameraNotActive.run();
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);
            isCameraActive = true;
            if (onCameraActive != null) onCameraActive.run();
        }
    };

    CameraState(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraManager.registerAvailabilityCallback(callback, null);
    }

    void setCallbacks(Runnable onCameraActive, Runnable onCameraNotActive) {
        this.onCameraActive = onCameraActive;
        this.onCameraNotActive = onCameraNotActive;
    }

    void destroy() {
        cameraManager.unregisterAvailabilityCallback(callback);
    }

    boolean isCameraActive() {
        return isCameraActive;
    }
}
