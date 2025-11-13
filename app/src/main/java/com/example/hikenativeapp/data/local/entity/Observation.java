package com.example.hikenativeapp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(
    tableName = "observations",
    foreignKeys = @ForeignKey(
        entity = Hike.class,
        parentColumns = "id",
        childColumns = "hike_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("hike_id")}
)
public class Observation {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "hike_id")
    private int hikeId;

    @ColumnInfo(name = "observation_text")
    private String observationText;

    @ColumnInfo(name = "observation_time")
    private String observationTime;

    @ColumnInfo(name = "comments")
    private String comments;

    @ColumnInfo(name = "photo_path")
    private String photoPath;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "last_updated")
    private String lastUpdated;

    @ColumnInfo(name = "is_deleted")
    private boolean isDeleted;

    // Constructor
    public Observation() {
        this.createdAt = getCurrentTimestamp();
        this.lastUpdated = getCurrentTimestamp();
        this.isDeleted = false;
    }

    @Ignore
    public Observation(int hikeId, String observationText, String observationTime, String comments) {
        this();
        this.hikeId = hikeId;
        this.observationText = observationText;
        this.observationTime = observationTime;
        this.comments = comments;
    }

    // Helper method to get current timestamp
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Update last_updated timestamp
    public void updateLastUpdated() {
        this.lastUpdated = getCurrentTimestamp();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHikeId() {
        return hikeId;
    }

    public void setHikeId(int hikeId) {
        this.hikeId = hikeId;
    }

    public String getObservationText() {
        return observationText;
    }

    public void setObservationText(String observationText) {
        this.observationText = observationText;
    }

    public String getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(String observationTime) {
        this.observationTime = observationTime;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}
