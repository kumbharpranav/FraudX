package com.fraudx.detector.services;

import com.fraudx.detector.models.News;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.util.Log;

public class GoogleSearchService {
    private static final String TAG = "GoogleSearchService";
    private static final String[] API_KEYS = {""};
    private static final String SEARCH_ENGINE_ID = "";
    private static int currentKeyIndex = 0;
    private static final ReentrantLock apiKeyLock = new ReentrantLock();
    private final OkHttpClient client = new OkHttpClient();

    public interface SearchCallback {
        void onSuccess(List<News> results);
        void onError(String error);
    }

    private String getNextApiKey() {
        apiKeyLock.lock();
        try {
            currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
            String nextKey = API_KEYS[currentKeyIndex];
            Log.d(TAG, "Switched to API key #" + currentKeyIndex);
            return nextKey;
        } finally {
            apiKeyLock.unlock();
        }
    }

    public void searchNews(String query, SearchCallback callback) {
        search(query, callback, 0);
    }

    public void searchScams(String query, SearchCallback callback) {
        search(query, callback, 0);
    }

    private void search(String query, SearchCallback callback, int retryCount) {
        // Avoid infinite retry loops
        if (retryCount >= API_KEYS.length) {
            callback.onError("Failed after trying all API keys. Quota exceeded for all keys.");
            return;
        }
        
        String currentApiKey;
        apiKeyLock.lock();
        try {
            currentApiKey = API_KEYS[currentKeyIndex];
        } finally {
            apiKeyLock.unlock();
        }
        
        String url = String.format("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s",
                currentApiKey, SEARCH_ENGINE_ID, query);

        Log.d(TAG, "Searching with API key #" + currentKeyIndex + " (retry: " + retryCount + ")");

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

                    if (jsonObject.has("error")) {
                        JSONObject error = jsonObject.getJSONObject("error");
                        int errorCode = error.getInt("code");
                        
                        if (errorCode == 429 || errorCode == 403 || 
                            error.getString("message").toLowerCase().contains("quota")) {
                            // Quota exceeded - try with next API key
                            Log.w(TAG, "API key #" + currentKeyIndex + " quota exceeded: " + error.getString("message"));
                            String nextApiKey = getNextApiKey();
                            
                            // Try search with next API key
                            search(query, callback, retryCount + 1);
                            return;
                        }
                        
                        callback.onError("API error: " + error.getString("message"));
                        return;
                    }

                    if (!jsonObject.has("items")) {
                        callback.onError("No results found");
                        return;
                    }

                    JSONArray items = jsonObject.getJSONArray("items");
                    List<News> results = new ArrayList<>();

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        
                        // Get title, snippet and link directly
                        String title = item.getString("title");
                        String snippet = item.getString("snippet");
                        String link = item.getString("link");
                        String displayLink = item.optString("displayLink", "Google");
                        
                        // Extract image URL from pagemap if available
                        String imageUrl = "";
                        if (item.has("pagemap") && item.getJSONObject("pagemap").has("cse_image")) {
                            JSONArray cseImages = item.getJSONObject("pagemap").getJSONArray("cse_image");
                            if (cseImages.length() > 0) {
                                imageUrl = cseImages.getJSONObject(0).optString("src", "");
                            }
                        }
                        
                        News news = new News(title, snippet, link, imageUrl, displayLink);
                        results.add(news);
                    }

                    callback.onSuccess(results);

                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        });
    }
}