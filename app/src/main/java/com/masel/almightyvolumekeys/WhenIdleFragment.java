package com.masel.almightyvolumekeys;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class WhenIdleFragment extends PreferenceFragmentCompat {

    public WhenIdleFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.when_idle, rootKey);
    }
}
