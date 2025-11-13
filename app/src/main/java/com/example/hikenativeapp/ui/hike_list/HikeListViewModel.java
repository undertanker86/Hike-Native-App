package com.example.hikenativeapp.ui.hike_list;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.repository.HikeRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class HikeListViewModel extends ViewModel {

    private static final String TAG = "HikeListViewModel";

    private HikeRepository repository;
    private MutableLiveData<List<Hike>> hikesLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> isEmpty = new MutableLiveData<>();
    private MutableLiveData<String> syncStatus = new MutableLiveData<>();

    public void setRepository(HikeRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Hike>> getHikes() {
        return hikesLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsEmpty() {
        return isEmpty;
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }

    public void loadHikesByUserId(int userId) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        isLoading.setValue(true);
        new Thread(() -> {
            try {
                List<Hike> hikes = repository.getHikesByUserId(userId).get();
                hikesLiveData.postValue(hikes);
                isEmpty.postValue(hikes == null || hikes.isEmpty());
                isLoading.postValue(false);
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error loading hikes: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void deleteHike(Hike hike) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        new Thread(() -> {
            try {
                repository.deleteHike(hike);
                // Reload the current user's hikes after deletion
            } catch (Exception e) {
                errorMessage.postValue("Error deleting hike: " + e.getMessage());
            }
        }).start();
    }

    public void softDeleteHike(int hikeId) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        new Thread(() -> {
            try {
                // Soft delete with automatic sync to Vector DB
                repository.softDeleteHikeAndSync(hikeId, new HikeRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Delete sync successful: " + message);
                        syncStatus.postValue("Đã đồng bộ xóa lên cloud");
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Delete sync failed: " + error);
                        syncStatus.postValue("Xóa local thành công (sẽ sync sau)");
                    }
                });
            } catch (Exception e) {
                errorMessage.postValue("Error soft deleting hike: " + e.getMessage());
            }
        }).start();
    }

    // ==================== SEARCH METHODS ====================

    /**
     * Simple search by name
     */
    public void searchHikesByName(int userId, String name) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        if (name == null || name.trim().isEmpty()) {
            loadHikesByUserId(userId);
            return;
        }

        isLoading.setValue(true);
        new Thread(() -> {
            try {
                List<Hike> hikes = repository.searchHikesByName(userId, name.trim()).get();
                hikesLiveData.postValue(hikes);
                isEmpty.postValue(hikes == null || hikes.isEmpty());
                isLoading.postValue(false);
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error searching hikes: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * Search by length range
     */
    public void searchHikesByLengthRange(int userId, double minLength, double maxLength) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        isLoading.setValue(true);
        new Thread(() -> {
            try {
                List<Hike> hikes = repository.searchHikesByLengthRange(userId, minLength, maxLength).get();
                hikesLiveData.postValue(hikes);
                isEmpty.postValue(hikes == null || hikes.isEmpty());
                isLoading.postValue(false);
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error searching hikes: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * Search by name and length range
     */
    public void searchHikesByNameAndLength(int userId, String name, double minLength, double maxLength) {
        if (repository == null) {
            errorMessage.setValue("Repository not initialized");
            return;
        }

        isLoading.setValue(true);
        new Thread(() -> {
            try {
                List<Hike> hikes = repository.searchHikesByNameAndLength(
                    userId, name != null ? name.trim() : "", minLength, maxLength).get();
                hikesLiveData.postValue(hikes);
                isEmpty.postValue(hikes == null || hikes.isEmpty());
                isLoading.postValue(false);
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error searching hikes: " + e.getMessage());
                isLoading.postValue(false);
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
