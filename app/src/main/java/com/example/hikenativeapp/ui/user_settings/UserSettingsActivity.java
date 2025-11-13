package com.example.hikenativeapp.ui.user_settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.AppDatabase;
import com.example.hikenativeapp.data.local.entity.User;
import com.example.hikenativeapp.ui.auth.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserSettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivAvatar;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Google Sign-In Client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        initializeViews();
        setupToolbar();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmail = findViewById(R.id.tv_email);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            // No user logged in, redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Load user data in background thread
        new Thread(() -> {
            try {
                // Get user from local database
                currentUser = AppDatabase.getDatabase(this)
                        .userDao()
                        .getUserByGoogleId(firebaseUser.getUid());

                runOnUiThread(() -> {
                    if (currentUser != null) {
                        // Display user information
                        tvDisplayName.setText(currentUser.getDisplayName());
                        tvEmail.setText(currentUser.getEmail());

                        // Load avatar from photo_url
                        loadAvatar(currentUser.getPhotoUrl());
                    } else {
                        // User not in database, show Firebase data
                        tvDisplayName.setText(firebaseUser.getDisplayName());
                        tvEmail.setText(firebaseUser.getEmail());

                        // Load avatar from Firebase photo URL
                        if (firebaseUser.getPhotoUrl() != null) {
                            loadAvatar(firebaseUser.getPhotoUrl().toString());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadAvatar(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(ivAvatar);
        } else {
            // Set default avatar
            ivAvatar.setImageResource(R.drawable.user);
        }
    }

    private void setupClickListeners() {
        // Logout Button
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Sign out from Firebase
                    mAuth.signOut();
                    // Sign out from Google
                    googleSignInClient.signOut();

                    // Redirect to Login Activity
                    Intent intent = new Intent(UserSettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(UserSettingsActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
