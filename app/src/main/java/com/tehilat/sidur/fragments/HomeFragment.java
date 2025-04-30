package com.tehilat.sidur.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.tehilat.sidur.CompassActivity;
import com.tehilat.sidur.R;
import com.tehilat.sidur.ViewerPageActivity;
import com.tehilat.sidur.adapters.EventsAdapter;
import com.tehilat.sidur.api.HebcalApiClient;
import com.tehilat.sidur.calendar.JewishCalendar;
import com.tehilat.sidur.calendar.JewishController;
import com.tehilat.sidur.calendar.JewishHolidayHelper;
import com.tehilat.sidur.models.EventsViewModel;
import org.jetbrains.annotations.Contract;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private SharedPreferences prefs;
    private EventsViewModel eventsViewModel;
    private EventsAdapter adapter;
    private RecyclerView actualEventsRecyclerView;
    private Button showAllButton;
    private TextView moreEventsText;
    private TextView candleLightingText;
    private TextView havdalahText;
    private boolean isExpanded = false;
    private List<JewishController.Item> allEvents;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String PREF_CANDLE_LIGHTING = "candle_lighting";
    private static final String PREF_HAVDALAH = "havdalah";

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Инициализация новых элементов для времени Шаббата
        candleLightingText = rootView.findViewById(R.id.candle_lighting_time);
        havdalahText = rootView.findViewById(R.id.havdalah_time);

        // Инициализация геолокации
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        initDateHelper(rootView);
        initListViews(rootView);
        setupRecyclerView(rootView);

        // Инициализация новых элементов
        actualEventsRecyclerView = rootView.findViewById(R.id.actualEventsRecyclerView);
        showAllButton = rootView.findViewById(R.id.show_all_button);
        moreEventsText = rootView.findViewById(R.id.more_events_text);

        // Инициализация кнопки "Компас"
        Button compassButton = rootView.findViewById(R.id.compass_button);
        compassButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CompassActivity.class);
            startActivity(intent);
        });

        // Обработчик кнопки "Показать все"
        showAllButton.setOnClickListener(v -> toggleEventsDisplay());

        // Загрузка сохраненных данных о Шаббате (для оффлайн-режима)
        loadShabbatTimes();

        // Проверка разрешений и запрос времени Шаббата
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchShabbatTimes();
        }

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchShabbatTimes();
            } else {
                Toast.makeText(getContext(), "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                loadShabbatTimes(); // Загружаем сохраненные данные, если разрешения нет
            }
        }
    }

    private void fetchShabbatTimes() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                String queryParams = String.format("?cfg=json&latitude=%f&longitude=%f&M=on&lg=s", location.getLatitude(), location.getLongitude());
                                fetchShabbatData(queryParams);
                            } else {
                                Toast.makeText(getContext(), "Не удалось определить местоположение", Toast.LENGTH_SHORT).show();
                                loadShabbatTimes(); // Загружаем сохраненные данные, если местоположение не определено
                            }
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
            loadShabbatTimes();
        }
    }

    private void fetchShabbatData(String queryParams) {
        HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishController.HebcalResponse response) {
                requireActivity().runOnUiThread(() -> {
                    // Извлекаем время зажигания свечей и Хавдалы
                    String candleLighting = null;
                    String havdalah = null;

                    for (JewishController.Item item : response.getItems()) {
                        if ("candles".equals(item.getCategory())) {
                            candleLighting = item.getTitle();
                            // Извлекаем только время из строки, например, "Candle lighting: 6:40pm" -> "6:40pm"
                            if (candleLighting != null && candleLighting.contains(":")) {
                                candleLighting = candleLighting.substring(candleLighting.lastIndexOf(":") - 1).trim();
                            }
                        } else if ("havdalah".equals(item.getCategory())) {
                            havdalah = item.getTitle();
                            // Извлекаем только время из строки, например, "Havdalah: 8:00pm" -> "8:00pm"
                            if (havdalah != null && havdalah.contains(":")) {
                                havdalah = havdalah.substring(havdalah.lastIndexOf(":") - 1).trim();
                            }
                        }
                    }

                    // Обновляем UI
                    if (candleLighting != null) {
                        candleLightingText.setText(String.format(getString(R.string.candle_lighting_format), candleLighting));
                        // Сохраняем данные
                        prefs.edit().putString(PREF_CANDLE_LIGHTING, candleLighting).apply();
                    }
                    if (havdalah != null) {
                        havdalahText.setText(String.format(getString(R.string.havdalah_format), havdalah));
                        // Сохраняем данные
                        prefs.edit().putString(PREF_HAVDALAH, havdalah).apply();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.error_on_load) + errorMessage, Toast.LENGTH_SHORT).show();
                    loadShabbatTimes(); // Загружаем сохраненные данные при ошибке
                });
            }
        });
    }

    private void loadShabbatTimes() {
        String savedCandleLighting = prefs.getString(PREF_CANDLE_LIGHTING, null);
        String savedHavdalah = prefs.getString(PREF_HAVDALAH, null);

        if (savedCandleLighting != null) {
            candleLightingText.setText(String.format(getString(R.string.candle_lighting_format), savedCandleLighting));
        }
        if (savedHavdalah != null) {
            havdalahText.setText(String.format(getString(R.string.havdalah_format), savedHavdalah));
        }
    }

    private void setupRecyclerView(@NonNull View rootView) {
        RecyclerView recyclerView = rootView.findViewById(R.id.actualEventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));

        adapter = new EventsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        eventsViewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);
        eventsViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            allEvents = events != null && !events.isEmpty() ? filterRelevantEvents(events) : new ArrayList<>();
            updateEventsDisplay();
        });

        if (eventsViewModel.getEvents().getValue() == null) {
            String queryParams = "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&geo=geoname&geonameid=3448439&lg=RU";
            HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
                @Override
                public void onSuccess(JewishController.HebcalResponse response) {
                    requireActivity().runOnUiThread(() -> {
                        allEvents = filterRelevantEvents(response.getItems());
                        eventsViewModel.setEvents(allEvents);
                        updateEventsDisplay();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), getString(R.string.error_on_load) + errorMessage, Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void updateEventsDisplay() {
        if (allEvents == null || allEvents.isEmpty()) {
            actualEventsRecyclerView.setVisibility(View.GONE);
            moreEventsText.setVisibility(View.GONE);
            showAllButton.setVisibility(View.GONE);
            return;
        }

        actualEventsRecyclerView.setVisibility(View.VISIBLE);
        showAllButton.setVisibility(View.VISIBLE);

        List<JewishController.Item> displayList;
        if (isExpanded) {
            displayList = new ArrayList<>(allEvents);
            showAllButton.setText(R.string.show_less);
            moreEventsText.setVisibility(View.GONE);
        } else {
            displayList = new ArrayList<>();
            displayList.add(allEvents.get(0));
            showAllButton.setText(R.string.show_all);

            if (allEvents.size() > 1) {
                moreEventsText.setVisibility(View.VISIBLE);
                moreEventsText.setText(String.format(getString(R.string.more_events), allEvents.size() - 1));
            } else {
                moreEventsText.setVisibility(View.GONE);
            }
        }

        adapter.setEvents(displayList);

        Animation animation = AnimationUtils.loadAnimation(getContext(),
                isExpanded ? R.anim.expand : R.anim.collapse);
        actualEventsRecyclerView.startAnimation(animation);
    }

    private void toggleEventsDisplay() {
        isExpanded = !isExpanded;
        updateEventsDisplay();
    }

    private List<JewishController.Item> filterRelevantEvents(List<JewishController.Item> events) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return events;

        LocalDate today = LocalDate.now();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return events.stream()
                    .map(event -> {
                        LocalDate eventDate = parseEventDate(event.getDate());
                        return eventDate != null && !eventDate.isBefore(today) ? event : null;
                    })
                    .filter(Objects::nonNull)
                    .limit(7)
                    .toList();
        }
        return events;
    }

    @Nullable
    private LocalDate parseEventDate(String dateString) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || dateString == null) return null;

        try {
            try {
                return OffsetDateTime.parse(dateString).toLocalDate();
            } catch (DateTimeParseException e) {
                return LocalDate.parse(dateString);
            }
        } catch (DateTimeParseException e) {
            Log.d("DateParser", "Failed to parse date: " + dateString);
            return null;
        }
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void initDateHelper(View rootView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();
            JewishCalendar.CalendarImpl impl = new JewishCalendar.CalendarImpl();

            JewishCalendar jc = JewishCalendar.gregorianToJewish(new JewishCalendar(now.getDayOfMonth(), now.getMonthValue(), now.getYear()), impl);
            TextView jewishCalendar = rootView.findViewById(R.id.jewishdate);
            jewishCalendar.setText(jc.getDay() + " " + JewishCalendar.getJewishMonthName(jc) + " " + jc.getYear());

            String currentHoliday = JewishHolidayHelper.getCurrentHoliday(jc.getMonth(), jc.getDay());
            if (Objects.equals(currentHoliday, "nohol")) {
                currentHoliday = getResources().getString(R.string.noholidays);
            }
        }

        TextView gregorianCalendar = rootView.findViewById(R.id.gregoriandate);
        gregorianCalendar.setText(new SimpleDateFormat("dd MMMM yyyy").format(new Date()));
    }

    private void initListViews(@NonNull View rootView) {
        ListView morningList = rootView.findViewById(R.id.dailyList);

        String[] morningPrayers = {
                getResources().getString(R.string.shararit),
                getResources().getString(R.string.mincha),
                getResources().getString(R.string.maariv),
                getResources().getString(R.string.tehilim),
                getResources().getString(R.string.birkathamazon),
                getResources().getString(R.string.travel),
                getResources().getString(R.string.kriat_shema),
        };

        ArrayAdapter<String> adapterShahar = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, morningPrayers);
        morningList.setAdapter(adapterShahar);

        morningList.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getActivity(), ViewerPageActivity.class);
            intent.putExtra("filePath", getPrayerFilePath(position));
            startActivity(intent);
        });

        setListViewHeightBasedOnChildren(morningList);
    }

    @NonNull
    @Contract(pure = true)
    private String getPrayerFilePath(int position) {
        String lang = prefs.getString("prayer_language", "Русский");
        String langCode;

        switch (lang) {
            case "English": langCode = "en"; break;
            case "עברית": langCode = "he"; break;
            case "Français": langCode = "fr"; break;
            case "Русский (транслит.)": langCode = "ru_tr"; break;
            case "Русский": default: langCode = "ru"; break;
        }

        String filePath;
        switch (position) {
            case 0: filePath = "file:///android_asset/pages/" + langCode + "/Shaharit.html"; break;
            case 1: filePath = "file:///android_asset/pages/" + langCode + "/Minha.html"; break;
            case 2: filePath = "file:///android_asset/pages/" + langCode + "/Maariv.html"; break;
            case 3: filePath = "file:///android_asset/tegilim/" + langCode + "/tegilim.html"; break;
            case 4: filePath = "file:///android_asset/pages/" + langCode + "/BirkatHamazon.html"; break;
            case 5: filePath = "file:///android_asset/pages/" + langCode + "/Travel.html"; break;
            case 6: filePath = "file:///android_asset/pages/" + langCode + "/Bedtime Shema.html"; break;
            case 7: filePath = "file:///android_asset/pages/" + langCode + "/Nasi.html"; break;
            default: filePath = "file:///android_asset/default.html"; break;
        }
        Log.d("FilePath", "Loading file: " + filePath);
        return filePath;
    }

    public static void setListViewHeightBasedOnChildren(@NonNull ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            totalHeight += 150;
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}