package com.fraudx.detector.models;

import java.io.Serializable;

public class ScamItem implements Serializable {
    private static final double CERTAINTY_THRESHOLD = 0.7; // Threshold for confidence level
    private String id;
    private String text;
    private String result;  // "SAFE", "SCAM", "UNCERTAIN", "FAKE", or "REAL"
    private String explanation;
    private double confidence; // Add confidence field
    private long timestamp;
    private boolean isUser; // true for user messages, false for bot responses

    // Required empty constructor for Firebase
    public ScamItem() {
        this.timestamp = System.currentTimeMillis();
        this.isUser = false;
    }

    public ScamItem(String text, String result, String explanation, double confidence) {
        this.text = text;
        this.result = result;
        this.explanation = explanation;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
        this.isUser = false;
    }

    public ScamItem(String text, String result, String explanation) {
        this(text, result, explanation, 1.0); // Default confidence of 1.0
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean isUser) {
        this.isUser = isUser;
    }

    public boolean isSafe() {
        return "SAFE".equals(result) || "safe".equals(result);
    }

    public boolean isScam() {
        return "SCAM".equals(result) || "scam".equals(result);
    }

    public boolean isUncertain() {
        return confidence < CERTAINTY_THRESHOLD;
    }

    public boolean isFake() {
        return "FAKE".equals(result) || "fake".equals(result);
    }

    public boolean isReal() {
        return "REAL".equals(result) || "real".equals(result);
    }
}