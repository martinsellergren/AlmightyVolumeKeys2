package com.masel.almightyvolumekeys;


import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class WhenMusicFragment extends PreferenceFragmentCompat {

    public WhenMusicFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.when_music, rootKey);
    }
}
