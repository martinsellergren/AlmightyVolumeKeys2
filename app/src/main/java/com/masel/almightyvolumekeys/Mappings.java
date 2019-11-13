package com.masel.almightyvolumekeys;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Mappings {

    /**
     * Map action-command to action.
     * @return Mappings based on user prefs and audio-state.
     */
    static Map<String, Action> getCurrent(MyContext myContext) {
        switch (DeviceState.getCurrent(myContext)) {
            case IDLE: return getIdleMappings(myContext.context);
            case MUSIC: return getMusicMappings(myContext.context);
//            case RINGING: return getRingingMappings(myContext.context);
//            case IN_CALL: return getInCallMappings(myContext.context);
            case RECORDING_AUDIO: return getRecMappings(myContext.context);
            default: return new HashMap<>();
        }
    }

    private static Map<String, Action> getIdleMappings(Context context) {
        Map<String, Action> mappings = new HashMap<>();
        mappings.put("11", new Actions.AudioRecording_Start());
        mappings.put("00", new Actions.DndMode_Toggle());
        mappings.put("111", new Actions.TellTime());
        mappings.put("1111", new Actions.Flashlight_On());
        mappings.put("0000", new Actions.Flashlight_Off());
        mappings.put("1110", new Actions.MediaControl_Play());
        return mappings;
    }

    private static Map<String, Action> getMusicMappings(Context context) {
        Map<String, Action> mappings = new HashMap<>();
        mappings.put("10", new Actions.MediaControl_NextTrack());
        mappings.put("01", new Actions.MediaControl_PrevTrack());
        mappings.put("1110", new Actions.MediaControl_Pause());
        return mappings;
    }
//
//    private static Map<String, Action> getRingingMappings(Context context) {
//        Map<String, Action> mappings = new HashMap<>();
//        return mappings;
//    }
//
//    private static Map<String, Action> getInCallMappings(Context context) {
//        Map<String, Action> mappings = new HashMap<>();
//        return mappings;
//    }

    private static Map<String, Action> getRecMappings(Context context) {
        Map<String, Action> mappings = new HashMap<>();
        mappings.put("1", new Actions.AudioRecording_StopAndSave());
        mappings.put("0", new Actions.AudioRecording_StopAndDiscard());
        return mappings;
    }

    private static List<Action> getMappedActions(Context context) {
        List<Action> actions = new ArrayList<>();
        actions.addAll(getIdleMappings(context).values());
        actions.addAll(getMusicMappings(context).values());
//        actions.addAll(getRingingMappings(context).values());
//        actions.addAll(getInCallMappings(context).values());
        actions.addAll(getRecMappings(context).values());
        return actions;
    }

    /**
     * @return Permissions needed for the mapped actions.
     */
    static List<String> getNeededPermissions(Context context) {
        List<String> neededPermissions = new ArrayList<>();

        for (Action action : getMappedActions(context)) {
            for (String permission : action.getNeededPermissions()) {
                if (!neededPermissions.contains(permission))
                    neededPermissions.add(permission);
            }
        }
        return neededPermissions;
    }
}
