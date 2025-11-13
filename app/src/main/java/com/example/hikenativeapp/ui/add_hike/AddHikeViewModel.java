package com.example.hikenativeapp.ui.add_hike;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hikenativeapp.api.WeatherService;
import com.example.hikenativeapp.data.weather.WeatherInfo;

public class AddHikeViewModel extends ViewModel {

    private WeatherService weatherService;
    private MutableLiveData<WeatherInfo> weatherInfoLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoadingWeather = new MutableLiveData<>();

    // Form data
    private MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private MutableLiveData<Double> currentLatitude = new MutableLiveData<>();
    private MutableLiveData<Double> currentLongitude = new MutableLiveData<>();

    public AddHikeViewModel() {
        weatherService = WeatherService.getInstance();
        selectedDate.setValue("");
        currentLatitude.setValue(0.0);
        currentLongitude.setValue(0.0);
        isLoadingWeather.setValue(false);
    }

    public void setWeatherApiKey(String apiKey) {
        weatherService.setApiKey(apiKey);
    }

    public LiveData<WeatherInfo> getWeatherInfo() {
        return weatherInfoLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoadingWeather() {
        return isLoadingWeather;
    }

    public LiveData<String> getSelectedDate() {
        return selectedDate;
    }

    public LiveData<Double> getCurrentLatitude() {
        return currentLatitude;
    }

    public LiveData<Double> getCurrentLongitude() {
        return currentLongitude;
    }

    public void setSelectedDate(String date) {
        selectedDate.setValue(date);
    }

    public void setCurrentLocation(double latitude, double longitude) {
        currentLatitude.setValue(latitude);
        currentLongitude.setValue(longitude);
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

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up if needed
    }
}
