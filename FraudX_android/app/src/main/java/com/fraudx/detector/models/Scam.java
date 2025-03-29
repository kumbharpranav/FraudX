package com.fraudx.detector.models;

public class Scam {
    private String id;
    private String title;
    private String description;
    private String url;
    private String type;
    private String source;
    private String riskLevel;
    private long timestamp;

    // Required empty constructor for Firebase
    public Scam() {}

    public Scam(String title, String description, String url, String type, String source, String riskLevel) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.type = type;
        this.source = source;
        this.riskLevel = riskLevel;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}