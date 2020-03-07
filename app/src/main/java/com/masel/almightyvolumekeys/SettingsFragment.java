package com.masel.almightyvolumekeys;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.masel.rec_utils.RecUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    //private ListPreference volumeKeysChange;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

//        volumeKeysChange = findPreference("ListPreference_VolumeKeysChange");
//        requestDoNotDisturbPermissionIfRingtoneSet(volumeKeysChange);

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
                if (!RecUtils.gotoApp(getContext(), "com.masel.thesoundrecorder2")) {
                    RecUtils.openAppOnPlayStore(getContext(), "com.masel.thesoundrecorder2");
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

//        if (!RecUtils.hasPermissionToSilenceDevice(getContext())) {
//            volumeKeysChange.setValue("Media volume");
//        }
    }

    private void requestDoNotDisturbPermissionIfRingtoneSet(ListPreference listPreference) {
        listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String value = (String)newValue;
            if (value.equals("Ringtone volume")) {
                RecUtils.requestPermissionToSilenceDevice(getContext());
            }
            return true;
        });
    }

    private void designForTimeInput(SeekBarPreference seekBar) {
        seekBar.setMin(0);
        seekBar.setMax(24);
        seekBar.setShowSeekBarValue(true);
    }
}
