package com.example.hikenativeapp.ui.chatbot;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hikenativeapp.data.chatbot.ChatMessage;
import com.example.hikenativeapp.data.repository.ChatbotRepository;

import java.util.List;

public class ChatbotViewModel extends AndroidViewModel {

    private final ChatbotRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private LiveData<List<ChatMessage>> chatHistory;

    public ChatbotViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatbotRepository(application);
    }

    public LiveData<List<ChatMessage>> getChatHistory(String userId) {
        if (chatHistory == null) {
            chatHistory = repository.getChatHistory(userId);
        }
        return chatHistory;
    }

    public void askQuestion(String userId, String question) {
        if (question == null || question.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập câu hỏi");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.askQuestion(userId, question, new ChatbotRepository.ChatCallback() {
            @Override
            public void onSuccess(String answer) {
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    public void clearChatHistory(String userId) {
        repository.clearChatHistory(userId);
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
