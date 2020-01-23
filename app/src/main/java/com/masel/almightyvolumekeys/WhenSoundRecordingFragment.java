package com.masel.almightyvolumekeys;


import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

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
                if (!Utils.gotoApp(getContext(), "com.masel.thesoundrecorder2")) {
                    Utils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder2");
                }

                return true;
            }
        });
    }
}
