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
    public void cleanup() {
        // Any cleanup operations if needed
    }

    public interface OnAuthenticationListener {
        void onSuccess(User user);
        void onFailure(String message);
    }
}
