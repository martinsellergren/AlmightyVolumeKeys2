package com.masel.almightyvolumekeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import java.util.Map;

public class ClearMappingsPreference extends Preference {

    public ClearMappingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTitle("Clear all now");
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String state = extractState(getKey());
                PreferenceScreen screen = topParent();
                if (screen == null) return true;

                for (Map.Entry<String, ?> entry : PreferenceManager.getDefaultSharedPreferences(context).getAll().entrySet()) {
                    if (entry.getKey().matches(String.format("listPreference_%s_command_.*", state))) {
                        MappingListPreference listPreference = screen.findPreference(entry.getKey());
                        if (listPreference != null) listPreference.setValue("No action");
                    }
                }

                return true;
            }
        });
    }

    private String extractState(String key) {
        return key.split("_")[1];
    }

    private PreferenceScreen topParent() {
        Preference parent = getParent();
        if (parent == null) return null;

        while (true) {
            if (parent.getParent() == null) return (PreferenceScreen) parent;
            parent = parent.getParent();
        }
    }

}
