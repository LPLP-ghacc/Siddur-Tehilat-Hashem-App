package com.tehilat.sidur;

import java.util.List;

public class JewishСontroller {
    public class HebcalResponse {
        private String title;
        private String date;
        private Location location;
        private Range range;
        private List<Item> items;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }

    public class Location {
        private String title;
        private String city;
        private String tzid;
        private double latitude;
        private double longitude;
        private String country;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getTzid() {
            return tzid;
        }

        public void setTzid(String tzid) {
            this.tzid = tzid;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    public class Range {
        private String start;
        private String end;

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }

    public class Item {
        private String title;
        private String date;
        private String category;
        private String hebrew;
        private String memo;
        private Leyning leyning;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getHebrew() {
            return hebrew;
        }

        public void setHebrew(String hebrew) {
            this.hebrew = hebrew;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        public Leyning getLeyning() {
            return leyning;
        }

        public void setLeyning(Leyning leyning) {
            this.leyning = leyning;
        }
    }

    public class Leyning {
        private String torah;
        private String haftarah;
        private String maftir;

        public String getTorah() {
            return torah;
        }

        public void setTorah(String torah) {
            this.torah = torah;
        }

        public String getHaftarah() {
            return haftarah;
        }

        public void setHaftarah(String haftarah) {
            this.haftarah = haftarah;
        }

        public String getMaftir() {
            return maftir;
        }

        public void setMaftir(String maftir) {
            this.maftir = maftir;
        }
    }
}
