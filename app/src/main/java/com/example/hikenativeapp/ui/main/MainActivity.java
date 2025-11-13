package com.example.hikenativeapp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.ui.auth.LoginActivity;
import com.example.hikenativeapp.ui.chatbot.ChatbotActivity;
import com.example.hikenativeapp.ui.hike_list.HikeListActivity;
import com.example.hikenativeapp.ui.report.ReportActivity;
import com.example.hikenativeapp.ui.user_settings.UserSettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvUserEmail;
    private CardView cardHikes, cardUserSettings, cardReports, cardChatbot;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupUserInfo();
        setupClickListeners();
        setupBackPressHandler();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvUserEmail = findViewById(R.id.tv_user_email);
        cardHikes = findViewById(R.id.card_hikes);
        cardUserSettings = findViewById(R.id.card_user_settings);
        cardReports = findViewById(R.id.card_reports);
        cardChatbot = findViewById(R.id.card_chatbot);
    }

    private void setupUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String displayName = currentUser.getDisplayName();

            if (displayName != null && !displayName.isEmpty()) {
                tvWelcome.setText("Welcome back, " + displayName + "!");
            } else {
                tvWelcome.setText("Welcome back!");
            }

            if (email != null) {
                tvUserEmail.setText(email);
            }
        } else {
            // No user logged in, redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupClickListeners() {
        // My Hikes - Navigate to HikeListActivity
        cardHikes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HikeListActivity.class);
            startActivity(intent);
        });

        // User Settings - Navigate to UserSettingsActivity
        cardUserSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserSettingsActivity.class);
            startActivity(intent);
        });

        // Reports - Navigate to ReportActivity
        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        // AI Chatbot - Navigate to ChatbotActivity
        cardChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });
    }

    private void setupBackPressHandler() {
        // Handle back press with modern API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    finishAffinity(); // Close all activities
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user info when returning to this activity
        setupUserInfo();
    }
}
