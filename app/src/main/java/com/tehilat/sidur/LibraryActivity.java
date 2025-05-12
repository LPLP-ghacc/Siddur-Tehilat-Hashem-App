package com.tehilat.sidur;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

public class LibraryActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private RequestQueue requestQueue;
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        requestQueue = Volley.newRequestQueue(this);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        fetchKitsurText();
    }

    private void fetchKitsurText() {
        String lang = prefs.getString("prayer_language", "עברית");
        String langCode = getLangCode(lang);

        progressBar.setVisibility(ProgressBar.VISIBLE);

        // Русский и французский не поддерживаются на Sefaria
        if (langCode.equals("ru") || langCode.equals("ru_tr") || langCode.equals("fr")) {
            displayText("<html><body><h1>Текст недоступен</h1><p>Китцур Шулхан Арух на " + lang + " не найден на Sefaria.</p></body></html>");
            progressBar.setVisibility(ProgressBar.GONE);
            return;
        }

        String url = "https://www.sefaria.org/api/v3/texts/Kitzur_Shulchan_Arukh.1.1?lang=" + langCode;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String text = extractText(response, langCode);
                        String htmlContent = "<html><body><h1>Китцур Шулхан Арух 1:1</h1>" + text + "</body></html>";
                        displayText(htmlContent);
                    } catch (Exception e) {
                        Log.e("API Error", "Ошибка обработки ответа: " + e.getMessage());
                        displayText("<html><body><h1>Ошибка</h1><p>Не удалось загрузить текст.</p></body></html>");
                    }
                    progressBar.setVisibility(ProgressBar.GONE);
                },
                error -> {
                    Log.e("API Error", "Ошибка запроса: " + error.getMessage());
                    displayText("<html><body><h1>Ошибка</h1><p>Не удалось загрузить текст.</p></body></html>");
                    progressBar.setVisibility(ProgressBar.GONE);
                });

        requestQueue.add(request);
    }

    private String extractText(JSONObject response, String langCode) {
        try {
            String key = langCode.equals("he") ? "he" : "en";
            JSONArray textArray = response.getJSONArray(key);
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < textArray.length(); i++) {
                text.append("<p>").append(textArray.getString(i)).append("</p>");
            }
            return text.toString();
        }
        catch (Exception e) {
            return "<p>Ошибка извлечения текста</p>";
        }
    }

    private void displayText(String htmlContent) {
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    private String getLangCode(String lang) {
        switch (lang) {
            case "English": return "en";
            case "עברית": return "he";
            case "Français": return "fr";
            case "Русский (транслит.)": return "ru_tr";
            case "Русский": default: return "ru";
        }
    }
}
