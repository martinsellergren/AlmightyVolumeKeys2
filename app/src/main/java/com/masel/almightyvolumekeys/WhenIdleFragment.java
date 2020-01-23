package com.masel.almightyvolumekeys;


import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;


public class WhenIdleFragment extends PreferenceFragmentCompat {

    public WhenIdleFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.when_idle, rootKey);
    }
}
