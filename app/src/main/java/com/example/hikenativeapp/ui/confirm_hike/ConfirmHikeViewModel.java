package com.example.hikenativeapp.ui.confirm_hike;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.repository.HikeRepository;

import java.util.concurrent.ExecutionException;

public class ConfirmHikeViewModel extends ViewModel {

    private static final String TAG = "ConfirmHikeViewModel";

    private HikeRepository repository;
    private MutableLiveData<Long> hikeIdLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isSaving = new MutableLiveData<>();
    private MutableLiveData<String> syncStatus = new MutableLiveData<>();

    public void setRepository(HikeRepository repository) {
        this.repository = repository;
    }

    public LiveData<Long> getHikeId() {
        return hikeIdLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }

    public void saveHike(Hike hike) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        isSaving.setValue(true);
        new Thread(() -> {
            try {
                // Save hike with automatic sync
                long hikeId = repository.insertHikeAndSync(hike, new HikeRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Sync successful: " + message);
                        syncStatus.postValue("Synced to the cloud");
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Sync failed: " + error);
                        syncStatus.postValue("Local save successful (will sync later)");
                    }
                }).get();

                hikeIdLiveData.postValue(hikeId);
                isSaving.postValue(false);
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error saving hike: " + e.getMessage());
                isSaving.postValue(false);
            }
        }).start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (repository != null) {
            repository.cleanup();
        }
    }
}
