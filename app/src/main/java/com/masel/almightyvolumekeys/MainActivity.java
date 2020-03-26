package com.masel.almightyvolumekeys;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Utils.validateActions(this); //todo

        setContentView(R.layout.activity_main);

        RecUtils.initSettingsSharedPreferences(this, R.xml.settings);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupTabs();
        setupSideMenu();
        setupProManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (proManager != null) proManager.destroy();
    }

    // region Unlock pro

    static ProManager proManager;

    private void setupProManager() {
        proManager = new ProManager(this);

        NavigationView navigationView = findViewById(R.id.navigationView);
        MenuItem unlockProButton = navigationView.getMenu().findItem(R.id.item_unlock_pro);

        Runnable proLocked = () -> {
            ProManager.saveIsLocked(this, true);
            unlockProButton.setTitle("Unlock pro");
            unlockProButton.setIcon(R.drawable.lock_locked_24dp);
            unlockProButton.setOnMenuItemClickListener(item -> {
                proManager.startPurchase(this);
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
                //RecUtils.showHeadsUpDialog(MainActivity.this, "Thanks for unlocking pro! Hope you like it!", () -> proManager.revertPro(this));
                RecUtils.showHeadsUpDialog(MainActivity.this, "Thanks for unlocking pro! Hope you like it!", null);
                return true;
            });
        };

        proManager.setStateActions(proLocked, proPending, proUnlocked);
    }

    // endregion

    // region Setup tabs

    static class MyPagerAdapter extends FragmentPagerAdapter {
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
        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START, true);

            switch(item.getItemId()) {
                case R.id.item_enableDisable:
                    openNotificationListenerSettings();
                    break;
                case R.id.item_settings:
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    break;
                case R.id.item_support:
                    GotoHelpPage.gotoHelp(MainActivity.this);
                    break;
                case R.id.item_unlock_pro:
                    // Handled elsewhere
                    break;
                case R.id.item_rate_app:
                    RecUtils.showRateAppDialog(MainActivity.this);
                    break;
                default:
                    throw new RuntimeException("Dead end");
            }
            return true;
        });
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }

    // endregion

    private void updateEnableServiceHeadsUp() {
        showEnableServicePopupIfNotEnabled();
        updateEnableServiceText();
    }

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
            RecUtils.showDialog(this,
                    null,
                    "Activate <b>Almighty Volume Keys</b> in the following screen.",
                    "Got it!",
                    this::openNotificationListenerSettings,
                    "Why?",
                    () -> GotoHelpPage.gotoHelp(this, "why activate"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        proManager.runStateAction();
        updateEnableServiceHeadsUp();
        requestIntentSpecifiedPermissions();
        //requestAllNeededPermissions();
    }

    // region Permission request

    /**
     * Only does something if main activity called with specific extra.
     * Won't work in API >= 29.
     */
    public static final String EXTRA_PERMISSION_REQUEST = "com.masel.almightyvolumekeys.EXTRA_PERMISSION_REQUEST";
    private void requestIntentSpecifiedPermissions() {
        String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSION_REQUEST);
        if (permissions == null) return;
        getIntent().removeExtra(EXTRA_PERMISSION_REQUEST);

        RecUtils.requestPermissions(this, Arrays.asList(permissions));
    }

    /**
     * Request permissions for all mapped actions.
     */
    private void requestAllNeededPermissions() {
        List<String> neededPermissions = new ArrayList<>();

        for (Map.Entry<String,String> mapping : Utils.getMappings(PreferenceManager.getDefaultSharedPreferences(this))) {
            Action mappedAction = Actions.getActionFromName(mapping.getValue());
            if (mappedAction == null) continue;

            for (String permission : mappedAction.getNeededPermissions(this)) {
                if (!neededPermissions.contains(permission)) neededPermissions.add(permission);
            }
        }

        RecUtils.requestPermissions(this, neededPermissions);
    }

    // endregion
}
