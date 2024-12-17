package com.tehilat.sidur;

import java.util.Calendar;

public class JewishHolidayHelper {

    // Массив праздников в формате {месяц, день, название}
    private static final Holiday[] holidays = {
            new Holiday(1, 1, "Rosh Hashanah"),
            new Holiday(1, 10, "Yom Kippur"),
            new Holiday(1, 15, "Sukkot"),
            new Holiday(3, 25, "Chanukah"),
            new Holiday(7, 15, "Pesach"),
            new Holiday(5, 6, "Shavuot"),
            new Holiday(12, 14, "Purim")
    };

    // Метод: проверка, есть ли сегодня праздник, и возвращение его названия
    public static String getCurrentHoliday(int currentMonth, int currentDay) {
        for (Holiday holiday : holidays) {
            if (holiday.month == currentMonth && holiday.day == currentDay) {
                return holiday.name;
            }
        }
        return "nohol";
    }

    // Метод: сколько дней до следующего праздника и его название
    public static NextHolidayInfo daysUntilNextHoliday(int currentMonth, int currentDay) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.MONTH, currentMonth - 1); // Java-календари используют индексацию месяцев с 0
        today.set(Calendar.DAY_OF_MONTH, currentDay);

        String nextHolidayName = "";
        int minDays = Integer.MAX_VALUE;

        for (Holiday holiday : holidays) {
            Calendar holidayDate = Calendar.getInstance();
            holidayDate.set(Calendar.MONTH, holiday.month - 1); // Приведение к индексации Java
            holidayDate.set(Calendar.DAY_OF_MONTH, holiday.day);

            // Если праздник уже прошёл в этом году, переносим его на следующий год
            if (holidayDate.before(today)) {
                holidayDate.add(Calendar.YEAR, 1);
            }

            // Вычисляем разницу в днях
            long diff = (holidayDate.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);
            if (diff < minDays) {
                minDays = (int) diff;
                nextHolidayName = holiday.name;
            }
        }

        return new NextHolidayInfo(nextHolidayName, minDays);
    }

    // Класс для представления ближайшего праздника
    public static class NextHolidayInfo {
        String name;
        int daysUntil;

        public NextHolidayInfo(String name, int daysUntil) {
            this.name = name;
            this.daysUntil = daysUntil;
        }

        @Override
        public String toString() {
            return "Next holiday: " + name + ", Days until: " + daysUntil;
        }
    }

    // Вспомогательный класс для представления праздника
    private static class Holiday {
        int month;
        int day;
        String name;

        Holiday(int month, int day, String name) {
            this.month = month;
            this.day = day;
            this.name = name;
        }
    }
}
