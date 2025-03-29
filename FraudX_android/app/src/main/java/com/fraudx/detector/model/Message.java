package com.fraudx.detector.model;

public class Message {
    private String text;
    private String indicator;
    private boolean isUser;
    private long timestamp;
    private Status status;

    public enum Status {
        SAFE,
        SCAM,
        ERROR
    }

    public Message(String text, String indicator) {
        this.text = text;
        this.indicator = indicator;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String text, String indicator, boolean isUser) {
        this.text = text;
        this.indicator = indicator;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getIndicator() {
        return indicator;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
} 