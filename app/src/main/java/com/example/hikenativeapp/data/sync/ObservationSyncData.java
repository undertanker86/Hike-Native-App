package com.example.hikenativeapp.data.sync;

import com.google.gson.annotations.SerializedName;

public class ObservationSyncData {
    @SerializedName("id_local")
    private int idLocal;

    @SerializedName("observation_text")
    private String observationText;

    @SerializedName("observation_time")
    private String observationTime;

    @SerializedName("comments")
    private String comments;

    @SerializedName("image_path")
    private String imagePath;

    public ObservationSyncData(int idLocal, String observationText, String observationTime,
                               String comments, String imagePath) {
        this.idLocal = idLocal;
        this.observationText = observationText;
        this.observationTime = observationTime;
        this.comments = comments;
        this.imagePath = imagePath;
    }

}

