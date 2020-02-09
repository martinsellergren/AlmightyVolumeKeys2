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
import android.provider.Settings;
import android.text.TextUtils;
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

    private UserInteraction userInteraction;

    @Override
    protected void onServiceConnected() {
        RecUtils.log("onServiceConnected()");

        if (Build.VERSION.SDK_INT >= 26) requestForeground();
        userInteraction = new UserInteraction(this, this::onAccessibilityServiceFail);
        cleanUpAfterCrashDuringRecording();
    }

    private void cleanUpAfterCrashDuringRecording() {
        KeyValueStore.setAlmightyVolumeKeysIsRecording(this, false);
        AudioRecorder.removeNotification(this);
    }

    /**
     * Fired only when screen is on. Consumes volume key presses and pass them along for processing.
     * Other events pass through.
     * @param event
     * @return True to consume event
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        RecUtils.log("onKeyEvent()");
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            userInteraction.onVolumeKeyEvent(event);
            return true;
        }

        return super.onKeyEvent(event);
    }

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

    @Override
    public boolean onUnbind(Intent intent) {
        RecUtils.log("onUnbind()");

        userInteraction.destroy();
        return super.onUnbind(intent);
    }

    /**
     * @return True if this accessibility service is enabled in settings.
     */
    static boolean isEnabled(Context context) {
        return RecUtils.almightyVolumeKeysEnabled(context);
    }

    private void onAccessibilityServiceFail() {
        if (Build.VERSION.SDK_INT < 24) return;

        RecUtils.gotoMainActivity(this);
        try {
            disableSelf();
        }
        catch (Exception e) {
            RecUtils.log("Failed to disable self");
        }
    }
}
