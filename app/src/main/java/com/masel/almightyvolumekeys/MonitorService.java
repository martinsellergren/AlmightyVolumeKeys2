package com.masel.almightyvolumekeys;

import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.service.notification.NotificationListenerService;

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
        });
    }

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
