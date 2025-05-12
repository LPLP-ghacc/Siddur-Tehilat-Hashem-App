package com.tehilat.sidur.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.tehilat.sidur.LibraryActivity;
import com.tehilat.sidur.R;
import com.tehilat.sidur.ViewerPageActivity;
import org.jetbrains.annotations.Contract;

public class AllPrayersFragment extends Fragment {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_all_prayers, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        initListViews(rootView);
        initKitsurButton(rootView);

        return rootView;
    }

    private void initKitsurButton(@NonNull View rootView) {
        Button kitsurButton = rootView.findViewById(R.id.kitsurButton);
        kitsurButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LibraryActivity.class);
            startActivity(intent);
        });
    }

    private void initListViews(@NonNull View rootView) {
        ListView allPrayersList = rootView.findViewById(R.id.allList);

        String[] prayers = {
                getResources().getString(R.string.travel),
                getResources().getString(R.string.mein_shalosh),
                getResources().getString(R.string.nasi),
                getResources().getString(R.string.avdala)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, prayers);
        allPrayersList.setAdapter(adapter);

        allPrayersList.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getActivity(), ViewerPageActivity.class);
            intent.putExtra("filePath", getPrayerFilePath(position));
            startActivity(intent);
        });

        setListViewHeightBasedOnChildren(allPrayersList);
    }

    @NonNull
    @Contract(pure = true)
    private String getPrayerFilePath(int position) {
        String lang = prefs.getString("prayer_language", "עברית");
        String langCode;

        switch (lang) {
            case "English": langCode = "en"; break;
            case "עברית": langCode = "he"; break;
            case "Français": langCode = "fr"; break;
            case "Русский (транслит.)": langCode = "ru_tr"; break;
            case "Русский": default: langCode = "ru"; break;
        }

        String filePath;
        switch (position) {
            case 0: filePath = "file:///android_asset/pages/" + langCode + "/Travel.html"; break;
            case 1: filePath = "file:///android_asset/pages/" + langCode + "/MeinShalosh.html"; break;
            case 2: filePath = "file:///android_asset/pages/" + langCode + "/Nasi.html"; break;
            case 3: filePath = "file:///android_asset/pages/" + langCode + "/Avdala.html"; break;
            default: filePath = "file:///android_asset/default.html"; break;
        }
        Log.d("FilePath", "Loading file: " + filePath);
        return filePath;
    }

    public static void setListViewHeightBasedOnChildren(@NonNull ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            totalHeight += 150;
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}