package com.masel.almightyvolumekeys;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Arrays;


//todo older devices

/**
 * Class for notifiers, i.e a temporary notification with vibration and text. Usually a heads-up-notification.
 */
class Notifier {

    private static final long DISPLAY_TIME = 3000;

    private static final int NOTIFICATION_ID = 8427283;
    private static final String NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID";

    private Context context;

    /**
     * NULL unless supported (>=API 26) */
    private NotificationChannel notificationChannel = null;

    private NotificationManagerCompat notificationManager;

    Notifier(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);
        createAndSetNotificationChannel(context);
    }

    private void createAndSetNotificationChannel(Context context) {
        if (notificationChannelIsSupported()) {
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "whatever", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("whatever");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        else {
            notificationChannel = null;
        }
    }

    private boolean notificationChannelIsSupported() {
        return Build.VERSION.SDK_INT >= 26;
    }


    /**
     * @param text Title of notification.
     * @param vibrationPattern [0]=silence-time, [1]=vib-time-ms, [0]=silence-time-ms, ...
     * @param waitOnVibration Sleep thread until vibration done.
     */
    void notify(String text, long[] vibrationPattern, boolean waitOnVibration) {
        Utils.log(Arrays.toString(vibrationPattern));

        if (notificationChannelIsSupported()) {
            notificationChannel.setVibrationPattern(vibrationPattern);
        }

        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_build_black_18dp)
                .setContentTitle(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(vibrationPattern)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
        new Handler().postDelayed(() -> notificationManager.cancel(NOTIFICATION_ID), DISPLAY_TIME);

//        if (waitOnVibration) {
//            long sum = 0; for (long x : vibrationPattern) sum += x;
//            SystemClock.sleep(sum);
//        }
    }

    void interrupt() {
        // todo
    }
}
