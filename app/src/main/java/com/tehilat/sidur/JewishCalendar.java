package com.tehilat.sidur;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class JewishCalendar {

    private int day;
    private int month;
    private int year;

    static String[] jewishMonthNamesLeap = {"Nisan", "Iyar", "Sivan", "Tammuz",
            "Av", "Elul", "Tishri", "Heshvan",
            "Kislev", "Tevet", "Shevat", "Adar I", "Adar II"};

    static String[] jewishMonthNamesNonLeap = {"Nisan", "Iyar", "Sivan", "Tammuz",
            "Av", "Elul", "Tishri", "Heshvan",
            "Kislev", "Tevet", "Shevat", "Adar"};

    public JewishCalendar(int day, int month, int year) {
        this.day = day; this.month = month; this.year = year;
    }
    public JewishCalendar(JewishCalendar date) {
        this.day = date.getDay(); this.month = date.getMonth(); this.year = date.getYear();
    }

    public int getDay() { return day; }
    public int getMonth() { return month; }
    public int getYear() { return year; }

    public void setDay(int day) { this.day = day; }
    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }

    public boolean areDatesEqual(@NonNull JewishCalendar date) {
        if ((day == date.getDay()) &&
                (month == date.getMonth()) &&
                (year == date.getYear()))
            return true;
        else
            return false;
    }

    public static JewishCalendar gregorianToJewish(JewishCalendar date2Convert, @NonNull CalendarImpl i) {
        int absolute = i.absoluteFromGregorianDate(date2Convert);
        JewishCalendar dateJewish = i.jewishDateFromAbsolute(absolute);
        return dateJewish;
    }

    @NonNull
    public static Calendar jewishToGregorian(JewishCalendar date2Convert, @NonNull CalendarImpl i) {
        int absolute = i.absoluteFromJewishDate(date2Convert);
        JewishCalendar dateGregorian = i.gregorianDateFromAbsolute(absolute);
        Calendar result = new GregorianCalendar(dateGregorian.getYear(), dateGregorian.getMonth(), dateGregorian.getDay());

        return result;
    }

    private static boolean hebrewLeapYear(int year) {
        if ((((year*7)+1) % 19) < 7)
            return true;
        else
            return false;
    }

    public static String getJewishMonthName(int monthNumber, int year) {
        if(hebrewLeapYear(year)) {
            return jewishMonthNamesLeap[monthNumber-1];
        } else {
            return jewishMonthNamesNonLeap[monthNumber-1];
        }
    }

    public static String getJewishMonthName(@NonNull JewishCalendar calendar) {
        if(hebrewLeapYear(calendar.getYear())) {
            return jewishMonthNamesLeap[calendar.getMonth()-1];
        } else {
            return jewishMonthNamesNonLeap[calendar.getMonth()-1];
        }
    }

    public int getHashCode() {
        return (year - 1583) * 366 + month * 31 + day;
    }

    public String toString() {
        return day + "." + month + "." + year;
    }
}

class CalendarImpl {
    public static int getWeekday(int absDate) { return (absDate % 7); }

