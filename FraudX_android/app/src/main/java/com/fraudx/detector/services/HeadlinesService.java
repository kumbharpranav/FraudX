package com.fraudx.detector.services;

import com.fraudx.detector.models.Headline;
import com.fraudx.detector.utils.GoogleSearchUtil;
import java.util.ArrayList;
import java.util.List;

public class HeadlinesService {
    private final GoogleSearchUtil searchUtil;

    public HeadlinesService() {
        this.searchUtil = new GoogleSearchUtil();
    }

    public interface HeadlinesCallback {
        void onSuccess(List<Headline> headlines);
        void onError(String error);
    }

    public void fetchTopHeadlines(HeadlinesCallback callback) {
        searchUtil.searchNews(new GoogleSearchUtil.SearchCallback() {
            @Override
            public void onSuccess(List<GoogleSearchUtil.SearchResult> results) {
                List<Headline> headlines = convertToHeadlines(results);
                callback.onSuccess(headlines);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private List<Headline> convertToHeadlines(List<GoogleSearchUtil.SearchResult> results) {
        List<Headline> headlines = new ArrayList<>();
        for (GoogleSearchUtil.SearchResult result : results) {
            headlines.add(new Headline(
                result.title,
                extractCategory(result.snippet),
                "Verified",
                result.link,
                result.jsonResponse // Add raw JSON response
            ));
        }
        return headlines;
    }

    private String extractCategory(String snippet) {
        String lowerSnippet = snippet.toLowerCase();
        if (lowerSnippet.contains("tech") || lowerSnippet.contains("technology")) {
            return "Technology";
        } else if (lowerSnippet.contains("business") || lowerSnippet.contains("finance")) {
            return "Business";
        } else if (lowerSnippet.contains("health") || lowerSnippet.contains("medical")) {
            return "Health";
        } else if (lowerSnippet.contains("crime") || lowerSnippet.contains("fraud")) {
            return "Crime";
        } else {
            return "General";
        }
    }

    public void getTopHeadlines(HeadlinesCallback callback) {
        // TODO: Implement actual API call
        // For now, return empty list
        callback.onSuccess(new ArrayList<>());
    }
}
