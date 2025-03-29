package com.fraudx.detector.services;

import android.util.Log;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class FakeNewsService {
    private static final String TAG = "FakeNewsService";
    private static final String API_URL = "https://www.fakedetector.run.place/service1/gemini?messageText=";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;

    public FakeNewsService() {
        // Configure client with longer timeouts
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public interface AnalysisCallback {
        void onSuccess(String result, String explanation);
        void onError(String error);
    }

    public void analyzeText(String text, AnalysisCallback callback) {
        try {
            // Log request for debugging
            Log.d(TAG, "Analyzing text: " + text);
            
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String url = API_URL + encodedText;
            
            Log.d(TAG, "Request URL: " + url);
            
            Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Android FraudX App")
                .get()
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "No response body";
                            Log.e(TAG, "Server error: " + response.code() + " - " + errorBody);
                            callback.onError("Server error: " + response.code());
                            return;
                        }

                        String responseData = response.body().string();
                        Log.d(TAG, "Response data: " + responseData);
                        
                        // Process the response data
                        processResponse(responseData, callback);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing request", e);
            callback.onError("Error preparing request: " + e.getMessage());
        }
    }

    private void processResponse(String responseData, AnalysisCallback callback) {
        try {
            // First try parsing as JSON
            JSONObject jsonResponse = new JSONObject(responseData);
            String result = jsonResponse.optString("result", "UNKNOWN");
            
            // Extract classification result and explanation
            String classificationResult = extractClassification(result);
            String remainingText = extractRemainingText(result, classificationResult);
            
            // If we found a clear classification from the result field
            if (!classificationResult.equals("UNCERTAIN")) {
                String explanation = remainingText;
                
                // If there's an explanation field in JSON, append it
                String jsonExplanation = jsonResponse.optString("explanation", "");
                if (!jsonExplanation.isEmpty()) {
                    explanation = (explanation.isEmpty() ? "" : explanation + " ") + jsonExplanation;
                }
                
                callback.onSuccess(classificationResult, explanation);
                return;
            }
            
            // If we couldn't extract a clear REAL/FAKE from result,
            // fallback to the normal JSON handling
            String explanation = jsonResponse.optString("explanation", "No explanation provided");
            
            // If no clear result in JSON, determine from explanation
            if ("UNKNOWN".equals(result)) {
                result = determineResult(explanation);
            }
            
            callback.onSuccess(result, explanation);
            
        } catch (JSONException e) {
            // If JSON parsing fails, try to handle as plain text
            Log.d(TAG, "JSON parsing failed, trying plain text processing", e);
            processPlainText(responseData, callback);
        }
    }
    
    private void processPlainText(String responseData, AnalysisCallback callback) {
        if (responseData == null || responseData.trim().isEmpty()) {
            callback.onError("Empty response from server");
            return;
        }
        
        // Extract classification from the text
        String classificationResult = extractClassification(responseData);
        String remainingText = extractRemainingText(responseData, classificationResult);
        
        // If we found a clear classification
        if (!classificationResult.equals("UNCERTAIN")) {
            callback.onSuccess(classificationResult, remainingText);
        } else if (responseData.toLowerCase().contains("fake")) {
            callback.onSuccess("FAKE", responseData);
        } else if (responseData.toLowerCase().contains("real")) {
            callback.onSuccess("REAL", responseData);
        } else {
            // If no clear indicator, use determineResult to analyze the full text
            String result = determineResult(responseData);
            callback.onSuccess(result, responseData);
        }
    }
    
    /**
     * Extracts REAL or FAKE classification by examining the start of the text
     */
    private String extractClassification(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "UNCERTAIN";
        }
        
        // Get the trimmed, uppercase version of the text
        String trimmedText = text.trim().toUpperCase();
        
        // Check for FAKE (allowing for case variations like "Fake", "fake", etc.)
        if (trimmedText.startsWith("FAKE") ||
            (trimmedText.length() >= 4 && trimmedText.substring(0, 4).equals("FAKE"))) {
            return "FAKE";
        }
        
        // Check for REAL (allowing for case variations like "Real", "real", etc.)
        if (trimmedText.startsWith("REAL") ||
            (trimmedText.length() >= 4 && trimmedText.substring(0, 4).equals("REAL"))) {
            return "REAL";
        }
        
        // Check initial word
        String[] parts = trimmedText.split("\\s+", 2);
        String firstWord = parts[0];
        
        if (firstWord.equals("FAKE") || firstWord.equals("REAL")) {
            return firstWord;
        }
        
        return "UNCERTAIN";
    }
    
    /**
     * Extracts the remaining text after removing the classification
     */
    private String extractRemainingText(String text, String classification) {
        if (text == null || text.trim().isEmpty() || classification.equals("UNCERTAIN")) {
            return text;
        }
        
        String trimmedText = text.trim();
        
        // If text starts with the classification word (case insensitive)
        if (trimmedText.toUpperCase().startsWith(classification)) {
            // Find the end of the classification word
            int endIndex = classification.length();
            
            // Skip any spaces after the classification
            while (endIndex < trimmedText.length() && Character.isWhitespace(trimmedText.charAt(endIndex))) {
                endIndex++;
            }
            
            if (endIndex < trimmedText.length()) {
                return trimmedText.substring(endIndex);
            } else {
                return "";
            }
        }
        
        // If the classification wasn't at the start, check the first word
        String[] parts = trimmedText.split("\\s+", 2);
        if (parts.length > 1 && parts[0].toUpperCase().equals(classification)) {
            return parts[1];
        }
        
        return text;
    }

    private String determineResult(String explanation) {
        if (explanation == null || explanation.isEmpty()) {
            return "UNCERTAIN";
        }
        
        String lowerExplanation = explanation.toLowerCase();
        if (lowerExplanation.contains("fake") || 
            lowerExplanation.contains("false") || 
            lowerExplanation.contains("misleading") || 
            lowerExplanation.contains("misinformation") ||
            lowerExplanation.contains("disinformation")) {
            return "FAKE";
        } else if (lowerExplanation.contains("real") || 
                   lowerExplanation.contains("true") || 
                   lowerExplanation.contains("verified") ||
                   lowerExplanation.contains("authentic")) {
            return "REAL";
        } else {
            return "UNCERTAIN";
        }
    }
}