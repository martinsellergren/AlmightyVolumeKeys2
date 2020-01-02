package com.masel.almightyvolumekeys;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.masel.rec_utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A ListPreference for mapping command to action, for a certain device-state.
 * When action picked, execute custom action if set, and request any needed permissions.
 *
 * Format of key (set in xml), eg: mappingListPreference_idle_command_111
 */
public class MappingListPreference extends ListPreference {

    public MappingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setTitle(titleFromKey(getKey()));
        CharSequence[] availableActions = entriesFromKey(getKey());
        setEntries(availableActions);
        setEntryValues(availableActions);
        setDefaultValue(new Actions.No_action().getName());
        setSummary("%s");

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String state = extractState(getKey());
                String actionName = newValue.toString();

                if (state.equals("music") && !actionName.equals(new Actions.No_action().getName())) {
                    showMusicMappingHeadsUpDialog(extractCommand(getKey()));
                }
                else if (actionName.equals(new Actions.Media_play().getName())) {
                    Utils.showHeadsUpDialog(getActivity(),
                            "This action will start playing the media you recently paused.\n\nTo control currently playing media, see the MEDIA-tab.",
                            () -> requestNeededPermissions(actionName));
                }
                else if (actionName.equals(new Actions.Sound_recorder_start().getName()) && !TheSoundRecorderConnection.appIsInstalled(context)) {
                    Utils.showHeadsUpDialog(getActivity(),
                            "For sound recording, you need to install another app: The Sound Recorder",
                            () -> Utils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder"));
                }
                else if (actionName.equals(new Actions.Sound_recorder_start().getName()) && TheSoundRecorderConnection.appIsInstalled(context)) {
                    Utils.showHeadsUpDialog(getActivity(),
                            "To stop recording, see the SOUND REC-tab (or click the Recording... notification).",
                            () -> requestNeededPermissions(actionName));
                }
                else {
                    requestNeededPermissions(actionName);
                }

                return true;
            }
        });
    }

    private void requestNeededPermissions(String actionName) {
        Action action = Actions.getActionFromName(actionName);
        Utils.requestPermissions(getActivity(), Arrays.asList(action.getNeededPermissions(getContext())));
    }

    private String extractCommand(String key) {
        return key.split("_")[3];
    }

    private String extractState(String key) {
        return key.split("_")[1];
    }

    /**
     * Returns array with actions from xml, depending on state. Only returns actions available on this device.
     */
    private CharSequence[] entriesFromKey(String key) {
        String state = extractState(key);
        int res;
        if (state.equals("idle")) res = R.array.idle_actions;
        else if (state.equals("music")) res = R.array.music_actions;
        else if (state.equals("soundrec")) res = R.array.soundrec_actions;
        else throw new RuntimeException("Dead end");

        String[] actions = getContext().getResources().getStringArray(res);
        List<String> filteredActions = new ArrayList<>();
        for (String actionName : actions) {
            Action action = Actions.getActionFromName(actionName);
            if (action.isAvailable(getContext())) {
                filteredActions.add(actionName);
            }
        }
        return filteredActions.toArray(new String[0]);
    }

    private String titleFromKey(String key) {
        String actionCode = extractCommand(key);
        String title = "";

        for (char c : actionCode.toCharArray()) {
            if (c == '1') title += "Up-";
            else if (c == '0') title += "Down-";
            else throw new RuntimeException("Dead end");
        }
        return title.substring(0, title.length()-1);
    }

    // region Music mapping heads up dialog

    /**
     * @param command e.g 101, for Up-Down-Up
     */
    private void showMusicMappingHeadsUpDialog(String command) {
        AppCompatActivity activity = (AppCompatActivity) Utils.getActivityOfPreference(this);
        if (activity == null) return;

        String text = getMusicMappingHeadsUpText(command);
        Utils.showHeadsUpDialog(activity, text, null);
    }

    private String getMusicMappingHeadsUpText(String command) {
        int fromMax = volumeChange(command, true);
        int fromMin = volumeChange(command, false);
        String fromMaxTxt = fromMax == 1 ? "ONE" : (fromMax == 2 ? "TWO" : "THREE");
        String fromMinTxt = fromMin == 1 ? "ONE" : (fromMin == 2 ? "TWO" : "THREE");

        String alt1 = "This command only works if your device is at least %s %s from %s volume.";
        String alt2 = "This command only works if your device is at least %s %s from MAX volume and %s %s from MIN volume.";

        if (fromMax > 0 && fromMin > 0) {
            return String.format(alt2, fromMaxTxt, fromMax == 1 ? "STEP" : "STEPS", fromMinTxt, fromMin == 1 ? "STEP" : "STEPS");
        }
        else if (fromMax != 0) {
            return String.format(alt1, fromMaxTxt, fromMax == 1 ? "STEP" : "STEPS", "MAX");
        }
        else {
            return String.format(alt1, fromMinTxt, fromMin == 1 ? "STEP" : "STEPS", "MIN");
        }
    }

    private int volumeChange(String command, boolean up) {
        int max = 0;
        int min = 0;
        int simVolume = 0;

        for (char c : command.toCharArray()) {
            if (c == '1') simVolume += 1;
            else simVolume -= 1;

            if (simVolume > max) max = simVolume;
            if (simVolume < min) min = simVolume;
        }

        return up ? max : Math.abs(min);
    }

    // endregion

    private AppCompatActivity getActivity() {
        AppCompatActivity activity = (AppCompatActivity) Utils.getActivityOfPreference(this);
        if (activity == null) {
            throw new RuntimeException("Error to get activity of preference");
        }
        return activity;
    }
}
