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
import com.masel.rec_utils.RecUtils;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private ProManager proManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecUtils.initSettingsSharedPreferences(this, R.xml.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupTabs();
        setupSideMenu();
    }

    // region Unlock pro

    private ProManager setupUnlockPro() {
        NavigationView navigationView = findViewById(R.id.navigationView);
        MenuItem unlockProButton = navigationView.getMenu().findItem(R.id.item_unlock_pro);

        ProManager proManager = new ProManager(this);

        Runnable proLocked = () -> {
            ProManager.saveIsLocked(this, true);
            unlockProButton.setTitle("Unlock pro");
            unlockProButton.setIcon(R.drawable.lock_locked_24dp);
            unlockProButton.setOnMenuItemClickListener(item -> {
                proManager.startPurchase();
                return true;
            });
        };

        Runnable proPending = () -> {
            ProManager.saveIsLocked(this, true);
            unlockProButton.setTitle("Unlock pro (pending)");
            unlockProButton.setIcon(R.drawable.lock_locked_24dp);
            unlockProButton.setOnMenuItemClickListener(item -> {
                RecUtils.showHeadsUpDialog(MainActivity.this, "The transaction hasn't gone through yet.", null);
                return true;
            });
        };

        Runnable proUnlocked = () -> {
            ProManager.saveIsLocked(this, false);
            unlockProButton.setTitle("Pro is unlocked");
            unlockProButton.setIcon(R.drawable.lock_open_24dp);
            unlockProButton.setOnMenuItemClickListener(item -> {
                //RecUtils.showHeadsUpDialog(MainActivity.this, "Thanks for unlocking pro! Hope you like it!", proManager::revertPro);
                RecUtils.showHeadsUpDialog(MainActivity.this, "Thanks for unlocking pro! Hope you like it!", null);
                return true;
            });
        };

        proManager.setStateActions(proLocked, proPending, proUnlocked);
        proManager.init();
        return proManager;
    }

    // endregion

    // region Setup tabs

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
                case 1: return "Media";
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

    // region Setup side-menu

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
                    case R.id.item_settings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;
                    case R.id.item_support:
                        startActivity(new Intent(MainActivity.this, SupportActivity.class));
                        break;
                    case R.id.item_unlock_pro:
                        //RecUtils.toast(MainActivity.this, "Unlock pro");
                        // Handled elsewhere
                        break;
                    case R.id.item_rate_app:
                        RecUtils.showRateAppDialog(MainActivity.this);
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
            RecUtils.toast(this, toastText);
            //openAccessibilitySettings();
            openNotificationListenerSettings();
        }
        else {
            RecUtils.showHeadsUpDialog(MainActivity.this,
                    "In the following screen, <b>find</b> the Almighty Volume Keys' accessibility <b>service</b> and activate it.",
                    () -> {
                        RecUtils.toast(MainActivity.this, toastText);
                        openNotificationListenerSettings();
                        //openAccessibilitySettings();
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

//    private void openAccessibilitySettings() {
//        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//        startActivity(intent);
//    }

    private void openNotificationListenerSettings() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
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

    private void showEnableServicePopupIfNotEnabled() {
        if (!MonitorService.isEnabled(this)) {
            RecUtils.showHeadsUpDialog(this, "This app needs to be activated!\nOpen side menu and activate.", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        proManager = setupUnlockPro();
        showEnableServicePopupIfNotEnabled();
        updateEnableServiceText();
        requestPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        proManager.destroy();
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

        RecUtils.requestPermissions(this, Arrays.asList(permissions));
    }

    // endregion
}
