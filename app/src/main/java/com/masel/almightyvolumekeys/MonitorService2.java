package com.masel.almightyvolumekeys;

import android.app.ActivityManager;
import android.content.Context;
import android.service.notification.NotificationListenerService;

import com.masel.rec_utils.AudioRecorder;
import com.masel.rec_utils.KeyValueStore;
import com.masel.rec_utils.RecUtils;

public class MonitorService2 extends NotificationListenerService {

    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private VolumeKeyCapture volumeKeyCapture;
    private VolumeKeyCaptureWhenMusic volumeKeyCaptureWhenMusic;
    private PreventSleepOnScreenOff preventSleepOnScreenOff;

    @Override
    public void onListenerConnected() {
        RecUtils.log("Monitor service started");
        Utils.requestForeground(this);

        cleanUpAfterCrashDuringRecording();
        myContext = new MyContext(this);
        volumeKeyInputController = new VolumeKeyInputController(myContext);
        volumeKeyCapture = new VolumeKeyCapture(myContext, volumeKeyInputController);
        volumeKeyCaptureWhenMusic = new VolumeKeyCaptureWhenMusic(myContext, volumeKeyInputController);
        preventSleepOnScreenOff = new PreventSleepOnScreenOff(myContext);

        volumeKeyInputController.setResetActionForVolumeKeyCaptureWhenMusic(volumeKeyCaptureWhenMusic.getResetAction());
        volumeKeyCapture.setResetVolumeKeyCaptureWhenMusicAndScreenOff(volumeKeyCaptureWhenMusic.getResetAction());
    }

    private void cleanUpAfterCrashDuringRecording() {
        KeyValueStore.setAlmightyVolumeKeysIsRecording(this, false);
        AudioRecorder.removeNotification(this);
    }


    @Override
    public void onListenerDisconnected() {
        RecUtils.log("Monitor service stopped");

        volumeKeyCapture.destroy();
        volumeKeyCaptureWhenMusic.destroy();
        volumeKeyInputController.destroy();
        preventSleepOnScreenOff.destroy();
        myContext.destroy();
    }

    static boolean isEnabled(Context context) {
        return RecUtils.almightyVolumeKeysEnabled(context);
    }

    static boolean isAvailable(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.isLowRamDevice();
    }
}
