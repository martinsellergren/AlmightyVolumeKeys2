package com.masel.almightyvolumekeys;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.masel.rec_utils.Utils;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupTabs();
        setupSideMenu();
    }

    // region setup tabs

    class MyPagerAdapter extends FragmentPagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public @NonNull Fragment getItem(int position) {
            switch (position) {
                case 0: return new WhenIdleFragment();
                case 1: return new WhenMusicFragment();
                case 2: return new WhenSoundRecordingFragment();
                default: throw new RuntimeException("Dead end");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "Idle";
                case 1: return "Music";
                case 2: return "Sound rec";
                default: throw new RuntimeException("Dead end");
            }
        }
    }

    private void setupTabs() {
        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    // endregion

    // region setup side-menu

    private void setupSideMenu() {
        drawerLayout = findViewById(R.id.drawerLayout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_app_bar_open_drawer_description, R.string.nav_app_bar_open_drawer_description);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                drawerLayout.closeDrawer(GravityCompat.START, true);

                switch(item.getItemId()) {
                    case R.id.item_enableDisable:
                        onEnableDisableClick();
                        break;
                    case R.id.item_support:
                        startActivity(new Intent(MainActivity.this, SupportActivity.class));
                        break;
                    case R.id.item_unlock_pro:
                        Utils.toast(MainActivity.this, "Unlock pro");
                        break;
                    case R.id.item_rate_app:
                        Utils.showRateAppDialog(MainActivity.this);
                        break;
                    default:
                        throw new RuntimeException("Dead end");
                }
                return true;
            }
        });
    }

    private void onEnableDisableClick() {
        String toastText = "Find the Almighty Volume Keys-service";
        if (MonitorService.isEnabled(MainActivity.this)) {
            Utils.toast(this, toastText);
            openAccessibilitySettings();
        }
        else {
            Utils.showHeadsUpDialog(MainActivity.this,
                    "In the following screen, find the Almighty Volume Keys' accessibility service and activate it.",
                    new Runnable() {
                        @Override
                        public void run() {
                            Utils.toast(MainActivity.this, toastText);
                            openAccessibilitySettings();
                        }
                    });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    // endregion

    private void updateEnableServiceText() {
        TextView textView_enableAVK = findViewById(R.id.textView_enableAVK);

        if (MonitorService.isEnabled(this)) {
            textView_enableAVK.setVisibility(View.GONE);
        }
        else {
            textView_enableAVK.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateEnableServiceText();

        requestPermissions();
    }

    // region Permission request

    /**
     * Only does something if main activity called with specific extra.
     */
    public static final String EXTRA_PERMISSION_REQUEST = "com.masel.almightyvolumekeys.EXTRA_PERMISSION_REQUEST";
    private void requestPermissions() {
        String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSION_REQUEST);
        if (permissions == null) return;
        getIntent().removeExtra(EXTRA_PERMISSION_REQUEST);

        Utils.requestPermissions(this, Arrays.asList(permissions));
    }

    // endregion
}
