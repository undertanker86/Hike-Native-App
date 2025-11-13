package com.example.hikenativeapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.example.hikenativeapp.data.local.AppDatabase;
import com.example.hikenativeapp.data.local.dao.ObservationDao;
import com.example.hikenativeapp.data.local.entity.Observation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ObservationRepository {

    private static final String TAG = "ObservationRepository";

    private ObservationDao observationDao;
    private ExecutorService executorService;
    private HikeRepository hikeRepository;

    public ObservationRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        observationDao = database.observationDao();
        executorService = Executors.newFixedThreadPool(4);
        hikeRepository = new HikeRepository(context);
    }

    // Helper method to get current timestamp
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Observation operations


    public Future<List<Observation>> getObservationsByHikeId(int hikeId) {
        return executorService.submit(() -> observationDao.getObservationsByHikeId(hikeId));
    }

    public Future<Observation> getObservationById(int observationId) {
        return executorService.submit(() -> observationDao.getObservationById(observationId));
    }



    public Future<Integer> getObservationCountByHikeId(int hikeId) {
        return executorService.submit(() -> observationDao.getObservationCountByHikeId(hikeId));
    }


    /**
     * Insert observation and automatically sync the parent hike to Vector DB
     */
    public Future<Long> insertObservationAndSync(Observation observation, HikeRepository.SyncCallback syncCallback) {
        return executorService.submit(() -> {
            long observationId = observationDao.insertObservation(observation);

            Log.d(TAG, " Observation inserted with ID: " + observationId);
            Log.d(TAG, "Syncing parent hike to update observations...");

            // Sync the parent hike (including all its observations) to backend
            hikeRepository.syncHikeToVectorDB(observation.getHikeId(), false, syncCallback);

            return observationId;
        });
    }

    /**
     * Update observation and automatically sync the parent hike to Vector DB
     */
    public Future<Void> updateObservationAndSync(Observation observation, HikeRepository.SyncCallback syncCallback) {
        return executorService.submit(() -> {
            observation.updateLastUpdated();
            observationDao.updateObservation(observation);

            Log.d(TAG, "Observation updated: " + observation.getId());
            Log.d(TAG, "Syncing parent hike to update observations...");

            // Sync the parent hike (including all its observations) to backend
            hikeRepository.syncHikeToVectorDB(observation.getHikeId(), false, syncCallback);

            return null;
        });
    }

    /**
     * Soft delete observation and automatically sync the parent hike to Vector DB
     */
    public Future<Void> softDeleteObservationAndSync(int observationId, HikeRepository.SyncCallback syncCallback) {
        return executorService.submit(() -> {
            // First, get the observation to know which hike it belongs to
            Observation observation = observationDao.getObservationById(observationId);
            if (observation == null) {
                Log.e(TAG, "Observation not found: " + observationId);
                if (syncCallback != null) {
                    syncCallback.onError("Observation not found");
                }
                return null;
            }

            int hikeId = observation.getHikeId();

            // Soft delete the observation
            observationDao.softDeleteObservation(observationId, getCurrentTimestamp());

            Log.d(TAG, "Observation soft deleted: " + observationId);
            Log.d(TAG, "Syncing parent hike to update observations...");

            // Sync the parent hike (the sync will include the deleted observation with is_deleted flag)
            hikeRepository.syncHikeToVectorDB(hikeId, false, syncCallback);

            return null;
        });
    }



    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
