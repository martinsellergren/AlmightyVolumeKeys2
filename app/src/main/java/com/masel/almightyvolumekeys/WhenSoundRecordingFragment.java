package com.masel.almightyvolumekeys;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WhenSoundRecordingFragment extends PreferenceFragmentCompat {

    public WhenSoundRecordingFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.when_sound_recording, rootKey);
    }
}
