package com.example.hikenativeapp.data.sync;

import com.google.gson.annotations.SerializedName;

public class HikeSyncResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("hike_id")
    private String hikeId;

    @SerializedName("documents_updated")
    private int documentsUpdated;

    public HikeSyncResponse() {
    }

    public HikeSyncResponse(boolean success, String message, String hikeId, int documentsUpdated) {
        this.success = success;
        this.message = message;
        this.hikeId = hikeId;
        this.documentsUpdated = documentsUpdated;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHikeId() {
        return hikeId;
    }

    public void setHikeId(String hikeId) {
        this.hikeId = hikeId;
    }

}

