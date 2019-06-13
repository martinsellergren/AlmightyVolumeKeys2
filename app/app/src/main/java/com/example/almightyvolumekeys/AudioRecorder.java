package com.example.almightyvolumekeys;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;

class AudioRecorder {

    /**
     * Null unless recording */
    private MediaRecorder recorder = null;

    private boolean isRecording = false;

    /**
     * Null unless recording. */
    private String outputFile = null;

    void start(Context context, String outputFile) {
        String permission = Manifest.permission.RECORD_AUDIO;
        if (!Utils.hasPermission(context, permission)) {
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            Log.i("<ME>", "No audio rec permission");
            return;
        }

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(outputFile);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.prepare();
            recorder.start();
            isRecording = true;
            outputFile = outputFile;
            Log.i("<ME>", "rec to: " + outputFile);
        } catch (IOException e) {
            Log.e("<ME>", e.getMessage());
        }
    }

    void stopAndSave() {
        recorder.stop();
        recorder.release();
        recorder = null;
        isRecording = false;
        outputFile = null;
    }

    // works with reset() ?
    void stopAndDiscard() {
        recorder.reset();
        recorder.release();
        recorder = null;
        isRecording = false;
        outputFile = null;
    }

    boolean isRecording() {
        return isRecording;
    }
}
