package com.fraudx.detector.utils;

import android.util.Log;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class GoogleSearchUtil {
    private static final String TAG = "GoogleSearchUtil";
    private static final String[] API_KEYS = {
 
       "Search_api"
    };
    private static final String SEARCH_ENGINE_ID = "search_cx";
    private static int currentKeyIndex = 0;
    private final OkHttpClient client;

    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onError(String error);
    }

    public static class SearchResult {
        public String title;
        public String link;
        public String snippet;
        public String jsonResponse;

        public SearchResult(String title, String link, String snippet, String jsonResponse) {
            this.title = title;
            this.link = link;
            this.snippet = snippet;
            this.jsonResponse = jsonResponse;
        }
    }

    public GoogleSearchUtil() {
        this.client = new OkHttpClient();
    }

    private static String getNextApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
        return API_KEYS[currentKeyIndex];
    }

    public void searchNews(SearchCallback callback) {
        search("Latest News India", callback);
    }

    public void searchScams(SearchCallback callback) {
        search("Latest Top Scams", callback);
    }

    public void search(String query, SearchCallback callback) {
        final String[] currentApiKey = {API_KEYS[currentKeyIndex]};
        String url = String.format("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s",
                currentApiKey[0], SEARCH_ENGINE_ID, query);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Search failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    // Check for quota exceeded
                    if (json.has("error") && json.getJSONObject("error").getInt("code") == 429) {
                        currentApiKey[0] = getNextApiKey();
                        search(query, callback); // Retry with new key
                        return;
                    }

                    // Parse results
                    List<SearchResult> results = new ArrayList<>();
                    JSONArray items = json.getJSONArray("items");
                    for (int i = 0; i < Math.min(items.length(), 4); i++) {
                        JSONObject item = items.getJSONObject(i);
                        results.add(new SearchResult(
                            item.getString("title"),
                            item.getString("link"),
                            item.getString("snippet"),
                            item.toString(2) // Pretty print JSON with 2 spaces indentation
                        ));
                    }
                    callback.onSuccess(results);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response", e);
                    callback.onError("Error processing response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
}