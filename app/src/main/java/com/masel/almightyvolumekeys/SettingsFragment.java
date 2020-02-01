package com.masel.almightyvolumekeys;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.masel.rec_utils.Utils;

public class SettingsFragment extends PreferenceFragmentCompat {

    public SettingsFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        SeekBarPreference preventSleepTimeout = findPreference("SeekBarPreference_preventSleepTimeout");
        preventSleepTimeout.setMin(0);
        preventSleepTimeout.setMax(180);
        preventSleepTimeout.setShowSeekBarValue(true);

        SeekBarPreference allowSleepStart = findPreference("SeekBarPreference_allowSleepStart");
        SeekBarPreference allowSleepEnd = findPreference("SeekBarPreference_allowSleepEnd");
        designForTimeInput(allowSleepStart);
        designForTimeInput(allowSleepEnd);

        Preference gotoSoundRec = findPreference("Preference_gotoSoundRec");
        gotoSoundRec.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!Utils.gotoApp(getContext(), "com.masel.thesoundrecorder2")) {
                    Utils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder2");
                }
                return true;
            }
        });
    }

    private void designForTimeInput(SeekBarPreference seekBar) {
        seekBar.setMin(0);
        seekBar.setMax(24);
        seekBar.setShowSeekBarValue(true);
    }
}
