package com.example.hikenativeapp.api;

import com.example.hikenativeapp.data.chatbot.ChatRequest;
import com.example.hikenativeapp.data.chatbot.ChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChatbotApi {
    @POST("ask")
    Call<ChatResponse> askQuestion(@Body ChatRequest request);
}

