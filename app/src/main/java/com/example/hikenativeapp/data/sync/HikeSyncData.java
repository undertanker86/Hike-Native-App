package com.example.hikenativeapp.data.sync;

import com.google.gson.annotations.SerializedName;

public class HikeSyncData {
    @SerializedName("id_local")
    private int idLocal;

    @SerializedName("name")
    private String name;

    @SerializedName("location")
    private String location;

    @SerializedName("hike_date")
    private String hikeDate;

    @SerializedName("parking_available")
    private boolean parkingAvailable;

    @SerializedName("length")
    private double length;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("description")
    private String description;

    @SerializedName("weather_condition")
    private String weatherCondition;

    @SerializedName("temperature")
    private Double temperature;

    @SerializedName("estimated_duration")
    private Double estimatedDuration;

    public HikeSyncData(int idLocal, String name, String location, String hikeDate,
                        boolean parkingAvailable, double length, String difficulty,
                        String description, String weatherCondition, Double temperature,
                        Double estimatedDuration) {
        this.idLocal = idLocal;
        this.name = name;
        this.location = location;
        this.hikeDate = hikeDate;
        this.parkingAvailable = parkingAvailable;
        this.length = length;
        this.difficulty = difficulty;
        this.description = description;
        this.weatherCondition = weatherCondition;
        this.temperature = temperature;
        this.estimatedDuration = estimatedDuration;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }


}

