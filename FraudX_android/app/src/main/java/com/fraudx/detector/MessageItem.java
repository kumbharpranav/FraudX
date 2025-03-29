package com.fraudx.detector;

public class MessageItem {
    private final String text;
    private final boolean isSafe;

    public MessageItem(String text, boolean isSafe) {
        this.text = text;
        this.isSafe = isSafe;
    }

    public String getText() {
        return text;
    }

    public boolean isSafe() {
        return isSafe;
    }
} 