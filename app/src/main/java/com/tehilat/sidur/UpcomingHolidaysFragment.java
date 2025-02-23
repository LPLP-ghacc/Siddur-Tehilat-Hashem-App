package com.tehilat.sidur;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpcomingHolidaysFragment extends Fragment {

    private RecyclerView holidaysRecyclerView;
    private ProgressBar progressBar;
    private HolidayAdapter holidayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upcoming_holidays, container, false);

        holidaysRecyclerView = rootView.findViewById(R.id.holidays_recycler_view);
        progressBar = rootView.findViewById(R.id.progress_bar);

        holidaysRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        holidayAdapter = new HolidayAdapter(new ArrayList<>());
        holidaysRecyclerView.setAdapter(holidayAdapter);

        fetchUpcomingHolidays();

        return rootView;
    }

    private void fetchUpcomingHolidays() {
        progressBar.setVisibility(View.VISIBLE);
        String queryParams = "?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&geo=geoname&geonameid=3448439&lg=RU";
        ApiClient.fetchHebcalData(queryParams, new ApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishСontroller.HebcalResponse response) {
                requireActivity().runOnUiThread(() -> {
                    List<JewishСontroller.Item> upcomingHolidays = filterAndSortHolidays(response.getItems());
                    holidayAdapter.updateHolidays(upcomingHolidays);
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(String errorMessage) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Ошибка: " + errorMessage, Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private List<JewishСontroller.Item> filterAndSortHolidays(List<JewishСontroller.Item> items) {
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now();
        }
        List<JewishСontroller.Item> currentHolidays = new ArrayList<>();
        List<JewishСontroller.Item> upcomingHolidays = new ArrayList<>();

        for (JewishСontroller.Item item : items) {
            if ("holiday".equals(item.getCategory())) {
                LocalDate eventDate = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    eventDate = LocalDate.parse(item.getDate().substring(0, 10));
                }
                if (eventDate.equals(today)) {
                    currentHolidays.add(item); // Сегодняшние праздники
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!eventDate.isBefore(today)) {
                        upcomingHolidays.add(item); // Будущие праздники
                    }
                }
            }
        }

        // Сортировка будущих праздников от ближнего к дальнему
        upcomingHolidays.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        // Объединяем: сначала текущие, затем будущие
        List<JewishСontroller.Item> result = new ArrayList<>(currentHolidays);
        result.addAll(upcomingHolidays.stream().limit(2 - currentHolidays.size()).collect(Collectors.toList()));
        return result;
    }
}