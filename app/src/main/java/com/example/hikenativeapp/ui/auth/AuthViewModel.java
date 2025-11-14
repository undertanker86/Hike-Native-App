package com.example.hikenativeapp.ui.auth;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hikenativeapp.data.local.entity.User;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.data.repository.UserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.ExecutionException;

public class AuthViewModel extends ViewModel {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private HikeRepository repository;
    private UserRepository Userepository;
    private MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<AuthState<User>> authState = new MutableLiveData<>();
    private static final String TAG = "AuthViewModel";
    public AuthViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        isLoggedIn.setValue(firebaseAuth.getCurrentUser() != null);
    }

    public void setRepository(HikeRepository repository) {
        this.repository = repository;
    }

    public void setRepository(UserRepository Userrepository) {
        this.Userepository = Userrepository;
    }


    public LiveData<AuthState<User>> getAuthState() {
        return authState;
    }

    // Sign in with Google
    public void signInWithGoogle(GoogleSignInAccount account) {

        // Set loading state for new auth state pattern
        authState.setValue(new AuthState<>(Status.LOADING, null, null));
        isLoading.setValue(true);

        // Add null check to prevent NullPointerException
        if (Userepository == null) {
            authState.setValue(new AuthState<>(Status.ERROR, null, "Repository not initialized"));
            isLoading.setValue(false);
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveOrUpdateUser(firebaseUser);
                            isLoggedIn.setValue(true);
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";
                        errorMessage.setValue(error);
                        authState.setValue(new AuthState<>(Status.ERROR, null, error));
                        isLoggedIn.setValue(false);
                    }
                });
    }

    // Save or update user in local database
    private void saveOrUpdateUser(FirebaseUser firebaseUser) {
        if (Userepository == null) return;

        User user = new User();
        user.setGoogleId(firebaseUser.getUid());
        user.setEmail(firebaseUser.getEmail());
        user.setDisplayName(firebaseUser.getDisplayName());
        user.setPhotoUrl(firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);

        // Check if user exists in local database using ExecutorService
        new Thread(() -> {
            try {
                User existingUser = Userepository.getUserByGoogleId(firebaseUser.getUid());
                if (existingUser != null) {
                    // Update existing user
                    user.setId(existingUser.getId());
                    Userepository.updateUser(user);
                    // Post the existing user with correct ID
                    currentUser.postValue(user);
                    authState.postValue(new AuthState<>(Status.SUCCESS, user, null));
                } else {
                    // Insert new user and get the generated ID
                    long newUserId = Userepository.insertUser(user);
                    user.setId((int) newUserId);

                    // Retrieve the user from database to ensure we have the correct ID
                    User savedUser = Userepository.getUserByGoogleId(firebaseUser.getUid());
                    if (savedUser != null) {
                        currentUser.postValue(savedUser);
                        authState.postValue(new AuthState<>(Status.SUCCESS, savedUser, null));
                    } else {
                        // Fallback to user object with set ID
                        currentUser.postValue(user);
                        authState.postValue(new AuthState<>(Status.SUCCESS, user, null));
                    }
                }
            } catch (Exception e) {
                String errorMsg = "Failed to save user: " + e.getMessage();
                errorMessage.postValue(errorMsg);
                authState.postValue(new AuthState<>(Status.ERROR, null, errorMsg));
            }
        }).start();
    }

    // Sign out
    public void signOut() {
        firebaseAuth.signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
        isLoggedIn.setValue(false);
        currentUser.setValue(null);
    }

    // Check if user is signed in
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Load current user from database
    public void getCurrentUserFromDatabase() {
        if (Userepository == null || firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String googleId = firebaseAuth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                User user = Userepository.getUserByGoogleId(googleId);
                if (user != null) {
                    currentUser.postValue(user);
                }
            } catch (Exception e) {
                errorMessage.postValue("Failed to load user: " + e.getMessage());
            }
        }).start();
    }

    // LiveData getters
    public LiveData<Boolean> getIsLoggedIn() {
        return isLoggedIn;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    // AuthState class for structured authentication state
    public static class AuthState<T> {
        private final Status status;
        private final T data;
        private final String message;

        public AuthState(Status status, T data, String message) {
            this.status = status;
            this.data = data;
            this.message = message;
        }

        public Status getStatus() {
            return status;
        }

        public T getData() {
            return data;
        }

        public String getMessage() {
            return message;
        }
    }

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}
