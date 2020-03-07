package com.masel.almightyvolumekeys;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
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

                for (Map.Entry<String, ?> entry : Utils.getMappings(getContext())) {
                    if (entry.getKey().matches(String.format("mappingListPreference_%s_command_.*", state))) {
                        MappingListPreference mappingListPreference = screen.findPreference(entry.getKey());
                        if (mappingListPreference != null) mappingListPreference.setValue(new Actions.No_action().getName());
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
