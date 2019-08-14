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


//todo older devices
class MyVibrator {

    private static final long DISPLAY_TIME = 3000;

    private static final int VIBRATE_NOTIFICATION_ID = 8427283;
    private static final String VIBRATE_NOTIFICATION_CHANNEL_ID = "VIBRATE_NOTIFICATION_CHANNEL_ID";


    /**
     * Text of notification that causes vibration. */
    private String text;

    /**
     * [0]=silence-time, [1]=vib-time-ms, [0]=silence-time-ms, ... */
    private long[] pattern;

    /**
     * Sleep thread until vib done. */
    private boolean wait;

    MyVibrator(String text, long[] pattern, boolean wait) {
        this.text = text;
        this.pattern = pattern;
        this.wait = wait;
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "whatever";
            String description = "whatever";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(VIBRATE_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setVibrationPattern(pattern);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    void vibrate(Context context) {
        createNotificationChannel(context);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        //@SuppressWarnings("deprecation")
        Notification notification = new NotificationCompat.Builder(context, VIBRATE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_build_black_18dp)
                .setContentTitle(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                //.setVibrate(pattern)
                .build();

        notificationManager.notify(VIBRATE_NOTIFICATION_ID, notification);

        new Handler().postDelayed(() -> {notificationManager.cancel(VIBRATE_NOTIFICATION_ID);}, DISPLAY_TIME);

        if (wait) {
            long sum = 0; for (long x : pattern) sum += x;
            SystemClock.sleep(sum);
        }
    }

    void interrupt() {
        // todo
    }
}
