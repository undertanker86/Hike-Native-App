package com.example.hikenativeapp.api;

import android.util.Log;

import com.example.hikenativeapp.data.weather.StormGlassResponse;
import com.example.hikenativeapp.data.weather.WeatherInfo;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.hikenativeapp.util.Constants;
public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.stormglass.io/v2/";
    private static final String PARAMS = "airTemperature,cloudCover,precipitation";
    private static final int MAX_FORECAST_DAYS = Constants.MAX_FORECAST_DAYS; // StormGlass limit

    private String apiKey;
    private StormGlassApi stormGlassApi;
    private static WeatherService instance;

    private WeatherService() {
        // Create logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Create OkHttp client
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        stormGlassApi = retrofit.create(StormGlassApi.class);
    }

    public static synchronized WeatherService getInstance() {
        if (instance == null) {
            instance = new WeatherService();
        }
        return instance;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Check if the selected date is within the allowed forecast range
     * @param selectedDate The selected date (format: YYYY-MM-DD)
     * @return true if the date is within the allowed forecast range, false otherwise
     */
    public boolean isDateWithinForecastRange(String selectedDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Date currentDate = new Date();
            Date hikeDate = dateFormat.parse(selectedDate);

            if (hikeDate == null) return false;

            // Calculate the number of days between the current date and the selected date
            long diffInMillis = hikeDate.getTime() - currentDate.getTime();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            return diffInDays <= MAX_FORECAST_DAYS;

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return false;
        }
    }
    /**
     * Get the weather forecast for a specific location and date
     * @param latitude Latitude
     * @param longitude Longitude
     * @param selectedDate Selected date (format: YYYY-MM-DD)
     * @param callback Callback to process the result
     */
    public void getWeatherForecast(double latitude, double longitude, String selectedDate,
                                    final WeatherCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API Key is not set");
            return;
        }

        // Check if the selected date is within the forecast range
        if (!isDateWithinForecastRange(selectedDate)) {
            callback.onError("Selected date is beyond the forecast range");
            return;
        }

        // Prepare start and end dates for the API
        String startDate = selectedDate + "T00:00:00Z";

        // The end date is the day after the selected date.
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                String endDate = dateFormat.format(calendar.getTime()) + "T00:00:00Z";

                Call<StormGlassResponse> call = stormGlassApi.getPointForecast(
                        apiKey, // Remove "Bearer " prefix - StormGlass expects just the API key
                        latitude,
                        longitude,
                        PARAMS,
                        startDate,
                        endDate
                );

                call.enqueue(new Callback<StormGlassResponse>() {
                    @Override
                    public void onResponse(Call<StormGlassResponse> call, Response<StormGlassResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            StormGlassResponse weatherResponse = response.body();

                            // Process weather data
                            if (weatherResponse.getHours() != null && !weatherResponse.getHours().isEmpty()) {
                                // Get the forecast at mid-day (12:00)
                                StormGlassResponse.HourlyForecast midDayForecast = null;

                                for (StormGlassResponse.HourlyForecast forecast : weatherResponse.getHours()) {
                                    if (forecast.getTime().contains("T12:00:00")) {
                                        midDayForecast = forecast;
                                        break;
                                    }
                                }

                                // If no mid-day forecast is found, use the first forecast
                                if (midDayForecast == null && !weatherResponse.getHours().isEmpty()) {
                                    midDayForecast = weatherResponse.getHours().get(0);
                                }

                                if (midDayForecast != null) {
                                    double temperature = midDayForecast.getTemperature();
                                    double cloudCover = midDayForecast.getCloudCoverPercentage();
                                    double precipitation = midDayForecast.getPrecipitationAmount();

                                    // Determine weather conditions based on data
                                    int weatherCondition = WeatherInfo.determineWeatherCondition(cloudCover, precipitation);

                                    // Create WeatherInfo object
                                    WeatherInfo weatherInfo = new WeatherInfo(temperature, weatherCondition, selectedDate);

                                    // Gọi callback thành công
                                    callback.onSuccess(weatherInfo);

                                    // Log data
                                    Log.d(TAG, "Weather data retrieved: Temp=" + temperature +
                                          "°C, Cloud=" + cloudCover + "%, Precip=" + precipitation +
                                          "mm, Condition=" + weatherInfo.getWeatherConditionString());
                                } else {
                                    callback.onError("No forecast data available for the selected date");
                                }
                            } else {
                                callback.onError("No forecast data available");
                            }
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "API Error: " + errorBody);
                                    callback.onError("API Error: " + errorBody);
                                } else {
                                    callback.onError("API Error: " + response.code());
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body: " + e.getMessage());
                                callback.onError("Error reading API response");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<StormGlassResponse> call, Throwable t) {
                        Log.e(TAG, "API call failed: " + t.getMessage());
                        callback.onError("Failed to fetch weather data: " + t.getMessage());
                    }
                });

            } else {
                callback.onError("Invalid date format");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            callback.onError("Error parsing date: " + e.getMessage());
        }
    }

    /**
     * Interface callback to handle results from API
     */
    public interface WeatherCallback {
        void onSuccess(WeatherInfo weatherInfo);
        void onError(String message);
    }
}
