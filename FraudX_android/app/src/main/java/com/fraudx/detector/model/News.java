package com.fraudx.detector.model;

public class News {
    private String id;
    private String title;
    private String snippet;
    private String link;
    private long timestamp;

    public News() {
        // Required empty constructor for Firebase
    }

    public News(String title, String snippet, String link) {
        this.title = title;
        this.snippet = snippet;
        this.link = link;
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

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 