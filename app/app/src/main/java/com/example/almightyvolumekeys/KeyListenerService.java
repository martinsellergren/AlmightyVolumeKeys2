package com.example.almightyvolumekeys;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.VolumeProvider;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;

public class KeyListenerService extends AccessibilityService {

    private ActionCommand actionCommand;
    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i("<ME>", "listener service onAccessibilityEvent()");
    }

    @Override
    public void onInterrupt() {
        Log.i("<ME>", "listener service onInterrupt()");
    }

    /**
     * Fired only when screen is on. Consumes the volume key press so nothing else happens
     * except specifies actions.
     * @param event
     * @return True to consume event
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() ==  KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                boolean up = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP;
                handleKeyPress(up);
            }
            else {
                //ignore
            }

            return true;
        }

        return super.onKeyEvent(event);
    }

    /**
     * Performs mapped action if appropriate. Else changes volume as normal.
     * Default volume change if more than 3 volume presses.
     * @param up True if volume up pressed, false if down.
     */
    private void handleKeyPress(boolean up) {
        if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
            actionCommand.cancel();
            adjustRelevantStreamVolume(up);
        }
        else {
            if (actionCommand.getLength() >= 4) {
                adjustRelevantStreamVolume(up);
            }
            actionCommand.addBit(up);
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

        if (audioManager.isMusicActive()) {
            Log.i("<ME>", "adjust music volume");
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, volumeChangeFlag);
        }
        else if (audioManager.getMode() == AudioManager.MODE_RINGTONE) {
            Log.i("<ME>", "adjust ringer volume");
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, dir, volumeChangeFlag);
        }
        else if (audioManager.getMode() == AudioManager.MODE_IN_CALL || audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            Log.i("<ME>", "adjust voice call volume");
            audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, dir, volumeChangeFlag);
        }
        else if (audioManager.getMode() == AudioManager.MODE_NORMAL) {
            int idleStream = AudioManager.STREAM_MUSIC; //todo, get from user settings
            Log.e("<ME>", "adjust idle stream: " + idleStream);
            audioManager.adjustStreamVolume(idleStream, dir, volumeChangeFlag);
        }
        else {
            throw new RuntimeException("Dead end");
        }
    }

    @Override
    protected void onServiceConnected() {
        Log.i("<ME>", "onServiceConnected()");

        if (Build.VERSION.SDK_INT >= 28) requestForeground();
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        setupMediaSessionForScreenOffCallbacks();

        Actions actions = new Actions(this);
        actionCommand = new ActionCommand(this, actions);
    }

    private void requestForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            CharSequence name = "whatever";
            String description = "whatever";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("default_channel_id", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "default_channel_id")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("test...")
                //.setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(101, notification);
    }

    private void setupMediaSessionForScreenOffCallbacks() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "TAG", mediaButtonReceiver, null);
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
     * therefor suppress this callback. To be safe, check if to perform the remap-action OR change volume
     * of appropriate stream.
     */
    private VolumeProviderCompat screenOffCallback() {
        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 2, 1) {
            @Override
            public void onAdjustVolume(int direction) {
                if (direction != 0) {
                    boolean up = direction > 0;
                    handleKeyPress(up);
                }
            }
        };
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("<ME>", "onUnbind()");
        mediaSession.setActive(false);
        mediaSession.release();

        return super.onUnbind(intent);
    }
}
