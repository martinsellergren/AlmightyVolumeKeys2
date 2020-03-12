package com.masel.almightyvolumekeys;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.masel.rec_utils.RecUtils;


/**
 * Class for notifiers, i.e a temporary notification with vibration and text. Usually a heads-up-notification.
 */
class Notifier {

    private static final long DISPLAY_TIME = 2000;

    private static final int NOTIFICATION_ID = 8427283;

    enum VibrationPattern {ON, OFF, SILENT, ERROR};
    private static final long[] VIBRATION_PATTERN_ARRAY_ON = new long[]{0,500};
    private static final long[] VIBRATION_PATTERN_ARRAY_OFF = new long[]{0,100,100,100};
    private static final long[] VIBRATION_PATTERN_ARRAY_SILENT = new long[]{0};
    private static final long[] VIBRATION_PATTERN_ARRAY_ERROR = new long[]{0,100,10,100,10,100};

    private static final String CHANNEL_ON_ID = "Heads up channel: ON";
    private static final String CHANNEL_OFF_ID = "Heads up channel: OFF";
    private static final String CHANNEL_SILENT_ID = "Heads up channel: SILENT";
    private static final String CHANNEL_ERROR_ID = "Heads up channel: ERROR";

    private Handler cancelNotificationHandler = new Handler();

//    /**
//     * NULL unless supported (>=API 26) */
//    private NotificationChannel channelOn = null;
//    private NotificationChannel channelOff = null;
//    private NotificationChannel channelError = null;

    private Context context;
    private NotificationManagerCompat notificationManager;

    Notifier(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);

        createChannel(VibrationPattern.ON);
        createChannel(VibrationPattern.OFF);
        createChannel(VibrationPattern.SILENT);
        createChannel(VibrationPattern.ERROR);
    }

    /**
     * Only created if supported (API >= 26)
     * pre: context set
     */
    private void createChannel(VibrationPattern vibrationPattern) {
        if (Build.VERSION.SDK_INT >= 26) {
            String NOTIFICATION_CHANNEL_GROUP_ID = "Heads ups";
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(NOTIFICATION_CHANNEL_GROUP_ID, NOTIFICATION_CHANNEL_GROUP_ID));

            String notificationChannelId = getNotificationChannelId(vibrationPattern);
            long[] vibrationPatternArray = getVibrationPatternArray(vibrationPattern);
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelId, NotificationManager.IMPORTANCE_HIGH);
            //channel.setDescription("whatever");
            channel.setVibrationPattern(vibrationPatternArray);
            channel.setSound(null, null);
            channel.setGroup(NOTIFICATION_CHANNEL_GROUP_ID);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private String getNotificationChannelId(VibrationPattern pattern) {
        switch (pattern) {
            case ON: return CHANNEL_ON_ID;
            case OFF: return CHANNEL_OFF_ID;
            case SILENT: return CHANNEL_SILENT_ID;
            case ERROR: return CHANNEL_ERROR_ID;
            default: throw new RuntimeException("Dead end");
        }
    }

    private long[] getVibrationPatternArray(VibrationPattern pattern) {
        switch (pattern) {
            case ON: return VIBRATION_PATTERN_ARRAY_ON;
            case OFF: return VIBRATION_PATTERN_ARRAY_OFF;
            case SILENT: return VIBRATION_PATTERN_ARRAY_SILENT;
            case ERROR: return VIBRATION_PATTERN_ARRAY_ERROR;
            default: throw new RuntimeException("Dead end");
        }
    }


    /**
     * @param text Title of notification.
     * @param vibrationPattern One of ON/OFF/ERROR
     * @param waitOnVibration Sleep thread until vibration done.
     */
    void notify(String text, VibrationPattern vibrationPattern, boolean waitOnVibration) {
        cancelNotificationHandler.removeCallbacksAndMessages(null);
        cancel();

        String channelId = getNotificationChannelId(vibrationPattern);
        long[] vibrationPatternArray = getVibrationPatternArray(vibrationPattern);

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.avk_notification_icon)
                .setContentTitle(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(vibrationPatternArray)
                .setSound(null)
                .setAutoCancel(true)
                .build();

        try {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        catch (Exception e) {
            RecUtils.log("Failed to notify: " + e.toString());
        }
        cancelNotificationHandler.postDelayed(this::cancel, DISPLAY_TIME);

        if (waitOnVibration) {
            long sum = 0; for (long x : vibrationPatternArray) sum += x;
            final long MARGIN = 50;
            SystemClock.sleep(sum + MARGIN);
        }
    }

    void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
        cancelNotificationHandler.removeCallbacksAndMessages(null);
    }
}
