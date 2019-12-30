package com.masel.almightyvolumekeys;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;

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

        if (Build.VERSION.SDK_INT >= 28) requestForeground();
        userInteraction = new UserInteraction(this);
    }

    /**
     * Fired only when screen is on. Consumes volume key presses and pass them along for processing.
     * Other events pass through.
     * @param event
     * @return True to consume event
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            userInteraction.onVolumeKeyEvent(event);
            return true;
        }

        return super.onKeyEvent(event);
    }

    private void requestForeground() {
        // todo: different devices

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

        Notification notification = new NotificationCompat.Builder(this, MONITOR_SERVICE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("test...")
                //.setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("<ME>", "onUnbind()");

        userInteraction.release();
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
