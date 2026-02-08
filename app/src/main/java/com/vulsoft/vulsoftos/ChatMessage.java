package com.vulsoft.vulsoftos;

public class ChatMessage {
    private String content;
    private boolean isUser;
    private long timestamp;

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String content, boolean isUser, long timestamp) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
