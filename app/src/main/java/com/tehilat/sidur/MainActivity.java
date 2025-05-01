package com.tehilat.sidur;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tehilat.sidur.api.HebcalApiClient;
import com.tehilat.sidur.calendar.JewishController;
import com.tehilat.sidur.fragments.DailyFragment;
import com.tehilat.sidur.fragments.HomeFragment;
import com.tehilat.sidur.fragments.SettingsFragment;
import com.tehilat.sidur.fragments.UpcomingHolidaysFragment;
import com.tehilat.sidur.models.EventsViewModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private EventsViewModel eventsViewModel;
    private SharedPreferences prefs;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String PREF_EVENTS_KEY = "cached_events";
    private static final String PREF_HOLIDAYS_KEY = "cached_holidays";

    BottomNavigationView botNav;
    HomeFragment home = new HomeFragment();
    DailyFragment daily = new DailyFragment();
    UpcomingHolidaysFragment holidays = new UpcomingHolidaysFragment();
    SettingsFragment settings = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация SharedPreferences
        prefs = getSharedPreferences("AppData", MODE_PRIVATE);

        // Инициализация ViewModel
        eventsViewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        // Инициализация геолокации
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Загрузка кэшированных данных
        loadCachedData();

        // Проверка разрешений и загрузка данных
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchData();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        botNav = findViewById(R.id.bottom_nav);

        // Установка начального фрагмента (например, HomeFragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainSidur, home) // Исправлено с fragment_container на mainSidur
                    .commit();
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchData();
            } else {
                Toast.makeText(this, "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                // Используем кэшированные данные
                loadCachedData();
            }
        }
    }

    private void fetchData() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // Получаем язык
                                String language = getHebcalLanguage();

                                // Формируем параметры для запроса событий и праздников
                                String queryParams = String.format(Locale.US,
                                        "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&latitude=%f&longitude=%f&lg=%s",
                                        latitude, longitude, language);

                                // Проверяем наличие интернета
                                if (isNetworkAvailable()) {
                                    fetchEventsAndHolidays(queryParams);
                                } else {
                                    Toast.makeText(MainActivity.this, "Нет интернета. Используются сохранённые данные.", Toast.LENGTH_SHORT).show();
                                    loadCachedData();
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show();
                                loadCachedData();
                            }
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
            loadCachedData();
        }
    }

    private void fetchEventsAndHolidays(String queryParams) {
        HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishController.HebcalResponse response) {
                runOnUiThread(() -> {
                    // Обрабатываем события
                    List<JewishController.Item> events = response.getItems();
                    eventsViewModel.setEvents(events);
                    cacheData(PREF_EVENTS_KEY, events);

                    // Обрабатываем праздники (фильтруем только категории "holiday")
                    List<JewishController.Item> holidays = new ArrayList<>();
                    for (JewishController.Item item : response.getItems()) {
                        if ("holiday".equals(item.getCategory())) {
                            holidays.add(item);
                        }
                    }
                    eventsViewModel.setHolidays(holidays);
                    cacheData(PREF_HOLIDAYS_KEY, holidays);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки данных: " + errorMessage, Toast.LENGTH_SHORT).show();
                    loadCachedData();
                });
            }
        });
    }

    private void loadCachedData() {
        // Загружаем кэшированные события
        List<JewishController.Item> cachedEvents = loadCachedList(PREF_EVENTS_KEY);
        if (cachedEvents != null && !cachedEvents.isEmpty()) {
            eventsViewModel.setEvents(cachedEvents);
        }

        // Загружаем кэшированные праздники
        List<JewishController.Item> cachedHolidays = loadCachedList(PREF_HOLIDAYS_KEY);
        if (cachedHolidays != null && !cachedHolidays.isEmpty()) {
            eventsViewModel.setHolidays(cachedHolidays);
        }
    }

    private void cacheData(String key, List<JewishController.Item> data) {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        prefs.edit().putString(key, json).apply();
    }

    private List<JewishController.Item> loadCachedList(String key) {
        String json = prefs.getString(key, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<JewishController.Item>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    private String getHebcalLanguage() {
        String userLang = prefs.getString("prayer_language", null);
        if (userLang != null) {
            switch (userLang) {
                case "Русский":
                case "Русский (транслит.)":
                    return "ru";
                case "English":
                    return "en";
                case "עברית":
                    return "he";
                case "Français":
                    return "fr";
                default:
                    return "en";
            }
        }
        String systemLang = Locale.getDefault().getLanguage();
        switch (systemLang) {
            case "ru": return "ru";
            case "en": return "en";
            case "he": return "he";
            case "fr": return "fr";
            default: return "en";
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}