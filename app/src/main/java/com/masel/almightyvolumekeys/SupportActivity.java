package com.masel.almightyvolumekeys;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_support, new SupportFragment())
                .commit();
    }
}
