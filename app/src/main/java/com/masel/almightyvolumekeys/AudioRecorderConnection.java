package com.masel.almightyvolumekeys;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.masel.rec_utils.AudioRecorder;

class AudioRecorderConnection {

    //region for TheSoundRecorder

    /**
     * Action codes as specified in TheSoundRecorder's rec-service. */
    private static final int ACTION_STOP_AND_SAVE = 0;
    private static final int ACTION_STOP_AND_DISCARD = 1;

    /**
     * NULL unless TheSoundRecorder's rec-service is running. */
    private Messenger messenger = null;

    //endregion

    private AudioRecorder audioRecorder;

    private Context context;

    private ServiceConnection theSoundRecorderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
            context.unbindService(this);
            bindToTheSoundRecorder();
        }
    };

    AudioRecorderConnection(Context context) {
        this.context = context;
        bindToTheSoundRecorder();
    }

    /**
     * @return True if I'm rec, or TheSoundRecorder.
     */
    boolean isRecording()  {
        return messenger != null;
    }

    /**
     * Stop my rec, or TheSoundRecorder.
     */
    void stopAndSave() {
        if (messenger != null)
            sendMessageToTheSoundRecorder(Message.obtain(null, ACTION_STOP_AND_SAVE, 0, 0));
    }

    /**
     * Stop and discard my rec, or TheSoundRecorder.
     */
    void stopAndDiscard() {
        if (messenger != null)
            sendMessageToTheSoundRecorder(Message.obtain(null, ACTION_STOP_AND_DISCARD, 0, 0));
    }

    /**
     * Start my recording (not TheSoundRecorder).
     * If already in rec, does nothing.
     */
    void start() {
        if (!isRecording()) {
            // todo
        }
    }

    //region for TheSoundRecorder

    /**
     * If TheSoundRecorder not installed, does nothing.
     */
    private void bindToTheSoundRecorder() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.masel.thesoundrecorder", "com.masel.thesoundrecorder.RecorderService"));
        boolean success = context.bindService(intent, theSoundRecorderServiceConnection, 0);
        if (!success) Utils.toast(context, "Failed to bind to TheSoundRecorder");
        else Utils.toast(context, "Successful bind to TheSoundRecorder");
    }

    void unbindFromTheSoundRecorder() {
        context.unbindService(theSoundRecorderServiceConnection);
    }

    private void sendMessageToTheSoundRecorder(Message msg) {
        if (messenger == null) return;
        try {
            messenger.send(msg);
        }
        catch (RemoteException e) {
            throw new RuntimeException("Tried to send message when service dead");
        }
    }

    // endregion
}
