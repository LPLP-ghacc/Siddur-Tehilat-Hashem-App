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
import java.util.ArrayList;
import java.util.List;

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.HolidayViewHolder> {

    private List<JewishСontroller.Item> holidays;
    private static final int MAX_EVENTS = 20; // Максимум 20 событий

    public HolidayAdapter(List<JewishСontroller.Item> holidays) {
        // Ограничиваем список до 20 элементов
        this.holidays = new ArrayList<>();
        if (holidays != null) {
            this.holidays.addAll(holidays.subList(0, Math.min(holidays.size(), MAX_EVENTS)));
        }
    }

    public void updateHolidays(List<JewishСontroller.Item> newHolidays) {
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
        JewishСontroller.Item holiday = holidays.get(position);

        // Устанавливаем заголовок и текст на иврите (с проверкой на null)
        holder.titleTextView.setText(holiday.getTitle() != null ? holiday.getTitle() : "");
        holder.hebrewTextView.setText(holiday.getHebrew() != null ? holiday.getHebrew() : "");

        // Обрабатываем дату
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Парсим дату события
                LocalDate eventDate = LocalDate.parse(holiday.getDate().substring(0, 10));
                LocalDate today = LocalDate.now();

                // Форматируем дату события
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
                holder.dateTextView.setText(eventDate.format(formatter));

                // Проверяем, является ли событие текущим
                if (eventDate.equals(today)) {
                    holder.itemView.setBackgroundColor(0xFFFFF3E0); // Светло-оранжевый фон
                    holder.titleTextView.setTextColor(0xFFE65100); // Оранжевый текст
                    holder.dateTextView.setText("Сегодня");
                    holder.dateTextView.setTextColor(0xFFE65100); // Оранжевый текст
                } else {
                    holder.itemView.setBackgroundColor(0xFFFFFFFF); // Белый фон
                    holder.titleTextView.setTextColor(0xFF000000); // Черный текст
                    holder.dateTextView.setTextColor(0xFF666666); // Серый текст
                }
            } catch (Exception e) {
                // Если не удалось распарсить дату, показываем заглушку
                holder.dateTextView.setText("Дата неизвестна");
                holder.itemView.setBackgroundColor(0xFFFFFFFF); // Белый фон
                holder.titleTextView.setTextColor(0xFF000000); // Черный текст
                holder.dateTextView.setTextColor(0xFF666666); // Серый текст
            }
        } else {
            // Для старых версий Android показываем дату без форматирования
            holder.dateTextView.setText(holiday.getDate() != null ? holiday.getDate() : "Дата неизвестна");
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // Белый фон
            holder.titleTextView.setTextColor(0xFF000000); // Черный текст
            holder.dateTextView.setTextColor(0xFF666666); // Серый текст
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