package com.example.hikenativeapp.data.chatbot;

import com.google.gson.annotations.SerializedName;

public class ChatResponse {
    @SerializedName("answer")
    private String answer;

    @SerializedName("sources")
    private String[] sources;

    @SerializedName("error")
    private String error;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String[] sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

