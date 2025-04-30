package com.tehilat.sidur.adapters;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.tehilat.sidur.R;
import com.tehilat.sidur.calendar.JewishController;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<JewishController.Item> events;

    public EventsAdapter(List<JewishController.Item> events) {
        this.events = events != null ? events : new ArrayList<>();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEvents(List<JewishController.Item> newEvents) {
        this.events = newEvents != null ? newEvents : new ArrayList<>();
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
        JewishController.Item event = events.get(position);
        holder.titleTextView.setText(event.getTitle());

        // Форматируем дату
        LocalDate eventDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            eventDate = LocalDate.parse(event.getDate().substring(0, 10));
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