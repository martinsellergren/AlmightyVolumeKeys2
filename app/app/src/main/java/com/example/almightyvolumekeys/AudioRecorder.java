package com.example.almightyvolumekeys;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.widget.Toast;
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

    void start(Context context) {
        if (isRecording) {
            Toast.makeText(context, "Already recording", Toast.LENGTH_LONG).show();
        }
        else if (!Utils.hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            Toast.makeText(context, "No audio rec permission", Toast.LENGTH_LONG).show();
        }
        else if (!Utils.isExternalStorageWritable()) {
            Toast.makeText(context, "External storage unavailable", Toast.LENGTH_LONG).show();
        }
        else {
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

                String msg = "rec to: " + outputFile;
                Utils.log(msg);
                //Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Utils.log(e.getMessage());
            }
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
