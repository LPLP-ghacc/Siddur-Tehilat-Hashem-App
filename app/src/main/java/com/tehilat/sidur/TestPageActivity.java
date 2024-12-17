package com.tehilat.sidur;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
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
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.test_page);

        WebView controller = (WebView) findViewById(R.id.simpleWebView);
        controller.getSettings().setJavaScriptEnabled(true);

        String filePath = getIntent().getStringExtra("filePath");
        String htmlContent = readFileFrom(filePath);
        boolean isDarkTheme = isDarkThemeActive();
        htmlContent = wrapHtmlContent(htmlContent, isDarkTheme);

        controller.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    @NonNull
    @Contract(pure = true)
    private String wrapHtmlContent(String originalHtml, boolean isDarkTheme) {

        String themeCss = isDarkTheme ? readFileFrom("pages/styles/dark.css") : readFileFrom("pages/styles/white.css");
        Log.i("PIP", themeCss);
        return "<html><head>" + themeCss + "</head>" + originalHtml + "</html>";
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
}


