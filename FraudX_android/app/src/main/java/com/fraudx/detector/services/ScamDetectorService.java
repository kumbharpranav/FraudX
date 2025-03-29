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

public class ScamDetectorService {
    private static final String TAG = "ScamDetectorService";
    private static final String API_URL = "https://www.fakedetector.run.place/service2/gemini?messageText=";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private Call currentCall;

    public ScamDetectorService() {
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
        cancelOngoing();
        try {
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

            currentCall = client.newCall(request);
            currentCall.enqueue(new Callback() {
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

    public void cancelOngoing() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    private void processResponse(String responseData, AnalysisCallback callback) {
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            String result = jsonResponse.optString("result", "UNKNOWN");
            
            String classificationResult = extractClassification(result);
            String remainingText = extractRemainingText(result, classificationResult);
            
            if (!classificationResult.equals("UNCERTAIN")) {
                String explanation = remainingText;
                
                String jsonExplanation = jsonResponse.optString("explanation", "");
                if (!jsonExplanation.isEmpty()) {
                    explanation = (explanation.isEmpty() ? "" : explanation + " ") + jsonExplanation;
                }
                
                callback.onSuccess(classificationResult, explanation);
                return;
            }
            
            String explanation = jsonResponse.optString("explanation", "No explanation provided");
            
            if ("UNKNOWN".equals(result)) {
                result = determineResult(explanation);
            }
            
            callback.onSuccess(result, explanation);
            
        } catch (JSONException e) {
            Log.d(TAG, "JSON parsing failed, trying plain text processing", e);
            processPlainText(responseData, callback);
        }
    }
    
    private void processPlainText(String responseData, AnalysisCallback callback) {
        if (responseData == null || responseData.trim().isEmpty()) {
            callback.onError("Empty response from server");
            return;
        }
        
        String classificationResult = extractClassification(responseData);
        String remainingText = extractRemainingText(responseData, classificationResult);
        
        if (!classificationResult.equals("UNCERTAIN")) {
            callback.onSuccess(classificationResult, remainingText);
        } else if (responseData.toLowerCase().contains("scam")) {
            callback.onSuccess("SCAM", responseData);
        } else if (responseData.toLowerCase().contains("safe")) {
            callback.onSuccess("SAFE", responseData);
        } else {
            String result = determineResult(responseData);
            callback.onSuccess(result, responseData);
        }
    }
    
    private String extractClassification(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "UNCERTAIN";
        }
        
        String trimmedText = text.trim().toUpperCase();
        
        if (trimmedText.startsWith("SCAM") ||
            (trimmedText.length() >= 4 && trimmedText.substring(0, 4).equals("SCAM"))) {
            return "SCAM";
        }
        
        if (trimmedText.startsWith("SAFE") ||
            (trimmedText.length() >= 4 && trimmedText.substring(0, 4).equals("SAFE"))) {
            return "SAFE";
        }
        
        String[] parts = trimmedText.split("\\s+", 2);
        String firstWord = parts[0];
        
        if (firstWord.equals("SCAM") || firstWord.equals("SAFE")) {
            return firstWord;
        }
        
        return "UNCERTAIN";
    }
    
    private String extractRemainingText(String text, String classification) {
        if (text == null || text.trim().isEmpty() || classification.equals("UNCERTAIN")) {
            return text;
        }
        
        String trimmedText = text.trim();
        
        if (trimmedText.toUpperCase().startsWith(classification)) {
            int endIndex = classification.length();
            
            while (endIndex < trimmedText.length() && Character.isWhitespace(trimmedText.charAt(endIndex))) {
                endIndex++;
            }
            
            if (endIndex < trimmedText.length()) {
                return trimmedText.substring(endIndex);
            } else {
                return "";
            }
        }
        
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
        if (lowerExplanation.contains("scam") || 
            lowerExplanation.contains("fraud") || 
            lowerExplanation.contains("suspicious") || 
            lowerExplanation.contains("phishing") ||
            lowerExplanation.contains("malicious")) {
            return "SCAM";
        } else if (lowerExplanation.contains("safe") || 
                   lowerExplanation.contains("legitimate") || 
                   lowerExplanation.contains("genuine") ||
                   lowerExplanation.contains("trustworthy")) {
            return "SAFE";
        } else {
            return "UNCERTAIN";
        }
    }
}