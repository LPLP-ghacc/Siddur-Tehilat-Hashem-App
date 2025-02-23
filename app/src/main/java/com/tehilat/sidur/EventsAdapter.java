package com.tehilat.sidur;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<JewishСontroller.Item> events;

    public EventsAdapter(List<JewishСontroller.Item> events) {
        this.events = events != null ? events : new java.util.ArrayList<>();
    }

    public void setEvents(List<JewishСontroller.Item> newEvents) {
        this.events = newEvents != null ? newEvents : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        JewishСontroller.Item event = events.get(position);
        holder.titleTextView.setText(event.getTitle());

        // Форматируем дату
        LocalDate eventDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            eventDate = LocalDate.parse(event.getDate().substring(0, 10));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.dateTextView.setText(eventDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        }

        // Показываем еврейское название, если есть
        if (event.getHebrew() != null && !event.getHebrew().isEmpty()) {
            holder.hebrewTextView.setText(event.getHebrew());
            holder.hebrewTextView.setVisibility(View.VISIBLE);
        } else {
            holder.hebrewTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, dateTextView, hebrewTextView;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.event_title);
            dateTextView = itemView.findViewById(R.id.event_date);
            hebrewTextView = itemView.findViewById(R.id.event_hebrew);
        }
    }
}