    private int month_list[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public int getLastDayOfGregorianMonth(int month, int year) {
        if ((month == 2) &&
                ((year % 4) == 0) &&
                ((year % 400) != 100) &&
                ((year % 400) != 200) &&
                ((year % 400) != 300))
            return 29;
        else
            return month_list[month-1];
    }

    public int absoluteFromGregorianDate(JewishCalendar date) {
        int value, m;

        value = date.getDay();

        for (m = 1; m < date.getMonth(); m++)
            value += getLastDayOfGregorianMonth(m, date.getYear());

        value += (365 * (date.getYear()-1));

        value += ((date.getYear()-1) / 4);

        value -= ((date.getYear()-1) / 100);

        value += ((date.getYear()-1) / 400);

        return (value);
    }

    public JewishCalendar gregorianDateFromAbsolute(int absDate) {
        int approx, y, m, day, month, year, temp;

        approx = absDate/366;

        y = approx;
        for (;;) {
            temp = absoluteFromGregorianDate(new JewishCalendar(1, 1, y+1));
            if (absDate < temp) break;
            y++;
        }
        year = y;

        m = 1;
        for (;;) {
            temp = absoluteFromGregorianDate(new JewishCalendar(getLastDayOfGregorianMonth(m, year), m, year));
            if (absDate <= temp) break;
            m++;
        }
        month = m;

        temp = absoluteFromGregorianDate(new JewishCalendar(1, month, year));
        day = absDate-temp+1;

        return new JewishCalendar(day, month, year);
    }

    public boolean hebrewLeapYear(int year) {
        if ((((year*7)+1) % 19) < 7)
            return true;
        else
            return false;
    }

    public int getLastMonthOfJewishYear(int year) {
        if (hebrewLeapYear(year))
            return 13;
        else
            return 12;
    }

    public int getLastDayOfJewishMonth(int month, int year) {
        if ((month == 2) ||
                (month == 4) ||
                (month == 6) ||
                (month == 10) ||
                (month == 13))
            return 29;
        if ((month == 12) && (!hebrewLeapYear(year)))
            return 29;
        if ((month == 8) && (!longHeshvan(year)))
            return 29;
        if ((month == 9) && (shortKislev(year)))
            return 29;
        return 30;
    }

    private int hebrewCalendarElapsedDays(int year) {
        int value, monthsElapsed, partsElapsed, hoursElapsed;
        int day, parts, alternativeDay;

        value = 235 * ((year-1) / 19);
        monthsElapsed = value;

        value = 12 * ((year-1) % 19);
        monthsElapsed += value;

        value = ((((year-1) % 19) * 7) + 1) / 19;
        monthsElapsed += value;

        partsElapsed = (((monthsElapsed % 1080) * 793) + 204);
        hoursElapsed = (5 +
                (monthsElapsed * 12) +
                ((monthsElapsed / 1080) * 793) +
                (partsElapsed / 1080));

        day = 1 + (29 * monthsElapsed) + (hoursElapsed/24);

        parts = ((hoursElapsed % 24) * 1080) +
                (partsElapsed % 1080);

        if ((parts >= 19440) ||

                (((day % 7) == 2) &&
                        (parts >= 9924)  &&
                        (!hebrewLeapYear(year))) ||

                (((day % 7) == 1) &&
                        (parts >= 16789) &&
                        (hebrewLeapYear(year-1))))
            alternativeDay = day+1;
        else
            alternativeDay = day;

        if (((alternativeDay % 7) == 0) ||
                ((alternativeDay % 7) == 3) ||
                ((alternativeDay % 7) == 5))
            alternativeDay++;

        return (alternativeDay);
    }

    private int daysInHebrewYear(int year) {
        return (hebrewCalendarElapsedDays(year+1) -
                hebrewCalendarElapsedDays(year));
    }

    private boolean longHeshvan(int year) {
        if ((daysInHebrewYear(year) % 10) == 5)
            return true;
        else
            return false;
    }

    private boolean shortKislev(int year) {
        if ((daysInHebrewYear(year) % 10) == 3)
            return true;
        else
            return false;
    }

    public int absoluteFromJewishDate(JewishCalendar date) {
        int value, returnValue, m;

        value = date.getDay();
        returnValue = value;

        if (date.getMonth() < 7) {
            for (m = 7; m <= getLastMonthOfJewishYear(date.getYear()); m++) {
                value = getLastDayOfJewishMonth(m, date.getYear());
                returnValue += value;
            }
            for (m = 1; m < date.getMonth(); m++) {
                value = getLastDayOfJewishMonth(m, date.getYear());
                returnValue += value;
            }
        } else {
            for (m = 7; m < date.getMonth(); m++) {
                value = getLastDayOfJewishMonth(m, date.getYear());
                returnValue += value;
            }
        }

        value = hebrewCalendarElapsedDays(date.getYear());
        returnValue += value;

        value = 1373429;
        returnValue -= value;

        return (returnValue);
    }

    public JewishCalendar jewishDateFromAbsolute(int absDate) {
        int approx, y, m, year, month, day, temp, start;

        approx = (absDate+1373429) / 366;

        y = approx;
        for (;;) {
            temp = absoluteFromJewishDate(new JewishCalendar(1, 7, y+1));
            if (absDate < temp) break;
            y++;
        }
        year = y;

        temp = absoluteFromJewishDate(new JewishCalendar(1, 1, year));
        if (absDate < temp)
            start = 7;
        else
            start = 1;

        m = start;
        for (;;) {
            temp = absoluteFromJewishDate(new JewishCalendar(getLastDayOfJewishMonth(m, year), m, year));
            if (absDate <= temp)
                break;
            m++;
        }
        month = m;

        temp = absoluteFromJewishDate(new JewishCalendar(1, month, year));
        day = absDate-temp+1;

        return new JewishCalendar(day, month, year);
    }
}
