package com.tehilat.sidur;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tehilat.sidur.fragments.DailyFragment;
import com.tehilat.sidur.fragments.HomeFragment;
import com.tehilat.sidur.fragments.SettingsFragment;
import com.tehilat.sidur.fragments.UpcomingHolidaysFragment;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView botNav;
    HomeFragment home = new HomeFragment();
    DailyFragment daily = new DailyFragment();
    SettingsFragment settings = new SettingsFragment();
    UpcomingHolidaysFragment holidays = new UpcomingHolidaysFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        botNav = findViewById(R.id.bottom_nav);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainSidur, home)
                .commit();

        botNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.home) {
                selectedFragment = home;
            } else if (item.getItemId() == R.id.daily) {
                selectedFragment = daily;
            } else if (item.getItemId() == R.id.holidays) {
                selectedFragment = holidays;
            } else if (item.getItemId() == R.id.settings) {
                selectedFragment = settings;
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.mainSidur, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}

