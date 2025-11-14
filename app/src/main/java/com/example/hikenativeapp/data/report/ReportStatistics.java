package com.example.hikenativeapp.data.report;

public class ReportStatistics {
    private int totalHikes;
    private double totalDistance;
    private double totalDuration;
    private double averageTemperature;
    private int hikesWithParking;
    private int hikesWithoutParking;

    // Difficulty breakdown
    private int easyHikes;
    private int moderateHikes;
    private int hardHikes;

    // Top locations
    private String topLocation;
    private int topLocationCount;

    public ReportStatistics() {
    }

    // Getters and Setters
    public int getTotalHikes() {
        return totalHikes;
    }

    public void setTotalHikes(int totalHikes) {
        this.totalHikes = totalHikes;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(double totalDuration) {
        this.totalDuration = totalDuration;
    }

    public double getAverageTemperature() {
        return averageTemperature;
    }

    public void setAverageTemperature(double averageTemperature) {
        this.averageTemperature = averageTemperature;
    }

    public int getHikesWithParking() {
        return hikesWithParking;
    }

    public void setHikesWithParking(int hikesWithParking) {
        this.hikesWithParking = hikesWithParking;
    }

    public int getHikesWithoutParking() {
        return hikesWithoutParking;
    }

    public void setHikesWithoutParking(int hikesWithoutParking) {
        this.hikesWithoutParking = hikesWithoutParking;
    }

    public int getEasyHikes() {
        return easyHikes;
    }

    public void setEasyHikes(int easyHikes) {
        this.easyHikes = easyHikes;
    }

    public int getModerateHikes() {
        return moderateHikes;
    }

    public void setModerateHikes(int moderateHikes) {
        this.moderateHikes = moderateHikes;
    }

    public int getHardHikes() {
        return hardHikes;
    }

    public void setHardHikes(int hardHikes) {
        this.hardHikes = hardHikes;
    }

    public String getTopLocation() {
        return topLocation;
    }

    public void setTopLocation(String topLocation) {
        this.topLocation = topLocation;
    }

    public int getTopLocationCount() {
        return topLocationCount;
    }

    public void setTopLocationCount(int topLocationCount) {
        this.topLocationCount = topLocationCount;
    }
}

