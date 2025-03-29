package com.fraudx.detector.model;

public class Headline {
    private String title;
    private String description;
    private String url;

    public Headline(String title, String description, String url) {
        this.title = title;
        this.description = description;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }
} 