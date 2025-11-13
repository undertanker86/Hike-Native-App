package com.example.hikenativeapp.data.chatbot;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("question")
    private String question;

    @SerializedName("user_id")
    private String userId;

    public ChatRequest(String question, String userId) {
        this.question = question;
        this.userId = userId;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

