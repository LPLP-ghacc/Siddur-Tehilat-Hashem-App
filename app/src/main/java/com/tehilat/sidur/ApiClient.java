package com.tehilat.sidur;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://www.hebcal.com/hebcal";

    public interface ApiResponseCallback {
        void onSuccess(JewishСontroller.HebcalResponse response);
        void onError(String errorMessage);
    }

    public static void fetchHebcalData(String queryParams, ApiResponseCallback callback) {
        OkHttpClient client = new OkHttpClient();

        String url = BASE_URL + queryParams;
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Unexpected code: " + response.code());
                    return;
                }

                assert response.body() != null;
                String responseBody = response.body().string();

                try {
                    Gson gson = new Gson();
                    JewishСontroller.HebcalResponse hebcalResponse = gson.fromJson(responseBody, JewishСontroller.HebcalResponse.class);
                    callback.onSuccess(hebcalResponse);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON parsing error", e);
                    callback.onError("JSON parsing error: " + e.getMessage());
                }
            }
        });


    }
}
