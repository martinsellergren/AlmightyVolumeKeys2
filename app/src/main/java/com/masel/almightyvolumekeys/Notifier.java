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


//todo older devices

/**
 * Class for notifiers, i.e a temporary notification with vibration and text. Usually a heads-up-notification.
 */
class Notifier {

    private static final long DISPLAY_TIME = 2000;

    private static final int NOTIFICATION_ID = 8427283;

    enum VibrationPattern {ON, OFF, ERROR};
    private static final long[] VIBRATION_PATTERN_ARRAY_ON = new long[]{0,500};
    private static final long[] VIBRATION_PATTERN_ARRAY_OFF = new long[]{0,100,100,100};
    private static final long[] VIBRATION_PATTERN_ARRAY_ERROR = new long[]{0,100,10,100,10,100};

    private static final String CHANNEL_ON_ID = "Heads up channel: ON";
    private static final String CHANNEL_OFF_ID = "Heads up channel: OFF";
    private static final String CHANNEL_ERROR_ID = "Heads up channel: ERROR";

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
        createChannel(VibrationPattern.ERROR);
    }

    /**
     * Only created if supported (API >= 26)
     * pre: context set
     */
    private void createChannel(VibrationPattern vibrationPattern) {
        if (notificationChannelIsSupported()) {
            String notificationChannelId = getNotificationChannelId(vibrationPattern);
            long[] vibrationPatternArray = getVibrationPatternArray(vibrationPattern);

            String NOTIFICATION_CHANNEL_GROUP_ID = "Heads ups";
            String notificationChannelGroupName = NOTIFICATION_CHANNEL_GROUP_ID;
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(NOTIFICATION_CHANNEL_GROUP_ID, notificationChannelGroupName));

            String notificationChannelName = notificationChannelId;
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_HIGH);
            //channel.setDescription("whatever");
            channel.setVibrationPattern(vibrationPatternArray);
            channel.setGroup(NOTIFICATION_CHANNEL_GROUP_ID);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean notificationChannelIsSupported() {
        return Build.VERSION.SDK_INT >= 26;
    }

    private String getNotificationChannelId(VibrationPattern pattern) {
        switch (pattern) {
            case ON: return CHANNEL_ON_ID;
            case OFF: return CHANNEL_OFF_ID;
            case ERROR: return CHANNEL_ERROR_ID;
            default: throw new RuntimeException("Dead end");
        }
    }

    private long[] getVibrationPatternArray(VibrationPattern pattern) {
        switch (pattern) {
            case ON: return VIBRATION_PATTERN_ARRAY_ON;
            case OFF: return VIBRATION_PATTERN_ARRAY_OFF;
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
        String channelId = getNotificationChannelId(vibrationPattern);
        long[] vibrationPatternArray = getVibrationPatternArray(vibrationPattern);

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.baseline_build_black_18dp)
                .setContentTitle(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(vibrationPatternArray)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
        new Handler().postDelayed(this::cancel, DISPLAY_TIME);

        if (waitOnVibration) {
            long sum = 0; for (long x : vibrationPatternArray) sum += x;
            SystemClock.sleep(sum);
        }
    }

    void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
