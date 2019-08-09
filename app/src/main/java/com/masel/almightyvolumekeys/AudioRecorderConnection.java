package com.masel.almightyvolumekeys;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.core.content.ContextCompat;

class AudioRecorderConnection {

    /**
     * Action codes as specified in RecorderService. */
    private static final int ACTION_STOP_AND_SAVE = 0;
    private static final int ACTION_STOP_AND_DISCARD = 1;

    /**
     * NULL unless rec-service running. */
    private Messenger messenger = null;

    private Context context;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
            context.unbindService(this);
            bindToRecService();
        }
    };

    AudioRecorderConnection(Context context) {
        this.context = context;
        bindToRecService();
    }

    private void bindToRecService() {
        boolean success = context.bindService(getRecServiceIntent(), serviceConnection, 0);
        if (!success) throw new RuntimeException("Failed to bind");
    }

    boolean isRecording()  {
        return messenger != null;
    }

    void stopAndSave() {
        if (messenger != null)
            sendMessage(Message.obtain(null, ACTION_STOP_AND_SAVE, 0, 0));
    }

    void stopAndDiscard() {
        if (messenger != null)
            sendMessage(Message.obtain(null, ACTION_STOP_AND_DISCARD, 0, 0));
    }

    /**
     * Send the start-service command. Service starts (or just continues if already running).
     */
    void start() {
        if (messenger == null)
            ContextCompat.startForegroundService(context, getRecServiceIntent());
    }

    void unbind() {
        context.unbindService(serviceConnection);
    }

    private void sendMessage(Message msg) {
        try {
            messenger.send(msg);
        }
        catch (RemoteException e) {
            throw new RuntimeException("Tried to send message when service dead");
        }
    }

    private Intent getRecServiceIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.masel.thesoundrecorder", "com.masel.thesoundrecorder.RecorderService"));
        return intent;
    }
}
