package com.example.almightyvolumekeys;

import java.util.HashMap;
import java.util.Map;

class Mappings {

    /**
     * Map action-command to action.
     * @return Mappings based on user prefs and audio-state.
     */
    static Map<String, Action> get(MyContext myContext) {
        Map<String, Action> idleMappings = new HashMap<>();
        idleMappings.put("1", new Actions.AudioRecording_Start());
        idleMappings.put("0", new Actions.AudioRecording_StopAndSave());
        idleMappings.put("10", new Actions.MediaControl_NextTrack());

        Map<String, Action> musicMappings = new HashMap<>();
        musicMappings.put("10", new Actions.MediaControl_NextTrack());
        //idleMappings.put("0", new Actions.MediaControl_NextTrack());

        switch (DeviceState.getCurrent(myContext)) {
            case IDLE: return idleMappings;
            case MUSIC: return musicMappings;
            case RINGING: return new HashMap<>();
            case IN_CALL: return new HashMap<>();
            case RECORDING_AUDIO: return new HashMap<>();
            default: return new HashMap<>();
        }
    }
}
