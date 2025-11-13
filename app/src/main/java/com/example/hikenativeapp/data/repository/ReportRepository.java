package com.example.hikenativeapp.data.repository;

import android.app.Application;
import android.util.Log;

import com.example.hikenativeapp.data.local.AppDatabase;
import com.example.hikenativeapp.data.local.dao.HikeDao;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.report.ReportStatistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportRepository {
    private static final String TAG = "ReportRepository";
    private final HikeDao hikeDao;
    private final ExecutorService executorService;

    public interface ReportCallback {
        void onSuccess(ReportStatistics statistics);
        void onError(String error);
    }

    public ReportRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        this.hikeDao = database.hikeDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Calculate statistics for users
     */
    public void getReportStatistics(int userId, ReportCallback callback) {
        executorService.execute(() -> {
            try {
                List<Hike> hikes = hikeDao.getHikesByUserId(userId);

                ReportStatistics stats = new ReportStatistics();

                if (hikes.isEmpty()) {
                    callback.onSuccess(stats);
                    return;
                }

                // Calculate metrics
                calculateBasicStats(hikes, stats);
                calculateDifficultyBreakdown(hikes, stats);
                calculateParkingStats(hikes, stats);
                calculateTopLocation(hikes, stats);

                Log.d(TAG, "Calculated statistics for " + hikes.size() + " hikes");
                callback.onSuccess(stats);

            } catch (Exception e) {
                Log.e(TAG, "Error calculating statistics", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Calculate basic statistics
     */
    private void calculateBasicStats(List<Hike> hikes, ReportStatistics stats) {
        stats.setTotalHikes(hikes.size());

        double totalDistance = 0;
        double totalDuration = 0;
        double totalTemp = 0;
        int tempCount = 0;

        for (Hike hike : hikes) {
            totalDistance += hike.getLength();
            totalDuration += hike.getEstimatedDuration();

            if (hike.getTemperature() > 0) {
                totalTemp += hike.getTemperature();
                tempCount++;
            }
        }

        stats.setTotalDistance(totalDistance);
        stats.setTotalDuration(totalDuration);

        if (tempCount > 0) {
            stats.setAverageTemperature(totalTemp / tempCount);
        }
    }

    /**
     * Calculate difficulty breakdown
     */
    private void calculateDifficultyBreakdown(List<Hike> hikes, ReportStatistics stats) {
        int easy = 0, moderate = 0, hard = 0;

        for (Hike hike : hikes) {
            String difficulty = hike.getDifficulty();
            if (difficulty != null) {
                switch (difficulty.toLowerCase()) {
                    case "easy":
                        easy++;
                        break;
                    case "moderate":
                        moderate++;
                        break;
                    case "hard":
                    case "difficult":
                        hard++;
                        break;
                }
            }
        }

        stats.setEasyHikes(easy);
        stats.setModerateHikes(moderate);
        stats.setHardHikes(hard);
    }

    /**
     * Calculate parking statistics
     */
    private void calculateParkingStats(List<Hike> hikes, ReportStatistics stats) {
        int withParking = 0;
        int withoutParking = 0;

        for (Hike hike : hikes) {
            if (hike.isParkingAvailable()) {
                withParking++;
            } else {
                withoutParking++;
            }
        }

        stats.setHikesWithParking(withParking);
        stats.setHikesWithoutParking(withoutParking);
    }

    /**
     * Find the most visited locations
     */
    private void calculateTopLocation(List<Hike> hikes, ReportStatistics stats) {
        Map<String, Integer> locationCounts = new HashMap<>();

        for (Hike hike : hikes) {
            String location = hike.getLocation();
            if (location != null && !location.isEmpty()) {
                locationCounts.put(location, locationCounts.getOrDefault(location, 0) + 1);
            }
        }

        // Find the location with the highest count
        String topLocation = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : locationCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topLocation = entry.getKey();
            }
        }

        stats.setTopLocation(topLocation);
        stats.setTopLocationCount(maxCount);
    }
}
