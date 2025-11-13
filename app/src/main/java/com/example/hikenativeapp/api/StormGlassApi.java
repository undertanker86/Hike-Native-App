package com.example.hikenativeapp.api;

import com.example.hikenativeapp.data.weather.StormGlassResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface StormGlassApi {

    @GET("weather/point")
    Call<StormGlassResponse> getPointForecast(
            @Header("Authorization") String apiKey,
            @Query("lat") double latitude,
            @Query("lng") double longitude,
            @Query("params") String params,
            @Query("start") String startDate, // ISO format: YYYY-MM-DD
            @Query("end") String endDate      // ISO format: YYYY-MM-DD
    );
}
