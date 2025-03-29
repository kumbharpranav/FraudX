package com.fraudx.detector.model;

public class Scam {
    private String id;
    private String title;
    private String description;
    private String url;
    private String riskLevel;
    private String source;
    private String imageUrl;
    private long timestamp;

    public Scam() {
        // Required empty constructor for Firebase
    }

    public Scam(String title, String description, String url, String riskLevel, String source, String imageUrl) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.riskLevel = riskLevel;
        this.source = source;
        this.imageUrl = imageUrl;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 