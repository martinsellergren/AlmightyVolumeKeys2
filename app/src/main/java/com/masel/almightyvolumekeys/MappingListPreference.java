package com.masel.almightyvolumekeys;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
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
 *
 * Shared preferences store the state, example:
 *  - mappingListPreference_idle_command_1 - No action
 *  - mappingListPreference_idle_command_0 - Do not disturb: on
 *  (tasker actions at the end:)
 *  - mappingListPreference_idle_command_11 - Tasker: camera
 *  - mappingListPreference_idle_command_00 - Tasker: Turn on screen
 *  (or if no Tasker tasks:)
 *  - mappingListPreference_idle_command_00 - Tasker task
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

        setOnPreferenceClickListener(preference -> {
            updateEntries();
            return false;
        });

        setOnPreferenceChangeListener((preference, newValue) -> {
            String state = extractState(getKey());
            String actionName = newValue.toString();
            Action action = Actions.getActionFromName(actionName);

            if (!action.isAvailable(context)) {
                Utils.showHeadsUpDialog(getActivity(), "This action is currently not available on your device. See Help for more info.", null);
                setValue(new Actions.No_action().getName());
                return false;
            }
            if (!actionName.equals(new Actions.No_action().getName())
                    && getValue().equals(new Actions.No_action().getName())
                    && state.equals("idle") && ProManager.loadIsLocked(context)
                    && getNumberOfSetActionsWhenIdle() >= 3) {
                Utils.showHeadsUpDialog(getActivity(),
                        "For more than three idle-actions, you <b>need to UNLOCK PRO</b> (see the side-menu).",
                        null);
                return false;
            }
            if (actionName.equals("Tasker task")) {
                prepareForTasker();
                return false;
            }

            if (state.equals("music") && !actionName.equals(new Actions.No_action().getName())) {
                showMusicMappingHeadsUpDialog(extractCommand(getKey()));
            }
            else if (actionName.equals(new Actions.Media_play().getName())) {
                Utils.showHeadsUpDialog(getActivity(),
                        "This action will start playing the media you <b>recently paused</b>.\n\nTo control currently playing media, see the <b>MEDIA-tab</b>.",
                        () -> requestNeededPermissions(actionName));
            }
            else if (actionName.equals(new Actions.Sound_recorder_start().getName()) && !TheSoundRecorderConnection.appIsInstalled(context)) {
                Utils.showHeadsUpDialog(getActivity(),
                        "For sound recording, you need to install another app: <b>The Sound Recorder</b>.",
                        () -> Utils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder2"));
            }
            else if (actionName.equals(new Actions.Sound_recorder_start().getName()) && TheSoundRecorderConnection.appIsInstalled(context)) {
                Utils.showHeadsUpDialog(getActivity(),
                        "To <b>stop recording</b>, see the <b>SOUND REC-tab</b> (or click the Recording... notification).",
                        () -> requestNeededPermissions(actionName));
            }
            else {
                requestNeededPermissions(actionName);
            }

            return true;
        });
    }

    private void prepareForTasker() {
        String infoText;
        Runnable endAction = null;

        switch (TaskerIntent.testStatus(getContext())) {
            case NotInstalled:
                infoText = "Tasker tasks can do pretty much anything. Install the app and create some tasks. The tasks can then be selected here.";
                endAction = () -> getActivity().startActivity(TaskerIntent.getTaskerInstallIntent(true));
                break;
            case NoPermission:
                infoText = "Tasker is installed but not accessible. Fix it by reinstalling this app (so Tasker is installed before Almighty Volume Keys).";
                break;
            case NotEnabled:
                infoText = "Tasker is installed but not enabled. Open Tasker and enable it.";
                endAction = this::openTasker;
                break;
            case AccessBlocked:
                infoText = "Need to <b>allow external access</b> in Tasker-settings.";
                endAction = () -> getActivity().startActivity(TaskerIntent.getExternalAccessPrefsIntent());
                break;
            case NoReceiver:
                infoText = "Seems something is wrong with Tasker. Try to reinstall or update Tasker.";
                break;
            case OK:
            default:
                infoText = "Open Tasker and create some tasks. The tasks can then be selected here.";
                endAction = this::openTasker;
        }

        Utils.showHeadsUpDialog(getActivity(), infoText, endAction);
    }

    private void openTasker() {
        Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("net.dinglisch.android.taskerm");
        if (intent != null) getContext().startActivity(intent);
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
        String[] actions = entriesWithState(state);
        setEntries(actions);
        setEntryValues(actions);
    }

    /**
     * Returns array with actions from xml, depending on state.
     * End of list has available tasker tasks dynamically loaded. Formatted 'Tasker: xxx', or if no tasker tasks, one entry: 'Tasker task')
     */
    private String[] entriesWithState(String state) {
        int res;
        if (state.equals("idle")) {
            List<String> entries = new ArrayList<>(Arrays.asList(getContext().getResources().getStringArray(R.array.idle_actions)));
            List<String> taskerEntries = taskerEntries();
            entries.addAll(taskerEntries);
            return entries.toArray(new String[]{});
        }
        else if (state.equals("music")) return getContext().getResources().getStringArray(R.array.music_actions);
        else if (state.equals("soundrec")) return getContext().getResources().getStringArray(R.array.soundrec_actions);
        else throw new RuntimeException("Dead end");
    }

    /**
     * @return Available tasker tasks, or 'Tasker task' if none.
     */
    private List<String> taskerEntries() {
        List<String> entries = new ArrayList<>();

        Cursor cursor = getContext().getContentResolver().query(Uri.parse("content://net.dinglisch.android.tasker/tasks"), null, null, null, null);
        if (cursor != null) {
            int nameCol = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                entries.add("Tasker: " + cursor.getString(nameCol));
            }
            cursor.close();
        }

        if (entries.isEmpty()) entries.add("Tasker task");
        return entries;
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
