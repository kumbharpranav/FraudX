package com.fraudx.detector.models;

public class Headline {
    private String title;
    private String category;
    private String status;
    private String url;
    private String jsonResponse;

    public Headline(String title, String category, String status, String url, String jsonResponse) {
        this.title = title;
        this.category = category;
        this.status = status;
        this.url = url;
        this.jsonResponse = jsonResponse;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }
}
