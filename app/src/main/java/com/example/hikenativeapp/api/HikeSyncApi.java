package com.example.hikenativeapp.api;

import com.example.hikenativeapp.data.sync.HikeSyncRequest;
import com.example.hikenativeapp.data.sync.HikeSyncResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface HikeSyncApi {

    @POST("sync/hike")
    Call<HikeSyncResponse> syncHike(
            @Header("Authorization") String authToken,
            @Body HikeSyncRequest request
    );
}

