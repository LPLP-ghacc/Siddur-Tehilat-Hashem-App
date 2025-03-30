package com.tehilat.sidur;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private SharedPreferences prefs;

    private ListView morningList;
    private TextView jewishCalendar;
    private TextView gregorianCalendar;
    private TextView holidayTextField;

    private EventsViewModel eventsViewModel;
    private EventsAdapter adapter;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        initDateHelper(rootView);
        initListViews(rootView);
        setupRecyclerView(rootView);

        return rootView;
    }

    private void setupRecyclerView(@NonNull View rootView) {
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new EventsAdapter(null);
        recyclerView.setAdapter(adapter);

        eventsViewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);

        eventsViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null && !events.isEmpty()) {
                adapter.setEvents(filterRelevantEvents(events));
            }
        });

        if (eventsViewModel.getEvents().getValue() == null) {
            fetchEvents();
        }
    }

    private void fetchEvents() {
        String queryParams = "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&geo=geoname&geonameid=3448439&lg=RU";
        ApiClient.fetchHebcalData(queryParams, new ApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishСontroller.HebcalResponse response) {
                requireActivity().runOnUiThread(() -> {
                    List<JewishСontroller.Item> events = filterRelevantEvents(response.getItems());
                    eventsViewModel.setEvents(events);
                });
            }

            @Override
            public void onError(String errorMessage) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Ошибка загрузки: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Nullable
    private LocalDate parseEventDate(String dateString) {
        try {
            // Попробуем парсить как OffsetDateTime
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return OffsetDateTime.parse(dateString).toLocalDate();
                }
            } catch (Exception e) {
                // Если не получилось, то просто парсим как LocalDate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return LocalDate.parse(dateString);
                }
            }
        } catch (Exception e) {
            Log.d("DateParser", "Failed to parse date: " + dateString);
            return null;
        }
        return null;
    }

    private List<JewishСontroller.Item> filterRelevantEvents(List<JewishСontroller.Item> events) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("EventFilter", "Received events count: " + events.size());

            LocalDate today = LocalDate.now();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return events.stream()
                        .map(event -> {
                            LocalDate eventDate = parseEventDate(event.getDate());
                            if (eventDate != null) {
                                Log.d("EventFilter", "Event: " + event.getTitle() + " | Date: " + eventDate);
                                return eventDate.isBefore(today) ? null : event;
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .limit(10)  // Покажем хотя бы 10 событий
                        .toList();
            }
        }
        return events;
    }

    private void initDateHelper(View rootView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();
            CalendarImpl impl = new CalendarImpl();

            JewishCalendar jc = JewishCalendar.gregorianToJewish(new JewishCalendar(now.getDayOfMonth(), now.getMonthValue(), now.getYear()), impl);
            jewishCalendar = rootView.findViewById(R.id.jewishdate);
            jewishCalendar.setText(jc.getDay() + " " + JewishCalendar.getJewishMonthName(jc) + " " + jc.getYear());

            String currentHoliday = JewishHolidayHelper.getCurrentHoliday(jc.getMonth(), jc.getDay());
            if (Objects.equals(currentHoliday, "nohol")) {
                currentHoliday = getResources().getString(R.string.noholidays);
            }

            // потом...
//            JewishHolidayHelper.NextHolidayInfo nextHoliday = JewishHolidayHelper.daysUntilNextHoliday(jc.getMonth(), jc.getDay());
//            holidayTextField = rootView.findViewById(R.id.holidaycalendar);
//            holidayTextField.setText(getResources().getString(R.string.todayis) + " " + currentHoliday + ", " +
//                    getResources().getString(R.string.in) + " " + nextHoliday.daysUntil + " " + getResources().getString(R.string.days) + " " + nextHoliday.name);
        }

        gregorianCalendar = rootView.findViewById(R.id.gregoriandate);
        gregorianCalendar.setText(new SimpleDateFormat("dd MMMM yyyy").format(new Date()));
    }

    private void initListViews(@NonNull View rootView) {
        morningList = rootView.findViewById(R.id.morningList);

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
            Intent intent = new Intent(getActivity(), TestPageActivity.class);
            intent.putExtra("filePath", getMorningPrayerFilePath(position));
            startActivity(intent);
        });

        setListViewHeightBasedOnChildren(morningList);
    }

    @NonNull
    @Contract(pure = true)
    private String getMorningPrayerFilePath(int position) {
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
