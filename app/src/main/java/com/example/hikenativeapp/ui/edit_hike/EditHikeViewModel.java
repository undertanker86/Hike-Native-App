package com.example.hikenativeapp.ui.edit_hike;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hikenativeapp.api.WeatherService;
import com.example.hikenativeapp.data.local.entity.Hike;
import com.example.hikenativeapp.data.repository.HikeRepository;
import com.example.hikenativeapp.data.weather.WeatherInfo;

import java.util.concurrent.ExecutionException;

public class EditHikeViewModel extends ViewModel {

    private static final String TAG = "EditHikeViewModel";

    private HikeRepository repository;
    private MutableLiveData<Hike> hikeLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoadingWeather = new MutableLiveData<>();
    private MutableLiveData<String> syncStatus = new MutableLiveData<>();
    private WeatherService weatherService;
    public EditHikeViewModel() {
        isLoading.setValue(false);
    }

    public void setRepository(HikeRepository repository) {
        this.repository = repository;
    }

    public LiveData<Hike> getHike() {
        return hikeLiveData;
    }

    public LiveData<Boolean> getUpdateSuccess() {
        return updateSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<Boolean> getIsLoadingWeather() {
        return isLoadingWeather;
    }

    private MutableLiveData<WeatherInfo> weatherInfoLiveData = new MutableLiveData<>();

    public LiveData<WeatherInfo> getWeatherInfo() {
        return weatherInfoLiveData;
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }

    public void loadHike(int hikeId) {
        isLoading.setValue(true);
        weatherService = WeatherService.getInstance();
        new Thread(() -> {
            try {
                Hike hike = repository.getHikeById(hikeId).get();
                hikeLiveData.postValue(hike);
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error loading hike: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }


    public void fetchWeatherForecast(double latitude, double longitude, String date) {
        isLoadingWeather.setValue(true);

        weatherService.getWeatherForecast(latitude, longitude, date, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherInfo weatherInfo) {
                weatherInfoLiveData.postValue(weatherInfo);
                isLoadingWeather.postValue(false);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("Error fetching weather: " + message);
                isLoadingWeather.postValue(false);
            }
        });
    }

    public void updateHike(Hike hike) {
        isLoading.setValue(true);

        new Thread(() -> {
            try {
                // Update hike with automatic sync to Vector DB
                repository.updateHikeAndSync(hike, new HikeRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Sync successful: " + message);
                        syncStatus.postValue("Sync successful: " + message);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Sync failed: " + error);
                        syncStatus.postValue("Update local successful, but sync failed: " + error);
                    }
                });

                updateSuccess.postValue(true);
            } catch (Exception e) {
                errorMessage.postValue("Error updating hike: " + e.getMessage());
                updateSuccess.postValue(false);
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public boolean checkChangeCoordinates(Hike oldHike, Hike newHike) {
        return oldHike.getLatitude() != newHike.getLatitude() || oldHike.getLongitude() != newHike.getLongitude();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up if needed
    }
}
