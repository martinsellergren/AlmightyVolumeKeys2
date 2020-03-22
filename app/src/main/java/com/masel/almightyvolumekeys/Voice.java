package com.masel.almightyvolumekeys;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

class Voice {

    private VolumeUtils volumeUtils;
    private TextToSpeech tts;

    private AudioStreamState beforeSpeechVolume = null;

    Voice(Context context, VolumeUtils volumeUtils) {
        this.volumeUtils = volumeUtils;

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (beforeSpeechVolume != null) volumeUtils.setVolume(beforeSpeechVolume.getStream(), beforeSpeechVolume.getVolume(), false);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if (beforeSpeechVolume != null) volumeUtils.setVolume(beforeSpeechVolume.getStream(), beforeSpeechVolume.getVolume(), false);
                    }
                });
            }
            else {
                tts = null;
            }
        });
    }

    boolean speak(String speech, int volumePercentage) {
        if (tts == null || volumeUtils == null) return false;

        if (volumePercentage != -1) {
            beforeSpeechVolume = new AudioStreamState(volumeUtils, AudioManager.STREAM_MUSIC);
            volumeUtils.setVolumePercentage(AudioManager.STREAM_MUSIC, volumePercentage, false);
        }
        else {
            beforeSpeechVolume = null;
        }

        int res = tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, speech);
        return res == TextToSpeech.SUCCESS;
    }

    boolean speak(String speech) {
        return speak(speech, -1);
    }

    private boolean isAvailable() {
        return tts != null;
    }

    void destroy() {
        tts.shutdown();
    }

    static boolean isAvailable(Context context) {
        Voice voice = new Voice(context, null);
        boolean isAvailable = voice.isAvailable();
        voice.destroy();
        return isAvailable;
    }
}
