package com.tehilat.sidur.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.tehilat.sidur.DonationActivity;
import com.tehilat.sidur.R;

import java.util.Locale;

public class SettingsFragment extends Fragment {
    private SharedPreferences prefs;

    // Массив доступных языков
    private static final String[] LANGUAGES = {"Русский", "Русский (транслит.)", "English", "עברית", "Français"};
    private static final String DEFAULT_LANGUAGE = "Русский"; // Язык по умолчанию, если системный язык не поддерживается

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Язык молитвы
        Spinner languageSpinner = rootView.findViewById(R.id.language_spinner);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, LANGUAGES);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);

        // Получаем текущий язык из настроек или определяем автоматически
        String currentLang = prefs.getString("prayer_language", null);
        if (currentLang == null) {
            // Если язык ещё не выбран, определяем системный язык
            currentLang = getSystemLanguage();
            // Сохраняем выбранный язык в настройки
            prefs.edit().putString("prayer_language", currentLang).apply();
        }
        languageSpinner.setSelection(langAdapter.getPosition(currentLang));

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString("prayer_language", LANGUAGES[position]).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // SeekBar для размера текста
        SeekBar textSizeSeekBar = rootView.findViewById(R.id.text_size_seekbar);
        textSizeSeekBar.setMax(200);
        textSizeSeekBar.setProgress(prefs.getInt("text_size", 100));
        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("text_size", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Тема
        Spinner themeSpinner = rootView.findViewById(R.id.theme_spinner);
        String[] themes = {"Светлая", "Темная", "По умолчанию"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, themes);
        themeSpinner.setAdapter(themeAdapter);
        String currentTheme = prefs.getString("theme", "По умолчанию");
        themeSpinner.setSelection(themeAdapter.getPosition(currentTheme));
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString("theme", themes[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Кнопка "Дать цдаку"
        Button donateButton = rootView.findViewById(R.id.donate_button);
        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DonationActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }

    // Метод для определения системного языка и сопоставления его с доступными языками
    private String getSystemLanguage() {
        // Получаем текущую локаль устройства
        String systemLanguage = Locale.getDefault().getLanguage();

        // Сопоставляем системный язык с доступными языками приложения
        switch (systemLanguage) {
            case "ru": // Русский
                return "Русский";
            case "en": // Английский
                return "English";
            case "he": // Иврит
                return "עברית";
            case "fr": // Французский
                return "Français";
            default:
                // Если системный язык не поддерживается, возвращаем язык по умолчанию
                return DEFAULT_LANGUAGE;
        }
    }
}