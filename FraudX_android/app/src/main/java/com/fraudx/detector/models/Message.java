package com.fraudx.detector.models;

public class Message {
    private String id;
    private String text;
    private String status;
    private boolean isBot;
    private long timestamp;

    public Message() {
        // Required empty constructor for Firebase
    }

    public Message(String text, String status, boolean isBot) {
        this.text = text;
        this.status = status;
        this.isBot = isBot;
        this.timestamp = System.currentTimeMillis();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
