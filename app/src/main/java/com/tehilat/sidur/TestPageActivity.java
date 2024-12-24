package com.tehilat.sidur;

import android.annotation.SuppressLint;
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

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestPageActivity extends AppCompatActivity
{
    private ScaleGestureDetector scaleGestureDetector;
    private int textZoom = 100; // Начальный размер текста (100%)
    private WebView controller;
    private WebSettings webSettings;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.test_page);

        // Инит для веб контроллера
        controller = (WebView) findViewById(R.id.simpleWebView);
        // Аппаратное ускорение
        controller.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webSettings = controller.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);

        // Кэширование контента
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // отключаем ненужные настройки
        webSettings.setDomStorageEnabled(true); // только если нужно
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSavePassword(false);

        // Установка начального размера текста (поддерживается начиная с API 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            webSettings.setTextZoom(textZoom);
        }

        // Включение зума - не требуется
        //webSettings.setBuiltInZoomControls(true);
        //webSettings.setDisplayZoomControls(true);
        //webSettings.setSupportZoom(true);
        //controller.setInitialScale(100);

        // Получение пути файла через Extra String из прошлой страницы
        String filePath = getIntent().getStringExtra("filePath");

        // Асинхронное чтение, обработка и загрузка
        new Thread(() -> {
            String htmlContent = readFileFrom(filePath);
            htmlContent = replacePlaceholdersWithLocalizedStrings(htmlContent);
            boolean isDarkTheme = isDarkThemeActive();
            String finalContent = wrapHtmlContent(htmlContent, isDarkTheme);

            // загрузка контента в webview (веб контроллер)
            runOnUiThread(() -> controller.setVisibility(View.INVISIBLE));
            runOnUiThread(() -> controller.loadDataWithBaseURL(null, finalContent, "text/html", "UTF-8", null));
            runOnUiThread(() -> controller.setVisibility(View.VISIBLE));
        }).start();

        // Обработка жестов
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    @NonNull
    @Contract(pure = true)
    private String wrapHtmlContent(String originalHtml, boolean isDarkTheme) {
        String themeCss = isDarkTheme ? readFileFrom("pages/styles/dark.css") : readFileFrom("pages/styles/white.css");
        return "<html><head>" + themeCss + "</head>" + originalHtml + "</html>";
    }

    private String replacePlaceholdersWithLocalizedStrings(String htmlContent) {
        // Заменяем плейсхолдеры локализованными строками
        htmlContent = htmlContent.replace("{{shaharit}}", getString(R.string.shararit));
        htmlContent = htmlContent.replace("{{maariv}}", getString(R.string.maariv));
        htmlContent = htmlContent.replace("{{korbanot}}", getString(R.string.korbanot));
        htmlContent = htmlContent.replace("{{mode_ani}}", getString(R.string.mode_ani));
        htmlContent = htmlContent.replace("{{tzitzit}}", getString(R.string.tzitzit));
        htmlContent = htmlContent.replace("{{talit}}", getString(R.string.talit));
        htmlContent = htmlContent.replace("{{mishna}}", getString(R.string.mishna));
        htmlContent = htmlContent.replace("{{baraita}}", getString(R.string.baraita));
        htmlContent = htmlContent.replace("{{kaddish_derabanan}}", getString(R.string.kaddish_derabanan));
        htmlContent = htmlContent.replace("{{hoidu}}", getString(R.string.hoidu));
        htmlContent = htmlContent.replace("{{psukei_dezimra}}", getString(R.string.psukei_dezimra));
        htmlContent = htmlContent.replace("{{barchu}}", getString(R.string.barchu));
        htmlContent = htmlContent.replace("{{shema}}", getString(R.string.shema));
        htmlContent = htmlContent.replace("{{amida}}", getString(R.string.amida));
        htmlContent = htmlContent.replace("{{tachanun}}", getString(R.string.tachanun));
        htmlContent = htmlContent.replace("{{avinu_malkeinu}}", getString(R.string.avinu_malkeinu));
        htmlContent = htmlContent.replace("{{hatzi_kaddish}}", getString(R.string.hatzi_kaddish));
        htmlContent = htmlContent.replace("{{readingTorah}}", getString(R.string.readingTorah));
        htmlContent = htmlContent.replace("{{raisingTorah}}", getString(R.string.raisingTorah));
        htmlContent = htmlContent.replace("{{ashrei}}", getString(R.string.ashrei));
        htmlContent = htmlContent.replace("{{beit_yaakob}}", getString(R.string.beit_yaakob));
        htmlContent = htmlContent.replace("{{psalmsoftheday}}", getString(R.string.psalmsoftheday));
        htmlContent = htmlContent.replace("{{hope_kave}}", getString(R.string.hope_kave));
        htmlContent = htmlContent.replace("{{kaddish_derabanan}}", getString(R.string.kaddish_derabanan));
        htmlContent = htmlContent.replace("{{aleinu}}", getString(R.string.aleinu));
        htmlContent = htmlContent.replace("{{kaddishyatom}}", getString(R.string.kaddishyatom));
        htmlContent = htmlContent.replace("{{rabeinutam}}", getString(R.string.rabeinutam));
        htmlContent = htmlContent.replace("{{sixmembers}}", getString(R.string.sixmembers));
        htmlContent = htmlContent.replace("{{kaddishshalem}}", getString(R.string.kaddishshalem));
        htmlContent = htmlContent.replace("{{mincha}}", getString(R.string.mincha));
        htmlContent = htmlContent.replace("{{vehu_rachum}}", getString(R.string.vehu_rachum));
        htmlContent = htmlContent.replace("{{kaddish}}", getString(R.string.kaddish));
        htmlContent = htmlContent.replace("{{birkathamazon}}", getString(R.string.birkathamazon));

        return htmlContent;
    }

    @NonNull
    private String readFileFrom(@NonNull String filePath) {
        StringBuilder builder = new StringBuilder();

        try {
            String assetFilePath = filePath.replace("file:///android_asset/", "");

            InputStream inputStream = getAssets().open(assetFilePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    private boolean isDarkThemeActive() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Обработка события жеста для масштабирования текста
        scaleGestureDetector.onTouchEvent(event);
        // Передача события WebView для корректного поведения
        controller.dispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor(); // Коэффициент масштабирования

            // Изменение размера текста
            if (scaleFactor > 1) { // Увеличение
                textZoom += 1;
            } else if (scaleFactor < 1) { // Уменьшение
                textZoom -= 1;
            }

            // Ограничиваем диапазон
            textZoom = Math.max(80, Math.min(textZoom, 105));

            // Применяем новый размер текста
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                webSettings.setTextZoom(textZoom);
            } else {
                // Для старых версий Android (до API 14)
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
}


