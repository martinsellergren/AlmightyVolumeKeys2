package com.masel.almightyvolumekeys;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

class Voice {

    private TextToSpeech tts;

    Voice(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.SUCCESS) {
                    tts = null;
                }
            }
        });
    }

    boolean speak(String speech) {
        if (tts == null) return false;

        int res = tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        return res == TextToSpeech.SUCCESS;
    }

    boolean isAvailable() {
        return tts != null;
    }

    void destroy() {
        tts.shutdown();
    }
}
