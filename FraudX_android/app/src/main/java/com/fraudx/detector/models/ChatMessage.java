package com.fraudx.detector.models;

public class ChatMessage {
    private String message;
    private boolean isBot;
    private String status;
    private long timestamp;

    public ChatMessage() {
        // Required empty constructor for Firebase
    }

    public ChatMessage(String message, boolean isBot) {
        this.message = message;
        this.isBot = isBot;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
