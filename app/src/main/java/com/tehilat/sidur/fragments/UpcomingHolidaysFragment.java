package com.tehilat.sidur.fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.tehilat.sidur.api.HebcalApiClient;
import com.tehilat.sidur.adapters.HolidayAdapter;
import com.tehilat.sidur.calendar.JewishController;
import com.tehilat.sidur.models.EventsViewModel;
import com.tehilat.sidur.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class UpcomingHolidaysFragment extends Fragment {

    private ProgressBar progressBar;
    private HolidayAdapter holidayAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences prefs;
    private EventsViewModel eventsViewModel;
    private double userLatitude = 0.0;
    private double userLongitude = 0.0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 102;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upcoming_holidays, container, false);

        // Инициализация SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Инициализация SwipeRefreshLayout
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Инициализация RecyclerView
        RecyclerView holidaysRecyclerView = rootView.findViewById(R.id.holidays_recycler_view);
        progressBar = rootView.findViewById(R.id.progress_bar);

        holidaysRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        holidayAdapter = new HolidayAdapter(new ArrayList<>());
        holidaysRecyclerView.setAdapter(holidayAdapter);

        // Инициализация геолокации
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Инициализация ViewModel
        eventsViewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);
        eventsViewModel.getHolidays().observe(getViewLifecycleOwner(), holidays -> {
            if (holidays != null) {
                Log.d("UpcomingHolidays", "Holidays received: " + holidays.size());
                List<JewishController.Item> filteredHolidays = filterAndSortHolidays(holidays);
                Log.d("UpcomingHolidays", "Filtered holidays: " + filteredHolidays.size());
                holidayAdapter.updateHolidays(filteredHolidays);
            } else {
                Log.d("UpcomingHolidays", "Holidays are null");
                holidayAdapter.updateHolidays(new ArrayList<>());
            }
            progressBar.setVisibility(View.GONE);
        });

        // Проверка разрешений и запуск загрузки данных
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            refreshData();
        }

        return rootView;
    }

    private void refreshData() {
        if (userLatitude == 0.0 || userLongitude == 0.0) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (!isAdded()) {
                                    swipeRefreshLayout.setRefreshing(false);
                                    return;
                                }
                                if (location != null) {
                                    userLatitude = location.getLatitude();
                                    userLongitude = location.getLongitude();
                                    fetchHolidays();
                                } else {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Не удалось определить местоположение", Toast.LENGTH_SHORT).show();
                                        swipeRefreshLayout.setRefreshing(false);
                                    });
                                }
                            }
                        });
            } catch (SecurityException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        } else {
            fetchHolidays();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshData();
            } else {
                Toast.makeText(getContext(), "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void fetchHolidays() {
        String language = getHebcalLanguage();
        String queryParams = String.format(Locale.US,
                "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&latitude=%f&longitude=%f&lg=%s",
                userLatitude, userLongitude, language);

        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Нет интернета. Данные недоступны.", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishController.HebcalResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<JewishController.Item> events = response.getItems();
                    eventsViewModel.setEvents(events);

                    List<JewishController.Item> holidays = new ArrayList<>();
                    for (JewishController.Item item : events) {
                        if ("holiday".equals(item.getCategory())) {
                            holidays.add(item);
                        }
                    }
                    Log.d("UpcomingHolidays", "Holidays fetched: " + holidays.size());
                    eventsViewModel.setHolidays(holidays);

                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
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
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private List<JewishController.Item> filterAndSortHolidays(List<JewishController.Item> items) {
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now();
        }
        List<JewishController.Item> currentHolidays = new ArrayList<>();
        List<JewishController.Item> upcomingHolidays = new ArrayList<>();

        for (JewishController.Item item : items) {
            if ("holiday".equals(item.getCategory())) {
                LocalDate eventDate = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    eventDate = LocalDate.parse(item.getDate().substring(0, 10));
                }
                if (eventDate != null && eventDate.equals(today)) {
                    currentHolidays.add(item);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (eventDate != null && !eventDate.isBefore(today)) {
                        upcomingHolidays.add(item);
                    }
                }
            }
        }

        upcomingHolidays.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        List<JewishController.Item> result = new ArrayList<>(currentHolidays);
        result.addAll(upcomingHolidays.stream().limit(30 - currentHolidays.size()).collect(Collectors.toList()));
        return result;
    }
}