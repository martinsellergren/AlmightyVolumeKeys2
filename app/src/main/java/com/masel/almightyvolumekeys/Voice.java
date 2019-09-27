package com.masel.almightyvolumekeys;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

class Voice {

    private TextToSpeech tts;
    private boolean isAvailable = false;

    Voice(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    isAvailable = true;
                }
            }
        });
    }

    boolean isAvailable() {
        return isAvailable;
    }

    boolean speak(String speech) {
        if (!isAvailable) return false;

        int res = tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        return res == TextToSpeech.SUCCESS;
    }

    void destroy() {
        tts.shutdown();
    }
}
