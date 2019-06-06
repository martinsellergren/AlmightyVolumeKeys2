package com.example.almightyvolumekeys;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class AudioRecorder {

    private Context context;
    private String outputFile = "";
    private MediaRecorder recorder = null;
    private boolean isRecording = false;

    public AudioRecorder(Context context) {
        this.context = context;
    }

    private void setup() {
        outputFile = String.format("%s/rec_test.%s", context.getExternalCacheDir().getAbsolutePath(), "3gp");
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(outputFile);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    public void start() {
        String permission = Manifest.permission.RECORD_AUDIO;
        if (!Utils.hasPermission(context, permission)) {
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            Log.i("<ME>", "No audio rec permission");
            return;
        }

        setup();
        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            Log.i("<ME>", "rec to: " + outputFile);
        } catch (IOException e) {
            Log.e("<ME>", e.getMessage());
        }
    }

    public void stopAndSave() {
        recorder.stop();
        recorder.release();
        recorder = null;
        isRecording = false;
    }

    public void stopAndDiscard() {

    }
}
