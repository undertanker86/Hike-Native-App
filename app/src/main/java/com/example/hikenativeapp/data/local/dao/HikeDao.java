package com.example.hikenativeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hikenativeapp.data.local.entity.Hike;

import java.util.List;

@Dao
public interface HikeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertHike(Hike hike);

    @Update
    void updateHike(Hike hike);

    // Hard delete (not recommended to use)
    @Delete
    void deleteHike(Hike hike);

    // Soft delete - mark as deleted
    @Query("UPDATE hikes SET is_deleted = 1, last_updated = :timestamp WHERE id = :hikeId")
    void softDeleteHike(int hikeId, String timestamp);

    // Restore deleted hike
    @Query("UPDATE hikes SET is_deleted = 0, last_updated = :timestamp WHERE id = :hikeId")
    void restoreHike(int hikeId, String timestamp);

    // Get hike by ID (only non-deleted)
    @Query("SELECT * FROM hikes WHERE id = :hikeId AND is_deleted = 0")
    Hike getHikeById(int hikeId);

    // Get hike by ID including deleted (for sync operations)
    @Query("SELECT * FROM hikes WHERE id = :hikeId")
    Hike getHikeByIdIncludingDeleted(int hikeId);

    // Get all hikes by user (only non-deleted)
    @Query("SELECT * FROM hikes WHERE user_id = :userId AND is_deleted = 0 ORDER BY hike_date DESC")
    List<Hike> getHikesByUserId(int userId);

    // ==================== SEARCH METHODS ====================

    // Search hikes by name only
    @Query("SELECT * FROM hikes WHERE user_id = :userId AND is_deleted = 0 AND name LIKE '%' || :name || '%' ORDER BY hike_date DESC")
    List<Hike> searchHikesByName(int userId, String name);

    // Search hikes by length range
    @Query("SELECT * FROM hikes WHERE user_id = :userId AND is_deleted = 0 AND length BETWEEN :minLength AND :maxLength ORDER BY hike_date DESC")
    List<Hike> searchHikesByLengthRange(int userId, double minLength, double maxLength);

    // Search by name AND length range
    @Query("SELECT * FROM hikes WHERE user_id = :userId AND is_deleted = 0 " +
           "AND name LIKE '%' || :name || '%' " +
           "AND length BETWEEN :minLength AND :maxLength " +
           "ORDER BY hike_date DESC")
    List<Hike> searchHikesByNameAndLength(int userId, String name, double minLength, double maxLength);
}
