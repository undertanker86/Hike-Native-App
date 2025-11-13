package com.example.hikenativeapp.ui.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hikenativeapp.R;
import com.example.hikenativeapp.ui.adapter.ChatAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";

    private ChatbotViewModel viewModel;
    private ChatAdapter adapter;
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private FloatingActionButton buttonSend;
    private LinearLayout progressContainer;
    private TextView typingDots;
    private LinearLayoutManager layoutManager;
    private ValueAnimator typingAnimator;

    private String userId; // This is Firebase UID (google_id)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Get Firebase user ID (this is google_id)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // IMPORTANT: This is the Firebase UID (google_id) that will be sent to backend
        userId = currentUser.getUid();
        Log.d(TAG, "🔑 Using Firebase UID (google_id): " + userId);
        Log.d(TAG, "📧 User email: " + currentUser.getEmail());

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_chat);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        progressContainer = findViewById(R.id.progress_container);
        typingDots = findViewById(R.id.typing_dots);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.chatbot_title);
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter();
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Add scroll listener to hide keyboard when scrolling
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatbotViewModel.class);

        // Observe chat history
        viewModel.getChatHistory(userId).observe(this, chatMessages -> {
            if (chatMessages != null) {
                int previousSize = adapter.getItemCount();
                adapter.setMessages(chatMessages);

                // Hide/show empty state
                View emptyState = findViewById(R.id.empty_state);
                emptyState.setVisibility(chatMessages.isEmpty() ? View.VISIBLE : View.GONE);

                // Auto scroll to bottom when new messages arrive
                if (!chatMessages.isEmpty()) {
                    int newSize = chatMessages.size();

                    // If new messages were added, scroll to bottom
                    if (newSize > previousSize) {
                        recyclerView.post(() -> {
                            recyclerView.smoothScrollToPosition(newSize - 1);
                        });
                    } else if (previousSize == 0) {
                        // Initial load
                        recyclerView.scrollToPosition(newSize - 1);
                    }
                }
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                buttonSend.setEnabled(!isLoading);
                editTextMessage.setEnabled(!isLoading);

                // Animate FAB
                if (isLoading) {
                    buttonSend.setAlpha(0.5f);
                    startTypingAnimation();
                } else {
                    buttonSend.setAlpha(1.0f);
                    stopTypingAnimation();
                }
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupListeners() {
        buttonSend.setOnClickListener(v -> sendMessage());

        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String message = editTextMessage.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_message, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "📤 Sending question to backend with user_id: " + userId);
        viewModel.askQuestion(userId, message);
        editTextMessage.setText("");

        // Hide keyboard after sending
        InputMethodManager imm =
            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editTextMessage.getWindowToken(), 0);
        }
    }

    private void startTypingAnimation() {
        if (typingAnimator != null && typingAnimator.isRunning()) {
            return;
        }

        typingAnimator = ValueAnimator.ofInt(0, 3);
        typingAnimator.setDuration(600);
        typingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        typingAnimator.addUpdateListener(animation -> {
            int dotCount = (int) animation.getAnimatedValue();
            String dots = "";
            for (int i = 0; i < 3; i++) {
                if (i < dotCount) {
                    dots += "●";
                } else {
                    dots += "○";
                }
            }
            typingDots.setText(dots);
        });
        typingAnimator.start();
    }

    private void stopTypingAnimation() {
        if (typingAnimator != null) {
            typingAnimator.cancel();
            typingAnimator = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTypingAnimation();
    }
}
