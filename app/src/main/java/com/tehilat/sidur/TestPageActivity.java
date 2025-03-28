package com.tehilat.sidur;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TestPageActivity extends AppCompatActivity {
    private ScaleGestureDetector scaleGestureDetector;
    private int textZoom = 100; // Начальный размер текста (100%)
    private WebView controller;
    private WebSettings webSettings;
    private SharedPreferences prefs;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.test_page);

        // Инициализация SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Инициализация WebView
        controller = findViewById(R.id.simpleWebView);
        controller.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webSettings = controller.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSavePassword(false);

        // Установка размера текста из настроек
        textZoom = prefs.getInt("text_size", 100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            webSettings.setTextZoom(textZoom);
        }

        // Получение пути файла
        String filePath = getIntent().getStringExtra("filePath");
        Log.d("TestPage", "Received filePath: " + filePath);

        if (filePath == null) {
            controller.loadData("<h1>Error: No file path provided</h1>", "text/html", "UTF-8");
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

        // Обработка жестов (если нужно)
        // scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    @NonNull
    @Contract(pure = true)
    private String wrapHtmlContent(String originalHtml, boolean isDarkTheme) {
        String themeCss = isDarkTheme ? readFileFrom("file:///android_asset/pages/styles/dark.css") : readFileFrom("file:///android_asset/pages/styles/white.css");
        return "<html><head>" + themeCss + "</head>" + originalHtml + "</html>";
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
            Log.d("TestPage", "Trying to read file: " + assetFilePath);
            InputStream inputStream = getAssets().open(assetFilePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            Log.d("TestPage", "File read successfully, length: " + builder.length());
        } catch (IOException e) {
            Log.e("TestPage", "Error reading file: " + filePath + ", " + e.getMessage());
            builder.append("<h1>Error: Could not load file - " + e.getMessage() + "</h1>");
        }
        return builder.toString();
    }

    private boolean isDarkThemeActive() {
        String theme = prefs.getString("theme", "По умолчанию");
        if ("Темная".equals(theme)) return true;
        if ("Светлая".equals(theme)) return false;
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    // Раскомментируй, если хочешь использовать масштабирование жестами
    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        controller.dispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            if (scaleFactor > 1) {
                textZoom += 1;
            } else if (scaleFactor < 1) {
                textZoom -= 1;
            }
            textZoom = Math.max(80, Math.min(textZoom, 105));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                webSettings.setTextZoom(textZoom);
            } else {
                if (textZoom <= 75) {
                    webSettings.setTextSize(WebSettings.TextSize.SMALLER);
                } else if (textZoom <= 125) {
                    webSettings.setTextSize(WebSettings.TextSize.NORMAL);
                } else if (textZoom <= 150) {
                    webSettings.setTextSize(WebSettings.TextSize.LARGER);
                } else {
                    webSettings.setTextSize(WebSettings.TextSize.LARGEST);
                }
            }
            return true;
        }
    }
    */
}