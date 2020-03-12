package com.masel.almightyvolumekeys;

import android.app.ActivityManager;
import android.content.Context;
import android.service.notification.NotificationListenerService;

import com.masel.rec_utils.AudioRecorder;
import com.masel.rec_utils.KeyValueStore;
import com.masel.rec_utils.RecUtils;

public class MonitorService extends NotificationListenerService {

    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private VolumeKeyCaptureUsingMediaSession volumeKeyCaptureUsingMediaSession;
    private VolumePollingKeyCapture volumePollingKeyCapture;

    @Override
    public void onListenerConnected() {
        Utils.runOnMainThread(() -> {
            RecUtils.log("Monitor service started");
            Utils.requestForeground(this);

            cleanUpAfterCrashDuringRecording();
            myContext = new MyContext(this);
            volumeKeyInputController = new VolumeKeyInputController(myContext);
            volumePollingKeyCapture = new VolumePollingKeyCapture(myContext, volumeKeyInputController);
            volumeKeyCaptureUsingMediaSession = new VolumeKeyCaptureUsingMediaSession(myContext, volumeKeyInputController);
            myContext.volumeUtils.setOnVolumeSet(volumePollingKeyCapture.getResetAction());

            PreventSleepOnScreenOff.init(myContext);
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

            volumeKeyCaptureUsingMediaSession.destroy();
            volumePollingKeyCapture.destroy();
            volumeKeyInputController.destroy();
            myContext.destroy();
        });
    }

    static boolean isEnabled(Context context) {
        return RecUtils.almightyVolumeKeysEnabled(context);
    }

    static boolean isAvailable(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.isLowRamDevice();
    }
}
