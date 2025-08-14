package com.tehilat.sidur;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ViewerPageActivity extends AppCompatActivity {
    private int textZoom = 100;
    private WebView controller;
    private WebSettings webSettings;
    private SharedPreferences prefs;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.viewer_page);

        // Инициализация SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Применение темы из настроек
        applyTheme(prefs.getString("theme", "default"));

        // Инициализация WebView
        controller = findViewById(R.id.simpleWebView);
        controller.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webSettings = controller.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSavePassword(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Установка размера текста
        textZoom = prefs.getInt("text_size", 100);
        textZoom = Math.min(textZoom, 200); // Исправлено: синхронизируем с максимумом SeekBar
        textZoom = Math.max(textZoom, 50);
        webSettings.setTextZoom(textZoom);

        // Кнопка "Назад"
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Получение пути файла
        String filePath = getIntent().getStringExtra("filePath");
        Log.d("TestPage", "Received filePath: " + filePath);

        if (filePath == null) {
            controller.loadData("<h1>Error: No file path provided</h1>", "text/html", "UTF-8");
            return;
        }

        if(filePath.contains("test.html")){
            new Thread(() -> {
                String htmlContent = readFileFrom(filePath);
                htmlContent = replacePlaceholdersWithLocalizedStrings(htmlContent);
                boolean isDarkTheme = isDarkThemeActive();
                String finalContent = wrapHtmlContent(htmlContent, isDarkTheme);
                runOnUiThread(() -> {
                    controller.setVisibility(View.INVISIBLE);
                    controller.loadDataWithBaseURL("file:///android_asset/", finalContent, "text/html", "UTF-8", null);
                    controller.setVisibility(View.VISIBLE);
                });
            }).start();

            return;
        }

        // Асинхронная загрузка контента
        new Thread(() -> {
            String htmlContent = readFileFrom(filePath);
            Log.d("TestPage", "HTML content length: " + htmlContent.length());
            htmlContent = replacePlaceholdersWithLocalizedStrings(htmlContent);
            boolean isDarkTheme = isDarkThemeActive();
            String finalContent = wrapHtmlContent(htmlContent, isDarkTheme);
            Log.d("TestPage", "Final content length: " + finalContent.length());

            runOnUiThread(() -> {
                controller.setVisibility(View.INVISIBLE);
                controller.loadDataWithBaseURL("file:///android_asset/", finalContent, "text/html", "UTF-8", null);
                controller.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private void applyTheme(String theme) {
        switch (theme) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "white":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @NonNull @Contract(pure = true)
    private String wrapHtmlContent(String originalHtml, boolean isDarkTheme)
    {
        String themeCss = isDarkTheme ? readFileFrom("file:///android_asset/pages/styles/dark.css") : readFileFrom("file:///android_asset/pages/styles/white.css");
        return "<html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=2.0, user-scalable=yes\">"
                + themeCss + "</head>"
                // "<script src=\"file:///android_asset/pages/scripts/menu.js\"></script>"
                + originalHtml + "</html>";
    }

    private String replacePlaceholdersWithLocalizedStrings(String htmlContent) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{shaharit}}", getString(R.string.shararit));
        placeholders.put("{{maariv}}", getString(R.string.maariv));
        placeholders.put("{{korbanot}}", getString(R.string.korbanot));
        placeholders.put("{{mode_ani}}", getString(R.string.mode_ani));
        placeholders.put("{{tzitzit}}", getString(R.string.tzitzit));
        placeholders.put("{{talit}}", getString(R.string.talit));
        placeholders.put("{{mishna}}", getString(R.string.mishna));
        placeholders.put("{{baraita}}", getString(R.string.baraita));
        placeholders.put("{{kaddish_derabanan}}", getString(R.string.kaddish_derabanan));
        placeholders.put("{{hoidu}}", getString(R.string.hoidu));
        placeholders.put("{{psukei_dezimra}}", getString(R.string.psukei_dezimra));
        placeholders.put("{{barchu}}", getString(R.string.barchu));
        placeholders.put("{{shema}}", getString(R.string.shema));
        placeholders.put("{{amida}}", getString(R.string.amida));
        placeholders.put("{{tachanun}}", getString(R.string.tachanun));
        placeholders.put("{{avinu_malkeinu}}", getString(R.string.avinu_malkeinu));
        placeholders.put("{{hatzi_kaddish}}", getString(R.string.hatzi_kaddish));
        placeholders.put("{{readingTorah}}", getString(R.string.readingTorah));
        placeholders.put("{{raisingTorah}}", getString(R.string.raisingTorah));
        placeholders.put("{{ashrei}}", getString(R.string.ashrei));
        placeholders.put("{{beit_yaakob}}", getString(R.string.beit_yaakob));
        placeholders.put("{{psalmsoftheday}}", getString(R.string.psalmsoftheday));
        placeholders.put("{{hope_kave}}", getString(R.string.hope_kave));
        placeholders.put("{{aleinu}}", getString(R.string.aleinu));
        placeholders.put("{{kaddishyatom}}", getString(R.string.kaddishyatom));
        placeholders.put("{{rabeinutam}}", getString(R.string.rabeinutam));
        placeholders.put("{{sixmembers}}", getString(R.string.sixmembers));
        placeholders.put("{{kaddishshalem}}", getString(R.string.kaddishshalem));
        placeholders.put("{{mincha}}", getString(R.string.mincha));
        placeholders.put("{{vehu_rachum}}", getString(R.string.vehu_rachum));
        placeholders.put("{{kaddish}}", getString(R.string.kaddish));
        placeholders.put("{{birkathamazon}}", getString(R.string.birkathamazon));
        placeholders.put("{{britmila}}", getString(R.string.britmila));
        placeholders.put("{{shevaberachot}}", getString(R.string.shevaberachot));
        placeholders.put("{{tehilim}}", getString(R.string.tehilim));
        placeholders.put("{{psalm}}", getString(R.string.psalm));
        placeholders.put("{{hazan}}", getString(R.string.hazan));
        placeholders.put("{{community}}", getString(R.string.community));
        placeholders.put("{{travel}}", getString(R.string.travel));
        placeholders.put("{{kriat_shema}}", getString(R.string.kriat_shema));
        placeholders.put("{{avdala}}", getString(R.string.avdala));
        placeholders.put("{{mein_shalosh}}", getString(R.string.mein_shalosh));

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            htmlContent = htmlContent.replace(entry.getKey(), entry.getValue());
        }
        return htmlContent;
    }

    @NonNull
    private String readFileFrom(@NonNull String filePath) {
        StringBuilder builder = new StringBuilder();
        try {
            String assetFilePath = filePath.replace("file:///android_asset/", "");
            InputStream inputStream = getAssets().open(assetFilePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            Log.d("TestPage", "File read successfully, length: " + builder.length());
        } catch (IOException e) {
            Log.e("TestPage", "Error reading file: " + filePath + ", " + e.getMessage());
            builder.append("<h1>Error: Could not load file - ").append(e.getMessage()).append("</h1>");
        }
        return builder.toString();
    }

    private boolean isDarkThemeActive() {
        String theme = prefs.getString("theme", "default");
        if ("dark".equals(theme)) return true;
        if ("white".equals(theme)) return false;
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}