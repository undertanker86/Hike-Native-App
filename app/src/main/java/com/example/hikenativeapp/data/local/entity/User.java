package com.example.hikenativeapp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "users",
    indices = {
        @Index(value = {"google_id"}, unique = true),
        @Index(value = {"email"}, unique = true)
    }
)
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "google_id")
    private String googleId;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "display_name")
    private String displayName;

    @ColumnInfo(name = "photo_url")
    private String photoUrl;

    // Constructor
    public User() {}

    public User(String googleId, String email, String displayName, String photoUrl) {
        this.googleId = googleId;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
