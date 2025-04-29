package com.tehilat.sidur.adapters;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tehilat.sidur.calendar.JewishController;
import com.tehilat.sidur.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.HolidayViewHolder> {

    private List<JewishController.Item> holidays;
    private static final int MAX_EVENTS = 20; // Максимум 20 событий

    public HolidayAdapter(List<JewishController.Item> holidays) {
        this.holidays = new ArrayList<>();
        if (holidays != null) {
            this.holidays.addAll(holidays.subList(0, Math.min(holidays.size(), MAX_EVENTS)));
        }
    }

    public void updateHolidays(List<JewishController.Item> newHolidays) {
        this.holidays.clear();
        if (newHolidays != null) {
            this.holidays.addAll(newHolidays.subList(0, Math.min(newHolidays.size(), MAX_EVENTS)));
        }
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
        JewishController.Item holiday = holidays.get(position);

        // Устанавливаем заголовок и текст на иврите (с проверкой на null)
        holder.titleTextView.setText(holiday.getTitle() != null ? holiday.getTitle() : "");
        holder.hebrewTextView.setText(holiday.getHebrew() != null ? holiday.getHebrew() : "");

        // Получаем контекст для доступа к ресурсам
        Context context = holder.itemView.getContext();

        // Обрабатываем дату
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Парсим дату события
                LocalDate eventDate = LocalDate.parse(holiday.getDate().substring(0, 10));
                LocalDate today = LocalDate.now();

                // Форматируем дату события
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("ru"));
                holder.dateTextView.setText(eventDate.format(formatter));

                // Проверяем, является ли событие текущим
                if (eventDate.equals(today)) {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.bg3));
                    holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.text1));
                    holder.dateTextView.setText("Сегодня");
                    holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.text1));
                } else {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.bg1));
                    holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.text4));
                    holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.text4));
                }
            } catch (Exception e) {
                // Если не удалось распарсить дату, показываем заглушку
                holder.dateTextView.setText("Дата неизвестна");
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.bg1));
                holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.text4));
                holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.text4));
            }
        } else {
            // Для старых версий Android показываем дату без форматирования
            holder.dateTextView.setText(holiday.getDate() != null ? holiday.getDate() : "Дата неизвестна");
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.bg1));
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.text4));
            holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.text4));
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