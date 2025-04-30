package com.tehilat.sidur.fragments;

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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tehilat.sidur.adapters.EventsAdapter;
import com.tehilat.sidur.models.EventsViewModel;
import com.tehilat.sidur.api.HebcalApiClient;
import com.tehilat.sidur.calendar.JewishCalendar;
import com.tehilat.sidur.calendar.JewishController;
import com.tehilat.sidur.calendar.JewishHolidayHelper;
import com.tehilat.sidur.R;
import com.tehilat.sidur.ViewerPageActivity;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class HomeFragment extends Fragment {

    private SharedPreferences prefs;


    // класс представления событий
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
        RecyclerView recyclerView = rootView.findViewById(R.id.actualEventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));

        adapter = new EventsAdapter(null);
        recyclerView.setAdapter(adapter);

        eventsViewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);
        eventsViewModel.getEvents().observe(getViewLifecycleOwner(),
                events -> adapter.setEvents(events != null && !events.isEmpty() ? filterRelevantEvents(events) : null));

        if (eventsViewModel.getEvents().getValue() == null) {
            String queryParams = "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&geo=geoname&geonameid=3448439&lg=RU";
            HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
                @Override
                public void onSuccess(JewishController.HebcalResponse response) {
                    requireActivity().runOnUiThread(() ->
                            eventsViewModel.setEvents(filterRelevantEvents(response.getItems())));
                }

                @Override
                public void onError(String errorMessage) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), getString(R.string.error_on_load) + errorMessage, Toast.LENGTH_SHORT).show());
                }
            });
        }
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
        // переделать под автоматически
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