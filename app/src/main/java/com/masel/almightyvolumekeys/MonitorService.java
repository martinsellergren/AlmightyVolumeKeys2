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
    private VolumeKeyCaptureUsingPolling volumeKeyCaptureUsingPolling;

    private static boolean isEnabled = false;

    @Override
    public void onListenerConnected() {
        Utils.runOnMainThread(() -> {
            RecUtils.log("Monitor service started");
            isEnabled = true;
            Utils.requestForeground(this);

            cleanUpAfterCrashDuringRecording();
            myContext = new MyContext(this);
            volumeKeyInputController = new VolumeKeyInputController(myContext);
            volumeKeyCaptureUsingMediaSession = new VolumeKeyCaptureUsingMediaSession(myContext, volumeKeyInputController);
            volumeKeyCaptureUsingPolling = new VolumeKeyCaptureUsingPolling(myContext, volumeKeyInputController);

            myContext.volumeUtils.setOnVolumeSet(volumeKeyCaptureUsingPolling.getResetAction());

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
            isEnabled = false;

            volumeKeyCaptureUsingMediaSession.destroy();
            volumeKeyCaptureUsingPolling.destroy();
            volumeKeyInputController.destroy();
            myContext.destroy();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isEnabled = false;
    }

    static boolean isEnabled() {
        return isEnabled;
    }

    static boolean isAvailable(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.isLowRamDevice();
    }
}
