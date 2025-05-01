package com.tehilat.sidur.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tehilat.sidur.calendar.JewishController;

import java.util.ArrayList;
import java.util.List;

public class EventsViewModel extends ViewModel {

    private final MutableLiveData<List<JewishController.Item>> events = new MutableLiveData<>();
    private final MutableLiveData<List<JewishController.Item>> holidays = new MutableLiveData<>();

    public EventsViewModel() {
        // Инициализируем пустыми списками
        events.setValue(new ArrayList<>());
        holidays.setValue(new ArrayList<>());
    }

    public LiveData<List<JewishController.Item>> getEvents() {
        return events;
    }

    public void setEvents(List<JewishController.Item> events) {
        this.events.setValue(events);
    }

    public LiveData<List<JewishController.Item>> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<JewishController.Item> holidays) {
        this.holidays.setValue(holidays);
    }
}