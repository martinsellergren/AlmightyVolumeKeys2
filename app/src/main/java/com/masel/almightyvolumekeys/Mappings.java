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
            case SOUNDREC: return getRecMappings(myContext.context);
            default: return new HashMap<>();
        }
    }

    private static Map<String, Action> getIdleMappings(Context context) {
        Map<String, Action> mappings = new HashMap<>();
        mappings.put("11", new Actions.Sound_recorder__start());
        mappings.put("00", new Actions.Do_not_disturb__toggle());
        mappings.put("111", new Actions.Tell_time());
        mappings.put("110", new Actions.Media__play());
        mappings.put("1111", new Actions.Flashlight__on());
        mappings.put("0000", new Actions.Flashlight__off());
        return mappings;
    }

    private static Map<String, Action> getMusicMappings(Context context) {
        Map<String, Action> mappings = new HashMap<>();
        mappings.put("10", new Actions.Media__next());
        mappings.put("01", new Actions.Media__previous());
        mappings.put("110", new Actions.Media__pause());
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
        mappings.put("1", new Actions.Sound_recorder__stop_and_save());
        mappings.put("0", new Actions.Sound_recorder__stop_and_trash());
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
