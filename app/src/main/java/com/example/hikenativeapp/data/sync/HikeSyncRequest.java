package com.example.hikenativeapp.data.sync;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HikeSyncRequest {
    @SerializedName("hike")
    private HikeSyncData hike;

    @SerializedName("observations")
    private List<ObservationSyncData> observations;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    public HikeSyncRequest(HikeSyncData hike, List<ObservationSyncData> observations, boolean isDeleted) {
        this.hike = hike;
        this.observations = observations;
        this.isDeleted = isDeleted;
    }

    // Getters and Setters
    public HikeSyncData getHike() {
        return hike;
    }

    public void setHike(HikeSyncData hike) {
        this.hike = hike;
    }

    public List<ObservationSyncData> getObservations() {
        return observations;
    }

    public void setObservations(List<ObservationSyncData> observations) {
        this.observations = observations;
    }

}

