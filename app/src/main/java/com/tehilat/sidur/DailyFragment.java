package com.tehilat.sidur;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class DailyFragment extends Fragment {

    private ListView variousBlessingsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_daily, container, false);

        initListViews(rootView);

        return rootView;
    }

    private void initListViews(@NonNull View rootView) {
        variousBlessingsList = rootView.findViewById(R.id.variousBlessingsList);

        String[] variousBlessings = new String[]{
                getResources().getString(R.string.birkat_hamazon),
                getResources().getString(R.string.bedtime_shma),
                getResources().getString(R.string.brit_mila),
                getResources().getString(R.string.sheva_brachot),
                getResources().getString(R.string.travelers_prayer),
                getResources().getString(R.string.mein_shalosh),
                getResources().getString(R.string.blessings),
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                variousBlessings
        );

        variousBlessingsList.setAdapter(adapter);
        setListViewHeightBasedOnChildren(variousBlessingsList);
    }

    public static void setListViewHeightBasedOnChildren(@NonNull ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);


            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED
            );
            totalHeight += 150;
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}