package com.example.almightyvolumekeys;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.List;

class Utils {

    static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @param manager
     * @return Active audio-stream for volume-change on volume key press. A stream as defined in AudioManager.
     * Most accurate if API >= 26 (alarm-stream etc). If none active: AudioManager.USE_DEFAULT_STREAM_TYPE.
     */
    static int getActiveAudioStream(AudioManager manager) {
        if (Build.VERSION.SDK_INT >= 26) {
            List<AudioPlaybackConfiguration> configurations = manager.getActivePlaybackConfigurations();
            if (configurations.size() == 0) {
                return AudioManager.USE_DEFAULT_STREAM_TYPE;
            }
            if (configurations.size() > 1) {
                Log.i("<ME>", "Multiple active audio-streams");
            }
            return configurations.get(0).getAudioAttributes().getVolumeControlStream();
        }
        else {
            if (manager.isMusicActive()) {
                return AudioManager.STREAM_MUSIC;
            }
            if (manager.getMode() == AudioManager.MODE_RINGTONE) {
                return AudioManager.STREAM_RING;
            }
            if (manager.getMode() == AudioManager.MODE_IN_CALL || manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
                return AudioManager.STREAM_VOICE_CALL;
            }
            if (manager.getMode() == AudioManager.MODE_NORMAL) {
                return AudioManager.USE_DEFAULT_STREAM_TYPE;
            }
            else {
                throw new RuntimeException("Dead end");
            }
        }
    }
}
