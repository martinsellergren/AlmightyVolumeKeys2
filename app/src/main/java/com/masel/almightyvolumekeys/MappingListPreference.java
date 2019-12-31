package com.masel.almightyvolumekeys;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

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

    private Runnable onActionPicked = null;

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
                if (onActionPicked != null) onActionPicked.run();
                Action pickedAction = Actions.getActionFromName(newValue.toString());
                if (pickedAction != null) {
                    requestNeededPermissions(pickedAction);
                }

                return true;
            }
        });
    }

    /**
     * Execute runnable when an action is picked. Default null.
     */
    public void setOnActionPickedAction(Runnable onActionPicked) {
        this.onActionPicked = onActionPicked;
    }

    private void requestNeededPermissions(Action action) {
        Activity activity = Utils.getActivityOfPreference(this);
        if (activity == null) {
            throw new RuntimeException("Error to get activity of preference");
        }
        Utils.requestPermissions(activity, Arrays.asList(action.getNeededPermissions()));
    }

    private String extractActionCode(String key) {
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
        String actionCode = extractActionCode(key);
        String title = "";

        for (char c : actionCode.toCharArray()) {
            if (c == '1') title += "Up-";
            else if (c == '0') title += "Down-";
            else throw new RuntimeException("Dead end");
        }
        return title.substring(0, title.length()-1);
    }
}
