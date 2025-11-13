package com.example.hikenativeapp.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hikenativeapp.data.local.AppDatabase;
import com.example.hikenativeapp.data.local.dao.UserDao;
import com.example.hikenativeapp.data.local.entity.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class UserRepository {
    private final UserDao userDao;
    private final FirebaseAuth firebaseAuth;

    public UserRepository(AppDatabase database) {
        this.userDao = database.userDao();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }


    public User getUserByGoogleId(String googleId) {
        return userDao.getUserByGoogleId(googleId);
    }

    public long insertUser(User user) {
        return userDao.insertUser(user);
    }

    public void updateUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.updateUser(user);
        });
    }

    public void authenticateWithGoogle(GoogleSignInAccount account, OnAuthenticationListener listener) {
        if (account == null) {
            listener.onFailure("Google sign-in failed");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Successfully authenticated with Firebase
                        String googleId = account.getId();
                        String email = account.getEmail();
                        String displayName = account.getDisplayName();

                        // Check if user already exists in local database
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            User existingUser = userDao.getUserByGoogleId(googleId);
                            if (existingUser != null) {
                                // User exists, update information if needed
                                existingUser.setEmail(email);
                                existingUser.setDisplayName(displayName);
                                userDao.updateUser(existingUser);
                                listener.onSuccess(existingUser);
                            } else {
                                // Create new user
                                User newUser = new User(googleId, email, displayName, null);
                                long id = userDao.insertUser(newUser);
                                newUser.setId((int) id);
                                listener.onSuccess(newUser);
                            }
                        });
                    } else {
                        // Authentication failed
                        listener.onFailure("Firebase authentication failed");
                    }
                });
    }

    public void cleanup() {
        // Any cleanup operations if needed
    }

    public interface OnAuthenticationListener {
        void onSuccess(User user);
        void onFailure(String message);
    }
}
