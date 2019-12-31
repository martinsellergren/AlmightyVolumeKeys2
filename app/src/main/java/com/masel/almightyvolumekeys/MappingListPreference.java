package com.masel.almightyvolumekeys;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.masel.rec_utils.Utils;

import java.util.Arrays;

/**
 * A ListPreference for mapping command to action.
 * When action picked, execute custom action if set, and request any needed permissions.
 *
 * Format of key (set in xml), eg: listPreference_idle_command_111
 */
public class MappingListPreference extends ListPreference {

    public MappingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setTitle(titleFromKey(getKey()));
        setEntries(entriesFromKey(getKey()));
        setEntryValues(entriesFromKey(getKey()));
        setDefaultValue("No action");
        setSummary("%s");

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (extractState(getKey()).equals("music") && !newValue.toString().equals("No action")) {
                    showMusicMappingHeadsUpDialog(extractActionCommand(getKey()));
                }

                Action pickedAction = Actions.getActionFromName(newValue.toString());
                if (pickedAction != null) {
                    requestNeededPermissions(pickedAction);
                }

                return true;
            }
        });
    }

    private void requestNeededPermissions(Action action) {
        Activity activity = Utils.getActivityOfPreference(this);
        if (activity == null) {
            throw new RuntimeException("Error to get activity of preference");
        }
        Utils.requestPermissions(activity, Arrays.asList(action.getNeededPermissions()));
    }

    private String extractActionCommand(String key) {
        return key.split("_")[3];
    }

    private String extractState(String key) {
        return key.split("_")[1];
    }

    private int entriesFromKey(String key) {
        String state = extractState(key);
        if (state.equals("idle")) return R.array.idle_actions;
        if (state.equals("music")) return R.array.music_actions;
        if (state.equals("soundrec")) return R.array.soundrec_actions;
        throw new RuntimeException("Dead end");
    }

    private String titleFromKey(String key) {
        String actionCode = extractActionCommand(key);
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
        new MusicMappingHeadsUpDialog(activity, text).show(activity.getSupportFragmentManager(), "music mapping heads up dialog");
    }

    public static class MusicMappingHeadsUpDialog extends DialogFragment {
        private AppCompatActivity activity;
        private String text;

        MusicMappingHeadsUpDialog(AppCompatActivity activity, String text) {
            this.activity = activity;
            this.text = text;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(text).setPositiveButton("Ok", null);
            return builder.create();
        }
    }

    private String getMusicMappingHeadsUpText(String command) {
        int fromMax = volumeChange(command, true);
        int fromMin = volumeChange(command, false);
        String fromMaxTxt = fromMax == 1 ? "ONE" : (fromMax == 2 ? "TWO" : "THREE");
        String fromMinTxt = fromMin == 1 ? "ONE" : (fromMin == 2 ? "TWO" : "THREE");

        String alt1 = "Heads up: This command only works if your device is at least %s %s from %s volume.";
        String alt2 = "Heads up: This command only works if your device is at least %s %s from MAX volume and %s %s from MIN volume.";

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
}
