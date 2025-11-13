package com.example.hikenativeapp.ui.add_observation;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;
import com.example.hikenativeapp.data.local.entity.Observation;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.data.repository.ObservationRepository;
import com.example.hikenativeapp.val.ObservationValidation;

import java.util.concurrent.ExecutionException;

public class AddObservationViewModel extends ViewModel {

    private static final String TAG = "AddObservationVM";

    private ObservationRepository repository;
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();

    public LiveData<Boolean> saveSuccess = _saveSuccess;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<Boolean> isLoading = _isLoading;

    public void setRepository(ObservationRepository repository) {
        this.repository = repository;
    }

    public void saveObservation(Observation observation) {
        _isLoading.postValue(true);

        new Thread(() -> {
            try {
                // Use insertObservationAndSync to automatically sync to backend
                long id = repository.insertObservationAndSync(observation, new HikeRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Sync successful: " + message);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Sync failed: " + error);
                    }
                }).get();

                _isLoading.postValue(false);

                if (id > 0) {
                    _saveSuccess.postValue(true);
                } else {
                    _errorMessage.postValue("Failed to add observation");
                }
            } catch (ExecutionException | InterruptedException e) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Error: " + e.getMessage());
            }
        }).start();
    }

    public boolean validateObservation(String observationText, String observationTime) {
        String error = ObservationValidation.validateObservation(observationText, observationTime);
        if (error != null) {
            _errorMessage.postValue(error);
            return false;
        }
        return true;
    }
}
