package com.fraudx.detector.utils;

import android.net.Uri;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetectorApiService {
    private static final String FAKE_NEWS_API = "https://www.fakedetector.run.place/service1/gemini";
    private static final String SCAM_API = "https://www.fakedetector.run.place/service2/gemini";
    
    private final OkHttpClient client;
    private static DetectorApiService instance;
    
    private DetectorApiService() {
        client = new OkHttpClient();
    }
    
    public static synchronized DetectorApiService getInstance() {
        if (instance == null) {
            instance = new DetectorApiService();
        }
        return instance;
    }
    
    public void detectFakeNews(String text, DetectionCallback callback) {
        String url = FAKE_NEWS_API + "?messageText=" + Uri.encode(text);
        makeRequest(url, callback);
    }
    
    public void detectScam(String text, DetectionCallback callback) {
        String url = SCAM_API + "?messageText=" + Uri.encode(text);
        makeRequest(url, callback);
    }
    
    private void makeRequest(String url, DetectionCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String result = jsonObject.getString("result");
                    callback.onSuccess(result);
                } catch (Exception e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        });
    }
    
    public interface DetectionCallback {
        void onSuccess(String result);
        void onError(String error);
    }
} 