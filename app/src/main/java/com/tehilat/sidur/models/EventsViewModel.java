package com.tehilat.sidur.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tehilat.sidur.calendar.JewishController;

import java.util.List;

public class EventsViewModel extends ViewModel {

    private final MutableLiveData<List<JewishController.Item>> eventsLiveData = new MutableLiveData<>();

    public LiveData<List<JewishController.Item>> getEvents() {
        return eventsLiveData;
    }

    public void setEvents(List<JewishController.Item> events) {
        eventsLiveData.setValue(events);
    }
}

