package com.example.hikenativeapp.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hikenativeapp.api.ChatbotApi;
import com.example.hikenativeapp.api.ChatbotService;
import com.example.hikenativeapp.data.chatbot.ChatMessage;
import com.example.hikenativeapp.data.chatbot.ChatRequest;
import com.example.hikenativeapp.data.chatbot.ChatResponse;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotRepository {

    private static final String TAG = "ChatbotRepository";
    private static final String COLLECTION_CHAT_MESSAGES = "chat_messages";

    private final FirebaseFirestore firestore;
    private final ChatbotApi chatbotApi;
    private final MutableLiveData<List<ChatMessage>> chatHistoryLiveData = new MutableLiveData<>();

    public interface ChatCallback {
        void onSuccess(String answer);
        void onError(String error);
    }

    public ChatbotRepository(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.chatbotApi = ChatbotService.getApi();
    }

    // Get chat history for a user from Firestore
    public LiveData<List<ChatMessage>> getChatHistory(String userId) {
        Log.d(TAG, "Setting up chat history listener for user: " + userId);

        // Simple query without orderBy to avoid index requirement
        // We'll sort in memory instead
        firestore.collection(COLLECTION_CHAT_MESSAGES)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to chat history", error);
                        // Post empty list on error to clear UI
                        chatHistoryLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    if (value != null) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                ChatMessage message = doc.toObject(ChatMessage.class);
                                // ID will be automatically set by @DocumentId annotation
                                messages.add(message);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing message: " + doc.getId(), e);
                            }
                        }

                        // Sort by timestamp in memory (ascending order - oldest first)
                        messages.sort((m1, m2) -> {
                            if (m1.getTimestamp() == null) return -1;
                            if (m2.getTimestamp() == null) return 1;
                            return m1.getTimestamp().compareTo(m2.getTimestamp());
                        });

                        Log.d(TAG, "Loaded " + messages.size() + " messages");
                        chatHistoryLiveData.postValue(messages);
                    } else {
                        Log.d(TAG, "No messages found");
                        chatHistoryLiveData.postValue(new ArrayList<>());
                    }
                });

        return chatHistoryLiveData;
    }

    // Send question to chatbot API
    public void askQuestion(String userId, String question, ChatCallback callback) {
        Log.d(TAG, "=== SENDING CHAT REQUEST ===");
        Log.d(TAG, "User ID (google_id): " + userId);
        Log.d(TAG, "Question: " + question);
        Log.d(TAG, "============================");

        ChatRequest request = new ChatRequest(question, userId);

        chatbotApi.askQuestion(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatResponse chatResponse = response.body();

                    if (chatResponse.getError() != null) {
                        callback.onError(chatResponse.getError());
                        return;
                    }

                    String answer = chatResponse.getAnswer();

                    // Save to Firestore
                    saveChatMessages(userId, question, answer, callback);

                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Save chat messages to Firestore
    private void saveChatMessages(String userId, String question, String answer, ChatCallback callback) {
        Log.d(TAG, "Saving chat messages to Firestore...");

        // Save user message
        ChatMessage userMessage = new ChatMessage(userId, question, null, true);

        firestore.collection(COLLECTION_CHAT_MESSAGES)
                .add(userMessage)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "User message saved with ID: " + documentReference.getId());

                    // Save bot response (with a slight delay to ensure timestamp order)
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        ChatMessage botMessage = new ChatMessage(userId, null, answer, false);

                        firestore.collection(COLLECTION_CHAT_MESSAGES)
                                .add(botMessage)
                                .addOnSuccessListener(docRef -> {
                                    Log.d(TAG, "Bot message saved with ID: " + docRef.getId());
                                    Log.d(TAG, "Both messages saved successfully!");
                                    callback.onSuccess(answer);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error saving bot message", e);
                                    callback.onError("Failed to save bot message: " + e.getMessage());
                                });
                    }, 100); // Delay 100ms to ensure bot message has timestamp after user message
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user message", e);
                    callback.onError("Failed to save user message: " + e.getMessage());
                });
    }

    // Clear chat history for a user
    public void clearChatHistory(String userId) {
        firestore.collection(COLLECTION_CHAT_MESSAGES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Chat history cleared for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error clearing chat history", e);
                });
    }
}
