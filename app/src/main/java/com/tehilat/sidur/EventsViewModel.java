package com.tehilat.sidur;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

public class EventsViewModel extends ViewModel {

    private final MutableLiveData<List<JewishСontroller.Item>> eventsLiveData = new MutableLiveData<>();

    public LiveData<List<JewishСontroller.Item>> getEvents() {
        return eventsLiveData;
    }

    public void setEvents(List<JewishСontroller.Item> events) {
        eventsLiveData.setValue(events);
    }
}
