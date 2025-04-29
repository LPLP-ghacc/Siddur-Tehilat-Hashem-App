package com.tehilat.sidur.fragments;

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

import com.tehilat.sidur.api.HebcalApiClient;
import com.tehilat.sidur.adapters.HolidayAdapter;
import com.tehilat.sidur.calendar.JewishController;
import com.tehilat.sidur.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpcomingHolidaysFragment extends Fragment {

    private ProgressBar progressBar;
    private HolidayAdapter holidayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upcoming_holidays, container, false);

        RecyclerView holidaysRecyclerView = rootView.findViewById(R.id.holidays_recycler_view);
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
        HebcalApiClient.fetchHebcalData(queryParams, new HebcalApiClient.ApiResponseCallback() {
            @Override
            public void onSuccess(JewishController.HebcalResponse response) {
                requireActivity().runOnUiThread(() -> {
                    List<JewishController.Item> upcomingHolidays = filterAndSortHolidays(response.getItems());
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
                if (eventDate.equals(today)) {
                    currentHolidays.add(item); // Сегодняшние праздники
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!eventDate.isBefore(today)) {
                        upcomingHolidays.add(item); // Будущие праздники
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