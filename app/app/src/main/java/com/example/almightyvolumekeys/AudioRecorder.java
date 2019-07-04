package com.example.almightyvolumekeys;

import android.Manifest;
import android.content.Context;
import android.media.MediaRecorder;
import java.io.IOException;
import java.util.Random;

class AudioRecorder {

    /**
     * Null unless recording */
    private MediaRecorder recorder = null;

    private boolean isRecording = false;

    /**
     * Null unless recording. */
    private String outputFile = null;

    void start(Context context) throws Action.ExecutionException {
        if (isRecording) {
            stopAndSave();
        }
        else if (!Utils.hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
            throw new Action.ExecutionException("No audio rec permission", true);
        }
        else if (!Utils.isExternalStorageWritable()) {
            throw new Action.ExecutionException("External storage unavailable", false);
        }

        String outputDir = context.getExternalCacheDir().getAbsolutePath();
        outputFile = String.format("%s/%s.mp3", outputDir, new Random().nextInt(10000));

        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(outputFile);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.prepare();
            recorder.start();
            isRecording = true;

            Utils.log("rec to: " + outputFile);
        } catch (IOException e) {
            Utils.log(e.getMessage());
        }
    }

    void stopAndSave() {
        if (isRecording) {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
            outputFile = null;
        }
    }

    // works with reset() ?
    void stopAndDiscard() {
        if (isRecording) {
            recorder.reset();
            recorder.release();
            recorder = null;
            isRecording = false;
            outputFile = null;
        }
    }

    boolean isRecording() {
        return isRecording;
    }
}
