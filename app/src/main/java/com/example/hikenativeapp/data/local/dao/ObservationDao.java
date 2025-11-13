package com.example.hikenativeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hikenativeapp.data.local.entity.Observation;

import java.util.List;

@Dao
public interface ObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertObservation(Observation observation);

    @Update
    void updateObservation(Observation observation);

    // Hard delete (not recommended to use)
    @Delete
    void deleteObservation(Observation observation);

    // Soft delete - mark as deleted
    @Query("UPDATE observations SET is_deleted = 1, last_updated = :timestamp WHERE id = :observationId")
    void softDeleteObservation(int observationId, String timestamp);

    // Restore deleted observation
    @Query("UPDATE observations SET is_deleted = 0, last_updated = :timestamp WHERE id = :observationId")
    void restoreObservation(int observationId, String timestamp);

    // Get observation by ID (only non-deleted)
    @Query("SELECT * FROM observations WHERE id = :observationId AND is_deleted = 0")
    Observation getObservationById(int observationId);

    // Get all observations by hike ID (only non-deleted)
    @Query("SELECT * FROM observations WHERE hike_id = :hikeId AND is_deleted = 0 ORDER BY observation_time ASC")
    List<Observation> getObservationsByHikeId(int hikeId);

    // Get all observations by hike ID including deleted (for sync operations)
    @Query("SELECT * FROM observations WHERE hike_id = :hikeId ORDER BY observation_time ASC")
    List<Observation> getObservationsByHikeIdIncludingDeleted(int hikeId);

    // Get all non-deleted observations
    @Query("SELECT * FROM observations WHERE is_deleted = 0 ORDER BY observation_time DESC")
    List<Observation> getAllObservations();

    @Query("SELECT * FROM observations WHERE (observation_text LIKE '%' || :searchQuery || '%' OR comments LIKE '%' || :searchQuery || '%') AND is_deleted = 0")
    List<Observation> searchObservations(String searchQuery);




    // Soft delete all observations by hike ID
    @Query("UPDATE observations SET is_deleted = 1, last_updated = :timestamp WHERE hike_id = :hikeId")
    void softDeleteObservationsByHikeId(int hikeId, String timestamp);

    // Get count of non-deleted observations for a hike
    @Query("SELECT COUNT(*) FROM observations WHERE hike_id = :hikeId AND is_deleted = 0")
    int getObservationCountByHikeId(int hikeId);
}
