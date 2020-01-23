package com.masel.almightyvolumekeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.masel.rec_utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        updateEntries();
        setDefaultValue(new Actions.No_action().getName());
        setSummary("%s");
        setSingleLineTitle(false);
        setNoActionIfCurrentlySetIsUnavailable();

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String state = extractState(getKey());
                String actionName = newValue.toString();
                Action action = Actions.getActionFromName(actionName);

                if (!action.isAvailable(context)) {
                    Utils.showHeadsUpDialog(getActivity(), "This action is currently not available on your device. See Help for more info.", null);
                    setValue(new Actions.No_action().getName());
                    return false;
                }

                if (state.equals("idle") && ProManager.loadIsLocked(context) && getNumberOfSetActionsWhenIdle() >= 3) {
                    Utils.showHeadsUpDialog(getActivity(),
                            "For more than three idle-actions, you need to UNLOCK PRO (see the side-menu).",
                            null);
                    return false;
                }

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
                            () -> Utils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder2"));
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

    private void setNoActionIfCurrentlySetIsUnavailable() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String mappedActionName = sharedPreferences.getString(getKey(), null);

        if (mappedActionName != null && !Actions.getActionFromName(mappedActionName).isAvailable(getContext())) {
            sharedPreferences.edit().putString(getKey(), new Actions.No_action().getName()).apply();
        }
    }

    private void updateEntries() {
        String state = extractState(getKey());
        String[] allActions = entriesWithState(state);
        //String[] availableActions = filterAvailableActions(allActions);
        setEntries(allActions);
        setEntryValues(allActions);
    }

    private String[] filterAvailableActions(String[] actions) {
        List<String> filteredActions = new ArrayList<>();
        for (String actionName : actions) {
            Action action = Actions.getActionFromName(actionName);

            if (action.isAvailable(getContext())) {
                filteredActions.add(actionName);
            }
        }
        return filteredActions.toArray(new String[0]);
    }

    /**
     * Returns array with actions from xml, depending on state. Only returns actions available on this device.
     */
    private String[] entriesWithState(String state) {
        int res;
        if (state.equals("idle")) res = R.array.idle_actions;
        else if (state.equals("music")) res = R.array.music_actions;
        else if (state.equals("soundrec")) res = R.array.soundrec_actions;
        else throw new RuntimeException("Dead end");
        return getContext().getResources().getStringArray(res);
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

    private String titleFromKey(String key) {
        String actionCode = extractCommand(key);
        String title = "Volume ";

        for (char c : actionCode.toCharArray()) {
            if (c == '1') title += "Up→";
            else if (c == '0') title += "Down→";
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

        String alt1 = "This command only works if your device is at least <b>%s %s</b> from <b>%s</b> volume.";
        String alt2 = "This command only works if your device is at least <b>%s %s</b> from <b>MAX</b> volume and <b>%s %s</b> from <b>MIN</b> volume.";

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

    private int getNumberOfSetActionsWhenIdle() {
        int count = 0;
        for (Map.Entry<String, ?> entry : PreferenceManager.getDefaultSharedPreferences(getContext()).getAll().entrySet()) {
            if (entry.getKey().matches(String.format("mappingListPreference_%s_command_.*", "idle"))) {
                if (!entry.getValue().equals(new Actions.No_action().getName())) count++;
            }
        }

        return count;
    }
}
