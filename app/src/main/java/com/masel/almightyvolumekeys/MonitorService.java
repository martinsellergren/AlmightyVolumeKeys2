package com.masel.almightyvolumekeys;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;

import com.masel.rec_utils.Utils;

public class MonitorService extends AccessibilityService {

    /**
     * Fields set on service start. */
    private ActionCommand actionCommand = null;
    private MyContext myContext = null;
    private VolumeChangeObserver volumeChangeObserver = null;

    // region required for AccessibilityService

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i("<ME>", "listener service onAccessibilityEvent()");
    }

    @Override
    public void onInterrupt() {
        Log.i("<ME>", "listener service onInterrupt()");
    }

    // endregion


    @Override
    protected void onServiceConnected() {
        Log.i("<ME>", "onServiceConnected()");

        if (Build.VERSION.SDK_INT >= 28) requestForeground();
        myContext = new MyContext(getApplicationContext());
        actionCommand = new ActionCommand(myContext);
        volumeChangeObserver = new VolumeChangeObserver(myContext.audioManager, actionCommand);
        volumeChangeObserver.start(myContext.context);
        setupMediaSessionForScreenOffCallbacks();
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
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                boolean up = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;
                handleVolumeKeyPress(up);
            } else {
                //ignore
            }

            return true;
        }

        return super.onKeyEvent(event);
    }

    /**
     * Adds press to action command if appropriate. Else changes volume as normal.
     * Default volume change if more than 3 volume presses.
     * @param up True if volume up pressed, false if down.
     */
    private void handleVolumeKeyPress(boolean up) {
        if (actionCommand.getLength() >= 4) {
            adjustRelevantStreamVolume(up);
            return;
        }

        DeviceState state = DeviceState.getCurrent(myContext);
        boolean passActionBit = state.equals(DeviceState.RECORDING_AUDIO) || state.equals(DeviceState.IDLE);
        if (!passActionBit) {
            adjustRelevantStreamVolume(up);
        }
        else {
            actionCommand.addBit(up, ActionCommand.DELTA_PRESS_TIME_FAST);
        }
    }

    /**
     * Finds relevant stream and changes its volume.
     * @param up else down
     *
     * todo: investigate AudioManager.adjustVolume(), USE_DEFAULT_STREAM_TYPE -constant
     */
    private void adjustRelevantStreamVolume(boolean up) {
        int dir = up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        int volumeChangeFlag = AudioManager.FLAG_SHOW_UI;
        int activeStream = Utils.getActiveAudioStream(myContext.audioManager);

        if (activeStream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
            activeStream = AudioManager.STREAM_MUSIC; //todo: get from user settings
        }

        myContext.audioManager.adjustStreamVolume(activeStream, dir, volumeChangeFlag);
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

    private void setupMediaSessionForScreenOffCallbacks() {
        MediaSessionCompat mediaSession = myContext.mediaSession;
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder(); stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        mediaSession.setPlaybackState(stateBuilder.build());
        VolumeProviderCompat volumeProvider = screenOffCallback();
        mediaSession.setPlaybackToRemote(volumeProvider);
        mediaSession.setActive(true);
    }

    /**
     * Fallback when onKeyEvent doesn't catch the event (i.e when screen is off).
     * Music active, in call, phone ringing etc "should" take control over the volume-keys and
     * therefor suppress this callback. To be safe, check if to add press to action-command OR
     * change volume of appropriate stream.
     */
    private VolumeProviderCompat screenOffCallback() {
        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 2, 1) {
            @Override
            public void onAdjustVolume(int direction) {
                if (direction != 0) {
                    boolean up = direction > 0;
                    handleVolumeKeyPress(up);
                }
            }
        };
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("<ME>", "onUnbind()");
        myContext.destroy();
        volumeChangeObserver.stop(myContext.context);

        return super.onUnbind(intent);
    }
}
