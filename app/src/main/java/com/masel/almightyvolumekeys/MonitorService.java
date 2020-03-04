package com.masel.almightyvolumekeys;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;

import com.masel.rec_utils.AudioRecorder;
import com.masel.rec_utils.KeyValueStore;
import com.masel.rec_utils.RecUtils;

public class MonitorService extends AccessibilityService {

    // region Required for AccessibilityService

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        RecUtils.log("listener service onAccessibilityEvent()");
    }

    @Override
    public void onInterrupt() {
        RecUtils.log("listener service onInterrupt()");
    }

    // endregion

    private MyContext myContext;
    private VolumeKeyInputController volumeKeyInputController;
    private VolumeKeyCaptureWhenScreenOffAndMusic volumeKeyCaptureWhenScreenOffAndMusic;
    private VolumeKeyCaptureWhenScreenOff volumeKeyCaptureWhenScreenOff;
    private PreventSleepOnScreenOff preventSleepOnScreenOff;

    @Override
    protected void onServiceConnected() {
        RecUtils.log("onServiceConnected()");
        if (Build.VERSION.SDK_INT >= 26) requestForeground();
        cleanUpAfterCrashDuringRecording();

        myContext = new MyContext(this);

        volumeKeyInputController = new VolumeKeyInputController(myContext);
        volumeKeyCaptureWhenScreenOff = new VolumeKeyCaptureWhenScreenOff(myContext, volumeKeyInputController);
        //volumeKeyCaptureWhenScreenOffAndMusic = new VolumeKeyCaptureWhenScreenOffAndMusic(myContext, volumeKeyInputController);

//        volumeKeyInputController.setResetActionForVolumeKeyCaptureWhenScreenOffAndMusic(volumeKeyCaptureWhenScreenOffAndMusic.getResetAction());
//        volumeKeyCaptureWhenScreenOff.setResetActionForVolumeKeyCaptureWhenScreenOffAndMusic(volumeKeyCaptureWhenScreenOffAndMusic.getResetAction());

        preventSleepOnScreenOff = new PreventSleepOnScreenOff(myContext);
    }

    private void cleanUpAfterCrashDuringRecording() {
        KeyValueStore.setAlmightyVolumeKeysIsRecording(this, false);
        AudioRecorder.removeNotification(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        RecUtils.log("onUnbind()");
        volumeKeyCaptureWhenScreenOff.destroy();
        volumeKeyCaptureWhenScreenOffAndMusic.destroy(myContext);
        volumeKeyInputController.destroy();
        preventSleepOnScreenOff.destroy();
        myContext.destroy();

        return super.onUnbind(intent);
    }


    private AudioStreamState resetAudioStreamState;

    /**
     * Fired only when screen is on. Consumes volume key presses and pass them along for processing.
     * When camera active, pass volume press through (take photo with volume keys is default on many devices).
     * Other events pass through.
     * @param event
     * @return True to consume event
     */
//    @Override
//    protected boolean onKeyEvent(KeyEvent event) {
//        myContext.accessibilityServiceFailing = false;
//
//        if (event.getKeyCode() != KeyEvent.KEYCODE_VOLUME_UP && event.getKeyCode() != KeyEvent.KEYCODE_VOLUME_DOWN) {
//            return super.onKeyEvent(event);
//        }
//        if (Utils.loadDefaultVolumeKeyActionWhenCameraActive(myContext) && myContext.cameraState.isCameraActive()) {
//            return super.onKeyEvent(event);
//        }
//
//        boolean volumeUp = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;
//        int relevantAudioStream = Utils.getRelevantAudioStream(myContext);
//
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            RecUtils.log("Accessibility service caught press");
//            volumeKeyInputController.keyDown(() -> myContext.volumeMovement.start(relevantAudioStream, volumeUp, true));
//            resetAudioStreamState = new AudioStreamState(myContext.audioManager, relevantAudioStream);
//        }
//        else {
//            volumeKeyInputController.keyUp(volumeUp, resetAudioStreamState);
//            myContext.volumeMovement.stop();
//            volumeKeyInputController.adjustVolumeIfAppropriate(relevantAudioStream, volumeUp, true);
//
//            //volumeKeyCaptureWhenScreenOff.reset();
//        }
//
//        return true;
//    }

    /**
     * Good for stability. Skip for older devices (API <26) (no notification-channels, can't hide notification).
     */
    private void requestForeground() {
        final String MONITOR_SERVICE_NOTIFICATION_CHANNEL_ID = "MONITOR_SERVICE_NOTIFICATION_CHANNEL_ID";
        final int NOTIFICATION_ID = 6664867;

        if (Build.VERSION.SDK_INT >= 26) {
            String name = "Monitor service (Hide me!)";
            String description = "This notification is necessary to ensure stability of the monitor service. But you can simply hide it if you like, just switch the toggle.";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(MONITOR_SERVICE_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent();
        notificationIntent.setComponent(new ComponentName("com.masel.almightyvolumekeys", "com.masel.almightyvolumekeys.MainActivity"));
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, MONITOR_SERVICE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.avk_notification_icon)
                .setContentTitle("Capturing volume key presses")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * @return True if this accessibility service is enabled in settings.
     */
    static boolean isEnabled(Context context) {
        return RecUtils.almightyVolumeKeysEnabled(context);
    }
}
