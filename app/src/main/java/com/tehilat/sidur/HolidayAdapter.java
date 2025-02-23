package com.tehilat.sidur;

import android.graphics.Color;
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

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.HolidayViewHolder> {

    private List<JewishСontroller.Item> holidays;

    public HolidayAdapter(List<JewishСontroller.Item> holidays) {
        this.holidays = holidays;
    }

    public void updateHolidays(List<JewishСontroller.Item> newHolidays) {
        this.holidays = newHolidays;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HolidayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_holiday, parent, false);
        return new HolidayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolidayViewHolder holder, int position) {
        JewishСontroller.Item holiday = holidays.get(position);
        LocalDate eventDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            eventDate = LocalDate.parse(holiday.getDate().substring(0, 10));
        }
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now();
        }

        holder.titleTextView.setText(holiday.getTitle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.dateTextView.setText(eventDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        }
        holder.hebrewTextView.setText(holiday.getHebrew());

        // Выделяем текущий праздник
        if (eventDate.equals(today)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0")); // Светло-оранжевый фон
            holder.titleTextView.setTextColor(Color.parseColor("#E65100")); // Оранжевый текст
            holder.dateTextView.setText("Сегодня");
            holder.dateTextView.setTextColor(Color.parseColor("#E65100"));
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE); // Обычный фон
            holder.titleTextView.setTextColor(Color.BLACK);
            holder.dateTextView.setTextColor(Color.parseColor("#666666"));
        }
    }

    @Override
    public int getItemCount() {
        return holidays.size();
    }

    static class HolidayViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, dateTextView, hebrewTextView;

        HolidayViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.holiday_title);
            dateTextView = itemView.findViewById(R.id.holiday_date);
            hebrewTextView = itemView.findViewById(R.id.holiday_hebrew);
        }
    }
}