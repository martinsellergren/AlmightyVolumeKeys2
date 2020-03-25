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

import com.masel.rec_utils.RecUtils;

import java.lang.reflect.Array;
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
                RecUtils.showHeadsUpDialog(getActivity(), "This action is not available on your device. See Help for more info.", null);
                setValue(new Actions.No_action().getName());
                return false;
            }
            if (!actionName.equals(new Actions.No_action().getName())
                    && getValue().equals(new Actions.No_action().getName())
                    && state.equals("idle")
                    && ProManager.loadIsLocked(context)
                    && getNumberOfMappedActions("idle") >= ProManager.numberOfFreeIdleActions) {
                RecUtils.showHeadsUpDialog(getActivity(),
                        String.format("For more than %s idle-actions, you need to <b>unlock pro</b>.", ProManager.numberOfFreeIdleActions),
                        () -> MainActivity.proManager.startPurchase(getActivity()));
                return false;
            }
            if (!actionName.equals(new Actions.No_action().getName())
                    && getValue().equals(new Actions.No_action().getName())
                    && state.equals("music")
                    && ProManager.loadIsLocked(context)
                    && getNumberOfMappedActions("music") >= ProManager.numberOfFreeMediaActions) {
                RecUtils.showHeadsUpDialog(getActivity(),
                        String.format("For more than %s media-actions, you need to <b>unlock pro</b>.", ProManager.numberOfFreeMediaActions),
                        () -> MainActivity.proManager.startPurchase(getActivity()));
                return false;
            }
            if (actionName.equals("Tasker task")) {
                prepareForTasker();
                return false;
            }

            List<String> headsUps = new ArrayList<>();
            RecUtils.SRunnable endAction = () -> requestNeededPermissions(actionName);

            if (actionName.equals(new Actions.Media_pause().getName())) {
                headsUps.add("To resume playing, use <b>Media: play</b>-action in the IDLE-tab.");
            }
            if (actionName.equals(new Actions.Media_play().getName())) {
                headsUps.add("This action will start playing the media you <b>recently paused</b>.\n\nTo control currently playing media, see the <b>MEDIA-tab</b>.");
            }
            if (actionName.equals(new Actions.Media_volume_0().getName())) {
                headsUps.add("MEDIA-commands starting with Volume Down won't work when at 0% media volume.");
            }
            if (actionName.equals(new Actions.Media_volume_100().getName())) {
                headsUps.add("MEDIA-commands starting with Volume Up won't work when at 100% media volume.");
            }
            if (actionName.equals(new Actions.Sound_recorder_start().getName()) && !TheSoundRecorderConnection.appIsInstalled(context)) {
                headsUps.add("For sound recording, you need to install another app: <b>The Sound Recorder</b>.");
                endAction = () -> RecUtils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder2");
            }
            if (actionName.equals(new Actions.Sound_recorder_start().getName()) && TheSoundRecorderConnection.appIsInstalled(context)) {
                headsUps.add("To <b>stop recording</b>, see the <b>SOUND REC-tab</b> (or click the Recording... notification).");
            }
            if (actionName.equals(new Actions.Sound_mode_sound_volume_100().getName())) {
                headsUps.add("This action will set <b>ringtone</b> and <b>notification</b> volume to 100%.");
            }
            if (HelpSystem.isLongPressHeadsUpAppropriate(getContext()) && extractCommand(getKey()).matches(".*[23].*")) {
                headsUps.add("<b>Long press guide:</b>\n1. Press and hold.\n2. Short vibration after 0.5 seconds.\n3. Release to execute action.\n\nMore info in Help.");
                endAction = () -> {
                    HelpSystem.longPressHeadsUpShown(getContext());
                    requestNeededPermissions(actionName);
                };
            }

            RecUtils.showNestedHeadsUpDialogs(getActivity(), headsUps, endAction);

            return true;
        });
    }

    private void prepareForTasker() {
        String infoText;
        RecUtils.SRunnable endAction = null;

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

        RecUtils.showHeadsUpDialog(getActivity(), infoText, endAction);
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
        RecUtils.requestPermissions(getActivity(), Arrays.asList(action.getNeededPermissions(getContext())));
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
            if (c == '0') title += "Down→";
            else if (c == '1') title += "Up→";
            else if (c == '2') title += "Long Down→";
            else if (c == '3') title += "Long Up→";
            else throw new RuntimeException("Dead end");
        }
        return title.substring(0, title.length()-1);
    }

    // region Music mapping heads up dialog

    /**
     * @param command e.g 101, for Up-Down-Up
     */
    private void showMusicMappingHeadsUpDialog(String command) {
        AppCompatActivity activity = (AppCompatActivity) RecUtils.getActivityOfPreference(this);
        if (activity == null) return;

        String text = getMusicMappingHeadsUpText(command);
        RecUtils.showHeadsUpDialog(activity, text, null);
    }

    private String getMusicMappingHeadsUpText(String command) {
        int fromMax = volumeChange(command, true);
        int fromMin = volumeChange(command, false);
        String fromMaxTxt = fromMax == 1 ? "ONE" : (fromMax == 2 ? "TWO" : (fromMax == 3 ? "THREE" : "FOUR"));
        String fromMinTxt = fromMin == 1 ? "ONE" : (fromMin == 2 ? "TWO" : (fromMin == 3 ? "THREE" : "FOUR"));

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
        AppCompatActivity activity = (AppCompatActivity) RecUtils.getActivityOfPreference(this);
        if (activity == null) {
            throw new RuntimeException("Error to get activity of preference");
        }
        return activity;
    }

    private int getNumberOfMappedActions(String state) {
        int count = 0;
        String regex = String.format("mappingListPreference_%s_command_.*", state);
        for (Map.Entry<String, ?> entry : Utils.getMappings(getContext())) {
            if (entry.getKey().matches(regex)) {
                if (!entry.getValue().equals(new Actions.No_action().getName())) count++;
            }
        }

        return count;
    }
}
