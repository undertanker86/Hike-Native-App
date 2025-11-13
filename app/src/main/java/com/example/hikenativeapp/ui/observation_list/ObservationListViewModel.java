package com.example.hikenativeapp.ui.observation_list;

import android.util.Log;
import androidx.lifecycle.ViewModel;

import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.data.repository.ObservationRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ObservationListViewModel extends ViewModel {

    private static final String TAG = "ObservationListVM";

    private ObservationRepository repository;

    public void setRepository(ObservationRepository repository) {
        this.repository = repository;
    }

    public List<Observation> getObservationsByHikeId(int hikeId) {
        try {
            return repository.getObservationsByHikeId(hikeId).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteObservation(int observationId) {
        // Use softDeleteObservationAndSync to automatically sync to backend
        new Thread(() -> {
            try {
                repository.softDeleteObservationAndSync(observationId, new HikeRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Sync successful after deleting observation: " + message);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Sync failed after deleting observation: " + error);
                    }
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error deleting observation", e);
            }
        }).start();
    }

    public int getObservationCount(int hikeId) {
        try {
            return repository.getObservationCountByHikeId(hikeId).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
