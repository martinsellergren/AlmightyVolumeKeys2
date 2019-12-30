package com.masel.almightyvolumekeys;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.masel.rec_utils.Utils;


/**
 * - Binds to TheSoundRecorder and sends stop-messages.
 * - Receive broadcasts from TheSoundRecorder when on-screen buttons stop-rec are pressed.
 */
class TheSoundRecorderConnection {

    /**
     * Action codes as specified in TheSoundRecorder's rec-service. */
    private static final int ACTION_STOP_AND_SAVE = 0;
    private static final int ACTION_STOP_AND_TRASH = 1;

    static class TheSoundRecorderNotInstalledException extends Exception {}

    /**
     * NULL unless TheSoundRecorder's rec-service is running. */
    private Messenger messenger = null;

    /**
     * AlmightyVolumeKeys' context. */
    private Context context;

    /**
     * Actions when TheSoundRecorder's stop-rec button is pressed. These buttons should stop TheSoundRecorder's
     * rec-service OR AlmightyVolumeKeys' local rec. */
    private Runnable onStopAndSaveButtonClick;
    private Runnable onStopAndTrashButtonClick;

    /**
     * - Init bind to TheSoundRecorder's service (when service started (or already running), or stopped: complete bind).
     * - Listen to broadcasts from TheSoundRecorder (onCreate(), or when stop-rec-button pressed).
     * - Set AlmightyVolumeKeys-is-recording-flag of TheSoundRecorder's key-value-store to false.
     *
     * @param context AlmightyVolumeKeys' context
     */
    TheSoundRecorderConnection(Context context, Runnable onStopAndSaveButtonClick, Runnable onStopAndTrashButtonClick) {
        this.context = context;
        this.onStopAndSaveButtonClick = onStopAndSaveButtonClick;
        this.onStopAndTrashButtonClick = onStopAndTrashButtonClick;

        bindToTheSoundRecorder();
        registerTheSoundRecorderReceivers();
    }

    /**
     * @return True if TheSoundRecorder is currently recording.
     */
    boolean isRecording() {
        return messenger != null;
    }

    // region Bind

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

    /**
     * If TheSoundRecorder not installed, does nothing. If already bound, does nothing.
     */
    private void bindToTheSoundRecorder() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.masel.thesoundrecorder", "com.masel.thesoundrecorder.RecorderService"));
        boolean success = context.bindService(intent, theSoundRecorderServiceConnection, 0);
        if (success) {
            Utils.log("Successful bind-init to TheSoundRecorder");
        }
        else {
            Utils.log("Failed to init bind to TheSoundRecorder");
        }
    }

    // endregion

    // region Send messages to TheSoundRecorder's rec-service, to control rec

    void stopAndSave() {
        sendMessageToTheSoundRecorder(Message.obtain(null, ACTION_STOP_AND_SAVE, 0, 0));
    }

    void stopAndTrash() {
        sendMessageToTheSoundRecorder(Message.obtain(null, ACTION_STOP_AND_TRASH, 0, 0));
    }

    private void sendMessageToTheSoundRecorder(Message msg) {
        if (messenger == null) return;
        try {
            messenger.send(msg);
        }
        catch (RemoteException e) {
            //throw new RuntimeException("Tried to send message when service dead");
        }
    }

    // endregion

    // region Send broadcasts to TheSoundRecorder's main activity, to reflect rec-state in layout.

    static void broadcastLocalRecStart(Context context) {
        sendBroadcast(context, "com.masel.almightyvolumekeys.START_REC");
    }

    static void broadcastLocalRecStop(Context context) {
        sendBroadcast(context, "com.masel.almightyvolumekeys.STOP_REC");
    }

    private static void sendBroadcast(Context context, String action) {
        Intent intent = new Intent(action);
        intent.setPackage("com.masel.thesoundrecorder");
        context.sendBroadcast(intent);
    }

    // endregion

    // region Receive broadcasts from TheSoundRecorder

    /**
     * If AlmightyVolumeKeys is running when TheSoundRecorder is installed, need to init the bind.
     * (Normally bind-init on AlmightyVolumeKeys enabled and immediately when connection lost with TheSoundRecorder's rec-service).
     */
    private BroadcastReceiver onCreateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            bindToTheSoundRecorder();
        }
    };

    /**
     * On stop-and-save-button in TheSoundRecorder.
     */
    private BroadcastReceiver stopAndSaveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onStopAndSaveButtonClick.run();
        }
    };

    /**
     * On stop-and-trash-button in TheSoundRecorder.
     */
    private BroadcastReceiver stopAndTrashReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onStopAndTrashButtonClick.run();
        }
    };

    private void registerTheSoundRecorderReceivers() {
        context.registerReceiver(onCreateReceiver, new IntentFilter("com.masel.thesoundrecorder.ON_CREATE"));
        context.registerReceiver(stopAndSaveReceiver, new IntentFilter("com.masel.thesoundrecorder.STOP_AND_SAVE_REC"));
        context.registerReceiver(stopAndTrashReceiver, new IntentFilter("com.masel.thesoundrecorder.STOP_AND_TRASH_REC"));
    }
    private void unregisterTheSoundRecorderReceivers() {
        try {
            context.unregisterReceiver(onCreateReceiver);
            context.unregisterReceiver(stopAndSaveReceiver);
            context.unregisterReceiver(stopAndTrashReceiver);
        }
        catch (IllegalArgumentException e) {}
    }

    // endregion

    void disconnect() {
        context.unbindService(theSoundRecorderServiceConnection);
        unregisterTheSoundRecorderReceivers();
    }

    // region Statics


    static boolean appIsInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.masel.thesoundrecorder", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // endregion
}
