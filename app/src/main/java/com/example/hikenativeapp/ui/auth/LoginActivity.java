package com.example.hikenativeapp.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.local.AppDatabase;
import com.example.hikenativeapp.data.repository.UserRepository;
import com.example.hikenativeapp.ui.main.MainActivity;
import com.example.hikenativeapp.util.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private Button btnGoogleSignIn;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private AuthViewModel authViewModel;
    private UserRepository repository;

    public static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupGoogleSignIn();
        setupViewModel();
        setupClickListeners();
        observeAuthState();
    }

    private void initializeViews() {
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Clear previous sign-in to force account selection
        googleSignInClient.signOut();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
        );
    }

    private void setupViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        repository = new UserRepository(AppDatabase.getDatabase(this));
        authViewModel.setRepository(repository);
    }

    private void observeAuthState() {
        authViewModel.getAuthState().observe(this, authState -> {
            if (authState != null) {
                switch (authState.getStatus()) {
                    case SUCCESS:
                        if (authState.getData() != null) {
                            saveUserPreferences(authState.getData());
                            navigateToMainActivity();
                        }
                        break;
                    case ERROR:
                        Toast.makeText(this, authState.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                    case LOADING:
                        break;
                }
            }
        });
    }

    private void setupClickListeners() {
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            authViewModel.signInWithGoogle(account);
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserPreferences(com.example.hikenativeapp.data.local.entity.User user) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(Constants.PREF_USER_ID, user.getId());
        editor.putString(Constants.PREF_GOOGLE_ID, user.getGoogleId());
        editor.putString(Constants.PREF_USER_EMAIL, user.getEmail());
        editor.putString(Constants.PREF_USER_NAME, user.getDisplayName());
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);

        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.cleanup();
        }
    }
}
