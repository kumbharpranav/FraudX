package com.fraudx.detector.models;

public class SearchResult {
    private final String title;
    private final String link;
    private final String snippet;
    private final String source;
    private final String date;
    private String jsonResponse;

    public SearchResult(String title, String link, String snippet, String source, String date) {
        this.title = title;
        this.link = link;
        this.snippet = snippet;
        this.source = source;
        this.date = date;
    }

    public String getTitle() { return title; }
    public String getLink() { return link; }
    public String getSnippet() { return snippet; }
    public String getSource() { return source; }
    public String getDate() { return date; }
    public String getJsonResponse() { return jsonResponse; }
    public void setJsonResponse(String jsonResponse) { this.jsonResponse = jsonResponse; }
}
