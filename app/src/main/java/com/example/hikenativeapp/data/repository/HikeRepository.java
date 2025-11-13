package com.example.hikenativeapp.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.hikenativeapp.api.HikeSyncApi;
import com.example.hikenativeapp.api.SyncService;
import com.example.hikenativeapp.data.local.AppDatabase;
import com.example.hikenativeapp.data.local.dao.HikeDao;
import com.example.hikenativeapp.data.local.dao.ObservationDao;
import com.example.hikenativeapp.data.local.dao.UserDao;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.local.entity.User;
import com.example.hikenativeapp.data.sync.HikeSyncRequest;
import com.example.hikenativeapp.data.sync.HikeSyncResponse;
import com.example.hikenativeapp.data.sync.SyncDataMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HikeRepository {

    private static final String TAG = "HikeRepository";

    private UserDao userDao;
    private HikeDao hikeDao;
    private ObservationDao observationDao;
    private ExecutorService executorService;
    private HikeSyncApi syncApi;

    public HikeRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        userDao = database.userDao();
        hikeDao = database.hikeDao();
        observationDao = database.observationDao();
        executorService = Executors.newFixedThreadPool(4);
        syncApi = SyncService.getApi();
    }

    // Helper method to get current timestamp
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Hike operations

    public Future<List<Hike>> getHikesByUserId(int userId) {
        return executorService.submit(() -> hikeDao.getHikesByUserId(userId));
    }

    public Future<Hike> getHikeById(int hikeId) {
        return executorService.submit(() -> hikeDao.getHikeById(hikeId));
    }


    public void restoreHike(int hikeId) {
        executorService.execute(() -> hikeDao.restoreHike(hikeId, getCurrentTimestamp()));
    }

    // Hard delete methods (use with caution)
    public void deleteHike(Hike hike) {
        executorService.execute(() -> hikeDao.deleteHike(hike));
    }



    // ==================== SYNC OPERATIONS ====================

    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Sync a hike and its observations to the backend Vector DB
     * IMPORTANT: Uses Firebase UID (google_id) for user identification
     */
    public void syncHikeToVectorDB(int hikeId, boolean isDeleted, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                // Get Firebase auth token
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    callback.onError("User not authenticated");
                    return;
                }

                // This is the Firebase UID (google_id)
                String firebaseUid = currentUser.getUid();
                Log.d(TAG, "=== SYNC HIKE REQUEST ===");
                Log.d(TAG, "Firebase UID (google_id): " + firebaseUid);
                Log.d(TAG, "Email: " + currentUser.getEmail());
                Log.d(TAG, "Hike ID: " + hikeId);
                Log.d(TAG, "Is Deleted: " + isDeleted);
                Log.d(TAG, "========================");

                // Get auth token
                currentUser.getIdToken(true).addOnSuccessListener(result -> {
                    String authToken = "Bearer " + result.getToken();

                    // Get hike and observations from database
                    executorService.execute(() -> {
                        try {
                            // IMPORTANT: Use includeDeleted methods when syncing deleted items
                            Hike hike = isDeleted
                                ? hikeDao.getHikeByIdIncludingDeleted(hikeId)
                                : hikeDao.getHikeById(hikeId);

                            if (hike == null) {
                                callback.onError("Hike not found");
                                return;
                            }

                            // Get observations including deleted ones if the hike is being deleted
                            List<Observation> observations = isDeleted
                                ? observationDao.getObservationsByHikeIdIncludingDeleted(hikeId)
                                : observationDao.getObservationsByHikeId(hikeId);

                            // Create sync request
                            HikeSyncRequest syncRequest = SyncDataMapper.createSyncRequest(
                                hike,
                                observations,
                                isDeleted
                            );

                            Log.d(TAG, "Sending sync request to backend...");
                            Log.d(TAG, "Hike name: " + hike.getName());
                            Log.d(TAG, "Location: " + hike.getLocation());
                            Log.d(TAG, "Observations count: " + observations.size());

                            // Send to backend
                            syncApi.syncHike(authToken, syncRequest).enqueue(new Callback<HikeSyncResponse>() {
                                @Override
                                public void onResponse(Call<HikeSyncResponse> call, Response<HikeSyncResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        HikeSyncResponse syncResponse = response.body();
                                        if (syncResponse.isSuccess()) {
                                            Log.d(TAG, "Sync successful: " + syncResponse.getMessage());
                                            callback.onSuccess(syncResponse.getMessage());
                                        } else {
                                            Log.e(TAG, "Sync failed: " + syncResponse.getMessage());
                                            callback.onError(syncResponse.getMessage());
                                        }
                                    } else {
                                        callback.onError("Server error: " + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(Call<HikeSyncResponse> call, Throwable t) {
                                    Log.e(TAG, "Sync request failed", t);
                                    callback.onError("Network error: " + t.getMessage());
                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Error preparing sync data", e);
                            callback.onError("Error: " + e.getMessage());
                        }
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get auth token", e);
                    callback.onError("Auth error: " + e.getMessage());
                });

            } catch (Exception e) {
                Log.e(TAG, "Sync error", e);
                callback.onError("Sync error: " + e.getMessage());
            }
        });
    }

    /**
     * Insert hike and automatically sync to Vector DB
     */
    public Future<Long> insertHikeAndSync(Hike hike, SyncCallback syncCallback) {
        return executorService.submit(() -> {
            long hikeId = hikeDao.insertHike(hike);

            // Trigger sync after insert
            syncHikeToVectorDB((int) hikeId, false, syncCallback);

            return hikeId;
        });
    }

    /**
     * Update hike and automatically sync to Vector DB
     */
    public void updateHikeAndSync(Hike hike, SyncCallback syncCallback) {
        executorService.execute(() -> {
            hike.updateLastUpdated();
            hikeDao.updateHike(hike);

            // Trigger sync after update
            syncHikeToVectorDB(hike.getId(), false, syncCallback);
        });
    }

    /**
     * Soft delete hike and sync deletion to Vector DB
     */
    public void softDeleteHikeAndSync(int hikeId, SyncCallback syncCallback) {
        executorService.execute(() -> {
            String timestamp = getCurrentTimestamp();

            // Soft delete the hike
            hikeDao.softDeleteHike(hikeId, timestamp);

            // Soft delete all observations related to this hike
            observationDao.softDeleteObservationsByHikeId(hikeId, timestamp);

            Log.d(TAG, "Soft deleted hike and its observations: hikeId=" + hikeId);

            // Trigger sync with deletion flag
            syncHikeToVectorDB(hikeId, true, syncCallback);
        });
    }

    // ==================== SEARCH OPERATIONS ====================

    /**
     * Simple search by name
     */
    public Future<List<Hike>> searchHikesByName(int userId, String name) {
        return executorService.submit(() -> hikeDao.searchHikesByName(userId, name));
    }

    /**
     * Search by length range
     */
    public Future<List<Hike>> searchHikesByLengthRange(int userId, double minLength, double maxLength) {
        return executorService.submit(() -> hikeDao.searchHikesByLengthRange(userId, minLength, maxLength));
    }

    /**
     * Search by name and length range combined
     */
    public Future<List<Hike>> searchHikesByNameAndLength(int userId, String name, double minLength, double maxLength) {
        return executorService.submit(() -> hikeDao.searchHikesByNameAndLength(userId, name, minLength, maxLength));
    }

    // Cleanup method
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
