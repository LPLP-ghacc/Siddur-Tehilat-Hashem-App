package com.tehilat.sidur.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.tehilat.sidur.DonationActivity;
import com.tehilat.sidur.R;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static final String DEFAULT_LANGUAGE = "English";
    private static final String[] LANGUAGES = {"Русский", "English", "עברית"};
    private static final String[] THEME_KEYS = {"white", "dark", "default"};
    private android.content.SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Язык молитвы
        Spinner languageSpinner = rootView.findViewById(R.id.language_spinner);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, LANGUAGES);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);

        String currentLang = prefs.getString("prayer_language", null);
        if (currentLang == null) {
            currentLang = getSystemLanguage();
            prefs.edit().putString("prayer_language", currentLang).apply();
        }
        languageSpinner.setSelection(langAdapter.getPosition(currentLang));

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString("prayer_language", LANGUAGES[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
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

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Тема
        Spinner themeSpinner = rootView.findViewById(R.id.theme_spinner);
        String[] themeNames = {
                getString(R.string.white_theme),
                getString(R.string.dark_theme),
                getString(R.string.default_theme)
        };
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, themeNames);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);

        String currentTheme = prefs.getString("theme", "default");
        int themePosition = getThemePosition(currentTheme);
        themeSpinner.setSelection(themePosition);

        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme = THEME_KEYS[position];
                if (!selectedTheme.equals(prefs.getString("theme", "default"))) {
                    prefs.edit().putString("theme", selectedTheme).apply();
                    applyTheme(selectedTheme);
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Кнопка "Дать цдаку"
        Button donateButton = rootView.findViewById(R.id.donate_button);
        donateButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DonationActivity.class);
            startActivity(intent);
        });

        return rootView;
    }

    private String getSystemLanguage() {
        String systemLanguage = Locale.getDefault().getLanguage();
        switch (systemLanguage) {
            case "ru":
                return "Русский";
            case "en":
                return "English";
            case "he":
                return "עברית";
            default:
                return DEFAULT_LANGUAGE;
        }
    }

    private void applyTheme(String theme) {
        switch (theme) {
            case "white":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                Log.w(TAG, "Unknown theme: " + theme + ", falling back to default");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private int getThemePosition(String themeKey) {
        for (int i = 0; i < THEME_KEYS.length; i++) {
            if (THEME_KEYS[i].equals(themeKey)) {
                return i;
            }
        }
        return 2; // Default theme position
    }
}