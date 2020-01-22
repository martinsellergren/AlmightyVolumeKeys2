package com.masel.almightyvolumekeys;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.masel.rec_utils.Utils;

public class WhenSoundRecordingFragment extends PreferenceFragmentCompat {

    public WhenSoundRecordingFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.when_sound_recording, rootKey);

        Preference info = findPreference("preference_soundrec_info");
        info.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!Utils.gotoApp(getContext(), "com.masel.thesoundrecorder")) {
                    Utils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder");
                }

                return true;
            }
        });
    }
}
