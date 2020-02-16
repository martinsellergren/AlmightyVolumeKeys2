package com.masel.almightyvolumekeys;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masel.rec_utils.AudioRecorder;
import com.masel.rec_utils.KeyValueStore;
import com.masel.rec_utils.RecUtils;

import java.io.IOException;

/**
 * This class controls the local recorder AND TheSoundRecorder's recorder (whichever is running).
 * Rec-props defined in TheSoundRecorder's key-value-store.
 * Uses the local recorder for recordings started by AlmightyVolumeKeys.
 *
 * Also maintains the AlmightyVolumeKeys-is-recording-flag of TheSoundRecorder's key-value-store
 * (to reflect the local recorder's state). For TheSoundRecorder to know what's going on.
 * This flag is initialized to false on TheSoundRecorder-start if it doesn't exist, so
 * it will always be available to read by TheSoundRecorder.
 */
class AudioRecorderDeligator {

    private Context context;

    /**
     * NULL unless currently recording locally. */
    @Nullable
    private AudioRecorder localRecorder = null;

    @NonNull
    private TheSoundRecorderConnection theSoundRecorder;

    AudioRecorderDeligator(Context context) {
        this.context = context;

        Runnable onStopAndSaveButtonClick = this::stopAndSave;
        Runnable onStopAndTrashButtonClick = this::stopAndTrash;
        theSoundRecorder = new TheSoundRecorderConnection(context, onStopAndSaveButtonClick, onStopAndTrashButtonClick);
    }

    /**
     * @return True if local rec or(/and) TheSoundRecorder.
     */
    boolean isRecording()  {
        return (localRecorder != null && localRecorder.isRecording()) || theSoundRecorder.isRecording();
    }

    /**
     * Stop local rec or(/and) TheSoundRecorder.
     *
     * If local rec stopped:
     * Flag in TheSoundRecorder's key-value-store: local rec stopped.
     * Send broadcast: local rec stopped.
     */
    void stopAndSave() {
        if (localRecorder != null) {
            localRecorder.coldStopAndSave(context);
            localRecorder = null;
            KeyValueStore.setAlmightyVolumeKeysIsRecording(context, false);
            TheSoundRecorderConnection.broadcastLocalRecStop(context);
        }

        theSoundRecorder.stopAndSave();
        AudioRecorder.removeNotification(context);
    }


    /**
     * Stop and trash local rec or(/and) TheSoundRecorder.
     *
     * If local rec stopped:
     * Flag in TheSoundRecorder's key-value-store: local rec stopped.
     * Send broadcast: local rec stopped.
     */
    void stopAndTrash() {
        if (localRecorder != null) {
            localRecorder.coldStopAndTrash(context);
            localRecorder = null;
            KeyValueStore.setAlmightyVolumeKeysIsRecording(context, false);
            TheSoundRecorderConnection.broadcastLocalRecStop(context);
        }

        theSoundRecorder.stopAndTrash();
        AudioRecorder.removeNotification(context);
    }

    /**
     * Start local recording (not TheSoundRecorder).
     * If TheSoundRecorder not installed, does nothing.
     * If already in rec, does nothing.
     *
     * Flag in TheSoundRecorder's key-value-store: local rec started.
     * Send broadcast: local rec started.
     */
    void start() throws Action.ExecutionException {
        if (!TheSoundRecorderConnection.appIsInstalled(context) || isRecording()) return;

        try {
            localRecorder = AudioRecorder.coldStart(context);
            AudioRecorder.showNotification(context);
            KeyValueStore.setAlmightyVolumeKeysIsRecording(context, true);
            TheSoundRecorderConnection.broadcastLocalRecStart(context);
        }
        catch (IOException e) {
            throw new Action.ExecutionException("Failed to start rec");
        }
    }

    void destroy() {
        if (isRecording()) {
            stopAndSave();
            RecUtils.toast(context, "Recording stopped unexpectedly");
        }

        theSoundRecorder.disconnect();
    }
}
