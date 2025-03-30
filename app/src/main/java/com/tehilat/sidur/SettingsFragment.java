package com.tehilat.sidur;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends Fragment {
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Язык молитвы
        Spinner languageSpinner = rootView.findViewById(R.id.language_spinner);
        String[] languages = {"Русский", "Русский (транслит.)", "English", "עברית", "Français"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        String currentLang = prefs.getString("prayer_language", "Русский");
        languageSpinner.setSelection(langAdapter.getPosition(currentLang));
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString("prayer_language", languages[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

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

        return rootView;
    }
}