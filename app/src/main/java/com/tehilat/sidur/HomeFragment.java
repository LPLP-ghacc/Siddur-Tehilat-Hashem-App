package com.tehilat.sidur;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private ListView morningList;
    private ListView minhaList;
    private ListView maarivList;

    private TextView jewishCalendar;
    private TextView gregorianCalendar;
    private TextView holidayTextField;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Set da jewish date to TextView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();

            int day = now.getDayOfMonth();
            int month = now.getMonthValue();
            int year = now.getYear();

            CalendarImpl impl = new CalendarImpl();

            JewishCalendar jc = JewishCalendar.gregorianToJewish(new JewishCalendar(day, month, year), impl);
            jewishCalendar = rootView.findViewById(R.id.jewishdate);
            String jewishDateString = jc.getDay() + " " + JewishCalendar.getJewishMonthName(jc) + " " + jc.getYear();
            jewishCalendar.setText(jewishDateString);

            String currentHoliday = JewishHolidayHelper.getCurrentHoliday(jc.getMonth(), jc.getDay());

            if(Objects.equals(currentHoliday, getResources().getString(R.string.noholidays))){
                currentHoliday = getResources().getString(R.string.noholidays);
            }

            JewishHolidayHelper.NextHolidayInfo daysUntilHoliday = JewishHolidayHelper.daysUntilNextHoliday(jc.getMonth(), jc.getDay());
            holidayTextField = rootView.findViewById(R.id.holidaycalendar);

            holidayTextField.setText(getResources().getString(R.string.todayis) + " " + currentHoliday +", " + getResources().getString(R.string.in) + " " + daysUntilHoliday.daysUntil + " " +
                    getResources().getString(R.string.days) + " " + daysUntilHoliday.name);
        }

        gregorianCalendar = rootView.findViewById(R.id.gregoriandate);

        // Set da gregorian date to TextView
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        String formattedDate = sdf.format(currentDate);
        gregorianCalendar.setText(formattedDate);

        initListViews(rootView);

        return rootView;
    }

    private void initListViews(@NonNull View rootView) {
        morningList = rootView.findViewById(R.id.morningList);

        String[] morningPrayers = new String[]{
                getResources().getString(R.string.morning_Shararit),
                getResources().getString(R.string.mincha)
        };

        ArrayAdapter<String> adapterShahar = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                morningPrayers
        );

        morningList.setAdapter(adapterShahar);

        morningList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), TestPageActivity.class);

                String filePath = getMorningPrayerFilePath(position);
                intent.putExtra("filePath", filePath);

                startActivity(intent);
            }
        });

        setListViewHeightBasedOnChildren(morningList);

        minhaList = rootView.findViewById(R.id.minhaList);

        String[] minhaPrayers = new String[]{
                getResources().getString(R.string.mincha),
        };

        ArrayAdapter<String> adapterMinha = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                minhaPrayers
        );

        minhaList.setAdapter(adapterMinha);

        minhaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), TestPageActivity.class);

                String filePath = getDayPrayerFilePath(position);
                intent.putExtra("filePath", filePath);

                startActivity(intent);
            }
        });

        setListViewHeightBasedOnChildren(minhaList);

        maarivList = rootView.findViewById(R.id.maarivList);

        String[] maarivPrayers = new String[]{
                getResources().getString(R.string.maariv_vehu_rachum),
                getResources().getString(R.string.maariv_amida),
                getResources().getString(R.string.maariv_aleinu),
        };

        ArrayAdapter<String> adapterMaariv = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                maarivPrayers
        );

        maarivList.setAdapter(adapterMaariv);
        setListViewHeightBasedOnChildren(maarivList);
    }

    private String getMorningPrayerFilePath(int position) {
        switch (position) {
            case 0:
                return "file:///android_asset/pages/3 Shararit.html";
            case 1:
                return "file:///android_asset/pages/Minha.html";
            default:
                return "file:///android_asset/default.html";
        }
    }

    private String getDayPrayerFilePath(int position) {
        switch (position) {
            case 0:
                return "file:///android_asset/pages/Minha.html";
            default:
                return "file:///android_asset/default.html";
        }
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