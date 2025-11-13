package com.example.hikenativeapp.data.chatbot;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ChatMessage {
    @DocumentId  // This will automatically map Firestore document ID to this field
    private String id;
    private String userId;
    private String message;
    private String response;
    @ServerTimestamp
    private Date timestamp;
    private boolean isUser; // true = user message, false = bot response

    // Empty constructor required for Firestore
    public ChatMessage() {
    }

    public ChatMessage(String userId, String message, String response, boolean isUser) {
        this.userId = userId;
        this.message = message;
        this.response = response;
        this.isUser = isUser;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponse() {
        return response;
    }


    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public long getTimestampLong() {
        return timestamp != null ? timestamp.getTime() : 0;
    }
}
