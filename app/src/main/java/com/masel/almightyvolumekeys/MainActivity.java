package com.masel.almightyvolumekeys;

import android.Manifest;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //requestPermissions();
    }

    private void requestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        neededPermissions.add(Manifest.permission.RECORD_AUDIO);

        List<String> reqPermissions = new ArrayList<>();
        for (String permission : neededPermissions) {
            if (!Utils.hasPermission(this, permission))
                reqPermissions.add(permission);
        }

        if (reqPermissions.size() > 0)
            ActivityCompat.requestPermissions(this, reqPermissions.toArray(new String[]{}), 0);
    }
}
