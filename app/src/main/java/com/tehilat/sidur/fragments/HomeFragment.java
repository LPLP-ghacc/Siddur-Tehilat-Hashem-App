package com.tehilat.sidur.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class HomeFragment extends Fragment {

    private SharedPreferences prefs;
    private EventsViewModel eventsViewModel;
    private EventsAdapter adapter;
    private RecyclerView actualEventsRecyclerView;
    private Button showAllButton;
    private TextView moreEventsText;
    private TextView candleLightingText;
    private TextView havdalahText;
    private TextView weeklyParashaText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isExpanded = false;
    private List<JewishController.Item> allEvents = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private double userLatitude = 0.0;
    private double userLongitude = 0.0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String PREF_CANDLE_LIGHTING = "candle_lighting";
    private static final String PREF_HAVDALAH = "havdalah";
    private static final String PREF_WEEKLY_PARASHA = "weekly_parasha"; // Ключ для кэширования

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Инициализация SwipeRefreshLayout
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Инициализация ScrollView
        ScrollView scrollView = rootView.findViewById(R.id.scroll_view);

        // Отключаем SwipeRefreshLayout, если ScrollView не вверху
        scrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            swipeRefreshLayout.setEnabled(scrollY == 0);
        });

        // Инициализация элементов для времени Шаббата и недельной главы
        candleLightingText = rootView.findViewById(R.id.candle_lighting_time);
        havdalahText = rootView.findViewById(R.id.havdalah_time);
        weeklyParashaText = rootView.findViewById(R.id.weekly_parasha); // Инициализация нового TextView

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

        // Загрузка сохраненных данных о Шаббате и недельной главе (для оффлайн-режима)
        loadShabbatTimes();
        loadWeeklyParasha();

        // Проверка разрешений и запрос времени Шаббата и недельной главы
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchShabbatTimes();
            fetchWeeklyParasha();
        }

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchShabbatTimes();
                fetchWeeklyParasha();
            } else {
                Toast.makeText(getContext(), "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                loadShabbatTimes();
                loadWeeklyParasha();
            }
        }
    }

    private void fetchShabbatTimes() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (!isAdded()) return;
                            if (location != null) {
                                userLatitude = location.getLatitude();
                                userLongitude = location.getLongitude();

                                // Определяем ближайший Шаббат относительно текущей даты
                                LocalDate today = null;
                                LocalDate startDate = null; // Пятница (зажигание свечей)
                                LocalDate endDate = null;   // Суббота (авдала)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    today = LocalDate.now();
                                    // Находим ближайшую пятницу (день зажигания свечей)
                                    int daysUntilFriday = (5 - today.getDayOfWeek().getValue() + 7) % 7;
                                    startDate = today.plusDays(daysUntilFriday);
                                    // Суббота — следующий день после пятницы
                                    endDate = startDate.plusDays(1);
                                }
                                assert startDate != null && endDate != null;
                                // Запрос без хардкода часового пояса
                                String queryParams = String.format(getResources().getConfiguration().locale,
                                        "?cfg=json&latitude=%f&longitude=%f&M=on&m=72&lg=s&start=%s&end=%s&b=18",
                                        userLatitude, userLongitude, startDate.toString(), endDate.toString());
                                fetchShabbatData(queryParams, startDate, endDate);
                            } else {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Не удалось определить местоположение", Toast.LENGTH_SHORT).show();
                                    loadShabbatTimes();
                                });
                            }
                        }
                    });
        } catch (SecurityException e) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Требуется разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                loadShabbatTimes();
            });
        }
    }

    private void fetchShabbatData(String queryParams, LocalDate candleDate, LocalDate havdalahDate) {
        HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishController.HebcalResponse response) {
                if (!isAdded()) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().runOnUiThread(() -> {
                        String candleLighting = null;
                        String havdalah = null;

                        // Логируем полный ответ API для отладки
                        Log.d("HebcalResponse", "Items: " + response.getItems().toString());

                        for (JewishController.Item item : response.getItems()) {
                            try {
                                LocalDate eventDate = OffsetDateTime.parse(item.getDate()).toLocalDate();
                                Log.d("ItemDebug", "Category: " + item.getCategory() + ", Date: " + item.getDate() + ", Title: " + item.getTitle());

                                if ("candles".equals(item.getCategory()) && eventDate.equals(candleDate)) {
                                    candleLighting = item.getDate();
                                    if (candleLighting != null) {
                                        try {
                                            OffsetDateTime dateTime = OffsetDateTime.parse(candleLighting);
                                            LocalTime time = dateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
                                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", getResources().getConfiguration().locale);
                                            candleLighting = time.format(formatter);
                                            Log.d("CandleLighting", "Parsed time for " + eventDate + ": " + candleLighting);
                                        } catch (DateTimeParseException e) {
                                            Log.e("TimeParse", "Failed to parse candle lighting time: " + candleLighting, e);
                                            candleLighting = item.getTitle();
                                            if (candleLighting != null && candleLighting.contains(":")) {
                                                candleLighting = candleLighting.substring(candleLighting.lastIndexOf(":") - 5).trim();
                                                candleLighting = convertToLocalTimeFormat(candleLighting);
                                            }
                                        }
                                    }
                                } else if ("havdalah".equals(item.getCategory()) && eventDate.equals(havdalahDate)) {
                                    havdalah = item.getDate();
                                    if (havdalah != null) {
                                        try {
                                            OffsetDateTime dateTime = OffsetDateTime.parse(havdalah);
                                            LocalTime time = dateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
                                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", getResources().getConfiguration().locale);
                                            havdalah = time.format(formatter);
                                            Log.d("Havdalah", "Parsed time for " + eventDate + ": " + havdalah);
                                        } catch (DateTimeParseException e) {
                                            Log.e("TimeParse", "Failed to parse havdalah time: " + havdalah, e);
                                            havdalah = item.getTitle();
                                            if (havdalah != null && havdalah.contains(":")) {
                                                havdalah = havdalah.substring(havdalah.lastIndexOf(":") - 5).trim();
                                                havdalah = convertToLocalTimeFormat(havdalah);
                                            }
                                        }
                                    }
                                }
                            } catch (DateTimeParseException e) {
                                Log.e("DateParse", "Failed to parse event date: " + item.getDate(), e);
                            }
                        }

                        if (candleLighting != null) {
                            candleLightingText.setText(String.format(getString(R.string.candle_lighting_format), candleLighting));
                            prefs.edit().putString(PREF_CANDLE_LIGHTING, candleLighting).apply();
                        } else {
                            Log.w("CandleLighting", "No candle lighting time found for target date: " + candleDate);
                        }
                        if (havdalah != null) {
                            havdalahText.setText(String.format(getString(R.string.havdalah_format), havdalah));
                            prefs.edit().putString(PREF_HAVDALAH, havdalah).apply();
                        } else {
                            Log.w("Havdalah", "No havdalah time found for target date: " + havdalahDate);
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.error_on_load) + errorMessage, Toast.LENGTH_SHORT).show();
                    loadShabbatTimes();
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

    // Метод для загрузки сохранённой недельной главы
    private void loadWeeklyParasha() {
        String savedParasha = prefs.getString(PREF_WEEKLY_PARASHA, null);
        if (savedParasha != null) {
            weeklyParashaText.setText(String.format(getString(R.string.weekly_parasha_format), savedParasha));
        }
    }

    // Метод для загрузки недельной главы из RSS
    private void fetchWeeklyParasha() {
        String language = getHebcalLanguage();
        String rssUrl = "https://www.hebcal.com/sedrot/index-" + language + ".xml";
        new FetchParashaTask().execute(rssUrl);
    }

    // AsyncTask для загрузки и парсинга RSS
    private class FetchParashaTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder xml = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    xml.append(line);
                }
                reader.close();

                // Парсинг XML
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.toString().getBytes()));
                doc.getDocumentElement().normalize();

                NodeList itemList = doc.getElementsByTagName("item");
                if (itemList.getLength() > 0) {
                    Element item = (Element) itemList.item(0); // Берем первый элемент
                    NodeList titleList = item.getElementsByTagName("title");
                    if (titleList.getLength() > 0) {
                        return titleList.item(0).getTextContent();
                    }
                }
            } catch (Exception e) {
                Log.e("FetchParashaTask", "Error fetching/parsing RSS", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (result != null) {
                    weeklyParashaText.setText(result);
                    prefs.edit().putString(PREF_WEEKLY_PARASHA, result).apply();
                } else {
                    Toast.makeText(getContext(), getString(R.string.error_on_load), Toast.LENGTH_SHORT).show();
                    loadWeeklyParasha();
                }
            });
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
            if (events != null) {
                allEvents = filterRelevantEvents(events);
                updateEventsDisplay();
            } else {
                allEvents = new ArrayList<>();
                updateEventsDisplay();
            }
        });
    }

    private void refreshData() {
        if (userLatitude == 0.0 || userLongitude == 0.0) {
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
                                    fetchEvents();
                                    fetchWeeklyParasha(); // Обновляем недельную главу при обновлении данных
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
            fetchEvents();
            fetchWeeklyParasha(); // Обновляем недельную главу при обновлении данных
        }
    }

    private void fetchEvents() {
        String language = getHebcalLanguage();
        String queryParams = String.format(getResources().getConfiguration().locale,
                "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&latitude=%f&longitude=%f&lg=%s",
                userLatitude, userLongitude, language);

        HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishController.HebcalResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<JewishController.Item> events = response.getItems();
                    eventsViewModel.setEvents(events);
                    cacheData("cached_events", events);

                    List<JewishController.Item> holidays = new ArrayList<>();
                    for (JewishController.Item item : events) {
                        if ("holiday".equals(item.getCategory())) {
                            holidays.add(item);
                        }
                    }
                    eventsViewModel.setHolidays(holidays);
                    cacheData("cached_holidays", holidays);

                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.error_on_load) + errorMessage, Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    private void cacheData(String key, List<JewishController.Item> data) {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        prefs.edit().putString(key, json).apply();
    }

    private String getHebcalLanguage() {
        String appLang = prefs.getString("app_language", "English");
        switch (appLang) {
            case "Русский":
                return "ru";
            case "English":
                return "en";
            case "עברית":
                return "he";
            default:
                return "en";
        }
    }

    private String convertToLocalTimeFormat(String timeStr) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return timeStr;
        }
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
            LocalTime time = LocalTime.parse(timeStr, inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HH:mm", getResources().getConfiguration().locale);
            return time.format(outputFormatter);
        } catch (DateTimeParseException e) {
            Log.e("TimeFormat", "Failed to parse time: " + timeStr, e);
            return timeStr;
        }
    }

    private void updateEventsDisplay() {
        if (allEvents == null) {
            allEvents = new ArrayList<>();
        }
        if (allEvents.isEmpty()) {
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
        String lang = prefs.getString("prayer_language", "עברית");
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
            case 5: filePath = "file:///android_asset/pages/" + langCode + "/Bedtime Shema.html"; break;
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