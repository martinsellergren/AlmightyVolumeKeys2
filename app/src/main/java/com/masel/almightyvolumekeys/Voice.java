package com.masel.almightyvolumekeys;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import com.masel.rec_utils.RecUtils;

class Voice {

    private AudioManager audioManager;
    private VolumeUtils volumeUtils;
    private TextToSpeech tts;

    private AudioStreamState beforeSpeechVolume = null;

    Voice(Context context, VolumeUtils volumeUtils) {
        this.volumeUtils = volumeUtils;
        this.audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if (beforeSpeechVolume != null) volumeUtils.setVolume(beforeSpeechVolume.getStream(), beforeSpeechVolume.getVolume(), false);
                        abandonAudioFocus();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if (beforeSpeechVolume != null) volumeUtils.setVolume(beforeSpeechVolume.getStream(), beforeSpeechVolume.getVolume(), false);
                        abandonAudioFocus();
                    }
                });
            }
            else {
                tts = null;
            }
        });
    }

    private AudioFocusRequestCompat focusRequest = null;

    private boolean requestAudioFocus() {
        focusRequest = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener(focusChange -> {})
                .setAudioAttributes(new AudioAttributesCompat.Builder()
                        .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .setUsage(AudioAttributesCompat.USAGE_ASSISTANT)
                        .setFlags(AudioAttributesCompat.FLAG_AUDIBILITY_ENFORCED)
                        .build())
                .setWillPauseWhenDucked(false)
                .build();

        int res = AudioManagerCompat.requestAudioFocus(audioManager, focusRequest);
        return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (focusRequest != null) AudioManagerCompat.abandonAudioFocusRequest(audioManager, focusRequest);
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

        boolean ok = requestAudioFocus();
        if (!ok) return false;

        int res = tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, speech);
        if (res == TextToSpeech.ERROR) {
            abandonAudioFocus();
            return false;
        }

        return true;
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
