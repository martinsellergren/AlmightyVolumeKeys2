package com.masel.almightyvolumekeys;

import java.util.HashMap;
import java.util.Map;

class Mappings {

    /**
     * Map action-command to action.
     * @return Mappings based on user prefs and audio-state.
     */
    static Map<String, Action> get(MyContext myContext) {
        Map<String, Action> idleMappings = new HashMap<>();
        idleMappings.put("1100", new Actions.AudioRecording_Start());
        idleMappings.put("1000", new Actions.Beep());

        Map<String, Action> musicMappings = new HashMap<>();
//        musicMappings.put("10", new Actions.MediaControl_NextTrack());
        //idleMappings.put("0", new Actions.MediaControl_NextTrack());

        Map<String, Action> recMappings = new HashMap<>();
        recMappings.put("0011", new Actions.AudioRecording_StopAndSave());

        switch (DeviceState.getCurrent(myContext)) {
            case IDLE: return idleMappings;
            case MUSIC: return musicMappings;
            case RINGING: return new HashMap<>();
            case IN_CALL: return new HashMap<>();
            case RECORDING_AUDIO: return recMappings;
            default: return new HashMap<>();
        }
    }
}
