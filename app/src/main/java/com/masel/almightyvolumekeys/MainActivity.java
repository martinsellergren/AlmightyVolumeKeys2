package com.masel.almightyvolumekeys;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.masel.rec_utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Utils.requestPermissions(this, Mappings.getNeededPermissions(this));
    }
}
