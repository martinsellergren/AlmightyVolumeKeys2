package com.masel.almightyvolumekeys;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.masel.rec_utils.AudioRecorder;
import com.masel.rec_utils.KeyValueStore;
import com.masel.rec_utils.RecUtils;

public class MonitorService extends NotificationListenerService {

    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private VolumeKeyCaptureUsingMediaSession volumeKeyCaptureUsingMediaSession;
    private VolumeKeyCaptureUsingPolling volumeKeyCaptureUsingPolling;

    @Override
    public void onListenerConnected() {
        Utils.runOnMainThread(() -> {
            RecUtils.log("Monitor service started");
            Utils.requestForeground(this);

            cleanUpAfterCrashDuringRecording();
            myContext = new MyContext(this);
            volumeKeyInputController = new VolumeKeyInputController(myContext);
            volumeKeyCaptureUsingMediaSession = new VolumeKeyCaptureUsingMediaSession(myContext, volumeKeyInputController);
            volumeKeyCaptureUsingPolling = new VolumeKeyCaptureUsingPolling(myContext, volumeKeyInputController);

            volumeKeyInputController.setAllowCurrentCommandToSetExtremeVolumeQuestions(
                    volumeKeyCaptureUsingPolling.isPrev3volumeOneStepFromMax(),
                    volumeKeyCaptureUsingPolling.isPrev3volumeOneStepFromMin());

            myContext.volumeUtils.addOnVolumeSetCallback((stream, volume) -> {
                if (stream == AudioManager.STREAM_MUSIC && volumeKeyCaptureUsingPolling.isActive()) volumeKeyCaptureUsingPolling.getResetAction().run();
            });

            DisableAppOnInactiveDevice.init(myContext);

            //setupOverlayWindow();
        });
    }

//    private void setupOverlayWindow() {
//        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//
//        View topLeftView = new View(this);
//        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
//        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, flags, PixelFormat.TRANSLUCENT);
//        topLeftParams.gravity = Gravity.LEFT | Gravity.TOP;
//        topLeftParams.x = 0;
//        topLeftParams.y = 0;
//        topLeftParams.width = 0;
//        topLeftParams.height = 0;
//        wm.addView(topLeftView, topLeftParams);
//    }

    private void cleanUpAfterCrashDuringRecording() {
        KeyValueStore.setAlmightyVolumeKeysIsRecording(this, false);
        AudioRecorder.removeNotification(this);
    }

    @Override
    public void onListenerDisconnected() {
        Utils.runOnMainThread(() -> {
            RecUtils.log("Monitor service stopped");

            try {
                volumeKeyCaptureUsingMediaSession.destroy();
                volumeKeyCaptureUsingPolling.destroy();
                volumeKeyInputController.destroy();
                myContext.destroy();
            }
            catch (Exception e) {}
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    static boolean isAvailable(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.isLowRamDevice();
    }

    // region Is enabled

    static boolean isEnabled(Context context) {
        return RecUtils.hasPermissionToSilenceDevice(context);
    }

    // endregion
}
