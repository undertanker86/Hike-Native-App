package com.example.hikenativeapp.data.sync;

import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.local.entity.Observation;

import java.util.ArrayList;
import java.util.List;

public class SyncDataMapper {
    /**
     * Convert Hike entity to HikeSyncData for API
     * Note: Hike entity stores date as String, so no conversion needed
     */
    public static HikeSyncData toSyncData(Hike hike) {
        return new HikeSyncData(
                hike.getId(),
                hike.getName(),
                hike.getLocation(),
                hike.getHikeDate() != null ? hike.getHikeDate() : "",
                hike.isParkingAvailable(),
                hike.getLength(),
                hike.getDifficulty(),
                hike.getDescription(),
                hike.getWeatherCondition(),
                hike.getTemperature(),
                hike.getEstimatedDuration()
        );
    }

    /**
     * Convert Observation entity to ObservationSyncData for API
     * Note: Observation entity stores time as String, so no conversion needed
     */
    public static ObservationSyncData toSyncData(Observation observation) {
        return new ObservationSyncData(
                observation.getId(),
                observation.getObservationText(),
                observation.getObservationTime() != null ? observation.getObservationTime() : "",
                observation.getComments(),
                observation.getPhotoPath()
        );
    }

    /**
     * Convert list of Observations to list of ObservationSyncData
     */
    public static List<ObservationSyncData> toSyncDataList(List<Observation> observations) {
        List<ObservationSyncData> syncDataList = new ArrayList<>();
        if (observations != null) {
            for (Observation observation : observations) {
                syncDataList.add(toSyncData(observation));
            }
        }
        return syncDataList;
    }

    /**
     * Create complete sync request from Hike and Observations
     */
    public static HikeSyncRequest createSyncRequest(Hike hike, List<Observation> observations, boolean isDeleted) {
        HikeSyncData hikeSyncData = toSyncData(hike);
        List<ObservationSyncData> observationSyncData = toSyncDataList(observations);

        return new HikeSyncRequest(hikeSyncData, observationSyncData, isDeleted);
    }
}
