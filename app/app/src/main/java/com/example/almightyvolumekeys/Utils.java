package com.example.almightyvolumekeys;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Utils {

    static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Map action-command to action corresponding to user preferences.
     * @param context
     * @param actions Available actions.
     * @return
     */
    static Map<String, Action> getMappings(Context context, Actions actions) {
        Map<String, Action> mappings = new HashMap<>();
        mappings.put("1", actions.new AudioRecording_Start());
        mappings.put("0", actions.new AudioRecording_StopAndSave());
        mappings.put("01", actions.new DefaultVolume_Up());
        mappings.put("10", actions.new MediaControl_NextTrack());
        return mappings;
    }
}
