package com.masel.almightyvolumekeys;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.masel.rec_utils.AudioRecorder;

/**
 * Controls audio-recording by working with TheSoundRecorder.
 * Rec-props defined in TheSoundRecorder's shared-prefs.
 * Uses a local recorder for recordings started by AlmightyVolumeKeys.
 * This class controls the local recorder OR TheSoundRecorder's recorder (whichever is running).
 */
class AudioRecorderConnection {

    /**
     * NULL unless local recording.
     */
    private AudioRecorder localRecorder = null;

    /**
     * NULL unless TheSoundRecorder's rec-service is running. */
    private Messenger messenger = null;

    private MyContext myContext;

    AudioRecorderConnection(MyContext myContext) {
        this.myContext = myContext;
        bindToTheSoundRecorder();
        registerBindWhenTheSoundRecorderIsStartedListener();
    }

    /**
     * @return True if local rec, or TheSoundRecorder.
     */
    boolean isRecording()  {
        return (localRecorder != null && localRecorder.isRecording()) || messenger != null;
    }

    /**
     * Stop local rec, or TheSoundRecorder.
     */
    void stopAndSave() {
        if (localRecorder != null) {
            localRecorder.stopAndSave();
            localRecorder = null;
        }

        if (messenger != null)
            sendMessageToTheSoundRecorder(Message.obtain(null, ACTION_STOP_AND_SAVE, 0, 0));

        AudioRecorder.removeNotification(myContext.context);
    }

    /**
     * Stop and discard local rec, or TheSoundRecorder.
     */
    void stopAndDiscard() {
        if (localRecorder != null) {
            localRecorder.stopAndDiscard();
            localRecorder = null;
        }

        if (messenger != null)
            sendMessageToTheSoundRecorder(Message.obtain(null, ACTION_STOP_AND_DISCARD, 0, 0));

        AudioRecorder.removeNotification(myContext.context);
    }

    /**
     * Start local recording (not TheSoundRecorder).
     * If TheSoundRecorder not installed, open play-store (needs rec-props).
     * If already in rec, does nothing.
     */
    void start() throws Action.ExecutionException {
        if (isRecording()) return;

        Context theSoundRecorderContext = myContext.getTheSoundRecorderContext();
        if (theSoundRecorderContext == null) {
            // todo: install TheSoundRecorder
            throw new Action.ExecutionException("TheSoundRecorder not installed");
        }
        else {
            localRecorder = AudioRecorder.coldStart(theSoundRecorderContext);
            if (localRecorder != null) {
                AudioRecorder.showNotification(myContext.context);
            }
            else {
                throw new Action.ExecutionException("Failed to start rec");
            }
        }
    }

    //region TheSoundRecorder connection

    /**
     * Action codes as specified in TheSoundRecorder's rec-service. */
    private static final int ACTION_STOP_AND_SAVE = 0;
    private static final int ACTION_STOP_AND_DISCARD = 1;

    private ServiceConnection theSoundRecorderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
            myContext.context.unbindService(this);
            bindToTheSoundRecorder();
        }
    };

    /**
     * If TheSoundRecorder not installed, does nothing.
     */
    private void bindToTheSoundRecorder() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.masel.thesoundrecorder", "com.masel.thesoundrecorder.RecorderService"));
        boolean success = myContext.context.bindService(intent, theSoundRecorderServiceConnection, 0);
        if (!success) Utils.toast(myContext.context, "Failed to init bind to TheSoundRecorder");
        else Utils.toast(myContext.context, "Successful bind-init to TheSoundRecorder");
    }

    void destroy() {
        localRecorder.stopAndSave();
        myContext.context.unbindService(theSoundRecorderServiceConnection);
        unregisterBindWhenTheSoundRecorderIsStartedListener();
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

    // region Init bind when TheSoundRecorder freshly installed

    private BroadcastReceiver bindWhenTheSoundRecorderIsStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            bindToTheSoundRecorder();
        }
    };
    /**
     * If app is running when TheSoundRecorder is installed, need to init the bind.
     * (Otherwise bind-init immediately when connection lost with TheSoundRecorder's rec-service).
     */
    private void registerBindWhenTheSoundRecorderIsStartedListener() {
        myContext.context.registerReceiver(bindWhenTheSoundRecorderIsStartedReceiver, new IntentFilter("com.masel.TheSoundRecorder.STARTED"));
    }
    private void unregisterBindWhenTheSoundRecorderIsStartedListener() {
        myContext.context.unregisterReceiver(bindWhenTheSoundRecorderIsStartedReceiver);
    }

    // endregion

    // endregion
}
