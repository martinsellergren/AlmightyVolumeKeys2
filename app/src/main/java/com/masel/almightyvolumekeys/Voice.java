package com.masel.almightyvolumekeys;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.masel.rec_utils.Utils;

class Voice {

    private TextToSpeech tts;
    private int beforeSpeechVolume;
    private AudioManager audioManager;

    Voice(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            restoreMusicVolume();
                        }

                        @Override
                        public void onError(String utteranceId) {
                            restoreMusicVolume();
                        }
                    });
                }
                else {
                    tts = null;
                }
            }
        });
    }

    boolean speak(String speech, int volumePercentage) {
        if (tts == null) return false;

        beforeSpeechVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Utils.setStreamVolumePercentage(audioManager, AudioManager.STREAM_MUSIC, volumePercentage);

        int res = tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, speech);
        return res == TextToSpeech.SUCCESS;
    }

    private boolean isAvailable() {
        return tts != null;
    }

    void destroy() {
        tts.shutdown();
    }

    static boolean isAvailable(Context context) {
        Voice voice = new Voice(context);
        boolean isAvailable = voice.isAvailable();
        voice.destroy();
        return isAvailable;
    }

    private void setMusicVolume(int volumePercentage) {
        int minVolume = Build.VERSION.SDK_INT >= 28 ? audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) : 0;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int targetVolume = (int)Math.round(minVolume + (maxVolume - minVolume) * ((double)volumePercentage / 100d));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    private void restoreMusicVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, beforeSpeechVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }
}
