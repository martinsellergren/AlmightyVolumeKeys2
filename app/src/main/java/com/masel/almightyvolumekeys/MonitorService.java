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
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;

import com.masel.rec_utils.AudioRecorder;
import com.masel.rec_utils.KeyValueStore;
import com.masel.rec_utils.Utils;

public class MonitorService extends AccessibilityService {

    // region Required for AccessibilityService

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i("<ME>", "listener service onAccessibilityEvent()");
    }

    @Override
    public void onInterrupt() {
        Log.i("<ME>", "listener service onInterrupt()");
    }

    // endregion

    private UserInteraction userInteraction;

    @Override
    protected void onServiceConnected() {
        Log.i("<ME>", "onServiceConnected()");

        if (Build.VERSION.SDK_INT >= 26) requestForeground();
        userInteraction = new UserInteraction(this);
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
        Utils.log("onKeyEvent()");
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
                .setSmallIcon(R.mipmap.avk_launcher)
                .setContentTitle("Capturing volume key presses")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("<ME>", "onUnbind()");

        userInteraction.destroy();
        return super.onUnbind(intent);
    }

    /**
     * @return True if this accessibility service is enabled in settings.
     */
    static boolean isEnabled(Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getApplicationContext().getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        }
        catch (Settings.SettingNotFoundException e) {
            return false;
        }

        String service = context.getPackageName() + "/" + MonitorService.class.getCanonicalName();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue == null) return false;

            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                String accessibilityService = splitter.next();
                if (accessibilityService.equalsIgnoreCase(service)) {
                    return true;
                }
            }
        }

        return false;
    }
}
