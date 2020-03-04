package com.masel.almightyvolumekeys;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.masel.rec_utils.RecUtils;

public class MyVibrator {

    private static final int NOTIFICATION_ID = 84272867;
    private static final String ID = "Long press vibration";
    private static final long VIBRATION_TIME = 150;

    private Context context;
    private NotificationManagerCompat notificationManager;
    private Notification notification;

    private Handler vibrationDoneHandler = new Handler();

    MyVibrator(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);

        createChannel();
        notification = createNotification();
    }

    /**
     * Only created if supported (API >= 26)
     * pre: context set
     */
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(ID, ID, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setVibrationPattern(new long[]{0, VIBRATION_TIME});
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(context, ID)
                .setSmallIcon(R.drawable.avk_notification_icon)
                .setContentTitle("...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, VIBRATION_TIME})
                .setSound(null)
                .setAutoCancel(true)
                .build();
    }

    void vibrate() {
        cancel();

        try {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        catch (Exception e) {
            RecUtils.log("Failed to vibrate: " + e.toString());
        }

        vibrationDoneHandler.postDelayed(this::cancel, VIBRATION_TIME);
    }

    void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
        vibrationDoneHandler.removeCallbacksAndMessages(null);
    }
}
