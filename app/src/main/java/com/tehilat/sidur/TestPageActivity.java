package com.tehilat.sidur;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestPageActivity extends AppCompatActivity
{
    private WebView controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.test_page);

        controller = (WebView) findViewById(R.id.simpleWebView);
        controller.getSettings().setJavaScriptEnabled(true);

        loadHTMLPageFrom("travel");
    }

    @NonNull
    private Page getPage(String fileName) throws IOException {
        String path = "file:///android_asset/" + fileName;
        List<String> lines = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            lines = Files.readAllLines(Paths.get(path));
        }

        Page result = new Page();

        for (String line : lines) {
            result.addContent(line);
        }

        return result;
    }

    private void setPage(Page page){

    }

    private void loadPageFrom(String fileName){
        String path = "file:///android_asset/" + fileName;
        controller.loadUrl(path);
    }

    private void loadHTMLPageFrom(String fileNameWithoutExtension){
        String path = "file:///android_asset/pages/" + fileNameWithoutExtension + ".html";
        controller.loadUrl(path);
    }

    public static class Page{
        private String content;

        public void setContent(String content){
            this.content = content;
        }

        public void addContent(String delta){
            this.content += content;
        }

        public String getContent(){
            return content;
        }
    }
}


