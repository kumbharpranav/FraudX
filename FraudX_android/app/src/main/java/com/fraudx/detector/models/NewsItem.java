package com.fraudx.detector.models;

import java.io.Serializable;

public class NewsItem implements Serializable {
    private String id;
    private String text;
    private String result;  // "REAL", "FAKE", or "UNCERTAIN"
    private String explanation;
    private long timestamp;
    private boolean isUser; // true for user messages, false for bot responses

    // Required empty constructor for Firebase
    public NewsItem() {
        this.timestamp = System.currentTimeMillis();
        this.isUser = false;
    }

    public NewsItem(String text, String result, String explanation) {
        this.text = text;
        this.result = result;
        this.explanation = explanation;
        this.timestamp = System.currentTimeMillis();
        this.isUser = false;
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

    public boolean isReal() {
        return "REAL".equals(result) || "real".equals(result);
    }

    public boolean isFake() {
        return "FAKE".equals(result) || "fake".equals(result);
    }

    public boolean isUncertain() {
        return !isReal() && !isFake();
    }
} 